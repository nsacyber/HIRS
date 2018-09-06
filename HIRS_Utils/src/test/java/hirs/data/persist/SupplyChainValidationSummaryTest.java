package hirs.data.persist;

import hirs.data.persist.certificate.Certificate;
import hirs.persist.DBCertificateManager;
import hirs.persist.DBManager;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import hirs.data.persist.certificate.CertificateTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests the functionality in SupplyChainValidationSummary, as well as the persistence of
 * SupplyChainValidationSummary and SupplyChainValidation.
 */
public class SupplyChainValidationSummaryTest extends SpringPersistenceTest {
    private Device device;
    private DeviceGroup deviceGroup;
    private List<Certificate> certificates;

    /**
     * Create a session factory to use for persistence testing and persist some certificates
     * for use by these tests.
     *
     * @throws Exception if there is a problem deserializing certificates or creating test device
     */
    @BeforeClass
    public void setup() throws Exception {
        certificates = CertificateTest.getAllTestCertificates();
        DBCertificateManager certMan = new DBCertificateManager(sessionFactory);
        for (Certificate cert : certificates) {
            certMan.save(cert);
        }

        deviceGroup = new DeviceGroup("TestDeviceGroup", "TestDeviceGroupDescription");
        DBManager<DeviceGroup> devicGroupMan = new DBManager<>(DeviceGroup.class, sessionFactory);
        deviceGroup = devicGroupMan.save(deviceGroup);

        device = DeviceTest.getTestDevice("TestDevice");
        device.setDeviceGroup(deviceGroup);
        DBManager<Device> deviceMan = new DBManager<>(Device.class, sessionFactory);
        device = deviceMan.save(device);
    }

    /**
     * Remove test certificates and close the session factory.
     */
    @AfterClass
    public void teardown() {
        DBCertificateManager certManager = new DBCertificateManager(sessionFactory);
        for (Certificate cert : certificates) {
            certManager.deleteCertificate(cert);
        }
    }

    /**
     * Resets the test state to a known good state. This resets
     * the database by removing all {@link Certificate} objects.
     */
    @AfterMethod
    public void resetTestState() {
        DBManager<SupplyChainValidationSummary> summaryManager = new DBManager<>(
                        SupplyChainValidationSummary.class,
                        sessionFactory
                );

        for (SupplyChainValidationSummary summary
                : summaryManager.getList(SupplyChainValidationSummary.class)) {
            summaryManager.delete(summary.getId());
        }
    }

    /**
     * Tests that an empty summary behaves as expected.
     */
    @Test
    public void testEmptySummary() {
        SupplyChainValidationSummary emptySummary = getTestSummary(
                0,
                0,
                certificates
        );

        Assert.assertEquals(emptySummary.getDevice(), device);
        Assert.assertEquals(emptySummary.getValidations(), Collections.EMPTY_LIST);
        Assert.assertEquals(emptySummary.getOverallValidationResult(), AppraisalStatus.Status.PASS);
        Assert.assertNotNull(emptySummary.getCreateTime());
    }

    /**
     * Test that a summary can't be created with a null validationIdentifier.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullValidationIdentifier() {
        new SupplyChainValidationSummary(null, Collections.emptyList());
    }

    /**
     * Test that a summary can't be created with a null validations list.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullValidationList() {
        new SupplyChainValidationSummary(device, null);
    }

    /**
     * Test that summaries with one and two component validations, which both represent successful
     * validations, have getters that return the expected information.
     */
    @Test
    public void testSuccessfulSummary() {
        SupplyChainValidationSummary oneValidation = getTestSummary(
                1,
                0,
                certificates
        );

        Assert.assertEquals(oneValidation.getDevice(), device);
        Assert.assertEquals(oneValidation.getValidations().size(), 1);
        Assert.assertEquals(oneValidation.getOverallValidationResult(),
                AppraisalStatus.Status.PASS);
        Assert.assertNotNull(oneValidation.getCreateTime());

        SupplyChainValidationSummary twoValidations = getTestSummary(
                2,
                0,
                certificates
        );

        Assert.assertEquals(twoValidations.getDevice(), device);
        Assert.assertEquals(twoValidations.getValidations().size(), 2);
        Assert.assertEquals(twoValidations.getOverallValidationResult(),
                AppraisalStatus.Status.PASS);
        Assert.assertNotNull(twoValidations.getCreateTime());
    }

    /**
     * Test that summaries with one and two component validations, of which one represents an
     * unsuccessful validations, have getters that return the expected information.
     */
    @Test
    public void testUnsuccessfulSummary() {
        SupplyChainValidationSummary oneValidation = getTestSummary(
                1,
                1,
                certificates
        );

        Assert.assertEquals(oneValidation.getDevice(), device);
        Assert.assertEquals(oneValidation.getValidations().size(), 1);
        Assert.assertEquals(oneValidation.getOverallValidationResult(),
                AppraisalStatus.Status.FAIL);
        Assert.assertNotNull(oneValidation.getCreateTime());

        SupplyChainValidationSummary twoValidations = getTestSummary(
                2,
                1,
                certificates
        );

        Assert.assertEquals(twoValidations.getDevice(), device);
        Assert.assertEquals(twoValidations.getValidations().size(), 2);
        Assert.assertEquals(twoValidations.getOverallValidationResult(),
                AppraisalStatus.Status.FAIL);
        Assert.assertNotNull(twoValidations.getCreateTime());

        SupplyChainValidationSummary twoBadValidations = getTestSummary(
                2,
                2,
                certificates
        );

        Assert.assertEquals(twoBadValidations.getDevice(), device);
        Assert.assertEquals(twoBadValidations.getValidations().size(), 2);
        Assert.assertEquals(twoBadValidations.getOverallValidationResult(),
                AppraisalStatus.Status.FAIL);
        Assert.assertNotNull(twoBadValidations.getCreateTime());
    }

