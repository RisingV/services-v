package vee.services.ignite;

import vee.ignite.base.IgniteConstants;
import vee.services.ServiceBus;
import vee.services.comm.SafeCloseAdaptor;
import vee.services.comm.U;
import org.apache.ignite.Ignite;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-28  <br/>
 */
public class IgniteServiceBus extends SafeCloseAdaptor implements ServiceBus, IgniteConstants {

    private final Ignite ignite;
    private final ServiceDeployer serviceDeployer;
    private final ServiceLoader serviceLoader;

    public IgniteServiceBus( Ignite ignite ) {
        this.ignite = ignite;
        this.serviceDeployer = new ServiceDeployer( this.ignite );
        this.serviceLoader = new ServiceLoader( this.ignite );
    }

    @Override
    public <S> void deployService( String serviceName, Class<S> serviceInterface, S serviceInstance ) {
        U.canServiceForRemoteCheck( serviceInterface );
        serviceDeployer.deployServiceForRemote( serviceName, serviceInterface, serviceInstance );
    }

    @Override
    public <S> S loadService( String serviceName, Class<S> serviceInterface ) {
        U.canServiceForRemoteCheck( serviceInterface );
        return serviceLoader.loadRemoteService( serviceName, serviceInterface );
    }

    @Override
    protected void doClose() throws Exception {
        ignite.close();
    }

}
