package vee.examples.service;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-09-23  <br/>
 */
public class DefaultEchoServiceImpl implements IEchoService {

    @Override
    public String echo( String msg ) {
        return msg;
    }

}
