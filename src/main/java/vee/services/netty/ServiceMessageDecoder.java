package vee.services.netty;

import vee.services.protocol.MessageConstants;
import vee.services.protocol.RemoteServiceRequest;
import vee.services.protocol.RemoteServiceResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-31  <br/>
 */
public class ServiceMessageDecoder extends ByteToMessageDecoder implements MessageConstants {

    enum State {
        READ_TYPE,
        READ_LEN,
        READ_BYTES
    }

    private State currentState = State.READ_TYPE;
    private int type = 0;
    private int len = 0;

    @Override
    protected void decode( ChannelHandlerContext ctx, ByteBuf in, List<Object> out ) throws Exception {
        OUTER:
        while ( true ) {
            switch ( currentState ) {
                case READ_TYPE:
                    if ( in.readableBytes() >= 4 ) {
                        type = in.readInt();
                        currentState = State.READ_LEN;
                    } else {
                        break OUTER;
                    }
                    break;
                case READ_LEN:
                    if ( in.readableBytes() >= 4 ) {
                        len = in.readInt();
                        currentState = State.READ_BYTES;
                    } else {
                        break OUTER;
                    }
                    break;
                case READ_BYTES:
                    if ( in.readableBytes() >= len ) {
                        decodeMessage( in, type, len, out );
                        currentState = State.READ_TYPE;
                    } else {
                        break OUTER;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private void decodeMessage( ByteBuf in, int type, int len, List<Object> out ) throws IOException, ClassNotFoundException {
        switch ( type ) {
            case REQUEST_FLAG:
                decodeRequest( len, in, out );
                break;
            case RESPONSE_FLAG:
                decodeResponse( len, in, out );
                break;
            default:
                throw new IllegalArgumentException( "invalid bytes found." );
        }
    }

    private void decodeRequest( int len, ByteBuf in, List<Object> out ) throws IOException, ClassNotFoundException {
        byte[] bytes = new byte[len];
        in.readBytes( bytes );
        out.add( RemoteServiceRequest.fromBytes( bytes ) );
    }

    private void decodeResponse( int len, ByteBuf in, List<Object> out ) throws IOException, ClassNotFoundException {
        byte[] bytes = new byte[len];
        in.readBytes( bytes );
        out.add( RemoteServiceResponse.fromBytes( bytes ) );
    }

}
