package vee.services.ignite;

import vee.services.comm.Callback;
import vee.services.comm.U;
import vee.services.protocol.RemoteServiceRequest;
import net.sf.cglib.proxy.InvocationHandler;
import org.apache.ignite.IgniteQueue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public class ServiceCallback implements InvocationHandler {

    private static final AtomicLong requestCounter = new AtomicLong( 0 );
    private final Map<String, String> methodDescCache = new HashMap<>();
    private final Function<String, IgniteQueue<byte[]>> requestQueueSelector;
    private final Map<Long, Callback> callbacks;
    private final String serviceName;
    private final String responseAddress;
    private final long timeoutMills;

    public ServiceCallback( Map<Long, Callback> callbacks, Function<String, IgniteQueue<byte[]>> requestQueueSelector,
                            String serviceName, String responseAddress, long timeoutMills ) {
        this.callbacks = callbacks;
        this.requestQueueSelector = requestQueueSelector;
        this.serviceName = serviceName;
        this.responseAddress = responseAddress;
        this.timeoutMills = timeoutMills;
    }

    @Override
    public Object invoke( Object proxied, Method method, Object[] args ) throws Throwable {
        final long ms = System.currentTimeMillis();

        long id = requestCounter.getAndIncrement();
        RemoteServiceRequest request = new RemoteServiceRequest();
        request.setId( id );
        request.setResponseAddress( responseAddress );
        request.setServiceName( serviceName );
        request.setMethodDesc( getMethodDesc( method ) );
        request.setArgs( args );
        request.setMaxResponseMills( timeoutMills );

        //before steps is not time-consuming, so use timeoutMills
        IgniteQueue<byte[]> requestQueue = requestQueueSelector.apply( serviceName );
        if ( !requestQueue.offer( request.toBytes(), timeoutMills, TimeUnit.MILLISECONDS ) ) {
            throw new TimeoutException( "remote service call timeout after " + timeoutMills + " mills." );
        }

        final MutableRef ref = new MutableRef();
        final Callback callback = ref::set;
        callbacks.put( id, callback );

        try {
            synchronized ( callback ) {
                long offset = System.currentTimeMillis() - ms;
                while ( null == ref.ref && ( offset < timeoutMills ) ) {
                    callback.wait( timeoutMills - offset );
                    offset = System.currentTimeMillis() - ms;
                }
            }
            if ( !ref.mark ) {
                throw new TimeoutException( "remote service call timeout after " + timeoutMills + " mills." );
            }
        } finally {
            callbacks.remove( id );
        }
        return ref.ref;
    }

    public String getMethodDesc( Method m ) {
        String methodName = m.getName();
        String desc = methodDescCache.get( methodName );
        if ( null == desc ) {
            synchronized ( methodDescCache ) {
                if ( !methodDescCache.containsKey( methodName ) ) {
                    desc = U.getMethodDescriptor( m );
                    methodDescCache.put( methodName, desc );
                }
            }
        }
        return desc;
    }

    static class MutableRef {
        Object ref;
        boolean mark;

        void set( Object ref ) {
            this.ref = ref;
            this.mark = true;
        }
    }

}
