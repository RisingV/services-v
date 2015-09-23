package vee.services.netty;

import vee.services.protocol.RemoteServiceRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-02  <br/>
 */
public class ServiceServerHandler extends SimpleChannelInboundHandler<RemoteServiceRequest> {

    private static final Logger log = LoggerFactory.getLogger( ServiceServerHandler.class );

    private final BiConsumer<RequestContext, RemoteServiceRequest> callback;

    public ServiceServerHandler( BiConsumer<RequestContext, RemoteServiceRequest> callback ) {
        this.callback = callback;
    }

    @Override
    protected void messageReceived( ChannelHandlerContext ctx, RemoteServiceRequest request ) throws Exception {
        callback.accept( response -> {
            ctx.writeAndFlush( response );
            if ( request.isCloseNotify() ) {
                ctx.close();
            }
        }, request );
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception {
        log.error( "error caught in server handler: ", cause );
        super.exceptionCaught( ctx, cause );
    }

}
