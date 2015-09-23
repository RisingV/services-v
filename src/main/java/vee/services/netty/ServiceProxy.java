package vee.services.netty;

import vee.services.comm.U;
import vee.services.exception.RemoteServiceException;
import vee.services.protocol.RemoteServiceRequest;
import vee.services.protocol.RemoteServiceResponse;
import net.sf.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public class ServiceProxy implements InvocationHandler {

    interface ServiceRequester {
        RemoteServiceResponse call( RemoteServiceRequest request ) throws Exception;
    }

    private final Map<String, String> methodDescCache = new HashMap<>();
    private final ServiceRequester serviceRequester;
    private final String serviceName;
    private final long timeoutMills;

    public ServiceProxy( String serviceName, long timeoutMills, ServiceRequester serviceRequester ) {
        this.serviceName = serviceName;
        this.serviceRequester = serviceRequester;
        this.timeoutMills = timeoutMills;
    }

    @Override
    public Object invoke( Object o, Method method, Object[] args ) throws Throwable {
        RemoteServiceRequest request = new RemoteServiceRequest();
        request.setServiceName( serviceName );
        request.setMethodDesc( getMethodDesc( method ) );
        request.setArgs( args );
        request.setMaxResponseMills( timeoutMills );
        RemoteServiceResponse response = serviceRequester.call( request );
        if ( !response.isSuccess() ) {
            throw new RemoteServiceException( response.getErrorMsg() );
        }
        return response.getResult();
    }

    public String getMethodDesc( Method m ) {
        String methodName = m.getName();
        String desc = methodDescCache.get( methodName );
        if ( null == desc ) {
            synchronized ( methodDescCache ) {
                if ( !methodDescCache.containsKey( methodName ) ) {
                    desc = U.getMethodDescriptor( m );
                    methodDescCache.put( methodName, desc );
                }
            }
        }
        return desc;
    }

}
