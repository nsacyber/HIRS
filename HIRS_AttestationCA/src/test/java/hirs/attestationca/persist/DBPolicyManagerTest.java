package hirs.attestationca.persist;

import hirs.appraiser.Appraiser;
import hirs.appraiser.TestAppraiser;
import hirs.attestationca.data.persist.DeviceTest;
import hirs.attestationca.servicemanager.DBDeviceManager;
import hirs.attestationca.servicemanager.DBPolicyManager;
import hirs.data.persist.Device;
import hirs.data.persist.policy.Policy;
import hirs.data.persist.TestPolicy;
import hirs.data.persist.TestPolicy2;
import hirs.persist.DeviceManager;
import hirs.persist.PolicyManager;
import hirs.persist.PolicyManagerException;
import hirs.persist.PolicyMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.List;

/**
 * <code>DBPolicyManagerTest</code> is a unit test class for the
 * <code>DBPolicyManager</code> class.
 */
public final class DBPolicyManagerTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(DBPolicyManagerTest.class);
    private static final String POLICY_NAME = "Test Policy";

    private Appraiser appraiser;

    /**
     * Creates a new <code>DBPolicyManagerTest</code>.
     */
    public DBPolicyManagerTest() {
        /* do nothing */
    }

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public void setup() {
        LOGGER.debug("retrieving session factory");
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * Creates an <code>Appraiser</code> and stores it in the database.
     */
    @BeforeMethod
    public void testSetup() {
        LOGGER.debug("setting up DBPolicyManager tests");
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();
        appraiser = new TestAppraiser("Test Appraiser");
        final Serializable id = session.save(appraiser);
        appraiser = (Appraiser) session.get(TestAppraiser.class, id);
        session.getTransaction().commit();
    }

    /**
     * Resets the test state to a known good state. This currently only resets
     * the database by removing all <code>Policy</code> objects.
     */
    @AfterMethod
    public void resetTestState() {
        LOGGER.debug("reset test state");
        LOGGER.debug("deleting all policies and appraisers");
        resetPolicyMapperTestState();
        resetAppraiserTestState();
        resetPolicyTestState();
        resetDeviceTestState();
        resetDeviceGroupTestState();
    }

    private void resetPolicyMapperTestState() {
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<PolicyMapper> criteriaQuery = builder.createQuery(PolicyMapper.class);
        Root<PolicyMapper> root = criteriaQuery.from(PolicyMapper.class);
        criteriaQuery.select(root);
        Query<PolicyMapper> query = session.createQuery(criteriaQuery);
        List<PolicyMapper> objects = query.getResultList();

        for (Object o : objects) {
            LOGGER.debug("deleting object: {}", o);
            session.delete(o);
        }
        LOGGER.debug("all {} removed", PolicyMapper.class);
        session.getTransaction().commit();
    }

    private void resetAppraiserTestState() {
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Appraiser> criteriaQuery = builder.createQuery(Appraiser.class);
        Root<Appraiser> root = criteriaQuery.from(Appraiser.class);
        criteriaQuery.select(root);
        Query<Appraiser> query = session.createQuery(criteriaQuery);
        List<Appraiser> objects = query.getResultList();

        for (Object o : objects) {
            LOGGER.debug("deleting object: {}", o);
            session.delete(o);
        }
        LOGGER.debug("all {} removed", Appraiser.class);
        session.getTransaction().commit();
    }

    private void resetPolicyTestState() {
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Policy> criteriaQuery = builder.createQuery(Policy.class);
        Root<Policy> root = criteriaQuery.from(Policy.class);
        criteriaQuery.select(root);
        Query<Policy> query = session.createQuery(criteriaQuery);
        List<Policy> objects = query.getResultList();

        for (Object o : objects) {
            LOGGER.debug("deleting object: {}", o);
            session.delete(o);
        }
        LOGGER.debug("all {} removed", Policy.class);
        session.getTransaction().commit();
    }

    private void resetDeviceTestState() {
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Device> criteriaQuery = builder.createQuery(Device.class);
        Root<Device> root = criteriaQuery.from(Device.class);
        criteriaQuery.select(root);
        Query<Device> query = session.createQuery(criteriaQuery);
        List<Device> objects = query.getResultList();

        for (Object o : objects) {
            LOGGER.debug("deleting object: {}", o);
            session.delete(o);
        }
        LOGGER.debug("all {} removed", Device.class);
        session.getTransaction().commit();
    }

    private void resetDeviceGroupTestState() {
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<DeviceGroup> criteriaQuery = builder.createQuery(DeviceGroup.class);
        Root<DeviceGroup> root = criteriaQuery.from(DeviceGroup.class);
        criteriaQuery.select(root);
        Query<DeviceGroup> query = session.createQuery(criteriaQuery);
        List<DeviceGroup> objects = query.getResultList();

        for (Object o : objects) {
            LOGGER.debug("deleting object: {}", o);
            session.delete(o);
        }
        LOGGER.debug("all {} removed", DeviceGroup.class);
        session.getTransaction().commit();
    }

    /**
     * Tests that the <code>DBPolicyManager</code> can save a
     * <code>Policy</code>.
     *
     * @throws hirs.persist.PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testSave() throws PolicyManagerException {
        final TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        final TestPolicy p2 = (TestPolicy) mgr.savePolicy(policy);
        Assert.assertEquals(p2, policy);
        Assert.assertTrue(isInDatabase(POLICY_NAME));
    }

    /**
     * Tests that the <code>DBPolicyManager</code> throws a
     * <code>PolicyManagerException</code> if a <code>Policy</code> is saved
     * twice.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test(expectedExceptions = PolicyManagerException.class)
    public void testSaveTwice() throws PolicyManagerException {
        final TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        final TestPolicy p2 = (TestPolicy) mgr.savePolicy(policy);
        mgr.savePolicy(p2);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that the <code>DBPolicyManager</code> throws a
     * <code>PolicyManagerException</code> if a <code>Policy</code> is saved
     * with the same name as an existing <code>Policy</code>.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test(expectedExceptions = PolicyManagerException.class)
    public void testSaveSameName() throws PolicyManagerException {
        final TestPolicy policy = new TestPolicy(POLICY_NAME);
        final TestPolicy policy2 = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        final TestPolicy p2 = (TestPolicy) mgr.savePolicy(policy);
        Assert.assertNotNull(p2);
        mgr.savePolicy(policy2);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that the <code>DBPolicyManager</code> throws a
     * <code>PolicyManagerException</code> if the policy parameter is null.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSaveNullPolicy() throws PolicyManagerException {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        mgr.savePolicy(null);
        Assert.fail("save did not fail");
    }

    /**
     * Tests that the <code>DBPolicyManager</code> can update a
     * <code>Policy</code>.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testUpdate() throws PolicyManagerException {
        final String updatedName = "Updated Policy";
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        final Policy policy = createPolicy(mgr);
        policy.setName(updatedName);
        mgr.updatePolicy(policy);
        Assert.assertTrue(isInDatabase(updatedName));
        Assert.assertFalse(isInDatabase(POLICY_NAME));
    }

    /**
     * Tests that the <code>DBPolicyManager</code> fails to update a
     * <code>Policy</code> that has the same name as an existing
     * <code>Policy</code>.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testUpdateSameName() throws PolicyManagerException {
        final String name1 = "Test Policy 1";
        final String name2 = "Test Policy 2";
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        createPolicy(mgr, name1);
        final Policy p2 = createPolicy(mgr, name2);
        p2.setName(name1);
        PolicyManagerException expected = null;
        try {
            mgr.updatePolicy(p2);
        } catch (PolicyManagerException e) {
            expected = e;
        }
        Assert.assertNotNull(expected);
        Assert.assertTrue(isInDatabase(name1));
        Assert.assertTrue(isInDatabase(name2));
    }

    /**
     * Tests that the <code>DBPolicyManager</code> throws a
     * <code>PolicyManagerException</code> if the policy parameter is null.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testUpdateNullPolicy() throws PolicyManagerException {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        mgr.updatePolicy(null);
        Assert.fail("save did not fail");
    }

    /**
     * Tests that the <code>DBPolicyManager</code> can get a <code>Policy</code>
     * .
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGet() throws PolicyManagerException {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        Assert.assertNull(mgr.getPolicy(POLICY_NAME));
        final Policy policy = createPolicy(mgr);
        final Policy getPolicy = mgr.getPolicy(POLICY_NAME);
        Assert.assertNotNull(getPolicy);
        Assert.assertEquals(getPolicy, policy);
        Assert.assertEquals(getPolicy.getId(), policy.getId());
    }

    /**
     * Tests that the <code>DBPolicyManager</code> returns null when an unknown
     * <code>Policy</code> is searched for.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetUnknown() throws PolicyManagerException {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        Assert.assertNull(mgr.getPolicy("Some Unknown Policy"));
    }

    /**
     * Tests that the <code>DBPolicyManager</code> returns null when the name is
     * set to null.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetNull() throws PolicyManagerException {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        Assert.assertNull(mgr.getPolicy(null));
    }

    /**
     * Tests that a list of <code>Policy</code> names can be retrieved from the
     * repository.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetPolicyList() throws PolicyManagerException {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        final String[] names = {"Policy1", "Policy2", "Policy3"};
        final Policy[] expectedPolicies = new Policy[names.length];
        for (int i = 0; i < names.length; ++i) {
            expectedPolicies[i] = createPolicy(mgr, names[i]);
        }
        final List<Policy> policies = mgr.getPolicyList(Policy.class);
        Assert.assertEquals(policies.size(), expectedPolicies.length);
        for (int i = 0; i < expectedPolicies.length; ++i) {
            Assert.assertTrue(policies.contains(expectedPolicies[i]));
        }
    }

    /**
     * Tests that a list of <code>Policy</code> names can be retrieved from the
     * repository with null <code>Class</code> given.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetPolicyListNullClass() throws PolicyManagerException {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        final String[] names = {"Policy1", "Policy2", "Policy3"};
        final Policy[] expectedPolicies = new Policy[names.length];
        for (int i = 0; i < names.length; ++i) {
            expectedPolicies[i] = createPolicy(mgr, names[i]);
        }
        final List<Policy> policies = mgr.getPolicyList(null);
        Assert.assertEquals(policies.size(), expectedPolicies.length);
        for (int i = 0; i < expectedPolicies.length; ++i) {
            Assert.assertTrue(policies.contains(expectedPolicies[i]));
        }
    }

    /**
     * Tests that a list of <code>Policy</code> names can be retrieved from the
     * repository when a subclass of <code>Policy</code> is used.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetPolicyListDifferentClass()
            throws PolicyManagerException {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        final String[] names = {"Policy1", "Policy2", "Policy3"};
        final Policy[] expectedPolicies = new Policy[names.length];
        for (int i = 0; i < names.length; ++i) {
            expectedPolicies[i] = createPolicy(mgr, names[i]);
        }
        final List<Policy> policies = mgr.getPolicyList(TestPolicy.class);
        Assert.assertEquals(policies.size(), expectedPolicies.length);
        for (int i = 0; i < expectedPolicies.length; ++i) {
            Assert.assertTrue(policies.contains(expectedPolicies[i]));
        }
    }

    /**
     * Tests that a list of <code>Policy</code> names can be retrieved from the
     * repository when a subclass of <code>Policy</code> is used and the list is
     * empty because all of the stored <code>Policy</code>s are of a different
     * class.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetPolicyListDifferentClassNoReturn()
            throws PolicyManagerException {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        final String[] names = {"Policy1", "Policy2", "Policy3"};
        for (int i = 0; i < names.length; ++i) {
            createPolicy(mgr, names[i]);
        }
        final List<Policy> policies = mgr.getPolicyList(TestPolicy2.class);
        Assert.assertEquals(policies.size(), 0);
    }

    /**
     * Tests that the <code>DBPolicyManager</code> can archive a <code>Policy</code>.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testArchive() throws PolicyManagerException {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        Assert.assertNull(mgr.getPolicy(POLICY_NAME));
        createPolicy(mgr);
        Assert.assertNotNull(mgr.getPolicy(POLICY_NAME));
        boolean archived = mgr.archive(POLICY_NAME);
        Assert.assertTrue(archived);
        Assert.assertTrue(mgr.getPolicy(POLICY_NAME).isArchived());
    }

    /**
     * Tests that the <code>DBPolicyManager</code> returns false when an unknown <code>Policy</code>
     * name is provided to the archive() method.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testArchiveUnknown() throws PolicyManagerException {
        final String name = "Some unknown policy";
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        boolean archived = mgr.archive(name);
        Assert.assertFalse(archived);
        Assert.assertNull(mgr.getPolicy(POLICY_NAME));
    }

    /**
     * Tests that the <code>DBPolicyManager</code> returns false when a null name is provided to the
     * archive() method.
     *
     * @throws PolicyManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testArchiveNull() throws PolicyManagerException {
        final String name = null;
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        boolean archived = mgr.archive(name);
        Assert.assertFalse(archived);
    }

    /**
     * Tests that a policy can be set for an appraiser and device group.
     *
     * @throws Exception
     *             if error occurs while creating test Device
     */
    @Test
    public void testSetAndGetPolicy() throws Exception {
        TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        policy = (TestPolicy) mgr.savePolicy(policy);
        DeviceGroup deviceGroup =
                createDeviceGroupWithDevice("Test Device Group", "Test Device");
        mgr.setPolicy(appraiser, deviceGroup, policy);
        final Policy retrievedPolicy = mgr.getPolicy(appraiser, deviceGroup);
        Assert.assertEquals(retrievedPolicy, policy);
    }

    /**
     * Tests that a policy can be set for an appraiser and device group, and
     * retrieved using the device.
     *
     * @throws Exception
     *             if error occurs while creating test Device
     */
    @Test
    public void testSetAndGetPolicyUsingDevice() throws Exception {
        TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        policy = (TestPolicy) mgr.savePolicy(policy);
        DeviceGroup deviceGroup =
                createDeviceGroupWithDevice("Test Device Group", "Test Device");
        mgr.setPolicy(appraiser, deviceGroup, policy);
        final Policy retrievedPolicy = mgr.getPolicy(
                appraiser, deviceGroup.getDevices().iterator().next()
        );
        Assert.assertEquals(retrievedPolicy, policy);
    }

    /**
     * Tests that a policy can be removed for an appraiser and device group.
     *
     * @throws Exception
     *             if error occurs while creating test Device
     */
    @Test
    public void testSetPolicyRemove() throws Exception {
        TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        policy = (TestPolicy) mgr.savePolicy(policy);
        DeviceGroup deviceGroup =
                createDeviceGroupWithDevice("Test Device Group", "Test Device");
        mgr.setPolicy(appraiser, deviceGroup, policy);
        Policy retrievedPolicy = mgr.getPolicy(appraiser, deviceGroup);
        Assert.assertEquals(retrievedPolicy, policy);

        mgr.setPolicy(appraiser, deviceGroup, null);
        retrievedPolicy = mgr.getPolicy(appraiser, deviceGroup);
        Assert.assertNull(retrievedPolicy);
    }

    /**
     * Tests that a <code>NullPointerExcpetion</code> is thrown if appraiser is
     * null.
     *
     * @throws Exception
     *             if error occurs while creating test Device
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSetPolicyNullAppraiser() throws Exception {
        TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        DeviceGroup deviceGroup =
                createDeviceGroupWithDevice("Test Device Group", "Test Device");
        policy = (TestPolicy) mgr.savePolicy(policy);
        mgr.setPolicy(null, deviceGroup, policy);
    }

    /**
     * Tests that default policy can be set multiple times for an appraiser.
     *
     * @throws Exception
     *             if error occurs while creating test Device
     */
    @Test
    public void testSetPolicyTwice() throws Exception {
        TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        policy = (TestPolicy) mgr.savePolicy(policy);
        DeviceGroup deviceGroup =
                createDeviceGroupWithDevice("Test Device Group", "Test Device");
        mgr.setPolicy(appraiser, deviceGroup, policy);
        Policy retrievedPolicy = mgr.getPolicy(appraiser, deviceGroup);
        Assert.assertEquals(retrievedPolicy, policy);

        TestPolicy2 policy2 = new TestPolicy2("Other Policy");
        policy2 = (TestPolicy2) mgr.savePolicy(policy2);
        mgr.setPolicy(appraiser, deviceGroup, policy2);
        retrievedPolicy = mgr.getPolicy(appraiser, deviceGroup);
        Assert.assertEquals(retrievedPolicy, policy2);
    }

    /**
     * Tests that if there are multiple device groups in the database, each with
     * different policies, the correct policies associated with each device
     * group are returned.
     *
     * @throws Exception
     *             if error occurs while creating test Device
     */
    @Test
    public void testSetDifferentPolicesOnDeviceGroups() throws Exception {
        TestPolicy policy = new TestPolicy(POLICY_NAME);
        final String policyName2 = "Test Policy 2";
        TestPolicy policy2 = new TestPolicy(policyName2);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        policy = (TestPolicy) mgr.savePolicy(policy);
        policy2 = (TestPolicy) mgr.savePolicy(policy2);
        DeviceGroup deviceGroup =
                createDeviceGroupWithDevice("Test Device Group", "Test Device");
        DeviceGroup deviceGroup2 =
                createDeviceGroupWithDevice("Test Device Group 2", "Test Device 2");
        mgr.setPolicy(appraiser, deviceGroup, policy);
        mgr.setPolicy(appraiser, deviceGroup2, policy2);
        Policy retrievedPolicy = mgr.getPolicy(appraiser, deviceGroup);
        Policy retrievedPolicy2 = mgr.getPolicy(appraiser, deviceGroup2);
        Assert.assertEquals(retrievedPolicy, policy);
        Assert.assertEquals(retrievedPolicy2, policy2);
        Assert.assertNotEquals(retrievedPolicy2, retrievedPolicy);

        TestPolicy2 otherPolicy = new TestPolicy2("Other Policy");
        otherPolicy = (TestPolicy2) mgr.savePolicy(otherPolicy);
        mgr.setPolicy(appraiser, deviceGroup, otherPolicy);
        retrievedPolicy = mgr.getPolicy(appraiser, deviceGroup);
        Assert.assertEquals(retrievedPolicy, otherPolicy);
    }

    /**
     * Tests that if the policy for a device group is not set, then null
     * is returned.
     *
     * @throws Exception
     *             if error occurs while creating test Device
     */
    @Test
    public void testGetPolicyReturnsNullWhenDeviceGroupPolicyNotSet() throws Exception {
        final PolicyManager policyMgr = new DBPolicyManager(sessionFactory);

        // save Test Policy
        TestPolicy testPolicy = new TestPolicy(POLICY_NAME);
        testPolicy = (TestPolicy) policyMgr.savePolicy(testPolicy);

        // save Test Default Policy
        TestPolicy testDefaultPolicy = new TestPolicy("Test Default Policy");
        testDefaultPolicy = (TestPolicy) policyMgr.savePolicy(testDefaultPolicy);

        // create device and put it in Default Group
        Device deviceInDefaultGroup = new Device("DeviceInDefaultGroup");
        DeviceGroup defaultGroup = createDeviceGroupWithDevice(
                DeviceGroup.DEFAULT_GROUP, deviceInDefaultGroup.getName()
        );

        // create device and put it in Test Device Group
        Device deviceInTestGroup = new Device("DeviceInTestGroup");
        DeviceGroup testDeviceGroup = createDeviceGroupWithDevice(
                "Test Device Group", deviceInTestGroup.getName()
        );

        // set default policy for TestAppraiser to Test Default Policy
        policyMgr.setDefaultPolicy(appraiser, testDefaultPolicy);

        // set policy for TestAppraiser & Test Device Group to Test Policy
        policyMgr.setPolicy(appraiser, testDeviceGroup, testPolicy);

        Assert.assertEquals(policyMgr.getPolicy(appraiser, defaultGroup), testDefaultPolicy);
        Assert.assertEquals(policyMgr.getPolicy(appraiser, testDeviceGroup), testPolicy);

        Assert.assertEquals(
                policyMgr.getPolicy(appraiser, deviceInDefaultGroup),
                testDefaultPolicy
        );
        Assert.assertEquals(policyMgr.getPolicy(appraiser, deviceInTestGroup), testPolicy);

        // remove policy for Test Group and make sure the policy for the device in the group is null
        policyMgr.setPolicy(appraiser, testDeviceGroup, null);
        Assert.assertNull(policyMgr.getPolicy(appraiser, testDeviceGroup));
        Assert.assertNull(policyMgr.getPolicy(appraiser, deviceInTestGroup));
    }

    /**
     * Tests that null is returned for null appraiser.
     *
     * @throws Exception
     *             if error occurs while creating test Device
     */
    @Test
    public void testGetPolicyNullAppraiser() throws Exception {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        DeviceGroup deviceGroup =
                createDeviceGroupWithDevice("Test Device Group", "Test Device");
        final Policy policy = mgr.getPolicy(null, deviceGroup);
        Assert.assertNull(policy);
    }

    /**
     * Tests that null is returned when a policy isn't set appraiser.
     *
     * @throws Exception
     *             if error occurs while creating test Device
     */
    @Test
    public void testGetPolicyNoneSet() throws Exception {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        DeviceGroup deviceGroup =
                createDeviceGroupWithDevice("Test Device Group", "Test Device");
        final Policy policy = mgr.getPolicy(appraiser, deviceGroup);
        Assert.assertNull(policy);
    }

    /**
     * Tests that default policy can be set for an appraiser.
     *
     * @throws Exception
     *      occurs if device/deviceGroup could not be persisted
     */
    @Test
    public void testSetAndGetDefaultPolicy() throws Exception {
        TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        policy = (TestPolicy) mgr.savePolicy(policy);
        createDeviceGroupWithDevice(DeviceGroup.DEFAULT_GROUP, "Test Device");
        mgr.setDefaultPolicy(appraiser, policy);
        final Policy defaultPolicy = mgr.getDefaultPolicy(appraiser);
        Assert.assertEquals(defaultPolicy, policy);
    }

    /**
     * Tests that default policy can be removed for an appraiser.
     *
     * @throws Exception
     *      occurs if device/deviceGroup could not be persisted
     */
    @Test
    public void testSetDefaultPolicyRemove() throws Exception {
        TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        policy = (TestPolicy) mgr.savePolicy(policy);
        createDeviceGroupWithDevice(DeviceGroup.DEFAULT_GROUP, "Test Device");
        mgr.setDefaultPolicy(appraiser, policy);
        Policy defaultPolicy = mgr.getDefaultPolicy(appraiser);
        Assert.assertEquals(defaultPolicy, policy);

        mgr.setDefaultPolicy(appraiser, null);
        defaultPolicy = mgr.getDefaultPolicy(appraiser);
        Assert.assertNull(defaultPolicy);
    }

    /**
     * Tests that a <code>NullPointerExcpetion</code> is thrown if appraiser is
     * null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSetDefaultPolicyNullAppraiser() {
        TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        policy = (TestPolicy) mgr.savePolicy(policy);
        mgr.setDefaultPolicy(null, policy);
    }

    /**
     * Tests that default policy can be set multiple times for an appraiser.
     *
     * @throws Exception
     *      occurs if device/deviceGroup could not be persisted
     */
    @Test
    public void testSetDefaultPolicyTwice() throws Exception {
        TestPolicy policy = new TestPolicy(POLICY_NAME);
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        policy = (TestPolicy) mgr.savePolicy(policy);
        createDeviceGroupWithDevice(DeviceGroup.DEFAULT_GROUP, "Test Device");
        mgr.setDefaultPolicy(appraiser, policy);
        Policy defaultPolicy = mgr.getDefaultPolicy(appraiser);
        Assert.assertEquals(defaultPolicy, policy);

        TestPolicy2 policy2 = new TestPolicy2("Other Policy");
        policy2 = (TestPolicy2) mgr.savePolicy(policy2);
        mgr.setDefaultPolicy(appraiser, policy2);
        defaultPolicy = mgr.getDefaultPolicy(appraiser);
        Assert.assertEquals(defaultPolicy, policy2);
    }

    /**
     * Tests that null is returned for null appraiser.
     */
    @Test
    public void testGetDefaultPolicyNullAppraiser() {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        final Policy policy = mgr.getDefaultPolicy(null);
        Assert.assertNull(policy);
    }

    /**
     * Tests that null is returned for null appraiser.
     */
    @Test
    public void testGetDefaultPolicyNoneSet() {
        final PolicyManager mgr = new DBPolicyManager(sessionFactory);
        final Policy policy = mgr.getDefaultPolicy(appraiser);
        Assert.assertNull(policy);
    }

    /**
     * Tests that the group count is returned for policies.
     * @throws Exception if createDeviceGroup fails.
     */
    @Test
    public void testGetGroupCountForPolicy() throws Exception {
        LOGGER.debug("started testGetGroupCountForPolicy");
        final PolicyManager policyManager = new DBPolicyManager(sessionFactory);
        final Policy policy1 = new TestPolicy("Policy1");
        final Policy policy2 = new TestPolicy("Policy2");
        final Policy policy3 = new TestPolicy("Policy3");
        final Policy policy4 = new TestPolicy("Policy4");
        final DeviceGroup deviceGroup1 = createDeviceGroupWithDevice("Group1", "Device1");
        final DeviceGroup deviceGroup2 = createDeviceGroupWithDevice("Group2", "Device2");
        final DeviceGroup deviceGroup3 = createDeviceGroupWithDevice("Group3", "Device3");

        policyManager.savePolicy(policy1);
        policyManager.savePolicy(policy2);
        policyManager.savePolicy(policy3);
        policyManager.savePolicy(policy4);

        policyManager.setPolicy(appraiser, deviceGroup1, policy2);
        policyManager.setPolicy(appraiser, deviceGroup2, policy4);
        policyManager.setPolicy(appraiser, deviceGroup3, policy4);

        Assert.assertEquals(policyManager.getGroupCountForPolicy(policy1), 0);
        Assert.assertEquals(policyManager.getGroupCountForPolicy(policy2), 1);
        Assert.assertEquals(policyManager.getGroupCountForPolicy(policy3), 0);
        Assert.assertEquals(policyManager.getGroupCountForPolicy(policy4), 2);
    }

    /**
     * Tests that the group count for a null policy is 0.
     * No group is using a null policy.
     */
    @Test
    public void testGetGroupCountForPolicyNull() {
        LOGGER.debug("started testGetGroupCountForPolicyNull");
        final PolicyManager policyManager = new DBPolicyManager(sessionFactory);

        Assert.assertEquals(policyManager.getGroupCountForPolicy(null), 0);
    }

    private Policy createPolicy(final PolicyManager mgr)
            throws PolicyManagerException {
        return createPolicy(mgr, null);
    }

    private Policy createPolicy(final PolicyManager mgr, final String name)
            throws PolicyManagerException {
        LOGGER.debug("creating policy in db");
        String policyName = name;
        if (name == null) {
            policyName = POLICY_NAME;
        }
        final TestPolicy policy = new TestPolicy(policyName);
        return mgr.savePolicy(policy);
    }

    private DeviceGroup createDeviceGroupWithDevice(
            final String deviceGroupName, final String deviceName
    ) throws Exception {
        DeviceGroupManager deviceGroupManager = new DBDeviceGroupManager(sessionFactory);
        DeviceManager deviceManager = new DBDeviceManager(sessionFactory);
        Device device = DeviceTest.getTestDevice(deviceName);
        DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        deviceGroup = deviceGroupManager.saveDeviceGroup(deviceGroup);
        deviceGroup.addDevice(device);
        device.setDeviceGroup(deviceGroup);
        deviceManager.saveDevice(device);
        deviceGroupManager.updateDeviceGroup(deviceGroup);
        return deviceGroup;
    }

    private boolean isInDatabase(final String name) {
        LOGGER.debug("checking if policy {} is in database", name);
        Policy policy = null;
        Transaction tx = null;
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        try {
            LOGGER.debug("retrieving policy");
            tx = session.beginTransaction();
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Policy> criteriaQuery = builder.createQuery(Policy.class);
            Root<Policy> root = criteriaQuery.from(Policy.class);
            criteriaQuery.select(root).where(builder.equal(root.get("name"), name));
            Query<Policy> query = session.createQuery(criteriaQuery);
            policy = query.getSingleResult();
//            policy =
//                    (Policy) session.createCriteria(Policy.class)
//                            .add(Restrictions.eq("name", name)).uniqueResult();
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve policy";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
        return policy != null;
    }
}
