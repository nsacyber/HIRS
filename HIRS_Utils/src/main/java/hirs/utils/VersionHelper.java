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
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class to get the current version from the VERSION file.
 */
@Log4j2
public final class VersionHelper {

    private static final String OPT_PREFIX = "/opt";
    private static final String ETC_PREFIX = "/etc";
    private static final String VERSION = "VERSION";

    private VersionHelper() {
        // intentionally blank, should never be instantiated
    }

    /**
     * Get the current version of HIRS_AttestationPortal that is installed.
     *
     * @return A string representing the current version.
     */
    public static String getVersion() {
        if (Files.exists(FileSystems.getDefault().getPath(OPT_PREFIX,
                "hirs", "aca", VERSION))) {
            return getVersion(FileSystems.getDefault().getPath(OPT_PREFIX,
                    "hirs", "aca", VERSION));
        } else if (Files.exists(FileSystems.getDefault().getPath(ETC_PREFIX,
                "hirs", "aca", VERSION))) {
            return getVersion(FileSystems.getDefault().getPath(ETC_PREFIX,
                    "hirs", "aca", VERSION));
        }

        return getVersion(VERSION);
    }

    /**
     * Get the current version of HIRS_AttestationCAPortal that is installed.
     *
     * @param filename that contains the version
     * @return A string representing the current version.
     */
    public static String getVersion(final Path filename) {
        String version;
        try {
            version = getFileContents(filename.toString());
        } catch (IOException ioEx) {
            log.error(ioEx.getMessage());
            version = "";
        }

        return version;
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
            version = getResourceContents(filename);
        } catch (Exception ex) {
            version = "";
            log.error(ex.getMessage());
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

