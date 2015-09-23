package vee.examples;

import vee.comm.ServiceLoaderUtil;
import vee.examples.service.IEchoService;
import vee.services.IServiceBusService;
import vee.services.ServiceBus;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-09-23  <br/>
 */
public class ServiceClientExample {

    public void doLoad() {
        IServiceBusService serviceBusService = ServiceLoaderUtil.loadService( IServiceBusService.class );
        ServiceBus serviceBus = serviceBusService.loadServiceBus( "test-group" );
        IEchoService echoService = serviceBus.loadService( "echo-service", IEchoService.class );
        echoService.echo( "hello, there." );
    }

}
