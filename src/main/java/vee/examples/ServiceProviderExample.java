package vee.examples;

import vee.comm.ServiceLoaderUtil;
import vee.examples.service.DefaultEchoServiceImpl;
import vee.examples.service.IEchoService;
import vee.services.IServiceBusService;
import vee.services.ServiceBus;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-09-23  <br/>
 */
public class ServiceProviderExample {

    public void doDepoy() {
        IServiceBusService serviceBusService = ServiceLoaderUtil.loadService( IServiceBusService.class );
        ServiceBus serviceBus = serviceBusService.loadServiceBus( "test-group" );
        serviceBus.deployService( "echo-service", IEchoService.class, new DefaultEchoServiceImpl() );
    }

}
