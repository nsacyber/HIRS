package hirs.persist;

import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Date;

/**
 * Contains fields associated with a job that runs periodically and (optionally) starts at
 * a specified time of day.
 */
@Embeddable
public class ScheduledJobInfo implements Serializable {

    private static final long MINIMUM_FREQUENCY_MILLISECONDS = 1;

    /**
     * The default start time of 22:00.
     */
    public static final LocalTime DEFAULT_START_TIME = new LocalTime(22, 0, 0);

    @Column
    private LocalTime startTime;

    @Column
    private long scheduledJobFrequencyMilliseconds;

    @Column
    private Date lastActualJobDate;

    @Column
    private Date lastJobExpectedRunDate;

    @Column
    private boolean isSchedulingEnabled;

    /**
     * Hibernate constructor.
     */
    protected ScheduledJobInfo() {

    }

    /**
     * Constructs a new ScheduleJobInfo with the specified parameters.
     * @param scheduledJobFrequencyMilliseconds the frequency in which to execute the periodic
     *                                          task
     * @param startTime the time of day to start the periodic job.
     * @param isSchedulingEnabled is the schedule is enabled or disabled
     */
    public ScheduledJobInfo(final long scheduledJobFrequencyMilliseconds,
                            final LocalTime startTime,
                            final boolean isSchedulingEnabled) {
        setScheduledJobFrequencyMilliseconds(scheduledJobFrequencyMilliseconds);
        setStartTime(startTime);
        setSchedulingEnabled(isSchedulingEnabled);
    }

    /**
     * Constructs a new ScheduleJobInfo with the specified parameters.
     * @param scheduledJobFrequencyMilliseconds the frequency in which to execute the periodic
     *                                          task
     * @param startTime the time of day to start the periodic job.
     */
    public ScheduledJobInfo(final long scheduledJobFrequencyMilliseconds,
                            final LocalTime startTime) {
        this(scheduledJobFrequencyMilliseconds, startTime, true);
    }

    /**
     * Constructs a new ScheduleJobInfo with the specified job frequency value.
     * @param scheduledJobFrequencyMilliseconds the frequency in which to execute the periodic
     *                                          task
     */
    public ScheduledJobInfo(final long scheduledJobFrequencyMilliseconds) {
        this(scheduledJobFrequencyMilliseconds, DEFAULT_START_TIME, true);
    }

