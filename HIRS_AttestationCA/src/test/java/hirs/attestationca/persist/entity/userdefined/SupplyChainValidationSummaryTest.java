package hirs.attestationca.persist.entity.userdefined;

import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.enums.HealthStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the functionality in SupplyChainValidationSummary.
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SupplyChainValidationSummaryTest extends AbstractUserdefinedEntityTest {

    /**
     * Test device.
     *
     */
    private Device device;

    /**
     * List of test certificates.
     *
     */
    private List<ArchivableEntity> certificates;

    /**
     * Create a set of certificates and a device for use by these tests.
     *
     * @throws Exception if there is a problem deserializing certificates or creating test device
     */
    @BeforeAll
    public void setup() throws Exception {

        certificates = getAllTestCertificates();
        device = getTestDevice("TestDevice");
    }

    /**
     * Tests that an empty summary behaves as expected.
     */
    @Test
    public void testEmptySummary() throws InterruptedException {
        SupplyChainValidationSummary emptySummary = getTestSummary(
                0,
                0
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
                0
        );

        //assertEquals(device, oneValidation.getDevice());
        assertEquals(device.getDeviceInfo(), oneValidation.getDevice().getDeviceInfo());
        assertEquals(1, oneValidation.getValidations().size());
        assertEquals(AppraisalStatus.Status.PASS, oneValidation.getOverallValidationResult());
        assertNotNull(oneValidation.getCreateTime());

        SupplyChainValidationSummary twoValidations = getTestSummary(
                2,
                0
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
                1
        );

        //assertEquals(device, oneValidation.getDevice());
        assertEquals(device.getDeviceInfo(), oneValidation.getDevice().getDeviceInfo());
        assertEquals(1, oneValidation.getValidations().size());
        assertEquals(AppraisalStatus.Status.FAIL, oneValidation.getOverallValidationResult());
        assertNotNull(oneValidation.getCreateTime());

        SupplyChainValidationSummary twoValidations = getTestSummary(
                2,
                1
        );

        //assertEquals(device, twoValidations.getDevice());
        assertEquals(device.getDeviceInfo(), twoValidations.getDevice().getDeviceInfo());
        assertEquals(2, twoValidations.getValidations().size());
        assertEquals(AppraisalStatus.Status.FAIL, twoValidations.getOverallValidationResult());
        assertNotNull(twoValidations.getCreateTime());

        SupplyChainValidationSummary twoBadValidations = getTestSummary(
                2,
                2
        );

        //assertEquals(device, twoBadValidations.getDevice());
        assertEquals(device.getDeviceInfo(), twoBadValidations.getDevice().getDeviceInfo());
        assertEquals(2, twoBadValidations.getValidations().size());
        assertEquals(AppraisalStatus.Status.FAIL, twoBadValidations.getOverallValidationResult());
        assertNotNull(twoBadValidations.getCreateTime());
    }

    /**
     * Utility method for getting a <code>Device</code> that can be used for
     * testing.
     *
     * @param name name for the <code>Device</code>
     *
     * @return device
     */
    public static Device getTestDevice(final String name) {
        final DeviceInfoReport deviceInfo = getTestDeviceInfoReport();
        return new Device(name, deviceInfo, HealthStatus.UNKNOWN,
                AppraisalStatus.Status.UNKNOWN, null,
                false, null, null);
    }

    /**
     * Utility method for getting a <code>SupplyChainValidationSummary</code> that can be used for
     * testing.
     *
     * @param numberOfValidations number of validations for the <code>SupplyChainValidationSummary</code>
     * @param numFail number of failed validations
     *
     * @return device
     */
    private SupplyChainValidationSummary getTestSummary(
            final int numberOfValidations,
            final int numFail
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
