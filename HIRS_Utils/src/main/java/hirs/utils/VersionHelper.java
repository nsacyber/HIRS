package hirs.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;

/**
 * Utility class to get the current version from the VERSION file.
 */
public final class VersionHelper {

    private static final Logger LOGGER = LogManager.getLogger(VersionHelper.class);
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
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.warn("Error reading version", e);
            version = "";
        } catch (NullPointerException e) {
            LOGGER.warn("File not found: " + filename);
            version = "";
        }
        return version;
    }

    /**
     * Read the symbolic link to VERSION in the top level HIRS directory.
     * @param filename "VERSION"
     * @return the version number from the file
     * @throws IOException
     */
    private static String getFileContents(final String filename) throws IOException {

        File versionFileLink = new File(VersionHelper.class.getClassLoader()
                                            .getResource(filename).getFile());
        String versionFilePath = versionFileLink.getCanonicalPath();
        BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(
                                         new FileInputStream(versionFilePath), "UTF-8"));
        String version = reader.readLine();
        reader.close();

        return version;
    }
}
