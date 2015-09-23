package vee.services.ignite;

import vee.ignite.base.IgniteConstants;
import vee.services.comm.U;
import vee.services.exception.ServiceNotAvailableException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteQueue;
import org.apache.ignite.IgniteSet;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CollectionConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public abstract class RemoteServiceSupport implements IgniteConstants {

    abstract Ignite getIgnite();

    private final Random r = new Random();

    protected IgniteQueue<byte[]> getClientRequestQueue( final String serviceName ) {
        return getQueue( serviceName, r );
    }

    protected IgniteQueue<byte[]> getServerRequstQueue( final String serviceName, final int queueSize ) {
        return createQueue( serviceName, queueSize );
    }

    protected IgniteQueue<byte[]> getClientResponseQueue( final String queueName, final int queueSize ) {
        // create on local side;
        IgniteQueue<byte[]> responseQueue = getIgnite().queue( queueName, queueSize, getCollectionConfiguration( CacheMode.PARTITIONED ) );
        if ( null == responseQueue ) {
            throw new ServiceNotAvailableException( "selected service queue: '" + queueName + "' not prepared." );
        }
        return responseQueue;
    }

    protected IgniteQueue<byte[]> getServerResponseQueue( final String responseAddress ) {
        return getIgnite().queue( responseAddress, 0, null );
    }

    private IgniteQueue<byte[]> getQueue( final String serviceName, final Random random ) {
        IgniteSet<String> availableServiceQueue = getIgnite().set( serviceName, getCollectionConfiguration( CacheMode.REPLICATED ) );
        if ( null == availableServiceQueue || availableServiceQueue.isEmpty() ) {
            throw new ServiceNotAvailableException( "no prepared service queue." );
        }
        List<String> queues = new ArrayList<>( availableServiceQueue );
        final int len = queues.size();
        String selected = queues.get( random.nextInt( len ) );
        IgniteQueue<byte[]> selectedQueue = getIgnite().queue( selected, 0, null ); // created on service provider side.
        if ( null == selectedQueue ) {
            throw new ServiceNotAvailableException( "selected service queue: '" + selected + "' not prepared." );
        }
        return selectedQueue;
    }

    private IgniteQueue<byte[]> createQueue( final String serviceName, final int queueSize ) {
        final String requestAddress = SERVICE_RESPONSE_QUEUE_PREFIX + serviceName + '-' + U.getHostRepresentation();
        IgniteSet<String> availableServiceQueue = getIgnite().set( serviceName, getCollectionConfiguration( CacheMode.REPLICATED ) );
        IgniteQueue<byte[]> selectedQueue = getIgnite().queue( requestAddress, queueSize, getCollectionConfiguration( CacheMode.PARTITIONED ) );
        availableServiceQueue.add( requestAddress );
        return selectedQueue;
    }

    protected CollectionConfiguration getCollectionConfiguration( CacheMode cacheMode ) {
        CollectionConfiguration cfg = new CollectionConfiguration();
        cfg.setCacheMode( cacheMode );
        cfg.setCollocated( true );
        cfg.setAtomicityMode( CacheAtomicityMode.ATOMIC );
        return cfg;
    }

}
