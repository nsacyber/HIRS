package hirs.data.persist.alert;

import hirs.alert.JsonAlertService;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the class <code>AlertMonitor</code>.
 */
public class AlertMonitorTest {

    /**
     * Tests the enable of an alert monitor.
     */
    @Test
    public final void testAlertSummaryEnable() {
        //JsonAlertMonitor used as instance for AlertMonitor methods
        JsonAlertMonitor am = new JsonAlertMonitor("test");

        // check for default value set to false
        Assert.assertFalse(am.isAlertOnSummaryEnabled());

        am.enableAlertOnSummary();
        Assert.assertTrue(am.isAlertOnSummaryEnabled());

        am.disableAlertOnSummary();
        Assert.assertFalse(am.isAlertOnSummaryEnabled());
    }

    /**
     * Tests that an alert monitor.
     */
    @Test
    public final void testIndividualAlertEnable() {
        //JsonAlertMonitor used as instance for AlertMonitor methods
        JsonAlertMonitor am = new JsonAlertMonitor("test");
        Assert.assertFalse(am.isIndividualAlertEnabled());

        am.enableIndividualAlert();
        Assert.assertTrue(am.isIndividualAlertEnabled());

        am.disableIndividualAlert();
        Assert.assertFalse(am.isIndividualAlertEnabled());
    }

    /**
     * name test.
     */
    @Test
    public final void testName() {
        //JsonAlertMonitor used as instance for AlertMonitor methods
        JsonAlertMonitor am = new JsonAlertMonitor("test");
        String testname = "Json1";
        am.setName(testname);
        String getname = am.getName();

        Assert.assertEquals(testname, getname);
    }

    /**
     * Tests setter/getter for AlertServiceType.
     */
    @Test
    public final void testAlertServiceType() {
        JsonAlertMonitor am = new JsonAlertMonitor("test");
        am.setAlertServiceType(JsonAlertService.NAME);
        Assert.assertEquals(am.getAlertServiceType(), JsonAlertService.NAME);
    }
}
