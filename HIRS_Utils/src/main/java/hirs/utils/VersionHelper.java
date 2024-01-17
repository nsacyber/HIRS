package hirs.utils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.extern.log4j.Log4j2;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Utility class to get the current version from the VERSION file.
 */
@Log4j2
public final class VersionHelper {

    private static final Path VERSION_PATH = FileSystems.getDefault().getPath(
            "/opt", "hirs", "aca", "VERSION");

    private VersionHelper() {
        // intentionally blank, should never be instantiated
    }

    /**
     * Get the current version of HIRS_Portal that is installed.
     *
     * @return A string representing the current version.
     */
    public static String getVersion() {
        return getVersion(VERSION_PATH);
    }

    /**
     * Get the current version of HIRS_AttestationCAPortal that is installed.
     *
     * @param filename that contains the version
     * @return A string representing the current version.
     */
    public static String getVersion(final Path filename) {
        return getVersion(filename.toString());
    }

    /**
     * Get the current version of HIRS_AttestationCAPortal that is installed.
     *
     * @param filename that contains the version
     * @return A string representing the current version.
     */
    public static String getVersion(final String filename) {
        String version;
        try {
            version = getFileContents(filename);
        } catch (Exception ex) {
            try {
                version = getResourceContents(filename);
            } catch (Exception e) {
                version = "";
                log.error(e.getMessage());
            }
        }
        return version;
    }

    /**
     * Read the symbolic link to VERSION in the top level HIRS directory.
     *
     * @param filename "VERSION"
     * @return the version number from the file
     * @throws IOException
     */
    private static String getFileContents(final String filename) throws IOException {
        final char[] buffer = new char[8192];
        final StringBuilder result = new StringBuilder();
        InputStream inputStream = new FileInputStream(filename);

        try (Reader reader = new InputStreamReader(inputStream, Charsets.UTF_8)) {
            int charsRead;
            while ((charsRead = reader.read(buffer, 0, buffer.length)) > 0) {
                result.append(buffer, 0, charsRead);
            }
        }

        return result.toString();
    }

    /**
     * Read the symbolic link to VERSION in the top level HIRS directory.
     *
     * @param filename "VERSION"
     * @return the version number from the file
     * @throws IOException
     */
    private static String getResourceContents(final String filename) throws IOException {
        URL url = Resources.getResource(filename);
        return Resources.toString(url, Charsets.UTF_8).trim();
    }
}

