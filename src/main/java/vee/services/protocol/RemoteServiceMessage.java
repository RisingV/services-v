package vee.services.protocol;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-31  <br/>
 */
public abstract class RemoteServiceMessage implements Serializable {

    private static final long serialVersionUID = 8114491457137051122L;

    private long id;
    private String serviceName;
    private String methodDesc;
    private boolean ping;
    private boolean closeNotify;

    public long getId() {
        return id;
    }

    public void setId( long id ) {
        this.id = id;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc( String methodDesc ) {
        this.methodDesc = methodDesc;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName( String serviceName ) {
        this.serviceName = serviceName;
    }

    public boolean isCloseNotify() {
        return closeNotify;
    }

    public void setCloseNotify( boolean closeNotify ) {
        this.closeNotify = closeNotify;
    }

    public boolean isPing() {
        return ping;
    }

    public void setPing( boolean ping ) {
        this.ping = ping;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    abstract public byte[] toBytes() throws IOException;

    abstract public int getMessageFlag();

}