    /**
     * Tests that a SupplyChainValidationSummary can be persisted.
     */
    @Test
    public void testSave() {
        DBManager<SupplyChainValidationSummary> supplyMan = new DBManager<>(
                SupplyChainValidationSummary.class, sessionFactory
        );

        SupplyChainValidationSummary summary = getTestSummary(
                2,
                1,
                certificates
        );
        SupplyChainValidationSummary savedSummary = supplyMan.save(summary);
        Assert.assertEquals(savedSummary, summary);
    }

    /**
     * Tests that an empty SupplyChainValidationSummary can be persisted and retrieved.
     */
    @Test
    public void testSaveAndGetEmpty() {
        DBManager<SupplyChainValidationSummary> supplyMan = new DBManager<>(
                SupplyChainValidationSummary.class, sessionFactory
        );

        SupplyChainValidationSummary emptySummary = getTestSummary(
                0,
                0,
                Collections.emptyList()
        );
        SupplyChainValidationSummary savedEmptySummary = supplyMan.save(emptySummary);

        SupplyChainValidationSummary retrievedEmptySummary =
                supplyMan.get(savedEmptySummary.getId());
        Assert.assertEquals(retrievedEmptySummary, emptySummary);
        Assert.assertEquals(retrievedEmptySummary.getValidations().size(), 0);
    }

    /**
     * Tests that a SupplyChainValidationSummary with a single validation can be persisted
     * and retrieved.
     */
    @Test
    public void testSaveAndGetSmall() {
        DBManager<SupplyChainValidationSummary> supplyMan = new DBManager<>(
                SupplyChainValidationSummary.class, sessionFactory
        );

        List<Certificate> singleCert = certificates.subList(0, 1);

        SupplyChainValidationSummary smallSummary = getTestSummary(
                1,
                0,
                singleCert
        );
        SupplyChainValidationSummary savedSmallSummary = supplyMan.save(smallSummary);

        SupplyChainValidationSummary retrievedSmallSummary =
                supplyMan.get(savedSmallSummary.getId());
        Assert.assertEquals(retrievedSmallSummary, smallSummary);
        Assert.assertEquals(
                new ArrayList<>(retrievedSmallSummary.getValidations())
                        .get(0).getCertificatesUsed(),
                singleCert
        );

    }

    /**
     * Tests that a SupplyChainValidationSummary can be retrieved and that its fields are properly
     * restored.
     */
    @Test
    public void testGet() {
        DBManager<SupplyChainValidationSummary> supplyMan = new DBManager<>(
                SupplyChainValidationSummary.class, sessionFactory
        );

        SupplyChainValidationSummary summary = getTestSummary(
                2,
                1,
                certificates
        );

        SupplyChainValidationSummary savedSummary = supplyMan.save(summary);
        Assert.assertEquals(savedSummary, summary);

        SupplyChainValidationSummary retrievedSummary = supplyMan.get(savedSummary.getId());
        Assert.assertNotNull(retrievedSummary);
        Assert.assertEquals(
                retrievedSummary.getDevice(),
                summary.getDevice()
        );
        Assert.assertEquals(retrievedSummary.getCreateTime(), summary.getCreateTime());
        Assert.assertEquals(
                retrievedSummary.getOverallValidationResult(),
                summary.getOverallValidationResult()
        );
        Assert.assertEquals(retrievedSummary.getValidations(), summary.getValidations());

        SupplyChainValidation failedValidation = null;
        for (SupplyChainValidation validation : retrievedSummary.getValidations()) {
            if (validation.getResult() != AppraisalStatus.Status.PASS) {
                failedValidation = validation;
                break;
            }
        }

        Assert.assertNotNull(failedValidation);
        Assert.assertEquals(
                failedValidation.getCertificatesUsed(),
                certificates
        );

    }

    private SupplyChainValidationSummary getTestSummary(
            final int numberOfValidations,
            final int numFail,
            final List<Certificate> certificates
    ) {
        SupplyChainValidation.ValidationType[] validationTypes =
                SupplyChainValidation.ValidationType.values();

        if (numberOfValidations > validationTypes.length) {
            throw new IllegalArgumentException(String.format(
                    "Cannot have more than %d validation types",
                    validationTypes.length
            ));
        }

        if (numFail > numberOfValidations) {
            throw new IllegalArgumentException(String.format(
                    "Cannot have more than %d failed validations",
                    validationTypes.length
            ));
        }

        List<SupplyChainValidation> validations = new ArrayList<>();
        for (int i = 0; i < numberOfValidations; i++) {
            boolean successful = true;
            if (i >= (numberOfValidations - numFail)) {
                successful = false;
            }

            AppraisalStatus.Status result = AppraisalStatus.Status.FAIL;
            if (successful) {
                result = AppraisalStatus.Status.PASS;
            }

            validations.add(SupplyChainValidationTest.getTestSupplyChainValidation(
                    validationTypes[i],
                    result,
                    certificates
            ));
        }

        return new SupplyChainValidationSummary(device, validations);
    }
}
