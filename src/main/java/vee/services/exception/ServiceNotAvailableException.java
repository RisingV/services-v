package vee.services.exception;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public class ServiceNotAvailableException extends RuntimeException {

    private static final long serialVersionUID = 8304615916808350628L;

    public ServiceNotAvailableException() {
    }

    public ServiceNotAvailableException( Throwable cause ) {
        super( cause );
    }

    public ServiceNotAvailableException( String message ) {
        super( message );
    }

    public ServiceNotAvailableException( String message, Throwable cause ) {
        super( message, cause );
    }

    public ServiceNotAvailableException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }

}
