package vee.services.exception;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public class LocalAddressResolveException extends RuntimeException {
    private static final long serialVersionUID = -802227367849157740L;

    public LocalAddressResolveException() {
    }

    public LocalAddressResolveException( Throwable cause ) {
        super( cause );
    }

    public LocalAddressResolveException( String message ) {
        super( message );
    }

    public LocalAddressResolveException( String message, Throwable cause ) {
        super( message, cause );
    }

    public LocalAddressResolveException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }

}
