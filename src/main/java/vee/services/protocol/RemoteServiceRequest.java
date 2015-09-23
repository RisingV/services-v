package vee.services.protocol;

import vee.comm.SerializeUtil;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public class RemoteServiceRequest extends RemoteServiceMessage implements MessageConstants {

    private static final long serialVersionUID = 4588811633034061508L;

    private String responseAddress;
    private Object[] args;
    private long maxResponseMills;

    public String getResponseAddress() {
        return responseAddress;
    }

    public void setResponseAddress( String responseAddress ) {
        this.responseAddress = responseAddress;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs( Object[] args ) {
        this.args = args;
    }

    public long getMaxResponseMills() {
        return maxResponseMills;
    }

    public void setMaxResponseMills( long maxResponseMills ) {
        this.maxResponseMills = maxResponseMills;
    }

    @Override
    public byte[] toBytes() throws IOException {
        return SerializeUtil.serializeToBytes( this );
    }

    @Override
    public int getMessageFlag() {
        return REQUEST_FLAG;
    }

    public static RemoteServiceRequest fromBytes( byte[] bytes ) throws IOException, ClassNotFoundException {
        return (RemoteServiceRequest) SerializeUtil.deserialzieToObject( bytes );
    }

}
