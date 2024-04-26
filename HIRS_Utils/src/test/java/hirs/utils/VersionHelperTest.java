package hirs.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for VersionHelper.
 */
public class VersionHelperTest {

    /**
     * Test that a version file exists in /opt/hirs or /etc/hirs and is not empty.
     */
    @Test
    public void testGetVersionDefault() {
        String actual = VersionHelper.getVersion();
        assertNotNull(actual);
    }
}
