package vee.ignite.base.cluster;

import org.apache.ignite.Ignite;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-27  <br/>
 */
public interface IgniteClusterService {

    Ignite getInstance( String groupName, InstanceType instanceType, NodeType nodeType );

}
