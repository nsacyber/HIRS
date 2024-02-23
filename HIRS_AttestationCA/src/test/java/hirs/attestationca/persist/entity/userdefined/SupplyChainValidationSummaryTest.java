package hirs.attestationca.persist.entity.userdefined;//package hirs.attestationca.persist.entity.userdefined;
//
//import hirs.attestationca.persist.PersistenceConfiguration;
//import hirs.attestationca.persist.SpringPersistenceTest;
//import hirs.attestationca.persist.entity.ArchivableEntity;
//import hirs.attestationca.persist.entity.manager.CertificateRepository;
//import hirs.attestationca.persist.entity.manager.DeviceRepository;
//import hirs.attestationca.persist.entity.manager.SupplyChainValidationSummaryRepository;
//import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
//import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReportTest;
//import hirs.attestationca.persist.enums.AppraisalStatus;
////import hirs.attestationca.portal.PersistenceJPAConfig;
//
//import hirs.attestationca.persist.enums.HealthStatus;
//import hirs.attestationca.persist.SpringPersistenceTestConfiguration;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
///**
// * Integration test that tests the functionality in SupplyChainValidationSummary,
// * as well as the persistence of SupplyChainValidationSummary and SupplyChainValidation.
// */
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class SupplyChainValidationSummaryTest {
//    private Device device;
//    private List<ArchivableEntity> certificates;
////    @Mock
//    @Autowired
//    private CertificateRepository certificateRepository;
////    @Mock
//    @Autowired
//    private DeviceRepository deviceRepository;
////    @Mock
//    @Autowired
//    private SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository;
//
//    /**
//     * Create a session factory to use for persistence testing and persist some certificates
//     * for use by these tests.
//     *
//     * @throws Exception if there is a problem deserializing certificates or creating test device
//     */
//    @BeforeAll
//    public void setup() throws Exception {
//
////        //DOES THIS NEED TO BE BEFORE EACH ??
////        MockitoAnnotations.initMocks(this);
////
//        certificates = CertificateTest.getAllTestCertificates();
////
////        for (ArchivableEntity cert : certificates) {
////            certificateRepository.save((Certificate) cert);
////        }
////
//////        device = DeviceTest.getTestDevice("TestDevice");
//        device = AbstractUserdefinedEntityTest.getTestDevice("TestDevice");
////        deviceRepository.save(device);
//    }
//
//    /**
//     * Remove test certificates and close the session factory.
//     */
//    @AfterAll
//    public void teardown() {
////        certificateRepository.deleteAll();
////        deviceRepository.delete(device);
//    }
//
//    @AfterEach
//    public void resetTestState() {
////        supplyChainValidationSummaryRepository.deleteAll();
//    }
//
//    /**
//     * Tests that an empty summary behaves as expected.
//     */
//    @Test
//    public void testEmptySummary() throws InterruptedException {
//        SupplyChainValidationSummary emptySummary = getTestSummary(
//                0,
//                0,
//                certificates
//        );
//
//        //assertEquals(device, emptySummary.getDevice());
//        assertEquals(device.getDeviceInfo(), emptySummary.getDevice().getDeviceInfo());
//        assertEquals(Collections.EMPTY_SET, emptySummary.getValidations());
//        assertEquals(AppraisalStatus.Status.PASS, emptySummary.getOverallValidationResult());
//        assertNotNull(emptySummary.getCreateTime());
//    }
//
//    /**
//     * Test that a summary can't be created with a null validationIdentifier.
//     */
//    @Test
//    public void testNullValidationIdentifier() {
//        assertThrows(IllegalArgumentException.class, () ->
//                new SupplyChainValidationSummary(null, Collections.emptyList()));
//    }
//
//    /**
//     * Test that a summary can't be created with a null validations list.
//     */
//    @Test
//    public void testNullValidationList() {
//        assertThrows(IllegalArgumentException.class, () ->
//                new SupplyChainValidationSummary(device, null));
//    }
//
//    /**
//     * Test that summaries with one and two component validations, which both represent successful
//     * validations, have getters that return the expected information.
//     */
//    @Test
//    public void testSuccessfulSummary() throws InterruptedException {
//        SupplyChainValidationSummary oneValidation = getTestSummary(
//                1,
//                0,
//                certificates
//        );
//
//        //assertEquals(device, oneValidation.getDevice());
//        assertEquals(device.getDeviceInfo(), oneValidation.getDevice().getDeviceInfo());
//        assertEquals(1, oneValidation.getValidations().size());
//        assertEquals(AppraisalStatus.Status.PASS, oneValidation.getOverallValidationResult());
//        assertNotNull(oneValidation.getCreateTime());
//
//        SupplyChainValidationSummary twoValidations = getTestSummary(
//                2,
//                0,
//                certificates
//        );
//
//        //assertEquals(device, twoValidations.getDevice());
//        assertEquals(device.getDeviceInfo(), twoValidations.getDevice().getDeviceInfo());
//        assertEquals(2, twoValidations.getValidations().size());
//        assertEquals(twoValidations.getOverallValidationResult(), AppraisalStatus.Status.PASS);
//        assertNotNull(twoValidations.getCreateTime());
//    }
//
//    /**
//     * Test that summaries with one and two component validations, of which one represents an
//     * unsuccessful validations, have getters that return the expected information.
//     */
//    @Test
//    public void testUnsuccessfulSummary() throws InterruptedException {
//        SupplyChainValidationSummary oneValidation = getTestSummary(
//                1,
//                1,
//                certificates
//        );
//
//        //assertEquals(device, oneValidation.getDevice());
//        assertEquals(device.getDeviceInfo(), oneValidation.getDevice().getDeviceInfo());
//        assertEquals(1, oneValidation.getValidations().size());
//        assertEquals(AppraisalStatus.Status.FAIL, oneValidation.getOverallValidationResult());
//        assertNotNull(oneValidation.getCreateTime());
//
//        SupplyChainValidationSummary twoValidations = getTestSummary(
//                2,
//                1,
//                certificates
//        );
//
//        //assertEquals(device, twoValidations.getDevice());
//        assertEquals(device.getDeviceInfo(), twoValidations.getDevice().getDeviceInfo());
//        assertEquals(2, twoValidations.getValidations().size());
//        assertEquals(AppraisalStatus.Status.FAIL, twoValidations.getOverallValidationResult());
//        assertNotNull(twoValidations.getCreateTime());
//
//        SupplyChainValidationSummary twoBadValidations = getTestSummary(
//                2,
//                2,
//                certificates
//        );
//
//        //assertEquals(device, twoBadValidations.getDevice());
//        assertEquals(device.getDeviceInfo(), twoBadValidations.getDevice().getDeviceInfo());
//        assertEquals(2, twoBadValidations.getValidations().size());
//        assertEquals(AppraisalStatus.Status.FAIL, twoBadValidations.getOverallValidationResult());
//        assertNotNull(twoBadValidations.getCreateTime());
//    }
////
////    /**
////     * Tests that a SupplyChainValidationSummary can be persisted.
////     */
////    @Test
////    public void testSave() throws InterruptedException {
//////        DBManager<SupplyChainValidationSummary> supplyMan = new DBManager<>(
//////                SupplyChainValidationSummary.class, sessionFactory
//////        );
////
////        SupplyChainValidationSummary summary = getTestSummary(
////                2,
////                1,
////                certificates
////        );
////        SupplyChainValidationSummary savedSummary = supplyChainValidationSummaryRepository.save(summary);
//////        SupplyChainValidationSummary savedSummary = supplyMan.save(summary);
////
////        System.out.println("XXXX testSave: summary.getDevice.getID: " + summary.getDevice().getId());
////        System.out.println("XXXX testSave: savedSummary.getDevice.getID: " + savedSummary.getDevice().getId());
////
////        assertEquals(summary, savedSummary);
////    }
//
////    /**
////     * Tests that an empty SupplyChainValidationSummary can be persisted and retrieved.
////     */
////    @Test
////    public void testSaveAndGetEmpty() {
////        DBManager<SupplyChainValidationSummary> supplyMan = new DBManager<>(
////                SupplyChainValidationSummary.class, sessionFactory
////        );
////
////        SupplyChainValidationSummary emptySummary = getTestSummary(
////                0,
////                0,
////                Collections.emptyList()
////        );
////        SupplyChainValidationSummary savedEmptySummary = supplyMan.save(emptySummary);
////
////        SupplyChainValidationSummary retrievedEmptySummary =
////                supplyMan.get(savedEmptySummary.getId());
////        Assert.assertEquals(retrievedEmptySummary, emptySummary);
////        Assert.assertEquals(retrievedEmptySummary.getValidations().size(), 0);
////    }
////
////    /**
////     * Tests that a SupplyChainValidationSummary with a single validation can be persisted
////     * and retrieved.
////     */
////    @Test
////    public void testSaveAndGetSmall() {
////        DBManager<SupplyChainValidationSummary> supplyMan = new DBManager<>(
////                SupplyChainValidationSummary.class, sessionFactory
////        );
////
////        List<ArchivableEntity> singleCert = certificates.subList(0, 1);
////
////        SupplyChainValidationSummary smallSummary = getTestSummary(
////                1,
////                0,
////                singleCert
////        );
////        SupplyChainValidationSummary savedSmallSummary = supplyMan.save(smallSummary);
////
////        SupplyChainValidationSummary retrievedSmallSummary =
////                supplyMan.get(savedSmallSummary.getId());
////        Assert.assertEquals(retrievedSmallSummary, smallSummary);
////        Assert.assertEquals(
////                new ArrayList<>(retrievedSmallSummary.getValidations())
////                        .get(0).getCertificatesUsed(),
////                singleCert
////        );
////
////    }
////
////    /**
////     * Tests that a SupplyChainValidationSummary can be retrieved and that its fields are properly
////     * restored.
////     */
////    @Test
////    public void testGet() {
////        DBManager<SupplyChainValidationSummary> supplyMan = new DBManager<>(
////                SupplyChainValidationSummary.class, sessionFactory
////        );
////
////        SupplyChainValidationSummary summary = getTestSummary(
////                2,
////                1,
////                certificates
////        );
////
////        SupplyChainValidationSummary savedSummary = supplyMan.save(summary);
////        Assert.assertEquals(savedSummary, summary);
////
////        SupplyChainValidationSummary retrievedSummary = supplyMan.get(savedSummary.getId());
////        Assert.assertNotNull(retrievedSummary);
////        Assert.assertEquals(
////                retrievedSummary.getDevice(),
////                summary.getDevice()
////        );
////        Assert.assertEquals(retrievedSummary.getCreateTime(), summary.getCreateTime());
////        Assert.assertEquals(
////                retrievedSummary.getOverallValidationResult(),
////                summary.getOverallValidationResult()
////        );
////        Assert.assertEquals(retrievedSummary.getValidations(), summary.getValidations());
////
////        SupplyChainValidation failedValidation = null;
////        for (SupplyChainValidation validation : retrievedSummary.getValidations()) {
////            if (validation.getResult() != AppraisalStatus.Status.PASS) {
////                failedValidation = validation;
////                break;
////            }
////        }
////
////        Assert.assertNotNull(failedValidation);
////        Assert.assertEquals(
////                failedValidation.getCertificatesUsed(),
////                certificates
////        );
////
////    }
//
//    /**
//     *
//     */
//    private SupplyChainValidationSummary getTestSummary(
//            final int numberOfValidations,
//            final int numFail,
//            final List<ArchivableEntity> certificates
//    ) throws InterruptedException {
//        SupplyChainValidation.ValidationType[] validationTypes =
//                SupplyChainValidation.ValidationType.values();
//
//        if (numberOfValidations > validationTypes.length) {
//            throw new IllegalArgumentException(String.format(
//                    "Cannot have more than %d validation types",
//                    validationTypes.length
//            ));
//        }
//
//        if (numFail > numberOfValidations) {
//            throw new IllegalArgumentException(String.format(
//                    "Cannot have more than %d failed validations",
//                    validationTypes.length
//            ));
//        }
//
//        Collection<SupplyChainValidation> validations = new HashSet<>();
//        for (int i = 0; i < numberOfValidations; i++) {
//            boolean successful = true;
//            if (i >= (numberOfValidations - numFail)) {
//                successful = false;
//            }
//
//            AppraisalStatus.Status result = AppraisalStatus.Status.FAIL;
//            if (successful) {
//                result = AppraisalStatus.Status.PASS;
//            }
//
//            validations.add(SupplyChainValidationTest.getTestSupplyChainValidation(
//                    validationTypes[i],
//                    result,
//                    certificates
//            ));
//        }
//
//        return new SupplyChainValidationSummary(device, validations);
//    }
//}
