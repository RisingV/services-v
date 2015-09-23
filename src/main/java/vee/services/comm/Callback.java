package vee.services.comm;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public interface Callback<T> {

    void call( T t );

}
