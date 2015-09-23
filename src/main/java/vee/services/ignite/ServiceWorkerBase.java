package vee.services.ignite;

import vee.ignite.base.IntervalUtil;
import vee.services.comm.Callback;
import org.apache.ignite.IgniteQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public abstract class ServiceWorkerBase {

    private static final Logger log = LoggerFactory.getLogger( RemoteServiceStub.class );

    protected static class ServiceWorker extends IntervalUtil implements Runnable {

        private final String serviceName;
        private final Callback<byte[]> callback;
        private final IgniteQueue<byte[]> responseQueue;
        private final AtomicBoolean keepOn;
        private final long pollingInterval;
        private final AtomicInteger runningCounter;
        private final WorkerMonitor monitor;

        public ServiceWorker( Callback<byte[]> callback, String serviceName, IgniteQueue<byte[]> responseQueue,
                              AtomicBoolean keepOn, long pollingInterval, AtomicInteger runningCounter, WorkerMonitor monitor ) {
            this.callback = callback;
            this.serviceName = serviceName;
            this.responseQueue = responseQueue;
            this.keepOn = keepOn;
            this.pollingInterval = pollingInterval;
            this.runningCounter = runningCounter;
            this.monitor = monitor;
        }

        public void doWork() {
            AtomicLong offset = new AtomicLong( 0 );
            while ( keepOn.get() ) {
                try {
                    monitor.checkIn();
                    byte[] bytes = responseQueue.poll( getPollingMs( offset, pollingInterval ), TimeUnit.MILLISECONDS );
                    if ( null == bytes ) {
                        recedeOffset( offset, pollingInterval );
                        continue;
                    } else {
                        recedeOffset( offset, pollingInterval );
                    }
                    callback.call( bytes );
                } catch ( Exception err ) {
                    log.error( "error in response polling worker for service: '" + serviceName + "' , ", err );
                }
            }
        }

        @Override
        public void run() {
            try {
                runningCounter.incrementAndGet();
                doWork();
            } finally {
                runningCounter.decrementAndGet();
            }
        }

    }

    protected static class WorkerMonitor {

        private AtomicLong checkInCounter = new AtomicLong( 0 );
        private AtomicLong timestamp = new AtomicLong( 0 );
        private final long maxBlockingDuration;
        private final long pollingIntervalMills;
        private final WorkerExecuteHook hook;

        public WorkerMonitor( WorkerExecuteHook hook, long maxBlockingDuration, long pollingIntervalMills ) {
            this.hook = hook;
            this.maxBlockingDuration = maxBlockingDuration;
            this.pollingIntervalMills = pollingIntervalMills;
            checkIn();
        }

        public void checkIn() {
            timestamp.set( System.currentTimeMillis() );
            checkInCounter.incrementAndGet();
        }

        public void dutyCheck() {
            if ( System.currentTimeMillis() - timestamp.get() > maxBlockingDuration ) {
                checkInCounter.set( 0 );
                hook.upgrade( this );
            } else {
                if ( checkInCounter.get() > ( maxBlockingDuration / pollingIntervalMills ) ) {
                    hook.degrade( this );
                }
            }
        }

    }

    interface WorkerExecuteHook {

        void upgrade( WorkerMonitor monitor );

        void degrade( WorkerMonitor monitor );

    }

    interface LaunchWorkerCallback {
        void call( WorkerMonitor monitor, AtomicBoolean keepOn );
    }

    protected class DefaultWorkerExecuteHook implements WorkerExecuteHook {

        private final AtomicInteger runningCounter;
        private final LaunchWorkerCallback launchWorkerCallback;
        private final Set<AtomicBoolean> spawnedWorkerKeepOns;
        private final String serviceName;
        private final int workerNum;
        private final int spawnTimes;

        public DefaultWorkerExecuteHook( LaunchWorkerCallback launchWorkerCallback, AtomicInteger runningCounter,
                                         Set<AtomicBoolean> spawnedWorkerKeepOns, String serviceName, int workerNum, int spawnTimes ) {
            this.launchWorkerCallback = launchWorkerCallback;
            this.runningCounter = runningCounter;
            this.spawnedWorkerKeepOns = spawnedWorkerKeepOns;
            this.serviceName = serviceName;
            this.workerNum = workerNum;
            this.spawnTimes = spawnTimes;
        }

        @Override
        public void upgrade( WorkerMonitor monitorReuse ) {
            if ( runningCounter.get() < spawnTimes * workerNum ) {
                AtomicBoolean keepOn0 = new AtomicBoolean( true );
                spawnedWorkerKeepOns.add( keepOn0 );
                launchWorkerCallback.call( monitorReuse, keepOn0 );
                log.info( " span worker for service '" + serviceName + "', current worker number: " + runningCounter.get() );
            } else {
                log.warn( " worker number of service '" + serviceName + "' reaches maximum: " + spawnTimes * workerNum );
            }
        }

        @Override
        public void degrade( WorkerMonitor monitor ) {
            if ( runningCounter.get() > workerNum ) {
                Iterator<AtomicBoolean> it = spawnedWorkerKeepOns.iterator();
                AtomicBoolean runKeepOn = null;
                if ( it.hasNext() ) {
                    runKeepOn = it.next();
                }
                if ( null != runKeepOn ) {
                    runKeepOn.set( false );
                    spawnedWorkerKeepOns.remove( runKeepOn );
                    log.info( " reduce worker number for service '" + serviceName + "', current worker number: " + runningCounter.get() );
                }
            }
        }
    }

}
