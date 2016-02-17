package vee.comm;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-29  <br/>
 */
public final class ServiceLoaderUtil {

    private ServiceLoaderUtil() {
    }

    public static <S> S loadService( Class<S> clazz ) {
        List<S> services = loadServices( clazz );
        if ( services.isEmpty() ) throw new IllegalStateException( "can't load service: " + ( null != clazz ? clazz.getName() : "null" ) );
        return services.get( 0 );
    }

    public static <S> List<S> loadServices( Class<S> clazz ) {
        Objects.requireNonNull( clazz );
        return StreamSupport.stream( ServiceLoader.load( clazz ).spliterator(), false ).collect( Collectors.toList() );
    }

}
