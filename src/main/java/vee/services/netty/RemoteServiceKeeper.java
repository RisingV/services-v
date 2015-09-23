package vee.services.netty;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-04  <br/>
 */
public interface RemoteServiceKeeper {

    ServiceDesc getServiceInstance( String serviceName );

    void addServiceInstance( ServiceDesc serviceDesc );

    void remoteServiceInstance( ServiceDesc serviceDesc );

}
