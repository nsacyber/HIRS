package hirs.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class tests the <code>PeriodicExecutor</code> class.
 */
public class PeriodicExecutorTest {
    private static final long LONG_PERIOD_TIME = 30;
    private static final long SHORT_PERIOD_TIME = 3;
    private static final long LONG_SLEEP_TIME = 10000;
    private static final long SHORT_SLEEP_TIME = 1000;
    private static final int EXPECTED_VALUE = 4;

    /**
     * Tests periodic execution of a normal task.
     *
     * @throws InterruptedException if the executor does not shut down cleanly.
     */
    @Test
    public final void testNormal() throws InterruptedException {
        final AtomicInteger integer = new AtomicInteger();
        PeriodicExecutor executor = new PeriodicExecutor();
        executor.start(new Runnable() {
            @Override
            public void run() {
                integer.addAndGet(1);
            }
        }, TimeUnit.SECONDS, SHORT_PERIOD_TIME);
        Thread.sleep(LONG_SLEEP_TIME);
        executor.stop(TimeUnit.MILLISECONDS, 0);

        // the integer should be incremented a total of 4 times:
        // t = 0s, 3s, 6s, 9s
        Assert.assertEquals(integer.get(), EXPECTED_VALUE);
    }

    /**
     * Tests periodic execution of a task which throws an exception.
     *
     * @throws InterruptedException if the executor does not shut down cleanly.
     */
    @Test
    public final void testWhileThrowingExceptions() throws InterruptedException {
        final AtomicInteger integer = new AtomicInteger();
        PeriodicExecutor executor = new PeriodicExecutor();
        executor.start(new Runnable() {
            @Override
            public void run() {
                integer.addAndGet(1);
                throw new RuntimeException();
            }
        }, TimeUnit.SECONDS, SHORT_PERIOD_TIME);
        Thread.sleep(LONG_SLEEP_TIME);
        executor.stop(TimeUnit.MILLISECONDS, 0);

        // the integer should be incremented a total of 4 times:
        // t = 0s, 3s, 6s, 9s
        Assert.assertEquals(integer.get(), EXPECTED_VALUE);
    }

    /**
     * Tests execution of a task with a long delay that only gets the chance to run
     * once before the executor is stopped.
     *
     * @throws InterruptedException if the executor does not shut down cleanly.
     */
    @Test
    public final void testRunsOnce() throws InterruptedException {
        final AtomicInteger integer = new AtomicInteger();
        PeriodicExecutor executor = new PeriodicExecutor();
        executor.start(new Runnable() {
            @Override
            public void run() {
                integer.addAndGet(1);
            }
        }, TimeUnit.SECONDS, LONG_PERIOD_TIME);
        Thread.sleep(SHORT_SLEEP_TIME);
        executor.stop(TimeUnit.MILLISECONDS, 0);

        // the integer should be incremented once at t = 0s
        Assert.assertEquals(integer.get(), 1);
    }
}
