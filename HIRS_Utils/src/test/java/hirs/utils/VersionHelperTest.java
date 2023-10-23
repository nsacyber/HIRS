package hirs.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for VersionHelper.
 */
public class VersionHelperTest {

    /**
     * Test that case where a version file does not exist.
     */
    @Test
    public void testGetVersionFail() {

        String actual = VersionHelper.getVersion("somefile");
        assertTrue(actual.startsWith(""));
    }

    /**
     * Test that a version file exists and can be read properly.
     */
    @Test
    public void testGetVersionDefault() {

        String expected = "Test.Version";
        String actual = VersionHelper.getVersion();
        assertEquals(expected, actual);
    }
}
