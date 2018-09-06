package hirs.data;

import hirs.persist.ScheduledJobInfo;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link ScheduledTaskCheckState}.
 */
public class ScheduledTaskCheckStateTest {

    private static final int SECONDS_DIFFERENCE = 4;
    private static final int MILLISECONDS_DIFFERENCE = SECONDS_DIFFERENCE * 1000;
    private static final int EXTRA_SLEEP_MILLISECONDS = 500;

    // default interval - used in test cases where the exceeded interval isn't explicitly checked
    private static final long DEFAULT_JOB_INTERVAL_MS = 20;

    /**
     * Tests that querying for the start time being exceeded indicates false if there is no
     * start time, and it continues to indicate false with subsequent calls. This is the case
     * of having the corresponding periodic feature disabled (never starts).
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void taskStartedNullScheduleInfo() {
        ScheduledTaskCheckState state = new ScheduledTaskCheckState();
        Assert.assertFalse(state.isJobScheduledToRun(null));
        Assert.assertFalse(state.isJobScheduledToRun(null));
    }

    /**
     * Tests that the ScheduledTaskCheckState indicates that the task has not started
     * if the start time is in the past. Tests that it is still false on a subsequent call
     * immediately after
     */
    @Test
    public void taskInitiallyNotStartedWithStartTimeInPast() {
        ScheduledTaskCheckState state = new ScheduledTaskCheckState();
        LocalTime startTime = new LocalTime().minusSeconds(SECONDS_DIFFERENCE);
        ScheduledJobInfo jobInfo = new ScheduledJobInfo(DEFAULT_JOB_INTERVAL_MS, startTime);
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo));
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo));
    }

    /**
     * Tests that the ScheduledTaskCheckState can correctly determine that a job is scheduled to run
     * when the start time is met and there's been no previous job.
     * This test requires sleeping a short time to allow the start time to be passed
     * over.
     * @throws InterruptedException if an exception occurs sleeping.
     */
    @Test
    public void taskStartedAfterExceedingStartThreshold() throws InterruptedException {
        ScheduledTaskCheckState state = new ScheduledTaskCheckState();
        LocalTime startTime = new LocalTime().plusSeconds(SECONDS_DIFFERENCE);
        ScheduledJobInfo jobInfo = new ScheduledJobInfo(DEFAULT_JOB_INTERVAL_MS, startTime);
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo));
        // now that last check time has been stored, check if periodic task has started
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo));

        Thread.sleep(MILLISECONDS_DIFFERENCE + EXTRA_SLEEP_MILLISECONDS);
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo));
        // the state indicate the periodic task has started. This verifies it still will return
        // that indicator.
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo));
    }

    /**
     * Tests that the ScheduledTaskCheckState indicates that the periodic task has not started
     * when the start time has elapsed, but the scheduling is disabled.
     * @throws InterruptedException if an exception occurs sleeping.
     */
    @Test
    public void taskNotStartedWhenSchedulingIsDisabled() throws InterruptedException {
        ScheduledTaskCheckState state = new ScheduledTaskCheckState();
        LocalTime startTime = new LocalTime().plusSeconds(SECONDS_DIFFERENCE);
        ScheduledJobInfo jobInfo = new ScheduledJobInfo(DEFAULT_JOB_INTERVAL_MS, startTime);
        jobInfo.setSchedulingEnabled(false);
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo));
        // now that last check time has been stored, check if periodic task has started
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo));

        Thread.sleep(MILLISECONDS_DIFFERENCE + EXTRA_SLEEP_MILLISECONDS);
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo));
        // the state indicate the periodic task has not started.
        // This verifies it still will return that indicator.
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo));
    }

    /**
     * Tests that the Periodic Check state correctly accounts for the start time being reset
     * and the check state being reset in that condition. Tests that the new periodic start time
     * can be crossed again.
     * @throws InterruptedException if an exception occurs sleeping.
     */
    @Test
    public void taskStartedAfterExceedingStartThresholdAndThenReset() throws InterruptedException {
        ScheduledTaskCheckState state = new ScheduledTaskCheckState();
        LocalTime startTime  = new LocalTime(5, 0, 0);
        LocalDateTime currentDateTime = new LocalDateTime(2010, 1, 2, 4, 58, 0);
        ScheduledJobInfo jobInfo = new ScheduledJobInfo(DEFAULT_JOB_INTERVAL_MS, startTime);
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, currentDateTime));
        // now that last check time has been stored, check if periodic task has started
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, currentDateTime));

        currentDateTime = new LocalDateTime(2010, 1, 2, 5, 1, 0);

        Assert.assertTrue(state.isJobScheduledToRun(jobInfo, currentDateTime));
        // the state indicate the periodic task has started. This verifies it still will return
        // that indicator.
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo, currentDateTime));


        // reset the start time
        jobInfo.setStartTime(new LocalTime(6, 0, 0));
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, currentDateTime));
        // now that last check time has been stored, check if periodic task has started
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, currentDateTime));

        // trigger a start, based on new start time
        currentDateTime = new LocalDateTime(2010, 1, 2, 6, 10, 0);
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo, currentDateTime));
        // the state indicate the periodic task has started. This verifies it still will return
        // that indicator.
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo, currentDateTime));

    }
    /**
     * Tests that the ScheduledTaskCheckState can correctly determine when the start time has been
     * crossed during a midnight crossing and the start time is just before midnight.
     */
    @Test
    public void taskStartedWithMidnightCrossingWithStartTimePriorToMidnight() {
        ScheduledTaskCheckState state = new ScheduledTaskCheckState();
        LocalTime startTime = new LocalTime(23, 58);
        ScheduledJobInfo jobInfo = new ScheduledJobInfo(DEFAULT_JOB_INTERVAL_MS, startTime);
        LocalDateTime preMidnightDateTime = new LocalDateTime(2010, 3, 4, 23, 56, 0);
        LocalDateTime afterMidnightDateTime = new LocalDateTime(2010, 3, 5, 0, 2, 0);

        // before time elapsed
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, preMidnightDateTime));
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, preMidnightDateTime));

        // midnight crossed
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo, afterMidnightDateTime));
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo, afterMidnightDateTime));
    }

    /**
     * Tests that the ScheduledTaskCheckState can correctly determine when the start time has been
     * crossed during a midnight crossing and the start time is just after midnight, and a check
     * is made after midnight, but before the start time, followed by a later check
     * after the start time.
     */
    @Test
    public void taskStartedWithMidnightCrossingWithStartTimeAfterMidnightCase1() {
        ScheduledTaskCheckState state = new ScheduledTaskCheckState();
        LocalTime startTime = new LocalTime(0, 2);
        ScheduledJobInfo jobInfo = new ScheduledJobInfo(DEFAULT_JOB_INTERVAL_MS, startTime);
        LocalDateTime preMidnightDateTime = new LocalDateTime(2010, 3, 4, 23, 56, 0);
        LocalDateTime afterMidnightBeforeThresholdDateTime = new LocalDateTime(2010, 3, 5, 0, 1, 0);
        LocalDateTime afterMidnightAfterThresholdDateTime = new LocalDateTime(2010, 3, 5, 0, 3, 0);

        // before time elapsed
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, preMidnightDateTime));
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, preMidnightDateTime));

        // midnight crossed, threshold not crossed
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo,
                afterMidnightBeforeThresholdDateTime));
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo,
                afterMidnightBeforeThresholdDateTime));

        // midnight crossed, threshold crossed
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo,
                afterMidnightAfterThresholdDateTime));
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo,
                afterMidnightAfterThresholdDateTime));
    }

    /**
     * Tests that the ScheduledTaskCheckState can correctly determine when the start time has been
     * crossed during a midnight crossing and the start time is just after midnight, and a check
     * is made after midnight after the start time, but without any post midnight, pre-start time
     * checks.
     */
    @Test
    public void taskStartedWithMidnightCrossingWithStartTimeAfterMidnightCase2() {
        ScheduledTaskCheckState state = new ScheduledTaskCheckState();
        LocalTime startTime = new LocalTime(0, 2);
        LocalDateTime preMidnightDateTime = new LocalDateTime(2010, 3, 4, 23, 56, 0);
        LocalDateTime afterMidnightAfterThresholdDateTime = new LocalDateTime(2010, 3, 5, 0, 3, 0);

        ScheduledJobInfo jobInfo = new ScheduledJobInfo(DEFAULT_JOB_INTERVAL_MS, startTime);
        // before time elapsed
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, preMidnightDateTime));
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, preMidnightDateTime));

        // midnight crossed, threshold crossed
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo,
                afterMidnightAfterThresholdDateTime));
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo,
                afterMidnightAfterThresholdDateTime));
    }

    /**
     * Tests that the ScheduledTaskCheckState can determine that the start time has been crossed
     * and a job should be scheduled. A subsequent check prior to the update interval being
     * exceeded will indicate the schedule job should not be updated. Finally, a later check
     * after the interval has been exceeded will indicate that a scheduled task is required again.
     */
    @Test
    public void timeSchedulingAdheresToUpdateIntervalAndStartTime() {

        final int reportIntervalMs = 500000;
        final int shortIntervalJump = 50;

        ScheduledTaskCheckState state = new ScheduledTaskCheckState();
        LocalTime startTime = new LocalTime(20, 0);
        ScheduledJobInfo jobInfo = new ScheduledJobInfo(reportIntervalMs, startTime);
        jobInfo.setLastActualJobDate(new LocalDateTime(2010, 1, 1, 0, 0, 0).toDate());

        LocalDateTime preStartDateTime = new LocalDateTime(2010, 3, 4, 19, 30, 0);
        LocalDateTime postStartDateTime = new LocalDateTime(2010, 3, 5, 20, 2, 0);

        // before time elapsed
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, preStartDateTime));
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, preStartDateTime));

        // midnight crossed
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo, postStartDateTime));
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo, postStartDateTime));

        // set the last job time
        jobInfo.setLastActualJobDate(postStartDateTime.toDate());

        // advance time by a short amount. The expected run time of the last job
        // Periodic reporting has started, but the frequency
        // interval has not been exceeded.
        LocalDateTime lastJobExpectedStartDateTime =
                new LocalDateTime(jobInfo.getLastJobExpectedRunDate());

        LocalDateTime shortAdvancedTime1 =
                lastJobExpectedStartDateTime.plusMillis(reportIntervalMs - 30);

        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, shortAdvancedTime1));
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, shortAdvancedTime1));

        // advance time beyond the interval amount. state should indicate it's time for another job

        LocalDateTime advancedTimeBeyondInterval =
                postStartDateTime.plusMillis(reportIntervalMs + shortIntervalJump);

        Assert.assertTrue(state.isJobScheduledToRun(jobInfo, advancedTimeBeyondInterval));
        Assert.assertTrue(state.isJobScheduledToRun(jobInfo, advancedTimeBeyondInterval));

        jobInfo.setLastActualJobDate(advancedTimeBeyondInterval.toDate());

        // advanced the time again, a short amount. The interval hasn't been exceeded. Verify
        // state returns to false
        LocalDateTime shortAdvancedTime2 = advancedTimeBeyondInterval.plusMinutes(2);

        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, shortAdvancedTime2));
        Assert.assertFalse(state.isJobScheduledToRun(jobInfo, shortAdvancedTime2));
    }
}
