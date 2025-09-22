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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class to get the current version from the VERSION file.
 */
@Log4j2
public final class VersionHelper {
    private static final String VERSION = "VERSION";

    private static final Path LINUX_OPT_PATH = Paths.get(
            System.getenv().getOrDefault("HIRS_LINUX_OPT", "/opt/hirs/aca/" + VERSION)
    );

    private static final Path LINUX_ETC_PATH = Paths.get(
            System.getenv().getOrDefault("HIRS_LINUX_ETC", "/etc/hirs/aca/" + VERSION)
    );

    private static final Path WINDOWS_PATH = Paths.get(
            System.getenv().getOrDefault("HIRS_WIN_PATH", "C:/ProgramData/hirs/aca/" + VERSION)
    );

    private static final int FILE_BUFFER_SIZE = 8192;

    private VersionHelper() {
        // intentionally blank, should never be instantiated
    }

    /**
     * Get the current version of HIRS_AttestationPortal that is installed.
     *
     * @return A string representing the current version.
     */
    public static String getVersion() {
        final String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win") && Files.exists(WINDOWS_PATH)) {
            return getVersion(WINDOWS_PATH);
        } else if (osName.contains("nux") || osName.contains("nix")) {
            if (Files.exists(LINUX_OPT_PATH)) {
                return getVersion(LINUX_OPT_PATH);
            } else if (Files.exists(LINUX_ETC_PATH)) {
                return getVersion(LINUX_ETC_PATH);
            }
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
            log.info(ioEx.getMessage());
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
     * @throws IOException if there are any issues attempting to read the input stream
     */
    private static String getFileContents(final String filename) throws IOException {
        final char[] buffer = new char[FILE_BUFFER_SIZE];
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
     * @throws IOException if there are any issues attempting to retrieve the resource contents
     */
    private static String getResourceContents(final String filename) throws IOException {
        URL url = Resources.getResource(filename);
        return Resources.toString(url, Charsets.UTF_8).trim();
    }
}

