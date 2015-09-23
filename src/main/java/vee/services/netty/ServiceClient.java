package vee.services.netty;

import vee.services.comm.Callback;
import vee.services.comm.NamedThreadFactory;
import vee.services.comm.SafeCloseAdaptor;
import vee.services.protocol.RemoteServiceRequest;
import vee.services.protocol.RemoteServiceResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-02  <br/>
 */
public class ServiceClient extends SafeCloseAdaptor {

    private static final ExecutorService exec = Executors.newCachedThreadPool( new NamedThreadFactory( "service-client-write-channel-sync-pool" ) );

    private final AtomicLong requestCounter = new AtomicLong( 0 );
    private final AtomicBoolean running = new AtomicBoolean( false );
    private final ConcurrentHashMap<Long, Callback<RemoteServiceResponse>> callbacks = new ConcurrentHashMap<>();
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final Bootstrap bootstrap = new Bootstrap();
    private final ServiceDesc serviceDesc;
    private ChannelHandlerContext channelHandlerContext;

    private Channel writeChannel;

    public ServiceClient( ServiceDesc serviceDesc ) {
        this.serviceDesc = serviceDesc;
    }

    public void start() throws InterruptedException {
        bootstrap.group( group )
                .channel( NioSocketChannel.class )
                .handler( new ServiceClientInitializer( callbacks, ctx -> channelHandlerContext = ctx ) );
        writeChannel = bootstrap.connect( serviceDesc.getHost(), serviceDesc.getPort() ).sync().channel();
        running.set( true );
    }

    @Override
    protected void doClose() {
        final AtomicBoolean notify = new AtomicBoolean( false );
        boolean doWait;
        if ( ( doWait = ( null != channelHandlerContext ) ) ) {
            channelHandlerContext.close().addListener( future -> {
                synchronized ( notify ) {
                    notify.set( true );
                    notify.notify();
                }
            } );
        } else if ( ( doWait = ( null != writeChannel ) ) ) {
            writeChannel.close().addListener( future -> {
                synchronized ( notify ) {
                    notify.set( true );
                    notify.notify();
                }
            } );
        }
        if ( doWait ) {
            synchronized ( notify ) {
                long ms = 0;
                while ( !notify.get() && ms < 100000 ) {
                    try {
                        notify.wait( 1000 );
                        ms += 1000;
                    } catch ( InterruptedException ignored ) {
                    }
                }
            }
        }
    }

    public long sendRequest( final RemoteServiceRequest request, final Callback<RemoteServiceResponse> callback ) {
        if ( null != writeChannel ) {
            final long requestId = requestCounter.incrementAndGet();
            request.setId( requestId );
            callbacks.put( requestId, response -> {
                callbacks.remove( requestId );
                callback.call( response );
            } );
            final ChannelFuture cf = writeChannel.writeAndFlush( request );
            exec.execute( () -> {
                try {
                    cf.sync();
                } catch ( Exception e ) {
                    handleNetworkError( request, callback, e );
                }
            } );
            return requestId;
        } else {
            throw new IllegalStateException( "service client not started." );
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private void handleNetworkError( final RemoteServiceRequest request, final Callback<RemoteServiceResponse> callback, final Throwable e ) {
        removeCallback( request.getId() );
        if ( e instanceof ClosedChannelException ) {
            running.set( false );
        } else {
            RemoteServiceResponse response = new RemoteServiceResponse();
            response.setId( request.getId() );
            response.setSuccess( false );
            response.setErrorMsg( e.getMessage() );
            callback.call( response );
        }
    }

    public void removeCallback( long id ) {
        callbacks.remove( id );
    }

}
