package hirs.repository;

import javax.persistence.Entity;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is a small layer on top of RepoPackage to aid with the processing of RPMs.
 */
@Entity
public class RPMRepoPackage extends RepoPackage {
    private static final Pattern RPM_FILENAME
            = Pattern.compile("(?<name>\\S+)-(?<version>\\S+)-"
            + "(?<release>\\S+)\\.(?<architecture>\\S+)\\.rpm");

    /**
     * Constructs an RPMRepoPackage.
     *
     * @param name             the package's name
     * @param version          the package's version
     * @param release          the package's release
     * @param architecture     the package's target architecture
     * @param sourceRepository the package's source repository
     */
    public RPMRepoPackage(final String name, final String version, final String release,
                          final String architecture, final Repository sourceRepository) {
        super(name, version, release, architecture, sourceRepository);
    }

    /**
     * Protected default constructor for Hibernate.
     */
    protected RPMRepoPackage() {
        super();
    }

    /**
     * Returns the conventional RPM name associated with this RPM, as would be used in its
     * filename.
     *
     * @return the RPM identifier
     */
    public final String getRPMIdentifier() {
        return getRPMIdentifier(getName(), getVersion(), getRelease(), getArchitecture());
    }

    /**
     * Returns the conventional RPM name associated with the given details, as would be used in its
     * filename.
     *
     * @param name an RPM's name
     * @param version an RPM's version
     * @param release an RPM's release string
     * @param architecture an RPM's architecture
     * @return the RPM identifier
     */
    private static String getRPMIdentifier(
            final String name,
            final String version,
            final String release,
            final String architecture) {
        return String.format("%s-%s-%s.%s", name, version, release, architecture);
    }

    /**
     * This function parses an RPM filename and returns a 'complete' version string in the format
     * version-release.architecture.
     *
     * @param filename an RPM's filename
     * @return the RPM's complete version, or null if the filename does not match the typical RPM
     *         format
     */
    public static String parseRPMCompleteVersion(final String filename) {
        if (!isRpmFilename(filename)) {
            return null;
        }
        return String.format("%s-%s.%s",
                parseVersion(filename),
                parseRelease(filename),
                parseArchitecture(filename)
        );
    }

    /**
     * Parse the name from the given RPM filename (ie, given kernel-2.6.32-642.6.1.el6.x86_64.rpm,
     * the string "kernel" would be returned).
     *
     * @param filename an RPM's filename
     * @return the name of the RPM, or null if the filename does not match the typical RPM format
     */
    public static String parseName(final String filename) {
        return parseItem(filename, "name");
    }

    /**
     * Parse the version from the given RPM filename (ie, given
     * kernel-2.6.32-642.6.1.el6.x86_64.rpm, the string "2.6.32" would be returned).
     *
     * @param filename an RPM's filename
     * @return the RPM's version, or null if the filename does not match the typical RPM format
     */
    public static String parseVersion(final String filename) {
        return parseItem(filename, "version");
    }

    /**
     * Parse the release from the given RPM filename (ie, given
     * kernel-2.6.32-642.6.1.el6.x86_64.rpm, the string "642.6.1.el6" would be returned).
     *
     * @param filename an RPM's filename
     * @return the RPM's release, or null if the filename does not match the typical RPM format
     */
    public static String parseRelease(final String filename) {
        return parseItem(filename, "release");
    }

    /**
     * Parse the architecture from the given RPM filename (ie, given
     * kernel-2.6.32-642.6.1.el6.x86_64.rpm, the string "x86_64" would be returned).
     *
     * @param filename an RPM's filename
     * @return the RPM's architecture, or null if the filename does not match the typical RPM format
     */
    public static String parseArchitecture(final String filename) {
        return parseItem(filename, "architecture");
    }

    /**
     * Returns true if the given filename matches the expected RPM format, or false otherwise.
     *
     * @param filename an RPM's filename
     * @return true if the given filename matches the expected RPM format, or false otherwise.
     */
    public static boolean isRpmFilename(final String filename) {
        return RPM_FILENAME.matcher(filename).matches();
    }

    private static String parseItem(final String filename, final String group) {
        Matcher matcher = RPM_FILENAME.matcher(filename);
        if (matcher.matches()) {
            return matcher.group(group);
        } else {
            return null;
        }
    }
}
