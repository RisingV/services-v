package vee.services.comm;

import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CollectionConfiguration;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-03  <br/>
 */
public abstract class IgniteSupport {

    protected CollectionConfiguration getCollectionConfiguration( CacheMode cacheMode ) {
        CollectionConfiguration cfg = new CollectionConfiguration();
        cfg.setCacheMode( cacheMode );
        cfg.setCollocated( true );
        cfg.setAtomicityMode( CacheAtomicityMode.ATOMIC );
        return cfg;
    }

}
