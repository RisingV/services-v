package vee.services.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-15  <br/>
 */
public class NamedThreadFactory implements ThreadFactory {

    private static final Logger log = LoggerFactory.getLogger( NamedThreadFactory.class );
    private static final AtomicInteger poolNumber = new AtomicInteger( 1 );

    private final Thread.UncaughtExceptionHandler defaultExceptionHandler = ( t, e ) -> {
        if ( log.isDebugEnabled() ) {
            e.printStackTrace();
        }
        log.error( "Uncaught Exception Detected: {} in Thread: {} ", new Object[] {e, t.getName()} );
    };

    private final Thread.UncaughtExceptionHandler exceptionHandler;

    private final AtomicInteger threadNumber = new AtomicInteger( 1 );
    private final ThreadGroup group;
    private final String prefix;
    private final boolean daemon;

    public NamedThreadFactory( String poolName ) {
        this( poolName, true, null );
    }

    public NamedThreadFactory( String poolName, boolean daemon, Thread.UncaughtExceptionHandler exceptionHandler ) {
        this.daemon = daemon;
        this.exceptionHandler = defaultExceptionHandler;
        SecurityManager s = System.getSecurityManager();
        group = ( s != null ) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        prefix = poolName + "-" + poolNumber.getAndIncrement() + "-thread-";
    }

    @Override
    public Thread newThread( Runnable r ) {
        Thread t = new Thread( group, r, prefix + threadNumber.getAndIncrement(), 0 );
        t.setDaemon( daemon );
        if ( t.getPriority() != Thread.NORM_PRIORITY ) {
            t.setPriority( Thread.NORM_PRIORITY );
        }
        t.setUncaughtExceptionHandler( null != exceptionHandler ? exceptionHandler : defaultExceptionHandler );
        return t;
    }

}
