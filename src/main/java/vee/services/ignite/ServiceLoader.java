package vee.services.ignite;

import vee.ignite.base.IgniteConstants;
import vee.ignite.base.cluster.InstanceType;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.cluster.ClusterGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public class ServiceLoader implements IgniteConstants {

    private final Ignite ignite;
    private final RemoteServiceProxies remoteServiceProxies;
    private List<AutoCloseable> closeables = new ArrayList<>();

    public ServiceLoader( Ignite ignite ) {
        this.ignite = ignite;
        this.remoteServiceProxies = new RemoteServiceProxies( this.ignite );
    }

    public <S> S loadRemoteService( String serviceName, Class<S> serviceInterface ) {
        RemoteServiceStub<S> serviceStub = remoteServiceProxies.makeProxy( serviceName, serviceInterface );
        closeables.add( serviceStub::shutdown );
        return serviceStub.getProxied();
    }

    public <S> S loadDistributedService( String serviceName, Class<S> serviceInterface ) {
        ClusterGroup group = ignite.cluster().forAttribute( INSTANCE_TYPE, InstanceType.SERVICE );
        IgniteServices igniteServices = ignite.services( group );
        return igniteServices.serviceProxy( serviceName, serviceInterface, false );
    }

    public void shutdown() {
        for ( AutoCloseable closeable : closeables ) {
            try {
                closeable.close();
            } catch ( Exception ignored ) {
            }
        }
        remoteServiceProxies.shutdown();
    }

}
