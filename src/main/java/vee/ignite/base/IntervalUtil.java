package vee.ignite.base;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-07-30  <br/>
 */
public abstract class IntervalUtil {

    public static void riseOffset( AtomicLong offset, final long defaultMs, final long zoomNum ) {
        offset.getAndAdd( defaultMs );
        offset.set( ( offset.get() % ( zoomNum * defaultMs ) ) );
    }

    public static void recedeOffset( AtomicLong offset, final long defaultMs ) {
        offset.set( 0 );
    }

    public static long getPollingMs( AtomicLong offset, final long defaultMs ) {
        return offset.get() + defaultMs;
    }

}
