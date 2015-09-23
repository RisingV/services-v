package vee.comm;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-29  <br/>
 */
public final class SerializeUtil {

    private static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    private SerializeUtil() {
    }

    public static byte[] serializeToBytes( Serializable obj ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FSTObjectOutput out = conf.getObjectOutput( baos );
        out.writeObject( obj );
        out.flush();
        return baos.toByteArray();
    }

    public static Object deserialzieToObject( byte[] bytes ) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream( bytes );
        FSTObjectInput in = conf.getObjectInput( bis );
        return in.readObject();
    }

}
