package hirs.persist;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * Tests for {@link ScheduledJobInfo}.
 */
public class ScheduledJobInfoTest {

    private static final long FREQUENCY_MS = 500L;
    private static final long INVALID_FREQUENCY = -1;
    private static final LocalTime START_TIME = new LocalTime(1, 2, 3);

    /**
     * Tests the default state of the ScheduledJobInfo fields.
     */
    @Test
    public void defaultState() {
        ScheduledJobInfo info = new ScheduledJobInfo(FREQUENCY_MS);
        Assert.assertNull(info.getLastActualJobDate());
        Assert.assertNull(info.getLastJobExpectedRunDate());
        Assert.assertNotNull(info.getStartTime());
        Assert.assertEquals(info.getScheduledJobFrequencyMilliseconds(), FREQUENCY_MS);
        Assert.assertTrue(info.isSchedulingEnabled());
    }

    /**
     * Tests the two parameter constructor of ScheduledJobInfo.
     */
    @Test
    public void constructWithMultipleParameters() {
        ScheduledJobInfo info = new ScheduledJobInfo(FREQUENCY_MS, START_TIME);
        Assert.assertEquals(info.getScheduledJobFrequencyMilliseconds(), FREQUENCY_MS);
        Assert.assertEquals(info.getStartTime(), START_TIME);
        Assert.assertTrue(info.isSchedulingEnabled());
    }

    /**
     * Tests that constructing a ScheduledJobInfoTest with an illegal frequency throws an exception.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void constructWithIllegalFrequency() {
        new ScheduledJobInfo(INVALID_FREQUENCY);
    }

    /**
     * Tests that setting a ScheduledJobInfoTest with an illegal frequency throws an exception.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void setIllegalFrequency() {
        ScheduledJobInfo info = new ScheduledJobInfo(FREQUENCY_MS);
        info.setScheduledJobFrequencyMilliseconds(INVALID_FREQUENCY);
    }

    /**
     * Tests that the frequency field can be get / set.
     */
    @Test
    public void frequencyField() {
        final long newFrequencyMs = 88L;
        ScheduledJobInfo info = new ScheduledJobInfo(FREQUENCY_MS);
        info.setScheduledJobFrequencyMilliseconds(newFrequencyMs);
        Assert.assertEquals(info.getScheduledJobFrequencyMilliseconds(), newFrequencyMs);
    }

    /**
     * Tests that the last job date field can be get / set, which sets the last expected date time.
     */
    @Test
    public void lastJobDateField() {
        final Date newDate = new Date();

        LocalDateTime expectedJobDate = new LocalDateTime(newDate)
                .withTime(START_TIME.getHourOfDay(),
                        START_TIME.getMinuteOfHour(), START_TIME.getSecondOfMinute(), 0);

        ScheduledJobInfo info = new ScheduledJobInfo(FREQUENCY_MS, START_TIME);
        info.setLastActualJobDate(newDate);
        Assert.assertEquals(info.getLastActualJobDate(), newDate);
        Assert.assertEquals(info.getLastJobExpectedRunDate(), expectedJobDate.toDate());
        Assert.assertEquals(info.getScheduledJobFrequencyMilliseconds(), FREQUENCY_MS);
    }

    /**
     * Tests that the start time field can be get / set.
     */
    @Test
    public void startTimeField() {
        final LocalTime startTime = new LocalTime();
        ScheduledJobInfo info = new ScheduledJobInfo(FREQUENCY_MS);
        info.setStartTime(startTime);
        Assert.assertEquals(info.getStartTime(), startTime);
        Assert.assertEquals(info.getScheduledJobFrequencyMilliseconds(), FREQUENCY_MS);
    }

    /**
     * Tests that the 'is scheduling enabled' field can be get / set.
     */
    @Test
    public void isScheduleEnabled() {
        ScheduledJobInfo info = new ScheduledJobInfo(FREQUENCY_MS);
        info.setSchedulingEnabled(false);
        Assert.assertFalse(info.isSchedulingEnabled());

        info.setSchedulingEnabled(true);
        Assert.assertTrue(info.isSchedulingEnabled());
    }

    /**
     * Tests the progression of the last expected job date works as expected given
     * setting new job dates that skip and do not skip over scheduled dates.
     */
    @Test
    public void progressionOfLastExpectedJobDate() {

        // every hour
        final long reportFrequencyMs = 3600000L;
        final LocalTime startTestTime = new LocalTime(3, 0, 0);

        final LocalDateTime baseTime = new LocalDateTime();

        LocalDateTime expectedJobDate1 = new LocalDateTime(baseTime)
                .withTime(startTestTime.getHourOfDay(),
                        startTestTime.getMinuteOfHour(), startTestTime.getSecondOfMinute(), 0);
        LocalDateTime expectedJobDate2 = expectedJobDate1.plusHours(5);
        LocalDateTime expectedJobDate3 = expectedJobDate2.plusMinutes(60);

        final LocalDateTime actualJobDate1 = expectedJobDate1.plusMinutes(5);
        final LocalDateTime actualJobDate2 = expectedJobDate2.plusMinutes(5);
        final LocalDateTime actualJobDate3 = expectedJobDate3.plusMinutes(5);

        ScheduledJobInfo info = new ScheduledJobInfo(reportFrequencyMs, startTestTime);
        info.setLastActualJobDate(actualJobDate1.toDate());
        Assert.assertEquals(info.getLastActualJobDate(), actualJobDate1.toDate());
        Assert.assertEquals(info.getLastJobExpectedRunDate(), expectedJobDate1.toDate());
        Assert.assertEquals(info.getScheduledJobFrequencyMilliseconds(), reportFrequencyMs);

        info.setLastActualJobDate(actualJobDate2.toDate());
        Assert.assertEquals(info.getLastActualJobDate(), actualJobDate2.toDate());
        Assert.assertEquals(info.getLastJobExpectedRunDate(), expectedJobDate2.toDate());

        info.setLastActualJobDate(actualJobDate3.toDate());
        Assert.assertEquals(info.getLastActualJobDate(), actualJobDate3.toDate());
        Assert.assertEquals(info.getLastJobExpectedRunDate(), expectedJobDate3.toDate());
    }
}
