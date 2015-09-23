package vee.services.exception;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-04  <br/>
 */
public class RemoteServiceException extends Exception {

    private static final long serialVersionUID = 5084224645768131270L;

    public RemoteServiceException() {
    }

    public RemoteServiceException( Throwable cause ) {
        super( cause );
    }

    public RemoteServiceException( String message ) {
        super( message );
    }

    public RemoteServiceException( String message, Throwable cause ) {
        super( message, cause );
    }

    public RemoteServiceException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }

}
