package hirs.utils;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

import java.net.URL;

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
        URL url = Resources.getResource("VERSION");
        String expected = "Test.Version";
        String actual = VersionHelper.getVersion(url.getPath());
        assertEquals(expected, actual);
    }
}
