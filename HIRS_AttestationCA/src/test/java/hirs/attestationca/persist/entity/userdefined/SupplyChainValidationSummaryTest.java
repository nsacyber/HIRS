package hirs.attestationca.persist.entity.userdefined;

import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.enums.AppraisalStatus;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test that tests the functionality in SupplyChainValidationSummary,
 * as well as the persistence of SupplyChainValidationSummary and SupplyChainValidation.
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SupplyChainValidationSummaryTest {
    private Device device;
    private List<ArchivableEntity> certificates;

    /**
     * Create a session factory to use for persistence testing and persist some certificates
     * for use by these tests.
     *
     * @throws Exception if there is a problem deserializing certificates or creating test device
     */
    @BeforeAll
    public void setup() throws Exception {

        certificates = CertificateTest.getAllTestCertificates();
        device = AbstractUserdefinedEntityTest.getTestDevice("TestDevice");
    }

    /**
     * Tests that an empty summary behaves as expected.
     */
    @Test
    public void testEmptySummary() throws InterruptedException {
        SupplyChainValidationSummary emptySummary = getTestSummary(
                0,
                0,
                certificates
        );

        //assertEquals(device, emptySummary.getDevice());
        assertEquals(device.getDeviceInfo(), emptySummary.getDevice().getDeviceInfo());
        assertEquals(Collections.EMPTY_SET, emptySummary.getValidations());
        assertEquals(AppraisalStatus.Status.PASS, emptySummary.getOverallValidationResult());
        assertNotNull(emptySummary.getCreateTime());
    }

    /**
     * Test that a summary can't be created with a null validationIdentifier.
     */
    @Test
    public void testNullValidationIdentifier() {
        assertThrows(IllegalArgumentException.class, () ->
                new SupplyChainValidationSummary(null, Collections.emptyList()));
    }

    /**
     * Test that a summary can't be created with a null validations list.
     */
    @Test
    public void testNullValidationList() {
        assertThrows(IllegalArgumentException.class, () ->
                new SupplyChainValidationSummary(device, null));
    }

    /**
     * Test that summaries with one and two component validations, which both represent successful
     * validations, have getters that return the expected information.
     */
    @Test
    public void testSuccessfulSummary() throws InterruptedException {
        SupplyChainValidationSummary oneValidation = getTestSummary(
                1,
                0,
                certificates
        );

        //assertEquals(device, oneValidation.getDevice());
        assertEquals(device.getDeviceInfo(), oneValidation.getDevice().getDeviceInfo());
        assertEquals(1, oneValidation.getValidations().size());
        assertEquals(AppraisalStatus.Status.PASS, oneValidation.getOverallValidationResult());
        assertNotNull(oneValidation.getCreateTime());

        SupplyChainValidationSummary twoValidations = getTestSummary(
                2,
                0,
                certificates
        );

        //assertEquals(device, twoValidations.getDevice());
        assertEquals(device.getDeviceInfo(), twoValidations.getDevice().getDeviceInfo());
        assertEquals(2, twoValidations.getValidations().size());
        assertEquals(twoValidations.getOverallValidationResult(), AppraisalStatus.Status.PASS);
        assertNotNull(twoValidations.getCreateTime());
    }

    /**
     * Test that summaries with one and two component validations, of which one represents an
     * unsuccessful validations, have getters that return the expected information.
     */
    @Test
    public void testUnsuccessfulSummary() throws InterruptedException {
        SupplyChainValidationSummary oneValidation = getTestSummary(
                1,
                1,
                certificates
        );

        //assertEquals(device, oneValidation.getDevice());
        assertEquals(device.getDeviceInfo(), oneValidation.getDevice().getDeviceInfo());
        assertEquals(1, oneValidation.getValidations().size());
        assertEquals(AppraisalStatus.Status.FAIL, oneValidation.getOverallValidationResult());
        assertNotNull(oneValidation.getCreateTime());

        SupplyChainValidationSummary twoValidations = getTestSummary(
                2,
                1,
                certificates
        );

        //assertEquals(device, twoValidations.getDevice());
        assertEquals(device.getDeviceInfo(), twoValidations.getDevice().getDeviceInfo());
        assertEquals(2, twoValidations.getValidations().size());
        assertEquals(AppraisalStatus.Status.FAIL, twoValidations.getOverallValidationResult());
        assertNotNull(twoValidations.getCreateTime());

        SupplyChainValidationSummary twoBadValidations = getTestSummary(
                2,
                2,
                certificates
        );

        //assertEquals(device, twoBadValidations.getDevice());
        assertEquals(device.getDeviceInfo(), twoBadValidations.getDevice().getDeviceInfo());
        assertEquals(2, twoBadValidations.getValidations().size());
        assertEquals(AppraisalStatus.Status.FAIL, twoBadValidations.getOverallValidationResult());
        assertNotNull(twoBadValidations.getCreateTime());
    }

    /**
     *
     */
    private SupplyChainValidationSummary getTestSummary(
            final int numberOfValidations,
            final int numFail,
            final List<ArchivableEntity> certificates
    ) throws InterruptedException {
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

        Collection<SupplyChainValidation> validations = new HashSet<>();
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
