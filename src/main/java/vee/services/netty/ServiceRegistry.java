package vee.services.netty;

import vee.services.comm.SafeCloseAdaptor;
import vee.services.comm.ServiceLocalProxy;
import vee.services.protocol.RemoteServiceResponse;
import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public class ServiceRegistry extends SafeCloseAdaptor {

    private static final Logger log = LoggerFactory.getLogger( ServiceRegistry.class );

    private final ServiceServerGroup serviceServerGroup;

    public ServiceRegistry( Ignite ignite, int beginPort ) {
        this.serviceServerGroup = new ServiceServerGroup( new RemoteServiceKeeperImpl( ignite ), beginPort );
    }

    public <S> void registerService( String serviceName, Class<S> serviceInterface, S serviceInstance ) {
        final ServiceLocalProxy<S> serviceLocalProxy = new ServiceLocalProxy<>( serviceInterface, serviceInstance );
        serviceServerGroup.deployRemoteService( serviceName, ( context, request) -> {
            RemoteServiceResponse response = new RemoteServiceResponse();
            response.setId( request.getId() );
            response.setServiceName( request.getServiceName() );
            try {
                response.setResult( serviceLocalProxy.invoke( request ) );
                response.setSuccess( true );
            } catch ( InvocationTargetException | IllegalAccessException e ) {
                log.error( "service invoke error, serviceName: {}, error: ", serviceName, e );
                response.setErrorMsg( e.getMessage() );
                response.setSuccess( false );
            }
            context.reply( response );
        } );
    }

    @Override
    protected void doClose() throws Exception {
        serviceServerGroup.close();
    }

}
