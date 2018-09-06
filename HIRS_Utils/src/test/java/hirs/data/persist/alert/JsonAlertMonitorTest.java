package hirs.data.persist.alert;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Provides tests for JsonAlertMonitor.
 */
public class JsonAlertMonitorTest {

    /**
     * Test the default state of a new object.
     */
    @Test
    public void testDefault() {
        final String name = "jsontest";
        final JsonAlertMonitor.JsonAlertMode mode = JsonAlertMonitor.JsonAlertMode.TCP;

        JsonAlertMonitor jsonMonitor = new JsonAlertMonitor(name);

        Assert.assertTrue(jsonMonitor.isTCP());
        Assert.assertFalse(jsonMonitor.isUDP());
        Assert.assertEquals(jsonMonitor.getJsonAlertMode(), mode);
    }

    /**
     * Test that it can be set to UDP mode.
     */
    @Test
    public void testSetUDP() {
        final String name = "jsontest";
        final JsonAlertMonitor.JsonAlertMode mode = JsonAlertMonitor.JsonAlertMode.UDP;

        JsonAlertMonitor jsonMonitor = new JsonAlertMonitor(name);
        jsonMonitor.setUDP();

        Assert.assertFalse(jsonMonitor.isTCP());
        Assert.assertTrue(jsonMonitor.isUDP());
        Assert.assertEquals(jsonMonitor.getJsonAlertMode(), mode);
    }

    /**
     * Test that it can be set to TCP mode explicitly.
     */
    @Test
    public void testSetTCPAgain() {
        final String name = "jsontest";

        JsonAlertMonitor jsonMonitor = new JsonAlertMonitor(name);

        jsonMonitor.setUDP();
        Assert.assertTrue(jsonMonitor.isUDP());

        jsonMonitor.setTCP();
        Assert.assertTrue(jsonMonitor.isTCP());
    }
}
