package vee.ignite.base.cluster.impl;

import vee.ignite.base.IgniteConstants;
import vee.comm.DataSourceUtil;
import vee.comm.DeployUtil;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-28  <br/>
 */
public class DataSourceHolder implements IgniteConstants {

    private static class LazyLoader {
        private static final DataSourceHolder instance = new DataSourceHolder();
    }

    public static DataSourceHolder getInstance() {
        return LazyLoader.instance;
    }

    private DataSource dataSource;

    private DataSourceHolder() {
        String propertiesFileName = System.getProperty( DB_CFG_FILE, DEFAULT_DB_CFG_FILE );
        try {
            dataSource = DataSourceUtil.createDataSource( DeployUtil.loadProperties( this.getClass(), propertiesFileName ) );
        } catch ( IOException e ) {
            throw new IllegalStateException( "load cluster config database properties file error.", e );
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

}
