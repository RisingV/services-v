package vee.services.comm;

import vee.services.protocol.RemoteServiceRequest;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public class ServiceLocalProxy<S> {

    private Map<String, FastMethod> fastMethods = new HashMap<>();
    private final Class<S> serviceInterface;
    private final S serviceInstance;

    public ServiceLocalProxy( Class<S> serviceInterface, S serviceInstance ) {
        this.serviceInterface = serviceInterface;
        this.serviceInstance = serviceInstance;
        init();
    }

    private void init() {
        FastClass fastClass = FastClass.create( serviceInstance.getClass() );
        Method[] methods = serviceInterface.getMethods();
        for ( Method m : methods ) {
            fastMethods.put( U.getMethodDescriptor( m ), fastClass.getMethod( m ) );
        }
        Class cls = (new Object()).getClass();
        methods = cls.getMethods();
        for ( Method m : methods ) {
            fastMethods.put( U.getMethodDescriptor( m ), fastClass.getMethod( m ) );
        }
    }

    public Object invoke( RemoteServiceRequest request ) throws InvocationTargetException, IllegalAccessException {
        FastMethod fm = fastMethods.get( request.getMethodDesc() );
        if ( null != fm ) {
            return fm.invoke( serviceInstance, request.getArgs() );
        } else {
            throw new IllegalAccessException( "no matched method description for '" + request.getMethodDesc() + '\'' );
        }
    }
}
