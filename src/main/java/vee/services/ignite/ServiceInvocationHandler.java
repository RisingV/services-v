package vee.services.ignite;

import net.sf.cglib.proxy.InvocationHandler;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-29  <br/>
 */
public class ServiceInvocationHandler implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 7658577725928997411L;

    //FastMethod is not Serializable
    private volatile transient Map<String, FastMethod> fastMethods;

    private final Class<?> serviceInterface;
    private final Object serviceInstance;

    public ServiceInvocationHandler( Class<?> serviceInterface, Object serviceInstance ) {
        this.serviceInterface = serviceInterface;
        this.serviceInstance = serviceInstance;
    }

    private void init() {
        synchronized ( this ) {
            if ( null == fastMethods ) {
                this.fastMethods = new HashMap<>();
                FastClass fastClass = FastClass.create( serviceInterface );
                for ( Method m : serviceInterface.getMethods() ) {
                    fastMethods.put( m.getName(), fastClass.getMethod( m ) );
                }
            }
        }
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        if ( null == fastMethods ) {
            init();
        }
        FastMethod fm = fastMethods.get( method.getName() );
        if ( null != fm ) {
            fm.invoke( serviceInstance, args );
        }
        return null;
    }

}
