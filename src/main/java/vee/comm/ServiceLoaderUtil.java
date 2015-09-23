package vee.comm;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-29  <br/>
 */
public final class ServiceLoaderUtil {

    private ServiceLoaderUtil() {
    }

    public static <S> S loadService( Class<S> clazz ) {
        if ( null != clazz ) {
            ServiceLoader<S> serviceLoader = ServiceLoader.load( clazz );
            Iterator<S> it = serviceLoader.iterator();
            if ( it.hasNext() ) {
                return it.next();
            }
        }
        throw new IllegalStateException( "can't load service: " + ( null != clazz ? clazz.getName() : "null" ) );
    }

}
