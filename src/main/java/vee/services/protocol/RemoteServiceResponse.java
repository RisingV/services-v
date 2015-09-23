package vee.services.protocol;


import vee.comm.SerializeUtil;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public class RemoteServiceResponse extends RemoteServiceMessage implements MessageConstants {

    private static final long serialVersionUID = 3452314087984029963L;

    private Object result;
    private boolean success;
    private String errorMsg;

    public Object getResult() {
        return result;
    }

    public void setResult( Object result ) {
        this.result = result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess( boolean success ) {
        this.success = success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg( String errorMsg ) {
        this.errorMsg = errorMsg;
    }

    @Override
    public byte[] toBytes() throws IOException {
        return SerializeUtil.serializeToBytes( this );
    }

    @Override
    public int getMessageFlag() {
        return RESPONSE_FLAG;
    }

    public static RemoteServiceResponse fromBytes( byte[] bytes ) throws IOException, ClassNotFoundException {
        return (RemoteServiceResponse) SerializeUtil.deserialzieToObject( bytes );
    }

}
