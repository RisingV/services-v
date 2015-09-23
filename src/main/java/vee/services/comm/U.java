package vee.services.comm;

import vee.services.exception.LocalAddressResolveException;
import jdk.nashorn.internal.codegen.types.Type;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.*;
import java.util.Enumeration;
import java.util.Random;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public final class U implements RemoteServiceConstants {

    private static final int MIN_PORT_NUMBER = 1;
    private static final int MAX_PORT_NUMBER = 65535;

    private static final String DEFAULT_TIMEOUTMILLS = "30000";
    private static final String DEFAULT_SERVICE_BEGIN_PORT = "8964";

    private U() {
    }

    public static String getMethodDescriptor( Method m ) {
        return m.getName() + Type.getMethodDescriptor( m.getReturnType(), m.getParameterTypes() );
    }

    public static void canServiceForRemoteCheck( Class<?> interfaceType ) {
        if ( null == interfaceType ) {
            throw new NullPointerException( "service interface is null." );
        }
        if ( !interfaceType.isInterface() ) {
            throw new IllegalStateException( '\'' + interfaceType.getName() + '\'' + " is not a interface class." );
        }
        Method[] methods = interfaceType.getDeclaredMethods();
        if ( null != methods && methods.length > 0 ) {
            for ( Method m : methods ) {
                Class<?> returnType = m.getReturnType();
                if ( !Serializable.class.isAssignableFrom( returnType ) ) {
                    throw new IllegalStateException( interfaceType.getName() + ": return type of method " + m.getName()
                            + " is '" + returnType.getName() + "' which is not assignable from '" + Serializable.class.getName() + '\'' );
                }
                Parameter[] parameters = m.getParameters();
                if ( null != parameters && parameters.length > 0 ) {
                    for ( Parameter p : parameters ) {
                        Class<?> paramType = p.getType();
                        if ( !Serializable.class.isAssignableFrom( paramType ) ) {
                            throw new IllegalStateException( interfaceType.getName() + ": parameter(" + p.getName() + ") + type of method " + m.getName()
                                    + " is '" + paramType.getName() + "' which is not assignable from '" + Serializable.class.getName() + '\'' );
                        }
                    }
                }
            }
        }
    }

    public static String getHostRepresentation() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            if ( null != hostname && !hostname.isEmpty() && "localhost".equals( hostname ) ) {
                return hostname;
            }
        } catch ( UnknownHostException ignored ) {
        }
        return String.valueOf( new Random().nextInt( (int) ( System.currentTimeMillis() / 1000 ) ) );
    }

    public static boolean isAvailableTcpPort( int port ) {
        if ( port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER ) {
            throw new IllegalArgumentException( "Invalid start port: " + port );
        }
        ServerSocket ss = null;
        try {
            ss = new ServerSocket( port );
            ss.setReuseAddress( true );
            return true;
        } catch ( IOException ignored ) {
        } finally {
            if ( ss != null ) {
                try {
                    ss.close();
                } catch ( IOException ignored ) {
                }
            }
        }
        return false;
    }

    public static boolean isAvailableUdpPort( int port ) {
        if ( port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER ) {
            throw new IllegalArgumentException( "Invalid start port: " + port );
        }
        ServerSocket ss = null;
        try {
            ss = new ServerSocket( port );
            ss.setReuseAddress( true );
            return true;
        } catch ( IOException ignored ) {
        } finally {
            if ( ss != null ) {
                try {
                    ss.close();
                } catch ( IOException ignored ) {
                }
            }
        }
        return false;
    }

    public static boolean isRemoteReachable( String ip, int port, int timeoutMills ) {
        try {
            Socket socket = new Socket();
            socket.connect( new InetSocketAddress( ip, port ), timeoutMills );
            socket.close();
            return true;
        } catch ( Exception e ) {
            return false;
        }
    }

    private static final String IPv4_PAT =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    public static String getLocalIpByInterfaceName( String name ) {
        if ( null == name ) return null;
        String last = null;
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while ( e.hasMoreElements() ) {
                NetworkInterface n = (NetworkInterface) e.nextElement();

                Enumeration ee = n.getInetAddresses();
                while ( ee.hasMoreElements() ) {
                    InetAddress i = (InetAddress) ee.nextElement();
                    last = i.getHostAddress();
                    if ( name.equals( n.getDisplayName() ) && last.matches( IPv4_PAT ) ) {
                        return last;
                    }
                }
            }
        } catch ( SocketException e ) {
            throw new LocalAddressResolveException( e );
        }
        return last;
    }

    public static String getServiceNetInterface() {
        return System.getProperty( REMOTE_SERVICE_INTERFACE, getDefaultNetInterface() );
    }

    public static long getServiceTimeout() {
        return Long.valueOf( System.getProperty( REMOTE_SERVICE_TIMEOUTMILLS, DEFAULT_TIMEOUTMILLS ) );
    }

    public static int getServiceBeginPort() {
        return Integer.valueOf( System.getProperty( REMOTE_SERVICE_PORT, DEFAULT_SERVICE_BEGIN_PORT ) );
    }

    public static String getDefaultNetInterface() {
        if ( "Mac OS X".equals( System.getProperty( "os.name" ) ) ) {
            return "en0";
        }
        return "eth0";
    }

}
