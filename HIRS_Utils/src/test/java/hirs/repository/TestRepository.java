package hirs.repository;

import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.persist.DBRepositoryManagerTest;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

/**
 * Serves as a mock Repository for testing repository functionality.
 */
@Entity
public class TestRepository extends Repository<RPMRepoPackage> {

    @Column
    private int measurementDelayMs;

    @Transient
    private int numRemotePackages;

    /**
     * Constructor for an empty repository.
     *
     * @param name the name of this test repository
     */
    public TestRepository(final String name) {
        super(name);
    }

    private TestRepository() {

    }

    @Override
    public final Set<RepoPackage> getUpdatedPackages(final RepoPackage oldPackage) {
        HashSet<RepoPackage> updatedPackages = new HashSet<>();
        for (RepoPackage pkg : getPackages()) {
            if (Integer.parseInt(pkg.getVersion()) > Integer.parseInt(oldPackage.getVersion())) {
                updatedPackages.add(pkg);
            }
        }
        return updatedPackages;
    }

    /**
     * Construct a new TestRepository.
     *
     * @param name the name of this repository
     * @param measurementDelayMs a simulated download & measurement delay, in ms
     */
    public TestRepository(final String name, final int measurementDelayMs) {
        super(name);
        this.measurementDelayMs = measurementDelayMs;
    }

    /**
     * Set the number of remote packages this repository should report.
     *
     * @param numRemotePackages the number of remote packages this repository should report
     */
    public final void setNumRemotePackages(final int numRemotePackages) {
        this.numRemotePackages = numRemotePackages;
    }

    /**
     * Mocks listing the remote packages in the repository.
     *
     * @return a Set of packages
     * @throws RepositoryException if an error is encountered listing the packages
     */
    @Override
    protected final Set<RPMRepoPackage> listRemotePackages() throws RepositoryException {
        int versionCounter = 0;
        Set<RPMRepoPackage> remotePackages = new HashSet<>();
        while (remotePackages.size() < numRemotePackages) {
            remotePackages.add(new RPMRepoPackage("TestPackage",
                    String.format("%d", ++versionCounter), "el6", "x86", this));
        }
        return remotePackages;
    }

    /**
     * Applies mock measurements to the given packages.
     *
     * @param repoPackage the package to measure
     * @param maxDownloadAttempts the package download attempt limit
     * @throws RepositoryException if an error is encountered measuring the packages
     */
    @Override
    protected final void measurePackage(final RPMRepoPackage repoPackage,
                                        final int maxDownloadAttempts)
            throws RepositoryException {
        Set<IMABaselineRecord> measurements = new HashSet<>();
        try {
            measurements.add(DBRepositoryManagerTest.getTestIMABaselineRecord());
            repoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        } catch (UnsupportedEncodingException e1) {
            throw new RepositoryException(e1);
        }

        try {
            Thread.sleep(measurementDelayMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
