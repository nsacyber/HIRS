package hirs.data.persist;

import hirs.data.persist.info.FirmwareInfo;
import org.apache.commons.lang3.StringUtils;
import static hirs.data.persist.DeviceInfoReport.NOT_SPECIFIED;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * FirmwareInfoTest is a unit test class for FirmwareInfo.
 */
public class FirmwareInfoTest {

    private static final String BIOS_VENDOR = "test bios vendor";
    private static final String BIOS_VERSION = "test bios version";
    private static final String BIOS_RELEASE_DATE = "test bios release date";

    private static final String LONG_BIOS_VENDOR = StringUtils.rightPad(
            "test bios vendor",
            257
    );
    private static final String LONG_BIOS_VERSION = StringUtils.rightPad(
            "test bios version",
            257
    );
    private static final String LONG_BIOS_RELEASE_DATE = StringUtils.rightPad(
            "test bios release date",
            33
    );

    /**
     * Tests instantiation of a FirmwareInfo object.
     */
    @Test
    public final void firmwareInfo() {
        new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
    }

    /**
     * Tests that the no-parameter constructor for FirmwareInfo contains expected values.
     */
    @Test
    public final void firmwareInfoNoParams() {
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        Assert.assertEquals(firmwareInfo.getBiosVendor(), NOT_SPECIFIED);
        Assert.assertEquals(firmwareInfo.getBiosVersion(), NOT_SPECIFIED);
        Assert.assertEquals(firmwareInfo.getBiosReleaseDate(), NOT_SPECIFIED);
    }

    /**
     * Tests that the getters for FirmwareInfo return the expected values.
     */
    @Test
    public final void firmwareInfoGetters() {
        FirmwareInfo firmwareInfo = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        Assert.assertEquals(firmwareInfo.getBiosVendor(), BIOS_VENDOR);
        Assert.assertEquals(firmwareInfo.getBiosVersion(), BIOS_VERSION);
        Assert.assertEquals(firmwareInfo.getBiosReleaseDate(), BIOS_RELEASE_DATE);
    }

    /**
     * Tests that an IllegalArgumentException is thrown if BIOS vendor is null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void biosVendorNullTest() {
        new FirmwareInfo(null, BIOS_VERSION, BIOS_RELEASE_DATE);
    }

    /**
     * Tests that an IllegalArgumentException is thrown if BIOS version is null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void biosVersionNullTest() {
        new FirmwareInfo(BIOS_VENDOR, null, BIOS_RELEASE_DATE);
    }

    /**
     * Tests that an IllegalArgumentException is thrown if BIOS release date is null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void biosReleaseDateNullTest() {
        new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, null);
    }

    /**
     * Tests that an IllegalArgumentException is thrown if BIOS vendor is longer than allowed.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void biosVendorLongTest() {
        new FirmwareInfo(LONG_BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
    }

    /**
     * Tests that an IllegalArgumentException is thrown if BIOS version is longer than allowed.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void biosVersionLongTest() {
        new FirmwareInfo(BIOS_VENDOR, LONG_BIOS_VERSION, BIOS_RELEASE_DATE);
    }

    /**
     * Tests that an IllegalArgumentException is thrown if BIOS release date is longer than allowed.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void biosReleaseDateLongTest() {
        new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, LONG_BIOS_RELEASE_DATE);
    }

    /**
     * Tests that two FirmwareInfo objects with the same BIOS vendor, version, and release date
     * create hash codes that are equal.
     */
    @Test
    public final void testEqualHashCode() {
        FirmwareInfo fi1 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        FirmwareInfo fi2 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        Assert.assertEquals(fi1.hashCode(), fi2.hashCode());
    }

    /**
     * Tests that two FirmwareInfo objects with different BIOS vendor information will generate
     * different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeBiosVendor() {
        String biosVendor2 = "test bios vendor 2";
        FirmwareInfo fi1 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        FirmwareInfo fi2 = new FirmwareInfo(biosVendor2, BIOS_VERSION, BIOS_RELEASE_DATE);
        Assert.assertNotEquals(fi1.hashCode(), fi2.hashCode());
    }

    /**
     * Tests that two FirmwareInfo objects with different BIOS version information will
     * generate different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeBiosVersion() {
        String biosVersion2 = "test bios version 2";
        FirmwareInfo fi1 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        FirmwareInfo fi2 = new FirmwareInfo(BIOS_VENDOR, biosVersion2, BIOS_RELEASE_DATE);
        Assert.assertNotEquals(fi1.hashCode(), fi2.hashCode());
    }

    /**
     * Tests that two FirmwareInfo objects with different BIOS release date information will
     * generate different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeBiosReleaseDate() {
        String biosReleaseDate2 = "test bios release date 2";
        FirmwareInfo fi1 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        FirmwareInfo fi2 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, biosReleaseDate2);
        Assert.assertNotEquals(fi1.hashCode(), fi2.hashCode());
    }

    /**
     * Tests that two FirmwareInfo objects with the same BIOS vendor, version, and release date
     * information are equal.
     */
    @Test
    public final void testEqual() {
        FirmwareInfo fi1 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        FirmwareInfo fi2 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        Assert.assertEquals(fi1, fi2);
    }

    /**
     * Tests that two FirmwareInfo objects with different BIOS vendor information are not equal.
     */
    @Test
    public final void testNotEqualBiosVendor() {
        String biosVendor2 = "test bios vendor 2";
        FirmwareInfo fi1 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        FirmwareInfo fi2 = new FirmwareInfo(biosVendor2, BIOS_VERSION, BIOS_RELEASE_DATE);
        Assert.assertNotEquals(fi1, fi2);
    }

    /**
     * Tests that two FirmwareInfo objects with different BIOS version information are not equal.
     */
    @Test
    public final void testNotEqualBiosVersion() {
        String biosVersion2 = "test bios version 2";
        FirmwareInfo fi1 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        FirmwareInfo fi2 = new FirmwareInfo(BIOS_VENDOR, biosVersion2, BIOS_RELEASE_DATE);
        Assert.assertNotEquals(fi1, fi2);
    }

    /**
     * Tests that two FirmwareInfo objects with different BIOS release date information are not
     * equal.
     */
    @Test
    public final void testNotEqualBiosReleaseDate() {
        String biosReleaseDate2 = "test bios release date 2";
        FirmwareInfo fi1 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, BIOS_RELEASE_DATE);
        FirmwareInfo fi2 = new FirmwareInfo(BIOS_VENDOR, BIOS_VERSION, biosReleaseDate2);
        Assert.assertNotEquals(fi1, fi2);
    }
}
