package vee.services.netty;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public class ServiceDesc implements Serializable {

    private static final long serialVersionUID = -2814963128527900746L;
    private String serviceName;
    private String host;
    private int port;
    private boolean available;

    public String getHost() {
        return host;
    }

    public void setHost( String host ) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort( int port ) {
        this.port = port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName( String serviceName ) {
        this.serviceName = serviceName;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable( boolean available ) {
        this.available = available;
    }

    @Override
    public int hashCode() {
        return ( serviceName.hashCode() & 0x0ff0 ) | ( host.hashCode() & 0xff00 ) | ( port & 0x00ff );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj instanceof ServiceDesc ) {
            ServiceDesc that = (ServiceDesc) obj;
            //may throw npe
            return this.serviceName.equals( that.serviceName ) &&
                    this.host.equals( that.host ) &&
                    this.port == that.port;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{ serviceName: " + serviceName + ", host: " + host + ", port: " + port + " }";
    }

}
