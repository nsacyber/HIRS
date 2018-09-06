package hirs.repository.spacewalk;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;

import hirs.repository.RPMRepoPackage;
import hirs.repository.Repository;

/**
 * Class representing a Spacewalk RPM Package.
 */
@Entity
public class SpacewalkPackage extends RPMRepoPackage {

    @Column
    private int spacewalkPackageId;

    /**
     * Protected default constructor for Hibernate.
     */
    protected SpacewalkPackage() {
    }

    /**
     * Constructs a Spacewalk channel using a map. The expected keys are as defined in the Spacewalk
     * 2.4 API.
     *
     * @param packageMap
     *            the map of key/value pairs of spacewalk package fields.
     * @param sourceRepository
     *            the associated repository for this package.
     * @return the SpacewalkPackage
     * @throws IllegalArgumentException
     *             if an argument passed in is null
     */
    public static SpacewalkPackage buildSpacewalkPackage(final Map<String, Object> packageMap,
                    final Repository sourceRepository) throws IllegalArgumentException {

        if (null == packageMap) {
            throw new NullPointerException("packageMap is null");
        }
        if (null == sourceRepository) {
            throw new NullPointerException("sourceRepository is null");
        }
        String name = tryGet(packageMap, "name").toString();
        String version = tryGet(packageMap, "version").toString();
        String release = tryGet(packageMap, "release").toString();
        String architecture = tryGet(packageMap, "arch_label").toString();
        int spacewalkPackageId = (int) tryGet(packageMap, "id");

        return new SpacewalkPackage(name, version, release, architecture, sourceRepository,
                        spacewalkPackageId);
    }

    /**
     * Constructs a new SpacewalkPackage.
     *
     * @param name
     *            the package's name
     * @param version
     *            the package's version
     * @param release
     *            the package's release
     * @param architecture
     *            the package's target architecture
     * @param sourceRepository
     *            the package's source repository
     * @param spacewalkPackageId
     *            the Spacewalk package ID
     */
    public SpacewalkPackage(final String name, final String version, final String release,
                    final String architecture, final Repository sourceRepository,
                    final int spacewalkPackageId) {
        super(name, version, release, architecture, sourceRepository);
        this.spacewalkPackageId = spacewalkPackageId;
    }

    /**
     * Gets the Spacewalk package ID.
     * @return the spacewalk package ID.
     */
    public int getSpacewalkPackageId() {
        return spacewalkPackageId;
    }

    private static Object tryGet(final Map<String, Object> map, final Object key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("map does not contain the key: " + key);
        }
        return map.get(key);
    }


}
