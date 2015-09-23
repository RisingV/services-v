package vee.services.support;

import vee.services.ServiceBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-12  <br/>
 */
public final class RemoteServiceSharedRegistry {

    private static final Logger log = LoggerFactory.getLogger( RemoteServiceSharedRegistry.class );

    private RemoteServiceSharedRegistry() {
    }

    static class Deployment<S> {
        String serviceName;
        Class<S> serviceInterface;
        S serviceInstance;

        public Deployment( String serviceName, Class<S> serviceInterface, S serviceInstance) {
            this.serviceName = serviceName;
            this.serviceInterface = serviceInterface;
            this.serviceInstance = serviceInstance;
        }

    }

    private static Map<String, Deployment<?>> deployments = new HashMap<>();

    private static ServiceBus serviceBusBind;

    static void bindServiceBus( ServiceBus serviceBus ) {
        serviceBusBind = serviceBus;
    }

    static synchronized void deployServices() {
        if ( null == serviceBusBind ) {
            throw new IllegalStateException( "service bus not bind." );
        }
        if ( deployments.isEmpty() ) return;
        Collection<Deployment<?>> toDeploys = deployments.values();
        for ( Deployment<?> d : toDeploys ) {
            doDeploy( d );
        }
        deployments.clear();
    }

    private static <S> void doDeploy( Deployment<S> deployment ) {
        serviceBusBind.deployService( deployment.serviceName, deployment.serviceInterface, deployment.serviceInstance );
        log.info( "service '{}' deployed and serve for remote.", deployment.serviceName );
    }

    public synchronized static <S> void registerService( String serviceName, Class<S> serviceInterface, S serviceInstance ) {
        if ( null == serviceBusBind ) {
            deployments.putIfAbsent( serviceName, new Deployment<>( serviceName, serviceInterface, serviceInstance ) );
        } else {
            doDeploy( new Deployment<>( serviceName, serviceInterface, serviceInstance ) );
        }
    }

}
