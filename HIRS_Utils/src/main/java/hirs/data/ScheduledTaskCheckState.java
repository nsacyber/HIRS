package hirs.data;

import hirs.persist.ScheduledJobInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.springframework.util.Assert;

import java.util.Date;

/**
 * Holds transient information associated with an object that is checked from
 * some start time for periodic checks or updates. This is used in the periodic client reporting
 * feature and other features.
 */
public class ScheduledTaskCheckState {

    private boolean hasStartTimePassed;
    private LocalDateTime lastStartCheckDateTime;

    // used to track changes in the configured start time
    private LocalTime previousStartTime = null;


    private static final Logger LOGGER = LogManager.getLogger(ScheduledTaskCheckState.class);

    /**
     * Overload for
     * {@link ScheduledTaskCheckState#isJobScheduledToRun(ScheduledJobInfo, LocalDateTime)}
     * the current system time.
     * @param scheduledJobInfo the scheduled job information
     * @return true if a job is scheduled to run, false otherwise.
     */
    public boolean isJobScheduledToRun(final ScheduledJobInfo scheduledJobInfo) {
        return isJobScheduledToRun(scheduledJobInfo, new LocalDateTime());
    }

    /**
     * Checks if a job is scheduled to run based on the job's start time, update frequency, and
     * the last time a job has executed.
     * @param scheduledJobInfo the scheduled job information
     * @param currentTime the current time
     * @return true if a job is scheduled to run, false otherwise.
     */
    public boolean isJobScheduledToRun(final ScheduledJobInfo scheduledJobInfo,
                                       final LocalDateTime currentTime) {

        boolean isSchedulingStarted = isScheduledStartTimePassed(scheduledJobInfo, currentTime);
        boolean isIntervalElapsed = isFrequencyIntervalElapsed(scheduledJobInfo, currentTime);

        LOGGER.debug("scheduling started: {}, interval elapsed: {}",
                isSchedulingStarted, isIntervalElapsed);

        return isSchedulingStarted && isIntervalElapsed;
    }

    /**
     * Gets flag indicating if the periodic start time has passed and the associated object
     * will be periodically performing a function, such as updating. Checks against the specified
     * current time.
     * @param scheduledJobInfo the associated info for this scheduled job
     * @param currentDateTime the current time.
     * @return true if the periodic start threshold has passed the specified start time,
     *          false otherwise
     */
    private boolean isScheduledStartTimePassed(final ScheduledJobInfo scheduledJobInfo,
                                              final LocalDateTime currentDateTime) {

        Assert.notNull(scheduledJobInfo, "scheduledJobInfo");

        LocalTime startTime = scheduledJobInfo.getStartTime();

        // if the start time of the task has changed, reset the fields associated with tracking
        // if the periodic event has started. This will allow the new start time to take effect
        if (null == startTime || !startTime.equals(previousStartTime)) {
            hasStartTimePassed = false;
            lastStartCheckDateTime = null;
        }

        previousStartTime = startTime;

        if (!scheduledJobInfo.isSchedulingEnabled() || null == startTime) {
            return false;
        }

        // if we've already determined that the start time has been crossed, then return true
        // immediately.
        if (hasStartTimePassed) {
            lastStartCheckDateTime = currentDateTime;
            return true;
        }

        // if the start time was not previously exceeded, then check to see if it's been exceeded
        // now.

        // Initialize the last check time to now if this is the first check.
        // The below comparison will fail (false).
        if (null == lastStartCheckDateTime) {
            lastStartCheckDateTime = currentDateTime;
        }

        // if midnight crossed (e.g. current time is 00:02, previous time is 23:56), then check
        // to see if the start time is chronologically in between the two times. Special logic case
        // here because the hours wrap around and can't be treated with a simple "in between" check
        if (currentDateTime.toLocalTime().isBefore(lastStartCheckDateTime.toLocalTime())) {

            // if the start time is later in time than the last check date, e.g. 23:57
            if (startTime.isAfter(lastStartCheckDateTime.toLocalTime())) {
                hasStartTimePassed = true;
            } else if (startTime.isBefore(currentDateTime.toLocalTime())) {
                // if the start time is earlier in the day than the current time, e.g. 00:01
                hasStartTimePassed = true;
            }
        } else if (startTime.isAfter(lastStartCheckDateTime.toLocalTime())
            && startTime.isBefore(currentDateTime.toLocalTime())) {
            // non midnight crossing. do simple "in between" time check

            hasStartTimePassed = true;
        }

        // update the last periodic check time so that when this method is called again,
        // a new "last time" value will be used.
        lastStartCheckDateTime = currentDateTime;

        return hasStartTimePassed;
    }

    /**
     * Determines if enough time has passed since the last periodic job was expected to run
     * to initiate a new periodic job.
     * @param scheduledJobInfo the ScheduledJobInfo associated with the job
     * @param currentDateTime the current date time
     * @return true if enough time has elapsed sinc the last job that the job can be initiated,
     * or if there's never been a periodic job, false otherwise.
     */
    private boolean isFrequencyIntervalElapsed(final ScheduledJobInfo scheduledJobInfo,
                                              final LocalDateTime currentDateTime) {
        Date lastJobExpectedRunDate = scheduledJobInfo.getLastJobExpectedRunDate();

        // this is the first job, so it's time to run the job.
        if (null == lastJobExpectedRunDate) {
            return true;
        }

        long jobFrequencyMilliseconds = scheduledJobInfo.getScheduledJobFrequencyMilliseconds();
        long millisSinceLastJob = currentDateTime.toDate().getTime()
                - lastJobExpectedRunDate.getTime();

        return millisSinceLastJob >= jobFrequencyMilliseconds;
    }
}
