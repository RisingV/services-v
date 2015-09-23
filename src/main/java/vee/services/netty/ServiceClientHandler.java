package vee.services.netty;

import vee.services.comm.Callback;
import vee.services.protocol.RemoteServiceResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-02  <br/>
 */
public class ServiceClientHandler extends SimpleChannelInboundHandler<RemoteServiceResponse> {

    private static final Logger log = LoggerFactory.getLogger( ServiceClientHandler.class );

    private final ConcurrentHashMap<Long, Callback<RemoteServiceResponse>> callbacks;
    private final Callback<ChannelHandlerContext> closeHook;

    public ServiceClientHandler( ConcurrentHashMap<Long, Callback<RemoteServiceResponse>> callbacks,
                                 Callback<ChannelHandlerContext> closeHook ) {
        this.callbacks = callbacks;
        this.closeHook = closeHook;
    }

    @Override
    public void channelActive( ChannelHandlerContext ctx ) throws Exception {
        super.channelActive( ctx );
        if ( null != closeHook ) {
            closeHook.call( ctx );
        }
    }

    @Override
    protected void messageReceived( ChannelHandlerContext ctx, RemoteServiceResponse response ) throws Exception {
        final Callback<RemoteServiceResponse> callback = callbacks.get( response.getId() );
        if ( null != callback ) {
            callback.call( response );
        } else {
            log.warn( "service: {}, callback not found for request id: {} ", response.getServiceName(), response.getId() );
        }
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception {
        log.error( "error caught in client handler: {} ", cause );
        super.exceptionCaught( ctx, cause );
    }

}
