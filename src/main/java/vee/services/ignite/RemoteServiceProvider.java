package vee.services.ignite;

import vee.ignite.base.IgniteConstants;
import vee.services.comm.Callback;
import vee.services.comm.NamedThreadFactory;
import vee.services.comm.ServiceLocalProxy;
import vee.services.protocol.RemoteServiceRequest;
import vee.services.protocol.RemoteServiceResponse;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public class RemoteServiceProvider<S> extends RemoteServiceSupport implements IgniteConstants {

    private final Logger log = LoggerFactory.getLogger( RemoteServiceProvider.class );

    private final ExecutorService exec = Executors.newCachedThreadPool( new NamedThreadFactory( "deployed-service-provider-worker-poll" ) );

    private final Ignite ignite;
    private final String serviceName;
    private final Class<S> serviceInterface;
    private final S serviceInstance;
    private final int cores;
    private InnerServiceProvider innerServiceProvider;

    public RemoteServiceProvider( Ignite ignite, String serviceName, Class<S> serviceInterface, S serviceInstance ) {
        this.ignite = ignite;
        this.serviceName = serviceName;
        this.serviceInterface = serviceInterface;
        this.serviceInstance = serviceInstance;
        this.cores = Runtime.getRuntime().availableProcessors();
    }

    public void start() {
        final ServiceLocalProxy<S> serviceLocalProxy = makeServiceLocalProxy( serviceInterface, serviceInstance );
        this.innerServiceProvider = new InnerServiceProvider( request -> {
            if ( !serviceName.equals( request.getServiceName() ) ) {
                log.warn( "not expected request from service '" + request.getServiceName() + "' to service '" + serviceName + '\'' );
                return;
            }
            RemoteServiceResponse response = new RemoteServiceResponse();
            response.setId( request.getId() );
            response.setMethodDesc( request.getMethodDesc() );
            response.setServiceName( serviceName );
            try {
                Object result = serviceLocalProxy.invoke( request );
                response.setResult( result );
                response.setSuccess( true );
            } catch ( Exception e ) {
                log.error( "local service proxy invoke error: ", e );
                response.setSuccess( false );
                response.setErrorMsg( e.getCause().getMessage() );
            }
            try {
                IgniteQueue<byte[]> respQ = getServerResponseQueue( request.getResponseAddress() );
                respQ.offer( response.toBytes(), request.getMaxResponseMills(), TimeUnit.MILLISECONDS );
            } catch ( Exception err ) {
                log.error( "error when send response for service '" + serviceName + '\'' );
            }
        }, getServerRequstQueue( serviceName, 1000 * 1000 ), 4 * cores, 4, 10, 1000 * 60 );
        innerServiceProvider.start();
    }

    public void shutdown() {
        innerServiceProvider.shutdown();
        exec.shutdown();
    }

    @Override
    Ignite getIgnite() {
        return ignite;
    }

    private ServiceLocalProxy<S> makeServiceLocalProxy( Class<S> serviceInterface, S serviceInstance ) {
        return new ServiceLocalProxy<>( serviceInterface, serviceInstance );
    }

    class InnerServiceProvider extends ServiceWorkerBase {

        private AtomicBoolean keepOn = new AtomicBoolean( true );
        private AtomicInteger runningCounter = new AtomicInteger();
        private Set<AtomicBoolean> spawnedWorkerKeepOns = new HashSet<>();
        private final Callback<RemoteServiceRequest> callback;
        private final IgniteQueue<byte[]> requestQueue;
        private final int workerNum;
        private final int spawnTimes;
        private final long pollingIntervalMills;
        private final long allWorksMaxBlockingMills;

        public InnerServiceProvider( Callback<RemoteServiceRequest> callback, IgniteQueue<byte[]> requestQueue,
                                     int workerNum, int spawnTimes, long pollingIntervalMills, long allWorksMaxBlockingMills ) {
            this.callback = callback;
            this.requestQueue = requestQueue;
            this.workerNum = workerNum;
            this.spawnTimes = spawnTimes;
            this.pollingIntervalMills = pollingIntervalMills;
            this.allWorksMaxBlockingMills = allWorksMaxBlockingMills;
        }

        public void start() {
            WorkerExecuteHook hook = new DefaultWorkerExecuteHook( this::launchWorker, runningCounter,
                    spawnedWorkerKeepOns, serviceName, workerNum, spawnTimes );
            WorkerMonitor monitor = new WorkerMonitor( hook, allWorksMaxBlockingMills, pollingIntervalMills );
            for ( int i = 0; i < workerNum; ++i ) {
                launchWorker( monitor, keepOn );
            }
            exec.execute( () -> {
                while ( keepOn.get() ) {
                    try {
                        TimeUnit.MILLISECONDS.sleep( 10 * pollingIntervalMills );
                        monitor.dutyCheck();
                    } catch ( Exception err ) {
                        log.error( "error in monitor duty check loop: ", err );
                    }
                }
            } );
        }

        private void launchWorker( final WorkerMonitor monitor, final AtomicBoolean keepOn ) {
            exec.execute( new ServiceWorker( bytes -> {
                if ( bytes.length > 0 ) {
                    try {
                        callback.call( RemoteServiceRequest.fromBytes( bytes ) );
                    } catch ( IOException | ClassNotFoundException e ) {
                        throw new RuntimeException( e );
                    }
                } else {
                    callback.call( null );
                }
            }, serviceName, requestQueue, keepOn, pollingIntervalMills, runningCounter, monitor ) );
        }

        public void shutdown() {
            keepOn.set( false );
            for ( AtomicBoolean ko : spawnedWorkerKeepOns ) {
                ko.set( false );
            }
            spawnedWorkerKeepOns.clear();
        }

    }

}
