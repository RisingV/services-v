package vee.comm;

import java.io.IOException;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-25  <br/>
 */
public final class DeployUtil implements GlobalConstants {

    private static final String PRODUCTION_SUFFIX = "production";

    private static final String FILE_SUFFIX = "properties";

    private static Boolean inProduction;

    private DeployUtil() {
    }

    public static boolean isInProduction() {
        if ( null == inProduction ) {
            try {
                inProduction = ( PRODUCTION == Integer.parseInt( System.getProperty( APP_DEPLOY_MODE ) ) );
            } catch ( Exception ignored ) {
                throw new IllegalStateException( "jvm argument(" + APP_DEPLOY_MODE + ") not specified or invalid." );
            }
        }
        return inProduction;
    }

    private static String getResourceName( String fileName ) {
        if ( isInProduction() ) {
            return '/' + fileName + '.' + PRODUCTION_SUFFIX + '.' + FILE_SUFFIX;
        } else {
            return '/' + fileName + '.' + FILE_SUFFIX;
        }
    }

    public static Properties loadProperties( Class<?> cls, String filedName ) throws IOException {
        Properties properties = new Properties();
        String resource = getResourceName( filedName );
        properties.load( cls.getResourceAsStream( resource ) );
        return properties;
    }

}
