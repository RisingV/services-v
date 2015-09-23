package vee.services.netty;

import vee.services.comm.IgniteSupport;
import vee.services.comm.RemoteServiceConstants;
import vee.services.exception.ServiceNotAvailableException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteSet;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CollectionConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public class RemoteServiceKeeperImpl extends IgniteSupport implements RemoteServiceKeeper, RemoteServiceConstants {

    private final Ignite ignite;
    private final Random r;
    private CollectionConfiguration collCfg;

    public RemoteServiceKeeperImpl( Ignite ignite ) {
        this.ignite = ignite;
        this.r = new Random();
        this.collCfg = getCollectionConfiguration( CacheMode.REPLICATED );
    }

    @Override
    public ServiceDesc getServiceInstance( String serviceName ) {
        IgniteSet<ServiceDesc> availableInstance = getInstances( serviceName );
        if ( null == availableInstance || availableInstance.isEmpty() ) {
            throw new ServiceNotAvailableException( "no available service instance for '" + serviceName + "'" );
        }
        final int len = availableInstance.size();
        List<ServiceDesc> services = new ArrayList<>( availableInstance );
        return services.get( r.nextInt( len ) );
    }

    @Override
    public void addServiceInstance( ServiceDesc serviceDesc ) {
        IgniteSet<ServiceDesc> availableInstance = getInstances( serviceDesc.getServiceName() );
        availableInstance.add( serviceDesc );
    }

    @Override
    public void remoteServiceInstance( ServiceDesc serviceDesc ) {
        IgniteSet<ServiceDesc> availableInstance = getInstances( serviceDesc.getServiceName() );
        availableInstance.remove( serviceDesc );
    }

    private IgniteSet<ServiceDesc> getInstances( final String serviceName ) {
        return ignite.set( AVAILABLE_SERVICE_INSTANCE + serviceName, collCfg );
    }

}
