package vee.services;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-28  <br/>
 */
public interface ServiceBus extends AutoCloseable {

    <S> void deployService( String serviceName, Class<S> serviceInterface, S serviceInstance );

    <S> S loadService( String serviceName, Class<S> serviceInterface );

    void close() throws Exception;

}
