package vee.services.netty;

import vee.services.comm.Callback;
import vee.services.protocol.RemoteServiceResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-02  <br/>
 */
public class ServiceClientInitializer extends ChannelInitializer<SocketChannel> {

    private final ConcurrentHashMap<Long, Callback<RemoteServiceResponse>> callbacks;
    private final Callback<ChannelHandlerContext> closeHook;

    public ServiceClientInitializer( ConcurrentHashMap<Long, Callback<RemoteServiceResponse>> callbacks,
                                     Callback<ChannelHandlerContext> closeHook ) {
        this.callbacks = callbacks;
        this.closeHook = closeHook;
    }

    @Override
    protected void initChannel( SocketChannel ch ) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast( new ServiceMessageDecoder() );
        pipeline.addLast( new ServiceMessageEncoder() );
        pipeline.addLast( new ServiceClientHandler( callbacks, closeHook ) );
    }

}
