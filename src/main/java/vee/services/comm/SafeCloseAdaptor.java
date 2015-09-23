package vee.services.comm;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-06  <br/>
 */
public abstract class SafeCloseAdaptor implements AutoCloseable {

    private final AtomicBoolean closed = new AtomicBoolean( false );

    abstract protected void doClose() throws Exception;

    @Override
    public void close() throws Exception {
        if ( !closed.get() ) {
            synchronized ( closed ) {
                if ( !closed.get() ) {
                    try {
                        doClose();
                    } finally {
                        closed.set( true );
                    }
                }
            }
        }
    }

}
