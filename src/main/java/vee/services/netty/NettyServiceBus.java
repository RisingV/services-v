package vee.services.netty;

import vee.services.ServiceBus;
import vee.services.comm.SafeCloseAdaptor;
import vee.services.comm.U;
import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-04  <br/>
 */
public class NettyServiceBus extends SafeCloseAdaptor implements ServiceBus {

    private static final Logger log = LoggerFactory.getLogger( NettyServiceBus.class );

    private final Ignite ignite;
    private final ServiceProxyFactory proxyFactory;
    private final ServiceRegistry serviceRegistry;

    public NettyServiceBus( Ignite ignite ) {
        this.ignite = ignite;
        this.proxyFactory = new ServiceProxyFactory( ignite, U.getServiceTimeout() );
        this.serviceRegistry = new ServiceRegistry( ignite, U.getServiceBeginPort() );
    }

    @Override
    public <S> void deployService( String serviceName, Class<S> serviceInterface, S serviceInstance ) {
        serviceRegistry.registerService( serviceName, serviceInterface, serviceInstance );
    }

    @Override
    public <S> S loadService( String serviceName, Class<S> serviceInterface ) {
        return proxyFactory.createProxy( serviceName, serviceInterface );
    }

    @Override
    protected void doClose() throws Exception {
        closeAndLog( proxyFactory );
        closeAndLog( serviceRegistry );
        closeAndLog( ignite ); // disconnect ignite cluster at last.
    }

    private void closeAndLog( AutoCloseable ac ) {
        try {
            ac.close();
        } catch ( Exception e ) {
            log.error( "close {} , error:", ac.getClass().getName(), e );
        }
    }

}
