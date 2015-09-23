package vee.services.netty;

import vee.services.protocol.RemoteServiceResponse;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-28  <br/>
 */
public interface RequestContext {

    void reply( RemoteServiceResponse response );

}
