package hirs.repository;

import hirs.data.persist.Digest;
import hirs.data.persist.DigestAlgorithm;
import hirs.data.persist.IMABaselineRecord;
import hirs.repository.measurement.PackageMeasurer;
import hirs.repository.measurement.RPMMeasurer;
import hirs.utils.exec.ExecBuilder;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Transient;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;

import javax.persistence.MappedSuperclass;

/**
 * Abstract base class representing a repository that deals with RPM packages.
 *
 * @param <T> the type of packages an implementing Repository contains
 */
@MappedSuperclass
public abstract class RPMRepository<T extends RPMRepoPackage> extends Repository<T> {

    private static final Logger LOGGER = LogManager.getLogger(RPMRepository.class);

    private static final int VER_CMP_EQUAL = 0;
    private static final int VER_CMP_FIRST_NEWER = 11;
    private static final int VER_CMP_SECOND_NEWER = 12;


    @Column
    private URL baseUrl;

    @Transient
    private PackageMeasurer rpmMeasurer = new RPMMeasurer(DigestAlgorithm.SHA1);

    /**
     * Construct a new Repository with a given name.
     *
     * @param name the name by which this Repository will be referenced
     */
    public RPMRepository(final String name) {
        super(name);
    }

    /**
     * Construct a new Repository with a given name.
     *
     * @param name the name by which this Repository will be referenced
     * @param baseUrl the base URL
     */
    public RPMRepository(final String name, final URL baseUrl) {
        super(name);
        if (null == baseUrl) {
            throw new NullPointerException("null baseUrl");
        }
        this.baseUrl = baseUrl;
    }

    /**
     * Protected default constructor for Hibernate.
     */
    protected RPMRepository() {
        super();
    }

    /**
     * Gets the repository's base URL.
     *
     * @return the repository's base URL.
     */
    public final URL getBaseUrl() {
        return baseUrl;
    }

    /**
     * Sets the repository's base URL.
     *
     * @param baseUrl the repository's base URL.
     */
    public final void setBaseUrl(final URL baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Search this repository for every measured package that is an update of the given package.
     *
     * @param oldPackage the package to find updates for
     * @return a <code>Set</code> of <code>RepoPackage</code>s that are updates of the given package
     */
    @Override
    public final Set<RepoPackage> getUpdatedPackages(final RepoPackage oldPackage) {
        Set<RepoPackage> updatedPackages = new HashSet<>();
        for (RepoPackage newPackage : this.getPackages()) {
            if (isUpdatedRPMPackage((RPMRepoPackage) oldPackage, (RPMRepoPackage) newPackage)) {
                updatedPackages.add(newPackage);
            }
        }
        return updatedPackages;
    }

    /**
     * Measures the package at the specified path.
     * @param repoPackage the repo package
     * @param rpmPath the path to the RPM
     * @throws RepositoryException if an IO error occurs processing the package.
     */
    protected void measurePackageAtPath(final RPMRepoPackage repoPackage, final Path rpmPath)
            throws RepositoryException {
        try {
            Multimap<Path, Digest> measurements = rpmMeasurer.measure(rpmPath);
            Set<IMABaselineRecord> packageRecords = new HashSet<>();
            for (Map.Entry<Path, Collection<Digest>> e : measurements.asMap().entrySet()) {
                for (Digest digest : e.getValue()) {
                    packageRecords.add(new IMABaselineRecord(
                            e.getKey().toAbsolutePath().toString(), digest
                    ));
                }
            }

            repoPackage.setAllMeasurements(packageRecords, rpmMeasurer.measureFile(rpmPath));
        } catch (IOException e) {
            LOGGER.error("IO Exception measuring package", e);
            throw new RepositoryException(e);
        } finally {
            try {
                Files.delete(rpmPath);
            } catch (IOException e) {
                LOGGER.error("Error deleting RPM " + rpmPath, e);
            }
        }
    }

    /**
     * Compare two packages to determine if the second is an updated version of the first. First
     * this checks whether the package name and architecture are the same. Then a version comparison
     * binary from rpmdevtools called rpmdev-vercmp is called to check the release and version
     * strings. This tool will give an exit status indicating which package is newer. This method
     * examines the result and returns a boolean representing whether or not the second package is
     * any release of a newer version or a newer release of the same version.
     *
     * The exit codes for rpmdev-vercmp are:
     * <ul>
     * <li>0 if the versions are equal</li>
     * <li>11 if the first is newer</li>
     * <li>12 if the second is newer</li>
     * </ul>
     *
     * @param oldPackage the old package to check against potential updates
     * @param newPackage the new package to check as a potential update of the old package
     * @return true if newPackage is an updated version of oldPackage, false otherwise
     */
    private static boolean isUpdatedRPMPackage(final RPMRepoPackage oldPackage,
            final RPMRepoPackage newPackage) {
        final int[] validExitCodes = new int[]{0, 11, 12};
        if (!oldPackage.getName().equals(newPackage.getName())
                || !oldPackage.getArchitecture().equals(newPackage.getArchitecture())) {
            return false;
        }

        int exitStatus;

        try {
            exitStatus = new ExecBuilder("rpmdev-vercmp")
                    .args(oldPackage.getRPMIdentifier(), newPackage.getRPMIdentifier())
                    .exitValues(validExitCodes)
                    .exec().getExitStatus();
        } catch (IOException e) {
            throw new RuntimeException("Error running rpmdev-vercmp", e);
        }

        switch (exitStatus) {
            case VER_CMP_EQUAL: return false;
            case VER_CMP_FIRST_NEWER: return false;
            case VER_CMP_SECOND_NEWER: return true;
            default: throw new RuntimeException("Unexpected exit status from rpmdev-vercmp");
        }
    }

    /**
     * Gets a temporary directory for extracting RPM packages to. The directory is queued for
     * deletion upon program exit
     * @return Path for temporary RPM extraction
     * @throws IOException if an IO exception occurs creating the temp directory.
     */
    protected static Path getTemporaryDirectoryForRPMExtraction()
            throws IOException {
        final Path tempPackageDirectory =
                    Files.createTempDirectory(
                            FileSystems.getDefault().getPath(FileUtils.getTempDirectoryPath()),
                            "hirs-rpm-extraction-");


        // recursively delete repo directory on exit
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    FileUtils.deleteDirectory(tempPackageDirectory.toFile());
                } catch (IOException e) {
                    LOGGER.warn("Failed to remove temp dir: " + tempPackageDirectory.toString(), e);
                }
            }
        });
        return tempPackageDirectory;
    }
}
