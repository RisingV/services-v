package vee.services.ignite;

import vee.ignite.base.IgniteConstants;
import vee.ignite.base.cluster.InstanceType;
import net.sf.cglib.proxy.Enhancer;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceConfiguration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public class ServiceDeployer implements IgniteConstants {

    private final Ignite ignite;
    private List<AutoCloseable> closeables = new ArrayList<>();

    public ServiceDeployer( Ignite ignite ) {
        this.ignite = ignite;
    }

    public <S> void deployServiceForRemote( String serviceName, Class<S> serviceInterface, S serviceInstance ) {
        RemoteServiceProvider<S> provider = new RemoteServiceProvider<>( ignite, serviceName, serviceInterface, serviceInstance );
        provider.start();
        closeables.add( provider::shutdown );
    }

    public <S> void deployDistributedService( String serviceName, Class<S> serviceInterface, S serviceInstance ) {
        ClusterGroup serviceGroup = ignite.cluster().forAttribute( INSTANCE_TYPE, InstanceType.SERVICE.toString() );
        IgniteServices igniteServices = ignite.services( serviceGroup );
        igniteServices.deploy( getServiceConfiguration( serviceName, serviceInterface, serviceInstance ) );
    }

    private <T> ServiceConfiguration getServiceConfiguration( String serviceName, Class<T> serviceInterface, T serviceInstance ) {
        ServiceConfiguration cfg = new ServiceConfiguration();
        cfg.setService( enhanceService( serviceInterface, serviceInstance ) );
        cfg.setName( serviceName );
        return cfg;
    }

    private <S> Service enhanceService( final Class<S> serviceInterface, final S serviceInstance ) {
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces( new Class[] {serviceInterface, Service.class, Serializable.class} );
        enhancer.setSerialVersionUID( 1l );
        enhancer.setCallback( new ServiceInvocationHandler( serviceInterface, serviceInstance ) );
        enhancer.createClass();
        return (Service) enhancer.create();
    }

    public void shutdown() {
        for ( AutoCloseable ac : closeables ) {
            try {
                ac.close();
            } catch ( Exception ignored ) {
            }
        }
    }

}
