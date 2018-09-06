package hirs.data.persist;

import static hirs.data.persist.DeviceInfoReport.NOT_SPECIFIED;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * OSInfoTest is a unit test class for OSInfo.
 */
public class OSInfoTest {

    private static final String OS_NAME = "test os";
    private static final String OS_VERSION = "test osVersion";
    private static final String OS_ARCH = "test osArch";
    private static final String DISTRIBUTION = "test distribution";
    private static final String DISTRIBUTION_RELEASE = "test distribution release";

    private static final String LONG_OS_NAME = StringUtils.rightPad("test os", 257);
    private static final String LONG_OS_VERSION = StringUtils.rightPad("test osVersion", 257);
    private static final String LONG_OS_ARCH = StringUtils.rightPad("test osArch", 33);

    /**
     * Tests instantiation of an OSInfo object.
     */
    @Test
    public final void osInfo() {
        new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
    }

    /**
     * Tests that the no-parameter constructor for OSInfo contains expected values.
     */
    @Test
    public final void osInfoNoParams() {
        OSInfo osInfo = new OSInfo();
        Assert.assertEquals(osInfo.getOSName(), NOT_SPECIFIED);
        Assert.assertEquals(osInfo.getOSVersion(), NOT_SPECIFIED);
        Assert.assertEquals(osInfo.getOSArch(), NOT_SPECIFIED);
        Assert.assertEquals(osInfo.getDistribution(), NOT_SPECIFIED);
        Assert.assertEquals(osInfo.getDistributionRelease(), NOT_SPECIFIED);
    }

    /**
     * Tests that the getters for OSInfo return the expected values.
     */
    @Test
    public final void osInfoGetters() {
        OSInfo osInfo =
                new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assert.assertEquals(osInfo.getOSName(), OS_NAME);
        Assert.assertEquals(osInfo.getOSVersion(), OS_VERSION);
        Assert.assertEquals(osInfo.getOSArch(), OS_ARCH);
        Assert.assertEquals(osInfo.getDistribution(), DISTRIBUTION);
        Assert.assertEquals(osInfo.getDistributionRelease(), DISTRIBUTION_RELEASE);
    }

    /**
     * Tests that a null pointer exception is thrown if OS name is null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void osNameNullTest() {
        new OSInfo(null, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
    }

    /**
     * Tests that a null pointer exception is thrown if OS version is null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void osVersionNullTest() {
        new OSInfo(OS_NAME, null, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
    }

    /**
     * Tests that a null pointer exception is thrown if OS arch is null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void osArchNullTest() {
        new OSInfo(OS_NAME, OS_VERSION, null, DISTRIBUTION, DISTRIBUTION_RELEASE);
    }

    /**
     * Tests that a null pointer exception is thrown if OS name is null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void osNameLongTest() {
        new OSInfo(LONG_OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
    }

    /**
     * Tests that a null pointer exception is thrown if OS version is null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void osVersionLongTest() {
        new OSInfo(OS_NAME, LONG_OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
    }

    /**
     * Tests that a null pointer exception is thrown if OS arch is null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void osArchLongTest() {
        new OSInfo(OS_NAME, OS_VERSION, LONG_OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
    }

    /**
     * Tests that distribution info may be null.
     */
    @Test
    public final void distributionNullTest() {
        new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, null, DISTRIBUTION_RELEASE);
    }

    /**
     * Tests that distribution release info may be null.
     */
    @Test
    public final void distributionReleaseNullTest() {
        new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, null);
    }

    /**
     * Tests that two OSInfo objects with the same name, make, version, and
     * distribution create hash codes that are equal.
     */
    @Test
    public final void testEqualHashCode() {
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assert.assertEquals(oi1.hashCode(), oi2.hashCode());
    }

    /**
     * Tests that two OSInfo objects with different name information will
     * generate different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeOSName() {
        String osName2 = "test os name 2";
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 = new OSInfo(osName2, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assert.assertNotEquals(oi1.hashCode(), oi2.hashCode());
    }

    /**
     * Tests that two OSInfo objects with different make information will
     * generate different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeOSMake() {
        String osMake2 = "test os make 2";
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 = new OSInfo(OS_NAME, osMake2, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assert.assertNotEquals(oi1.hashCode(), oi2.hashCode());
    }

    /**
     * Tests that two OSInfo objects with different version information will
     * generate different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeOSVersion() {
        String osVersion2 = "test os version 2";
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 =
                new OSInfo(OS_NAME, OS_VERSION, osVersion2, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assert.assertNotEquals(oi1.hashCode(), oi2.hashCode());
    }

    /**
     * Tests that two OSInfo objects with different distribution information
     * will generate different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeDistribution() {
        String distribution2 = "test distribution 2";
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, distribution2, DISTRIBUTION_RELEASE);
        Assert.assertNotEquals(oi1.hashCode(), oi2.hashCode());
    }

    /**
     * Tests that two OSInfo objects with the same name, make, version, and
     * distribution information are equal.
     */
    @Test
    public final void testEqual() {
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assert.assertEquals(oi1, oi2);
    }

    /**
     * Tests that two OSInfo objects with different name information are not
     * equal.
     */
    @Test
    public final void testNotEqualOSName() {
        String osName2 = "test os Name 2";
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 = new OSInfo(osName2, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assert.assertNotEquals(oi1, oi2);
    }

    /**
     * Tests that two OSInfo objects with different make information are not
     * equal.
     */
    @Test
    public final void testNotEqualOSMake() {
        String osMake2 = "test os make 2";
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 = new OSInfo(OS_NAME, osMake2, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assert.assertNotEquals(oi1, oi2);
    }

    /**
     * Tests that two OSInfo objects with different version information are not
     * equal.
     */
    @Test
    public final void testNotEqualOSVersion() {
        String osVersion2 = "test os version 2";
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 =
                new OSInfo(OS_NAME, OS_VERSION, osVersion2, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assert.assertNotEquals(oi1, oi2);
    }

    /**
     * Tests that two OSInfo objects with different distribution information are
     * not equal.
     */
    @Test
    public final void testNotEqualDistribution() {
        String distribution2 = "test distribution 2";
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, distribution2, DISTRIBUTION_RELEASE);
        Assert.assertNotEquals(oi1, oi2);
    }
}
