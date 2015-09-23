package vee.services.netty;

import vee.services.comm.Callback;
import vee.services.comm.SafeCloseAdaptor;
import vee.services.protocol.RemoteServiceRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.Future;
import java.util.function.BiConsumer;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-02  <br/>
 */
public class ServiceServer extends SafeCloseAdaptor {

    private final int port;
    private final BiConsumer<RequestContext, RemoteServiceRequest> callback;
    private Callback<Exception> shutdownCallback;
    private Channel serverChannel;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final ServerBootstrap serverBootstrap = new ServerBootstrap();

    public ServiceServer( final int port, final BiConsumer<RequestContext, RemoteServiceRequest> callback ) {
        this.port = port;
        this.callback = callback;
    }

    public void start( final Callback<Future> startCallback ) throws InterruptedException {
        serverBootstrap.group( bossGroup, workerGroup )
                .channel( NioServerSocketChannel.class )
                .handler( new LoggingHandler( LogLevel.INFO ) )
                .childHandler( new ServiceServerInitializer( callback ) );
        ChannelFuture cf = serverBootstrap.bind( port ).sync();
        if ( null != startCallback ) {
            cf.addListener( startCallback::call );
        }
        serverChannel = cf.channel();
    }

    public void onShutdown( Callback<Exception> callback ) {
        this.shutdownCallback = callback;
    }

    @Override
    protected void doClose() {
        if ( null != shutdownCallback ) {
            shutdownCallback.call( null );
        }
        if ( null != serverChannel ) {
            ChannelFuture cf = serverChannel.close();
            cf.addListener( future -> shutdownWorkers() );
        } else {
            shutdownWorkers();
        }
    }

    private void shutdownWorkers() {
        try {
            bossGroup.shutdownGracefully().await();
        } catch ( InterruptedException ignored ) {
        }
        try {
            workerGroup.shutdownGracefully().await();
        } catch ( InterruptedException ignored ) {
        }
    }

}
