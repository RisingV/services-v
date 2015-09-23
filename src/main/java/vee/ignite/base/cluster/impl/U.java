package vee.ignite.base.cluster.impl;

import org.apache.ignite.internal.util.IgniteUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-27  <br/>
 */
final class U extends IgniteUtils {

    private U() {}

    private static final String IPv4_PAT =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    public static String getLocalIpByInterfaceName( String name ) throws SocketException {
        if ( null == name ) return null;
        String last = null;
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
        return last;
    }

}
