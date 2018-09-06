package hirs.repository;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * This class tests the static methods of {@link RPMRepoPackage}.
 */
public class RPMRepoPackageTest {
    private static final String RPM_FILENAME = "vim-common-7.4.629-5.el6.x86_64.rpm";
    private static final String RPM_COMPLETE_VER = "7.4.629-5.el6.x86_64";
    private static final String RPM_NAME = "vim-common";
    private static final String RPM_VER = "7.4.629";
    private static final String RPM_REL = "5.el6";
    private static final String RPM_ARCH = "x86_64";

    private static final String INVALID_RPM_FILENAME = "malformatted.rpm";
    private static final String NOT_AN_RPM_FILENAME = "malformatted.rpm";

    /**
     * Tests that isRpmFilename works to identify properly formatted RPM filenames.
     */
    @Test
    public void testIsRpmFilename() {
        Assert.assertTrue(RPMRepoPackage.isRpmFilename(RPM_FILENAME));
        Assert.assertFalse(RPMRepoPackage.isRpmFilename(INVALID_RPM_FILENAME));
        Assert.assertFalse(RPMRepoPackage.isRpmFilename(NOT_AN_RPM_FILENAME));
    }

    /**
     * Tests that parseRPMCompleteVersion returns the correct full version string, or null if
     * the given filename is not a typical RPM filename.
     */
    @Test
    public void testParseRPMCompleteVersion() {
        Assert.assertEquals(RPMRepoPackage.parseRPMCompleteVersion(RPM_FILENAME), RPM_COMPLETE_VER);
        Assert.assertNull(RPMRepoPackage.parseRPMCompleteVersion(INVALID_RPM_FILENAME));
        Assert.assertNull(RPMRepoPackage.parseRPMCompleteVersion(NOT_AN_RPM_FILENAME));
    }

    /**
     * Tests that parseName returns the correct RPM name, or null if
     * the given filename is not a typical RPM filename.
     */
    @Test
    public void testParseName() {
        Assert.assertEquals(RPMRepoPackage.parseName(RPM_FILENAME), RPM_NAME);
        Assert.assertNull(RPMRepoPackage.parseName(INVALID_RPM_FILENAME));
        Assert.assertNull(RPMRepoPackage.parseName(NOT_AN_RPM_FILENAME));
    }

    /**
     * Tests that parseVersion returns the correct RPM version, or null if
     * the given filename is not a typical RPM filename.
     */
    @Test
    public void testParseVersion() {
        Assert.assertEquals(RPMRepoPackage.parseVersion(RPM_FILENAME), RPM_VER);
        Assert.assertNull(RPMRepoPackage.parseVersion(INVALID_RPM_FILENAME));
        Assert.assertNull(RPMRepoPackage.parseVersion(NOT_AN_RPM_FILENAME));
    }

    /**
     * Tests that parseRelease returns the correct RPM release, or null if
     * the given filename is not a typical RPM filename.
     */
    @Test
    public void testParseRelease() {
        Assert.assertEquals(RPMRepoPackage.parseRelease(RPM_FILENAME), RPM_REL);
        Assert.assertNull(RPMRepoPackage.parseRelease(INVALID_RPM_FILENAME));
        Assert.assertNull(RPMRepoPackage.parseRelease(NOT_AN_RPM_FILENAME));
    }

    /**
     * Tests that parseArchitecture returns the correct RPM architecture, or null if
     * the given filename is not a typical RPM filename.
     */
    @Test
    public void testParseArchitecture() {
        Assert.assertEquals(RPMRepoPackage.parseArchitecture(RPM_FILENAME), RPM_ARCH);
        Assert.assertNull(RPMRepoPackage.parseArchitecture(INVALID_RPM_FILENAME));
        Assert.assertNull(RPMRepoPackage.parseArchitecture(NOT_AN_RPM_FILENAME));
    }
}
