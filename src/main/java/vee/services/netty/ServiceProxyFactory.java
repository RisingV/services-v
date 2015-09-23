package vee.services.netty;

import vee.services.comm.SafeCloseAdaptor;
import net.sf.cglib.proxy.Enhancer;
import org.apache.ignite.Ignite;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public class ServiceProxyFactory extends SafeCloseAdaptor {

    private final long defaultTimeoutMills;
    private final ServiceClientGroup serviceClientGroup;

    public ServiceProxyFactory( Ignite ignite, long defaultTimeoutMills ) {
        this.serviceClientGroup = new ServiceClientGroup( new RemoteServiceKeeperImpl( ignite ), defaultTimeoutMills );
        this.defaultTimeoutMills = defaultTimeoutMills;
    }

    public <S> S createProxy( final String serviceName, final Class<S> serviceInterface ) {
        return createProxy( serviceName, serviceInterface, defaultTimeoutMills );
    }

    public <S> S createProxy( final String serviceName, final Class<S> serviceInterface, final long timeoutMills ) {
        return enhanceProxy( serviceName, serviceInterface, timeoutMills );
    }

    @SuppressWarnings( "unchecked" )
    private <S> S enhanceProxy( final String serviceName, final Class<?> serviceInterface, final long timeoutMills ) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSerialVersionUID( 1l );
        enhancer.setInterfaces( new Class[] {serviceInterface, Serializable.class} );
        enhancer.setCallback( new ServiceProxy( serviceName, timeoutMills, request -> serviceClientGroup.invokeRemoteService( request, timeoutMills ) ) );
        return (S) enhancer.create();
    }

    @Override
    protected void doClose() throws Exception {
        serviceClientGroup.close();
    }

}
