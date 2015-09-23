package vee.services.ignite;

import vee.services.comm.Callback;
import vee.services.protocol.RemoteServiceResponse;
import org.apache.ignite.IgniteQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public class RemoteServiceStub<S> extends ServiceWorkerBase {

    private static final Logger log = LoggerFactory.getLogger( RemoteServiceStub.class );

    private AtomicBoolean keepOn = new AtomicBoolean( true );
    private AtomicInteger runningCounter = new AtomicInteger();
    private Set<AtomicBoolean> spawnedWorkerKeepOns = new HashSet<>();
    private final Callback<RemoteServiceResponse> callback;
    private final IgniteQueue<byte[]> responseQueue;
    private final String serviceName;
    private final S serviceProxied;
    private final ExecutorService exec;
    private final int workerNum;
    private final int spawnTimes;
    private final long pollingIntervalMills;
    private final long allWorksMaxBlockingMills;

    public RemoteServiceStub( Callback<RemoteServiceResponse> callback, IgniteQueue<byte[]> responseQueue,
                              String serviceName, S serviceProxied, ExecutorService exec, int workerNum,
                              int spawnTimes, final long pollingIntervalMills, final long allWorksMaxBlockingMills ) {
        this.callback = callback;
        this.responseQueue = responseQueue;
        this.serviceName = serviceName;
        this.serviceProxied = serviceProxied;
        this.exec = exec;
        this.workerNum = workerNum;
        this.spawnTimes = spawnTimes;
        this.pollingIntervalMills = pollingIntervalMills;
        this.allWorksMaxBlockingMills = allWorksMaxBlockingMills;
    }

    RemoteServiceStub<S> start() {
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
        return this;
    }

    public S getProxied() {
        return serviceProxied;
    }

    public void shutdown() {
        keepOn.set( false );
        Iterator<AtomicBoolean> it = spawnedWorkerKeepOns.iterator();
        if ( it.hasNext() ) {
            it.next().set( false );
        }
        spawnedWorkerKeepOns.clear();
    }

    private void launchWorker( final WorkerMonitor monitor, final AtomicBoolean keepOn ) {
        exec.execute( new ServiceWorker( bytes -> {
            if ( bytes.length > 0 ) {
                try {
                    callback.call( RemoteServiceResponse.fromBytes( bytes ) );
                } catch ( IOException | ClassNotFoundException e ) {
                    throw new RuntimeException( e );
                }
            }
        }, serviceName, responseQueue, keepOn, pollingIntervalMills, runningCounter, monitor ) );
    }

}
