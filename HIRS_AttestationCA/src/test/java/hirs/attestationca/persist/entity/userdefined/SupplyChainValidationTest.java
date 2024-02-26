package hirs.attestationca.persist.entity.userdefined;

import hirs.attestationca.persist.enums.AppraisalStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Simple tests for the {@link SupplyChainValidation} class.  Tests for the persistence of this
 * class are located in { SupplyChainValidationSummaryTest}.
 */
class SupplyChainValidationTest extends AbstractUserdefinedEntityTest {

    /**
     * Test that this class' getter methods work properly.
     *
     * @throws IOException if there is a problem deserializing certificates
     */
    @Test
    public void testGetters() throws IOException {
        SupplyChainValidation validation = getTestSupplyChainValidation();
        assertEquals(
                validation.getValidationType(),
                SupplyChainValidation.ValidationType.ENDORSEMENT_CREDENTIAL
        );
        assertEquals(
                validation.getCertificatesUsed(),
                getAllTestCertificates()
        );
        assertEquals(validation.getMessage(), VALIDATION_MESSAGE);
    }

    /**
     * Test that a SupplyChainValidation can't be instantiated with a null validation type.
     *
     * @throws IOException if there is a problem deserializing certificates
     */
    @Test
    public void testNullValidationType() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
                new SupplyChainValidation(
                        null,
                        AppraisalStatus.Status.PASS,
                        getAllTestCertificates(),
                        VALIDATION_MESSAGE
                ));
    }

    /**
     * Test that a SupplyChainValidation can't be instantiated with a null certificate list.
     *
     * @throws IOException if there is a problem deserializing certificates
     */
    @Test
    public void testNullCertificates() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
                new SupplyChainValidation(
                        SupplyChainValidation.ValidationType.ENDORSEMENT_CREDENTIAL,
                        AppraisalStatus.Status.PASS,
                        null,
                        VALIDATION_MESSAGE
                ));
    }

    /**
     * Test that a SupplyChainValidation can be instantiated with a null message.
     *
     * @throws IOException if there is a problem deserializing certificates
     */
    @Test
    public void testNullMessage() throws IOException {
        new SupplyChainValidation(
                SupplyChainValidation.ValidationType.ENDORSEMENT_CREDENTIAL,
                AppraisalStatus.Status.PASS,
                getAllTestCertificates(),
                VALIDATION_MESSAGE
        );
    }

    /**
     * Construct a SupplyChainValidation for use in tests.  It will have a validation
     * type of ENDORSEMENT_CREDENTIAL, will represent a successful validation, and will use
     * multiple test certificates.
     *
     * @return the test SupplyChainValidation
     * @throws IOException if there si
     */
    public static SupplyChainValidation getTestSupplyChainValidation() throws IOException {
        return getTestSupplyChainValidation(
                SupplyChainValidation.ValidationType.ENDORSEMENT_CREDENTIAL,
                AppraisalStatus.Status.PASS,
                getAllTestCertificates()
        );
    }
}
