package hirs.repository;

import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.baseline.IMABaselineRecord;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for the <code>RepoPackage</code> class.
 */
public class RepoPackageTest {
    private static final String NAME = "test-package";
    private static final String VERSION = "1.2.3";
    private static final String ARCHITECTURE = "x86";
    private static final String RELEASE = "el6";
    private static final Repository REPOSITORY = new Repository("TestRepo") {
        @Override
        public Set getUpdatedPackages(final RepoPackage repoPackage) {
            return null;
        }
        @Override
        protected Set listRemotePackages() throws RepositoryException {
            return null;
        }

        @Override
        protected void measurePackage(final RepoPackage repoPackage, final int maxDownloadAttempts)
                throws RepositoryException {

        }
    };
    private static final String PATH = new File("/tmp/test").toPath().toAbsolutePath().toString();
    private static final String DIGEST_STRING = "c068d837b672cb3f80ac";

    private RepoPackage repoPackage;

    /**
     * Initializes a basic RepoPackage.
     *
     * @throws Exception if creating a RepoPackage fails.
     */
    @BeforeMethod
    public final void setUp() throws Exception {
        repoPackage = new RPMRepoPackage(NAME, VERSION, RELEASE, ARCHITECTURE, REPOSITORY);
    }

    /**
     * Removes the reference to the used RepoPackage.
     */
    @AfterMethod
    public final void tearDown() {
        repoPackage = null;
    }

    /**
     * Tests that getName returns the correct name.
     */
    @Test
    public final void testGetName() {
        Assert.assertEquals(repoPackage.getName(), NAME);
    }

    /**
     * Tests that getVersion returns the correct version.
     */
    @Test
    public final void testGetVersion() {
        Assert.assertEquals(repoPackage.getVersion(), VERSION);
    }

    /**
     * Tests that getRelease returns the correct release.
     */
    @Test
    public final void testGetRelease() {
        Assert.assertEquals(repoPackage.getRelease(), RELEASE);
    }

    /**
     * Tests that getArchitecture returns the correct architecture.
     */
    @Test
    public final void testGetArchitecture() {
        Assert.assertEquals(repoPackage.getArchitecture(), ARCHITECTURE);
    }

    /**
     * Tests that getSourceRepository returns the correct source repository.
     */
    @Test
    public final void testGetSourceRepository() {
        Assert.assertEquals(repoPackage.getSourceRepository(), REPOSITORY);
    }

    /**
     * Tests that setAllMeasurements sets measurements without error.
     *
     * @throws UnsupportedEncodingException if UTF-8 isn't supported on the platform.
     */
    @Test
    public final void testSetMeasurements() throws UnsupportedEncodingException {
        repoPackage.setAllMeasurements(new HashSet<IMABaselineRecord>(), getTestDigest());
    }

    /**
     * Tests that setAllMeasurements cannot be called twice without throwing a RuntimeException.
     *
     * @throws UnsupportedEncodingException if UTF-8 isn't supported on the platform.
     */
    @Test(expectedExceptions = RuntimeException.class)
    public final void testSetMeasurementsTwice() throws UnsupportedEncodingException {
        repoPackage.setAllMeasurements(new HashSet<IMABaselineRecord>(), getTestDigest());
        repoPackage.setAllMeasurements(new HashSet<IMABaselineRecord>(), getTestDigest());
    }

    /**
     * Tests that getMeasurements returns the correct measurements once they're set.
     *
     * @throws UnsupportedEncodingException if UTF-8 isn't supported on the platform.
     */
    @Test
    public final void testGetMeasurements() throws UnsupportedEncodingException {
        Set<IMABaselineRecord> measurements = new HashSet<>();
        IMABaselineRecord imaBaselineRecord = new IMABaselineRecord(PATH, getTestDigest());
        measurements.add(imaBaselineRecord);
        repoPackage.setAllMeasurements(measurements, getTestDigest());
        Assert.assertTrue(repoPackage.getPackageRecords().contains(imaBaselineRecord));
        Assert.assertEquals(repoPackage.getPackageMeasurement(), getTestDigest());
    }

    /**
     * Tests that calling getMeasurements before the measurements are set results in a
     * RuntimeException.
     */
    @Test(expectedExceptions = RuntimeException.class)
    public final void testGetPackageRecordsWithoutBeingSet() {
        repoPackage.getPackageRecords();
    }

    /**
     * Tests that isMeasured returns the correct status before and after measurement.
     *
     * @throws UnsupportedEncodingException if UTF-8 isn't supported on the platform.
     */
    @Test
    public final void testIsMeasured() throws UnsupportedEncodingException {
        Assert.assertFalse(repoPackage.isMeasured());
        repoPackage.setAllMeasurements(new HashSet<IMABaselineRecord>(), getTestDigest());
        Assert.assertTrue(repoPackage.isMeasured());
    }

    /**
     * Tests that getMeasurementDate returns a Date object if measurements have been set.
     *
     * @throws UnsupportedEncodingException if UTF-8 isn't supported on the platform.
     */
    @Test
    public final void testGetMeasurementDate() throws UnsupportedEncodingException {
        repoPackage.setAllMeasurements(new HashSet<IMABaselineRecord>(), getTestDigest());
        Assert.assertNotNull(repoPackage.getMeasurementDate());
    }

    /**
     * Tests that getMeasurementDate returns null if measurements haven't been set.
     */
    @Test
    public final void testGetMeasurementDateWithoutBeingSet() {
        Assert.assertNull(repoPackage.getMeasurementDate());
    }

    /**
     * Generates a test digest.
     * @return the generated digest
     * @throws UnsupportedEncodingException when an unsupported encryption algorithm is requested
     */
    public static Digest getTestDigest() throws UnsupportedEncodingException {
        return new Digest(DigestAlgorithm.SHA1, DIGEST_STRING.getBytes("UTF-8"));
    }
}
