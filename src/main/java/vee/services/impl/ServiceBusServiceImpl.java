package vee.services.impl;

import vee.comm.ServiceLoaderUtil;
import vee.ignite.base.cluster.IgniteClusterService;
import vee.ignite.base.cluster.InstanceType;
import vee.ignite.base.cluster.NodeType;
import vee.services.IServiceBusService;
import vee.services.ServiceBus;
import vee.services.netty.NettyServiceBus;
import org.apache.ignite.Ignite;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-28  <br/>
 */
public class ServiceBusServiceImpl implements IServiceBusService {

    private IgniteClusterService igniteClusterService;

    public ServiceBusServiceImpl() {
        this.igniteClusterService = ServiceLoaderUtil.loadService( IgniteClusterService.class );
    }

    @Override
    public ServiceBus loadServiceBus( String groupName ) {
        Ignite ignite = igniteClusterService.getInstance( groupName, InstanceType.SERVICE, NodeType.SERVER );
        return new NettyServiceBus( ignite );
    }

}
