package hirs.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for VersionHelper.
 */
public class VersionHelperTest {

    /**
     * Test that case where a version file does not exist.
     */
    @Test
    public void testGetVersionFail() {

        String version = VersionHelper.getVersion("somefile");
        Assert.assertEquals(version, "");
    }

    /**
     * Test that a version file exists and can be read properly.
     */
    @Test
    public void testGetVersionDefault() {

        String expected = "Test.Version";
        String actual = VersionHelper.getVersion();
        Assert.assertEquals(actual, expected);
    }
}
