package vee.ignite.base.cluster.impl;

import vee.ignite.base.IgniteConstants;
import vee.ignite.base.cluster.IgniteClusterService;
import vee.ignite.base.cluster.InstanceType;
import vee.ignite.base.cluster.NodeType;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;

import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-27  <br/>
 */
public class IgniteClusterServiceImpl implements IgniteClusterService, IgniteConstants {

    private static final AtomicInteger instanceCounter = new AtomicInteger( 1 );

    private final DataSourceHolder dataSourceHolder;
    private final String localAddress;

    public IgniteClusterServiceImpl() {
        dataSourceHolder = DataSourceHolder.getInstance();
        String netInterface = System.getProperty( NET_ADAPTOR, ETH0 );
        try {
            localAddress = U.getLocalIpByInterfaceName( netInterface );
        } catch ( SocketException e ) {
            throw new IllegalStateException( "can't get ip address of interface '" + netInterface + '\'' );
        }
    }

    @Override
    public Ignite getInstance( String groupName, InstanceType instanceType, NodeType nodeType ) {
        IgniteConfiguration cfg = getConfiguration( groupName, getInstanceName( groupName, instanceType, nodeType ) );
        switch ( nodeType ) {
            case SERVER: break;
            case CLIENT: cfg.setClientMode( true ); break;
        }
        Map<String, String> userAttrs = new HashMap<>();
        userAttrs.put( GROUP_NAME, groupName );
        userAttrs.put( INSTANCE_TYPE, instanceType.toString() );
        cfg.setUserAttributes( userAttrs );
        return Ignition.start( cfg );
    }

    private String getInstanceName( String groupName, InstanceType instanceType, NodeType nodeType ) {
        return localAddress + '-' + groupName + '-' + String.valueOf( instanceCounter.getAndIncrement() ) +
                '-' + instanceType.toString() + '-' + nodeType.toString();
    }

    private IgniteConfiguration getConfiguration( String groupName, String instanceName ) {
        TcpDiscoveryDatabaseIpFinder ipFinder = new TcpDiscoveryDatabaseIpFinder( groupName, instanceName );
        ipFinder.setDataSource( dataSourceHolder.getDataSource() );
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        spi.setIpFinder( ipFinder );
        spi.setLocalAddress( localAddress );
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setDiscoverySpi( spi );
        cfg.setGridName( instanceName );
        cfg.setPeerClassLoadingEnabled( true );
        return cfg;
    }

}
