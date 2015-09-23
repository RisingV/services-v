package vee.services.netty;

import vee.services.protocol.RemoteServiceMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-31  <br/>
 */
public class ServiceMessageEncoder extends MessageToMessageEncoder<RemoteServiceMessage>  {

    @Override
    protected void encode( ChannelHandlerContext ctx, RemoteServiceMessage msg, List<Object> out ) throws Exception {
        byte[] msgBytes = msg.toBytes();
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt( msg.getMessageFlag() );
        buf.writeInt( msgBytes.length );
        buf.writeBytes( msgBytes );
        out.add( buf );
    }

}
