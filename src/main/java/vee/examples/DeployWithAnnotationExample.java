package vee.examples;

import vee.examples.service.IEchoService;
import vee.services.support.AutoDeploy;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-09-23  <br/>
 */

//with annotation AutoDeploy, this service for remote request, but only works when instance is a spring bean.
@AutoDeploy( type = IEchoService.class )
public class DeployWithAnnotationExample implements IEchoService {

    @Override
    public String echo( String msg ) {
        return msg;
    }

}
