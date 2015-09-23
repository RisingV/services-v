package vee.services.netty;

import vee.services.comm.NamedThreadFactory;
import vee.services.comm.SafeCloseAdaptor;
import vee.services.comm.U;
import vee.services.exception.NoAvailablePortException;
import vee.services.protocol.RemoteServiceRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public class ServiceServerGroup extends SafeCloseAdaptor {

    private static final ExecutorService exec = Executors.newCachedThreadPool( new NamedThreadFactory( "service-servers-launch-handle-close-pool" ) );

    static int MAX_PORT_TRY = 10;

    private Map<String, ServiceServer> serviceServers = new ConcurrentHashMap<>();
    private final RemoteServiceKeeper serviceKeeper;
    private final AtomicInteger portAllocator;
    private final String localAddress;

    public ServiceServerGroup( RemoteServiceKeeper serviceKeeper, int beginPort ) {
        this.serviceKeeper = serviceKeeper;
        this.portAllocator = new AtomicInteger( beginPort );
        this.localAddress = U.getLocalIpByInterfaceName( U.getServiceNetInterface() );
    }

    public void deployRemoteService( final String serviceName, final BiConsumer<RequestContext, RemoteServiceRequest> callback ) {
        if ( serviceServers.containsKey( serviceName ) ) {
            throw new IllegalArgumentException( "service server of '" + serviceName + "' is started!" );
        }
        final int port = getPort();
        final ServiceServer server = new ServiceServer( port, ( c, r ) -> exec.execute( () -> callback.accept( c, r ) ) );
        try {
            server.start( future -> {
                final ServiceDesc desc = addServiceDesc( serviceName, localAddress, port );
                serviceServers.put( serviceName, server );
                server.onShutdown( e -> {
                    serviceKeeper.remoteServiceInstance( desc );
                    serviceServers.remove( serviceName );
                } );
            } );
        } catch ( InterruptedException e ) {
            throw new IllegalStateException( "deploy service interrupted.", e );
        }
    }

    private ServiceDesc addServiceDesc( String serviceName, String host, int port ) {
        ServiceDesc serviceDesc = new ServiceDesc();
        serviceDesc.setServiceName( serviceName );
        serviceDesc.setHost( host );
        serviceDesc.setPort( port );
        serviceKeeper.addServiceInstance( serviceDesc );
        return serviceDesc;
    }

    private int getPort() {
        int begin = portAllocator.get(), last = begin;
        for ( int i = 0; i < MAX_PORT_TRY; ++i ) {
            last = portAllocator.getAndIncrement();
            if ( U.isAvailableTcpPort( last ) ) {
                return last;
            }
        }
        throw new NoAvailablePortException( "no available port in range [" + begin + "," + last + "]." );
    }

    @Override
    protected void doClose() throws InterruptedException {
        List<ServiceServer> servers = new ArrayList<>( serviceServers.values() );
        final int number = servers.size();
        final CountDownLatch cdl = new CountDownLatch( number );
        for ( ServiceServer ss : servers ) {
            exec.execute( () -> {
                try {
                    ss.close();
                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                } finally {
                    cdl.countDown();
                }
            } );
        }
        cdl.await();
    }

}
