package vee.services.ignite;

import vee.ignite.base.IgniteConstants;
import vee.services.comm.Callback;
import vee.services.comm.NamedThreadFactory;
import vee.services.comm.U;
import net.sf.cglib.proxy.Enhancer;
import org.apache.ignite.Ignite;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public class RemoteServiceProxies extends RemoteServiceSupport implements IgniteConstants {

    private final ExecutorService exec = Executors.newCachedThreadPool( new NamedThreadFactory( "remote-service-proxy-worker-poll" ) );

    private final Map<Long, Callback> callbacks = new ConcurrentHashMap<>();
    private final Ignite ignite;
    private final int serviceQueueSize;
    private final long defaultTimeoutMills;
    private final int cores;

    public RemoteServiceProxies( Ignite ignite ) {
        this( ignite, 1000 * 20, 1000 * 1000 );
    }

    public RemoteServiceProxies( Ignite ignite, long defaultTimeoutMills, int serviceQueueSize ) {
        this.defaultTimeoutMills = defaultTimeoutMills;
        this.ignite = ignite;
        this.serviceQueueSize = serviceQueueSize;
        this.cores = Runtime.getRuntime().availableProcessors();
    }

    public <S> RemoteServiceStub<S> makeProxy( final String serviceName, final Class<S> serviceInterface ) {
        return makeProxy( serviceName, serviceInterface, defaultTimeoutMills );
    }

    @SuppressWarnings( "unchecked" )
    public <S> RemoteServiceStub<S> makeProxy( final String serviceName, final Class<S> serviceInterface, final long timeoutMills ) {
        final String responseAddress = SERVICE_RESPONSE_QUEUE_PREFIX + serviceName + '-' + U.getHostRepresentation();
        final S proxied = enhanceProxy( serviceName, responseAddress, serviceInterface, timeoutMills );

        return new RemoteServiceStub<>( resp -> {
            Callback callback = callbacks.get( resp.getId() );
            if ( null != callback ) {
                synchronized ( callback ) {
                    callback.call( resp.getResult() );
                    callback.notifyAll();
                }
            }
        }, getClientResponseQueue( responseAddress, serviceQueueSize ), serviceName, proxied, exec, cores * 4, 4, 10, 1000 * 60 ).start();
    }

    public void shutdown() {
        exec.shutdown();
    }

    @SuppressWarnings( "unchecked" )
    private <S> S enhanceProxy( final String serviceName, final String responseAddress, final Class<?> serviceInterface, final long timeoutMills ) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSerialVersionUID( 1l );
        enhancer.setInterfaces( new Class[] {serviceInterface, Serializable.class} );
        enhancer.setCallback( new ServiceCallback( callbacks, this::getClientRequestQueue, serviceName, responseAddress, timeoutMills ) );
        return (S) enhancer.create();
    }

    @Override
    Ignite getIgnite() {
        return ignite;
    }

}
