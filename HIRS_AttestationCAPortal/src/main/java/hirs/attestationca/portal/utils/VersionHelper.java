package hirs.attestationca.portal.utils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.IOException;
import java.net.URL;

/**
 * Utility class to get the current version from the VERSION file.
 */
public final class VersionHelper {

    private static final String VERSION_FILENAME = "VERSION";

    private VersionHelper() {
        // intentionally blank, should never be instantiated
    }

    /**
     * Get the current version of HIRS_Portal that is installed.
     *
     * @return A string representing the current version.
     */
    public static String getVersion() {
        return getVersion(VERSION_FILENAME);
    }

    /**
     * Get the current version of HIRS_Portal that is installed.
     *
     * @param filename
     *            that contains the version
     * @return A string representing the current version.
     */
    public static String getVersion(final String filename) {
        String version;
        try {
            version = getFileContents(filename);
        } catch (Exception e) {
            version = "";
        }
        return version;
    }

    /**
     * Read the symbolic link to VERSION in the top level HIRS directory.
     * @param filename "VERSION"
     * @return the version number from the file
     * @throws java.io.IOException
     */
    private static String getFileContents(final String filename) throws IOException {
        URL url = Resources.getResource(filename);
        return Resources.toString(url, Charsets.UTF_8).trim();
    }
}

