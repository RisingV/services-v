package vee.examples;

import vee.examples.service.IEchoService;
import vee.services.support.FromRemote;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-09-23  <br/>
 */
public class InjectWithAnnotationExample {

    //with annotation FromRemote, inject service proxy to this field, but only works when instance is a spring bean.
    @FromRemote
    private IEchoService echoService;

    public String doSomething( String msg ) {
        return echoService.echo( msg );
    }

}
