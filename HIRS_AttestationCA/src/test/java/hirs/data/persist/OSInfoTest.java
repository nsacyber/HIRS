package hirs.data.persist;

import hirs.attestationca.persist.entity.userdefined.info.OSInfo;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static hirs.utils.enums.DeviceInfoEnums.NOT_SPECIFIED;

/**
 * OSInfoTest is a unit test class for OSInfo.
 */
public class OSInfoTest {

    private static final String OS_NAME = "test os";
    private static final String OS_VERSION = "test osVersion";
    private static final String OS_ARCH = "test osArch";
    private static final String DISTRIBUTION = "test distribution";
    private static final String DISTRIBUTION_RELEASE = "test distribution release";
    private static final int PRIMARY_SIZE = 257;
    private static final int SECONDARY_SIZE = 33;

    private static final String LONG_OS_NAME = StringUtils.rightPad(OS_NAME, PRIMARY_SIZE);
    private static final String LONG_OS_VERSION = StringUtils.rightPad(OS_VERSION, PRIMARY_SIZE);
    private static final String LONG_OS_ARCH = StringUtils.rightPad(OS_ARCH, SECONDARY_SIZE);

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
        Assertions.assertEquals(NOT_SPECIFIED, osInfo.getOsName());
        Assertions.assertEquals(NOT_SPECIFIED, osInfo.getOsVersion());
        Assertions.assertEquals(NOT_SPECIFIED, osInfo.getOsArch());
        Assertions.assertEquals(NOT_SPECIFIED, osInfo.getDistribution());
        Assertions.assertEquals(NOT_SPECIFIED, osInfo.getDistributionRelease());
    }

    /**
     * Tests that the getters for OSInfo return the expected values.
     */
    @Test
    public final void osInfoGetters() {
        OSInfo osInfo =
                new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assertions.assertEquals(OS_NAME, osInfo.getOsName());
        Assertions.assertEquals(OS_VERSION, osInfo.getOsVersion());
        Assertions.assertEquals(OS_ARCH, osInfo.getOsArch());
        Assertions.assertEquals(DISTRIBUTION, osInfo.getDistribution());
        Assertions.assertEquals(DISTRIBUTION_RELEASE, osInfo.getDistributionRelease());
    }

    /**
     * Tests that a null pointer exception is thrown if OS name is null.
     */
    @Test
    public final void osNameNullTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new OSInfo(null, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE));
    }

    /**
     * Tests that a null pointer exception is thrown if OS version is null.
     */
    @Test
    public final void osVersionNullTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new OSInfo(OS_NAME, null, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE));
    }

    /**
     * Tests that a null pointer exception is thrown if OS arch is null.
     */
    @Test
    public final void osArchNullTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new OSInfo(OS_NAME, OS_VERSION, null, DISTRIBUTION, DISTRIBUTION_RELEASE));
    }

    /**
     * Tests that a null pointer exception is thrown if OS name is null.
     */
    @Test
    public final void osNameLongTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new OSInfo(LONG_OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE));
    }

    /**
     * Tests that a null pointer exception is thrown if OS version is null.
     */
    @Test
    public final void osVersionLongTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new OSInfo(OS_NAME, LONG_OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE));
    }

    /**
     * Tests that a null pointer exception is thrown if OS arch is null.
     */
    @Test
    public final void osArchLongTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new OSInfo(OS_NAME, OS_VERSION, LONG_OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE));
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
        Assertions.assertEquals(oi2.hashCode(), oi1.hashCode());
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
        Assertions.assertNotEquals(oi2.hashCode(), oi1.hashCode());
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
        Assertions.assertNotEquals(oi2.hashCode(), oi1.hashCode());
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
        Assertions.assertNotEquals(oi2.hashCode(), oi1.hashCode());
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
        Assertions.assertNotEquals(oi2.hashCode(), oi1.hashCode());
    }

    /**
     * Tests that two OSInfo objects with the same name, make, version, and
     * distribution information are equal.
     */
    @Test
    public final void testEqual() {
        OSInfo oi1 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        OSInfo oi2 = new OSInfo(OS_NAME, OS_VERSION, OS_ARCH, DISTRIBUTION, DISTRIBUTION_RELEASE);
        Assertions.assertEquals(oi2, oi1);
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
        Assertions.assertNotEquals(oi2, oi1);
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
        Assertions.assertNotEquals(oi2, oi1);
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
        Assertions.assertNotEquals(oi2, oi1);
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
        Assertions.assertNotEquals(oi2, oi1);
    }
}
