package vee.comm;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-08  <br/>
 */
public final class DataSourceUtil {

    private DataSourceUtil() {
    }

    public static DataSource createDataSource( Properties properties ) {
        DataSource source;
        try {
            source = DruidDataSourceFactory.createDataSource( properties );
        } catch ( Exception e ) {
            throw new IllegalStateException( "create datasource failure!", e );
        }

        return source;
    }

    public static void closeDatasource( DataSource dataSource ) {
        if ( dataSource instanceof DruidDataSource ) {
            ( (DruidDataSource) dataSource ).close();
        } else {
            throw new IllegalStateException( "close datasource failure! don't known how to close." );
        }
    }

}
