package hirs.data.persist;

import hirs.data.persist.enums.DigestAlgorithm;
import org.apache.commons.codec.binary.Hex;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * Unit tests for the class <code>IMADeviceState</code>.
 */
public final class IMADeviceStateTest {

    /**
     * Default constructor.
     */
    public IMADeviceStateTest() {
        /* do nothing */
    }

    /**
     * Tests that <code>IMADeviceState</code> can be created with a valid
     * <code>Device</code>.
     */
    @Test
    public void testCreateState() {
        final Device device = new Device("Test Device");
        new IMADeviceState(device);
    }

    /**
     * Tests that <code>IMADeviceState</code> cannot be created with a null
     * <code>Device</code>.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCreateStateWithNullDevice() {
        final Device device = null;
        new IMADeviceState(device);
    }

    /**
     * Tests that boot-cycle ID is initialized to null in constructor.
     */
    @Test
    public void testGetDefaultBootcycleId() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        Assert.assertNull(state.getBootcycleId());
    }

    /**
     * Tests that boot-cycle ID can be set.
     */
    @Test
    public void testSetBootcycleId() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        final String bootcycleId = "Mon Apr 20 09:32";
        Assert.assertNotEquals(state.getBootcycleId(), bootcycleId);
        state.setBootcycleId(bootcycleId);
        Assert.assertEquals(state.getBootcycleId(), bootcycleId);
    }

    /**
     * Tests that boot-cycle ID can be set to null.
     */
    @Test
    public void testSetBootcycleIdNull() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        state.setBootcycleId(null);
        Assert.assertNull(state.getBootcycleId());
        final String bootcycleId = "Mon Apr 20 09:32";
        state.setBootcycleId(bootcycleId);
        Assert.assertEquals(state.getBootcycleId(), bootcycleId);
        state.setBootcycleId(null);
        Assert.assertNull(state.getBootcycleId());
    }

    /**
     * Tests that default index is set to -1 in constructor. This indicates that
     * no records have been appraised in the previous report.
     */
    @Test
    public void testGetDefaultIndex() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        Assert.assertEquals(state.getIndex(), 0);
    }

    /**
     * Tests that index can be set to valid values.
     */
    @Test
    public void testSetIndex() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        final int[] indices = new int[] {0, 100};
        for (int i = 0; i < indices.length; ++i) {
            final int index = indices[i];
            state.setIndex(index);
            Assert.assertEquals(state.getIndex(), index);
        }
    }

    /**
     * Tests that index cannot be set to a value less than 0.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSetIndexToInvalidValue() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        final int invalidIndex = -1;
        state.setIndex(invalidIndex);
    }

    /**
     * Tests that default state returns PCR state of null.
     */
    @Test
    public void testGetDefaulPcrState() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        Assert.assertNull(state.getPcrState());
    }

    /**
     * Tests that the PCR state can be set.
     */
    @Test
    public void testSetPcrState() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        final byte[] pcr = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05,
                0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
                0x10, 0x11, 0x12, 0x13};
        Assert.assertNull(state.getPcrState());
        state.setPcrState(pcr);
        Assert.assertEquals(state.getPcrState(), pcr);
    }

    /**
     * Tests that PCR state can be set to null to indicate that it was not saved
     * in the state.
     */
    @Test
    public void testSetPcrStateNull() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        final byte[] pcr = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05,
                0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
                0x10, 0x11, 0x12, 0x13};
        state.setPcrState(pcr);
        Assert.assertEquals(state.getPcrState(), pcr);
        state.setPcrState(null);
        Assert.assertNull(state.getPcrState());
    }

    /**
     * Tests that the most recent full report date is null if it has not been set yet.
     */
    @Test
    public void testGetMostRecentFullReportDate() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        Assert.assertNull(state.getMostRecentFullReportDate());
    }

    /**
     * Tests that the most recent full report date can be set and unset.
     */
    @Test
    public void testSetMostRecentFullReportDate() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        Date now = new Date();
        state.setMostRecentFullReportDate(now);
        Assert.assertEquals(state.getMostRecentFullReportDate(), now);
        state.setMostRecentFullReportDate(null);
        Assert.assertEquals(state.getMostRecentFullReportDate(), null);
    }

    /**
     * Tests that two <code>IMADeviceState</code> objects are equal and have
     * same hash code if have same <code>Device</code>.
     */
    @Test
    public void testEquivalence() {
        final Device device = new Device("Test Device");
        final IMADeviceState state1 = new IMADeviceState(device);
        final IMADeviceState state2 = new IMADeviceState(device);
        Assert.assertEquals(state1, state2);
        Assert.assertEquals(state1.hashCode(), state2.hashCode());
    }

    /**
     * Tests that two <code>IMADeviceState</code> objects are not equal and have
     * different hash codes if they have different <code>Device</code>s.
     */
    @Test
    public void testNotEquivalent() {
        final Device device1 = new Device("Test Device 1");
        final IMADeviceState state1 = new IMADeviceState(device1);
        final Device device2 = new Device("Test Device 2");
        final IMADeviceState state2 = new IMADeviceState(device2);
        Assert.assertNotEquals(state1, state2);
        Assert.assertNotEquals(state1.hashCode(), state2.hashCode());
    }

    /**
     * Tests that an <code>IMADeviceState</code> is not equal with null.
     */
    @Test
    public void testNotEqualsWithNull() {
        final Device device = new Device("Test Device");
        final IMADeviceState state = new IMADeviceState(device);
        Assert.assertFalse(state.equals(null));
    }

    private Digest getDigest(final String sha1) {
        try {
            final byte[] hash = Hex.decodeHex(sha1.toCharArray());
            return new Digest(DigestAlgorithm.SHA1, hash);
        } catch (Exception e) {
            throw new RuntimeException("unexpected exception", e);
        }
    }
}
