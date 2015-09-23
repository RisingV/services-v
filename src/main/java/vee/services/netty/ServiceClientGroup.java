package vee.services.netty;

import vee.services.comm.Callback;
import vee.services.comm.NamedThreadFactory;
import vee.services.comm.SafeCloseAdaptor;
import vee.services.comm.U;
import vee.services.protocol.RemoteServiceRequest;
import vee.services.protocol.RemoteServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public class ServiceClientGroup extends SafeCloseAdaptor {

    private static final Logger log = LoggerFactory.getLogger( ServiceClientGroup.class );

    private static final ExecutorService exec = Executors.newCachedThreadPool( new NamedThreadFactory( "service-client-launch-close-pool" ) );

    private final RemoteServiceKeeper serviceKeeper;
    private final long defaultTimeoutMills;
    private final Map<ServiceDesc, ServiceClient> serviceClients = new ConcurrentHashMap<>();

    public ServiceClientGroup( RemoteServiceKeeper serviceKeeper, long defaultTimeoutMills ) {
        this.serviceKeeper = serviceKeeper;
        this.defaultTimeoutMills = defaultTimeoutMills;
    }

    public RemoteServiceResponse invokeRemoteService( RemoteServiceRequest request ) throws InterruptedException, TimeoutException {
        return invokeRemoteService( request, defaultTimeoutMills );
    }

    public RemoteServiceResponse invokeRemoteService( RemoteServiceRequest request, long timeoutMills ) throws InterruptedException, TimeoutException {
//        return invokeRemoteServiceNoRecover( request, timeoutMills );
        return invokeRemoteService( request, timeoutMills, 3, true );
    }

    @SuppressWarnings( "unchecked" )
    private RemoteServiceResponse invokeRemoteService( RemoteServiceRequest request, long timeoutMills, int slice, boolean tryRecover )
            throws InterruptedException, TimeoutException {

        final MutableRef ref = new MutableRef();
        final Callback<RemoteServiceResponse> callback = response -> {
            synchronized ( ref ) {
                ref.set( response );
                ref.notify();
            }
        };

        List<Callback<Void>> cleanCallbacks = new ArrayList<>();
        final Client client = getClient( request );
        long id = client.instance.sendRequest( request, callback );
        cleanCallbacks.add( v -> client.instance.removeCallback( id ) );
        ServiceDesc currentDesc = client.desc;

        final long beginMs = System.currentTimeMillis();
        long totalCostMs = 0, sliceMs = timeoutMills / slice;
        try {
            for (; totalCostMs < timeoutMills; totalCostMs = System.currentTimeMillis() - beginMs ) {
                synchronized ( ref ) {
                    ref.wait( sliceMs );
                }
                if ( !ref.mark ) {
                    if ( tryRecover ) {
                        Client newClient = tryRecoverService( request, callback, currentDesc, cleanCallbacks );
                        if ( null != newClient ) currentDesc = newClient.desc;
                    }
                } else {
                    return (RemoteServiceResponse) ref.ref;
                }
            }
        } finally {
            for ( Callback c : cleanCallbacks ) {
                c.call( null );
            }
        }
        throw new TimeoutException( "remote service call timeout after " + totalCostMs + " mills." );
    }

    private Client tryRecoverService( final RemoteServiceRequest request, final Callback<RemoteServiceResponse> responseCallback,
                                      final ServiceDesc preServiceDesc, List<Callback<Void>> cleanCallbacks ) {
        if ( !U.isRemoteReachable( preServiceDesc.getHost(), preServiceDesc.getPort(), 200 ) ) {
            log.warn( "service " + preServiceDesc + " is not reachable, remove it." );
            serviceKeeper.remoteServiceInstance( preServiceDesc );
            closeAndLog( serviceClients.remove( preServiceDesc ) );
        }

        final Client client = getClient( request );
        log.warn( "redirecting request to " + client.desc );
        final long id = client.instance.sendRequest( request, responseCallback );
        cleanCallbacks.add( v -> client.instance.removeCallback( id ) );
        return client;
    }

    private Client getClient( RemoteServiceRequest request ) {
        ServiceDesc desc = serviceKeeper.getServiceInstance( request.getServiceName() );
        ServiceClient client = doGetClient( desc );
        return new Client( desc, client );
    }

    private ServiceClient doGetClient( ServiceDesc desc ) {
        ServiceClient client = serviceClients.get( desc );
        if ( null == client ) {
            synchronized ( serviceClients ) {
                client = tryStartNewClient( desc );
            }
        } else {
            if ( !client.isRunning() ) {
                final ServiceClient toClose = client;
                exec.execute( () -> closeAndLog( toClose ) );
                synchronized ( serviceClients ) {
                    serviceClients.remove( desc );
                    client = tryStartNewClient( desc );
                }
            }
        }
        return client;
    }

    private ServiceClient tryStartNewClient( ServiceDesc desc ) {
        if ( !serviceClients.containsKey( desc ) ) {
            final ServiceClient innerClient = new ServiceClient( desc );
            try {
                innerClient.start();
            } catch ( Exception e ) {
                throw new IllegalStateException( "connecting service: " + desc + " failed. ", e );
            }
            serviceClients.put( desc, innerClient );
            return innerClient;
        } else {
            return serviceClients.get( desc );
        }
    }

    @Deprecated
    private RemoteServiceResponse invokeRemoteServiceNoRecover( RemoteServiceRequest request, long timeoutMills )
            throws InterruptedException, TimeoutException {
        ServiceDesc desc = serviceKeeper.getServiceInstance( request.getServiceName() );
        ServiceClient client = doGetClient( desc );

        final MutableRef ref = new MutableRef();
        final Callback<RemoteServiceResponse> callback = response -> {
            synchronized ( ref ) {
                ref.set( response );
                ref.notify();
            }
        };
        long id = 0;
        try {
            id = client.sendRequest( request, callback );
            synchronized ( ref ) {
                ref.wait( timeoutMills );
            }
            if ( !ref.mark ) {
                throw new TimeoutException( "remote service call timeout after " + timeoutMills + " mills." );
            }
        } finally {
            if ( !ref.mark && id > 0 ) {
                client.removeCallback( id );
            }
        }
        return (RemoteServiceResponse) ref.ref;
    }

    @Override
    protected void doClose() throws InterruptedException {
        List<ServiceClient> clients = new ArrayList<>( serviceClients.values() );
        CountDownLatch cdl = new CountDownLatch( clients.size() );
        for ( ServiceClient sc : clients ) {
            exec.execute( () -> {
                try {
                    closeAndLog( sc );
                } finally {
                    cdl.countDown();
                }
            } );
        }
        cdl.await();
    }

    private void closeAndLog( AutoCloseable ac ) {
        if ( null != ac ) {
            try {
                ac.close();
            } catch ( Exception e ) {
                log.error( "closing '{}' failed, detail: {} ", ac.getClass().getName(), e );
            }
        }
    }

    static class MutableRef {
        volatile Object ref = null;
        volatile boolean mark = false;

        void set( Object ref ) {
            this.ref = ref;
            this.mark = true;
        }
    }

    static class Client {
        public Client( ServiceDesc desc, ServiceClient instance ) {
            this.desc = desc;
            this.instance = instance;
        }

        ServiceClient instance;
        ServiceDesc desc;
    }

}