    /**
     * Gets the start time of the scheduled job.
     * @return the scheduled start time, or null if there is no start time.
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time for the scheduled job.
     * @param startTime the scheduled start time. Can be null to specify there is no specified
     *                  start time
     */
    public void setStartTime(final LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the frequency, in milliseconds, that the associated job should execute.
     * @return the frequency that the job should exeucte.
     */
    public long getScheduledJobFrequencyMilliseconds() {
        return scheduledJobFrequencyMilliseconds;
    }

    /**
     * Sets the frequency, in milliseconds, that the associated job should execute.
     * @param scheduledJobFrequencyMilliseconds the frequency that the job should exeucte.
     */
    public void setScheduledJobFrequencyMilliseconds(final long scheduledJobFrequencyMilliseconds) {
        if (scheduledJobFrequencyMilliseconds < MINIMUM_FREQUENCY_MILLISECONDS) {
            throw new IllegalArgumentException("Frequency must be at least "
                    + MINIMUM_FREQUENCY_MILLISECONDS);
        }

        this.scheduledJobFrequencyMilliseconds = scheduledJobFrequencyMilliseconds;
    }

    /**
     * Gets the date (or null) of the last periodic job actual request date. Note, this is not
     * necessarily when the job was *expected* to run.
     * @return the time of the last job.
     */
    public Date getLastActualJobDate() {
        if (null == lastActualJobDate) {
            return null;
        }

        return (Date) lastActualJobDate.clone();
    }

    /**
     * Sets the date (or null) of the last periodic job's actual request date. Note, this is not
     * necessarily when the job was *expected* to run.
     * @param lastActualJobDate the time of the last  job.
     */
    public void setLastActualJobDate(final Date lastActualJobDate) {
        if (null == lastActualJobDate) {
            this.lastActualJobDate = null;
        } else {
            this.lastActualJobDate = (Date) lastActualJobDate.clone();
        }
        calculateLastJobExpectedRunTime();
    }


    /**
     * Gets the date (or null) of when the last periodic job was expected to run.
     * Used to determine if the periodic interval has been exceeded.
     * @return the expected time of the last job.
     */
    public Date getLastJobExpectedRunDate() {
        if (null == lastJobExpectedRunDate) {
            return null;
        }

        return (Date) lastJobExpectedRunDate.clone();
    }

    /**
     * Sets the date (or null) of when the last periodic job was expected to run.
     * Used to determine if the periodic interval has been exceeded.
     * @param lastJobExpectedDate the expected time of the last job.
     */
    private void setLastJobExpectedRunDate(final Date lastJobExpectedDate) {
        if (null == lastJobExpectedDate) {
            this.lastJobExpectedRunDate = null;
        } else {
            this.lastJobExpectedRunDate = (Date) lastJobExpectedDate.clone();
        }
    }

    /**
     * Clears the last job expected run date. To be used when the last job date is null,
     * either because a job has never been run, or it's been updated due to changing other
     * schedule settings.
     */
    private void clearLastJobExpectedRunDate() {
        setLastJobExpectedRunDate(null);
    }

    /**
     * Gets flag indicating if the scheduling is enabled.
     * @return true if scheduling is enabled, false otherwise
     */
    public boolean isSchedulingEnabled() {
        return isSchedulingEnabled;
    }

    /**
     * Sets flag indicating if the scheduling is enabled.
     * @param schedulingEnabled true if scheduling is enabled, false otherwise
     */
    public void setSchedulingEnabled(final boolean schedulingEnabled) {
        isSchedulingEnabled = schedulingEnabled;
    }

    /**
     * Determines the time that the last job should have run. e.g, if this info
     * indicates to run every 24 hours at 12:00 and the last job was at 12:03 on Day X,
     * then the expected job run time would be 12:00 on Day X. This is necessary because
     * determining when the next job should be done, in <code>ScheduleTaskCheckState</code>
     * should be based on when the last job was scheduled to run, not when it actually ran. This
     * is to ensure that the scheduled jobs do not drift.
     *
     * The expected date of the last job is based on the job's interval, and the time the last
     * job ran. This method examines the previous last expected job time and the current job's
     * actual run time to determine how much time has elapsed since the last job.
     *
     * If there was no previously computed expected time, then the expected time is the start
     * time of the schedule.
     */
    private void calculateLastJobExpectedRunTime() {

        // reset the last expected job date if there is now no last job date.
        if (null == lastActualJobDate) {
            clearLastJobExpectedRunDate();
            return;
        }

        Date previousLastExpectedRunTime = this.getLastJobExpectedRunDate();
        LocalDateTime newLastExpectedJobTime;
        // if there was a last expected run date, simply add the interval to that date
        if (null != previousLastExpectedRunTime) {

            // how much real time has past since the previous job's expected run time and the
            // current job's actual run time
            Interval timeElapsedSinceLastExpectedTimeAndCurrentJob =
                new Interval(previousLastExpectedRunTime.getTime(), lastActualJobDate.getTime());

            // the time elapsed since the last expected report time.
            // the system may have skipped over any number of expected jobs that run.
            // The number of skipped jobs isn't important, but the amount of time that's passed
            // since the last job is important. We know the amount of time that's passed
            // since the last expected job, so use modulo division to determine how much time
            // has passed since the last scheduled job.
            Period timeElapsedSinceLastExpectedReportTime =
                new Period(timeElapsedSinceLastExpectedTimeAndCurrentJob.toDurationMillis()
                % this.getScheduledJobFrequencyMilliseconds());


            // set the last job expected time to be the last job date minus
            // the amount of time since the last expected job.
            newLastExpectedJobTime = new LocalDateTime(lastActualJobDate)
                    .minus(timeElapsedSinceLastExpectedReportTime);
        } else {
            // otherwise, the expected run time is the start time at today's date.
            // this only applies if the start time has passed today
            LocalTime jobStartTime = this.getStartTime();
            newLastExpectedJobTime =
                    new LocalDateTime(lastActualJobDate).withTime(jobStartTime.getHourOfDay(),
                    jobStartTime.getMinuteOfHour(), jobStartTime.getSecondOfMinute(),
                    jobStartTime.getMillisOfSecond());
        }
        setLastJobExpectedRunDate(newLastExpectedJobTime.toDate());
    }
}
