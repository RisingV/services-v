package vee.services.exception;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public class NoAvailablePortException extends RuntimeException {

    public NoAvailablePortException() {
    }

    public NoAvailablePortException( Throwable cause ) {
        super( cause );
    }

    public NoAvailablePortException( String message ) {
        super( message );
    }

    public NoAvailablePortException( String message, Throwable cause ) {
        super( message, cause );
    }

    public NoAvailablePortException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }

}
