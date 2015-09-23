package vee.services.netty;

import vee.services.protocol.RemoteServiceRequest;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.function.BiConsumer;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-02  <br/>
 */
public class ServiceServerInitializer extends ChannelInitializer<SocketChannel> {

    private final BiConsumer<RequestContext, RemoteServiceRequest> callback;

    public ServiceServerInitializer( BiConsumer<RequestContext, RemoteServiceRequest> callback ) {
        this.callback = callback;
    }

    @Override
    protected void initChannel( SocketChannel ch ) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast( new ServiceMessageDecoder() );
        pipeline.addLast( new ServiceMessageEncoder() );
        pipeline.addLast( new ServiceServerHandler( callback ) );
    }

}
