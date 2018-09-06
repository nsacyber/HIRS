package hirs.data.persist;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the class <code>TPMDeviceState</code>.
 */
public final class TPMDeviceStateTest {

    /**
     * Tests that <code>TPMDeviceState</code> can be created with a valid <code>Device</code>.
     */
    @Test
    public void testCreateState() {
        final Device device = new Device("Test Device");
        new TPMDeviceState(device);
    }

    /**
     * Tests that <code>TPMDeviceState</code> cannot be created with a null <code>Device</code>.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCreateStateWithNullDevice() {
        final Device device = null;
        new TPMDeviceState(device);
    }

    /**
     * Tests that a <code>TPMDeviceState</code>'s report is initialized to null.
     */
    @Test
    public void testGetReportInitiallyNull() {
        final Device device = new Device("Test Device");
        final TPMDeviceState state = new TPMDeviceState(device);
        Assert.assertNull(state.getReport());
    }

    /**
     * Tests that the list of records is initialized to a blank list in constructor.
     */
    @Test
    public void testGetDefaultPcrIds() {
        final Device device = new Device("Test Device");
        final TPMDeviceState state = new TPMDeviceState(device);
        Assert.assertEquals(state.getTPMMeasurementRecords().size(), 0);

    }

    /**
     * Tests that updating the TPM device state given a TPM report works correctly.
     */
    @Test
    public void testSetTPMMeasurementRecords() {
        final Device device = new Device("Test Device");
        final TPMDeviceState state = new TPMDeviceState(device);

        final TPMReport tpmReport = TPMReportTest.getTestReport();

        state.setTPMReport(tpmReport);

        Assert.assertEquals(state.getReport().getTPMMeasurementRecords(),
                tpmReport.getTPMMeasurementRecords());
        Assert.assertEquals(state.getTPMMeasurementRecords().size(), 1);

        final int sha1HashLength = 20;
        final TPMMeasurementRecord expectedRecord = new TPMMeasurementRecord(
                TPMReportTest.TEST_REPORT_PCR_ID, TPMReportTest.getTestDigest(sha1HashLength));

        Assert.assertTrue(state.contains(expectedRecord));
    }

    /**
     * Tests that contains will return false if the report is null.
     */
    @Test
    public void testContainsNullReport() {
        final Device device = new Device("Test Device");
        final TPMDeviceState state = new TPMDeviceState(device);

        final int sha1HashLength = 20;
        final int pcrId = 4;

        TPMMeasurementRecord record =
                new TPMMeasurementRecord(pcrId, TPMReportTest.getTestDigest(sha1HashLength));

        Assert.assertEquals(state.getTPMMeasurementRecords().size(), 0);

        Assert.assertFalse(state.contains(record));
    }

    /**
     * Tests that updating the TPM device state given a TPM report works correctly.
     */
    @Test
    public void testReset() {
        final Device device = new Device("Test Device");
        final TPMDeviceState state = new TPMDeviceState(device);

        final TPMReport tpmReport = TPMReportTest.getTestReport();

        Assert.assertEquals(state.getTPMMeasurementRecords().size(), 0);
        Assert.assertNull(state.getReport());

        state.setTPMReport(tpmReport);

        Assert.assertEquals(state.getTPMMeasurementRecords().size(), 1);
        Assert.assertNotNull(state.getReport());

        state.setTPMReport(null);

        Assert.assertEquals(state.getTPMMeasurementRecords().size(), 0);
        Assert.assertNull(state.getReport());
    }

    /**
     * Tests that two <code>TPMDeviceState</code> objects are equal and have same hash code if have
     * same <code>Device</code>.
     */
    @Test
    public void testEquivalence() {
        final String deviceName = "Test Device";
        final Device device1 = new Device(deviceName);
        final Device device2 = new Device(deviceName);
        final TPMDeviceState state1 = new TPMDeviceState(device1);
        final TPMDeviceState state2 = new TPMDeviceState(device2);
        Assert.assertEquals(state1, state2);
        Assert.assertEquals(state1.hashCode(), state2.hashCode());
    }

    /**
     * Tests that two <code>TPMDeviceState</code> objects are not equal and have different hash
     * codes if they have different <code>Device</code>s.
     */
    @Test
    public void testNotEquivalent() {
        final Device device1 = new Device("Test Device 1");
        final TPMDeviceState state1 = new TPMDeviceState(device1);
        final Device device2 = new Device("Test Device 2");
        final TPMDeviceState state2 = new TPMDeviceState(device2);
        Assert.assertNotEquals(state1, state2);
        Assert.assertNotEquals(state1.hashCode(), state2.hashCode());
    }

    /**
     * Tests that an <code>TPMDeviceState</code> is not equal with null.
     */
    @Test
    public void testNotEqualsWithNull() {
        final Device device = new Device("Test Device");
        final TPMDeviceState state = new TPMDeviceState(device);
        Assert.assertFalse(state.equals(null));
    }

}
