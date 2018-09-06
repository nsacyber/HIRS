package hirs.data.persist;

import static org.apache.logging.log4j.LogManager.getLogger;

import hirs.appraiser.Appraiser;
import hirs.appraiser.DeviceInfoAppraiser;
import hirs.appraiser.IMAAppraiser;
import hirs.appraiser.TPMAppraiser;

import java.util.HashSet;

import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * HIRSPolicyTest is a unit test class for HIRSPolicy. The super class methods
 * from <code>HibernateTest</code> are used to test persistence.
 */
public class HIRSPolicyTest extends HibernateTest<HIRSPolicy> {

    private static final Logger LOGGER = getLogger(HIRSPolicyTest.class);
    private HIRSPolicy policy;
    /**
     * Initializes a <code>HIRSPolicy</code> with all the report types required.
     */
    @BeforeClass
    public final void initializePolicy() {
        policy = getTestPolicy();
    }

    /**
     * Makes a new policy to start fresh for the next test.
     */
    @AfterMethod
    public final void resetTestPolicy() {
        LOGGER.debug("resetting hirs policy");
        policy = getTestPolicy();
    }

    /**
     * Tests that constructor throws an exception when a null name is given.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void hirsPolicyNullName() {
        new HIRSPolicy(null);
    }

    /**
     * Tests that the set of <code>DeviceInfoAppraiser</code>,
     * <code>TPMAppraiser</code>, and <code>IMAAppraiser</code> can be set and
     * gotten. Also tests that the dependency check passes appropriately.
     */
    @Test
    public final void setDeviceTPMIMA() {
        HashSet<Class<? extends Appraiser>> appraiserSet =
                new HashSet<>();
        appraiserSet.add(DeviceInfoAppraiser.class);
        appraiserSet.add(TPMAppraiser.class);
        appraiserSet.add(IMAAppraiser.class);
        policy.setRequiredAppraisers(appraiserSet);
        appraiserSet = new HashSet<>(policy.
                getRequiredAppraisers());
        Assert.assertTrue(appraiserSet.contains(DeviceInfoAppraiser.class));
        Assert.assertTrue(appraiserSet.contains(TPMAppraiser.class));
        Assert.assertTrue(appraiserSet.contains(IMAAppraiser.class));
    }

    /**
     * Tests that the set of <code>DeviceInfoAppraiser</code> and
     * <code>TPMAppraiser</code> can be set and gotten. Also tests that the
     * dependency check passes appropriately.
     */
    @Test
    public final void setDeviceTPM() {
        HashSet<Class<? extends Appraiser>> appraiserSet =
                new HashSet<>();
        appraiserSet.add(DeviceInfoAppraiser.class);
        appraiserSet.add(TPMAppraiser.class);
        policy.setRequiredAppraisers(appraiserSet);
        appraiserSet = new HashSet<>(policy.
                getRequiredAppraisers());
        Assert.assertTrue(appraiserSet.contains(DeviceInfoAppraiser.class));
        Assert.assertTrue(appraiserSet.contains(TPMAppraiser.class));
    }

    /**
     * Tests that the set of <code>DeviceInfoAppraiser</code> and
     * <code>IMAAppraiser</code> can be set and gotten. Also tests that the
     * dependency check passes appropriately.
     */
    @Test
    public final void setDeviceIMA() {
        HashSet<Class<? extends Appraiser>> appraiserSet =
                new HashSet<>();
        appraiserSet.add(DeviceInfoAppraiser.class);
        appraiserSet.add(IMAAppraiser.class);
        policy.setRequiredAppraisers(appraiserSet);
        appraiserSet = new HashSet<>(policy.
                getRequiredAppraisers());
        Assert.assertTrue(appraiserSet.contains(DeviceInfoAppraiser.class));
        Assert.assertTrue(appraiserSet.contains(IMAAppraiser.class));
    }

    /**
     * Tests that the set of just a <code>DeviceInfoAppraiser</code> can be set
     * and gotten. Also tests that the dependency check passes appropriately.
     */
    @Test
    public final void setDevice() {
        HashSet<Class<? extends Appraiser>> appraiserSet =
                new HashSet<>();
        appraiserSet.add(DeviceInfoAppraiser.class);
        appraiserSet.add(TPMAppraiser.class);
        policy.setRequiredAppraisers(appraiserSet);
        appraiserSet = new HashSet<>(policy.
                getRequiredAppraisers());
        Assert.assertTrue(appraiserSet.contains(DeviceInfoAppraiser.class));
        Assert.assertTrue(appraiserSet.contains(TPMAppraiser.class));
    }

    /**
     * Make a test policy that can be used in this class and other test classes.
     * This one contains all three appraisers.
     * @return a test policy
     */
    public static final HIRSPolicy getTestPolicy() {
        HIRSPolicy testPolicy = new HIRSPolicy("Unit Test HIRS Policy");
        HashSet<Class<? extends Appraiser>> appraiserSet = new HashSet<>();
        appraiserSet.add(DeviceInfoAppraiser.class);
        appraiserSet.add(TPMAppraiser.class);
        appraiserSet.add(IMAAppraiser.class);
        testPolicy.setRequiredAppraisers(appraiserSet);
        return testPolicy;
    }

    @Override
    protected final Class<?> getDefaultClass() {
        return policy.getClass();
    }

    @Override
    protected final HIRSPolicy getDefault(final Session session) {
        return policy;
    }

    @Override
    protected final void update(final HIRSPolicy object) {
        HashSet<Class<? extends Appraiser>> appraiserSet =
                new HashSet<>();
        appraiserSet.add(DeviceInfoAppraiser.class);
        appraiserSet.add(TPMAppraiser.class);
        object.setRequiredAppraisers(appraiserSet);
    }

    @Override
    protected final void assertGetEqual(final HIRSPolicy defaultObject,
            final HIRSPolicy retrieved) {
        Assert.assertEquals(defaultObject, retrieved);
        Assert.assertEquals(defaultObject.getRequiredAppraisers(),
                retrieved.getRequiredAppraisers());
    }

    @Override
    protected final void assertUpdateEqual(final HIRSPolicy object,
            final HIRSPolicy update) {
      Assert.assertFalse(update.getRequiredAppraisers().contains(
              IMAAppraiser.class));
    }

    @Override
    protected final Class<?>[] getCleanupClasses() {
        return new Class<?>[] {HIRSPolicy.class};
    }
}
