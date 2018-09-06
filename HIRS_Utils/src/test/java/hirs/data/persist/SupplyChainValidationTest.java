package hirs.data.persist;

import org.testng.Assert;
import org.testng.annotations.Test;
import hirs.data.persist.certificate.CertificateTest;

import java.io.IOException;
import java.util.List;
import hirs.data.persist.certificate.Certificate;

/**
 * Simple tests for the {@link SupplyChainValidation} class.  Tests for the persistence of this
 * class are located in {@link SupplyChainValidationSummaryTest}.
 */
public class SupplyChainValidationTest {
    private static final String MESSAGE = "Some message.";

    /**
     * Test that this class' getter methods work properly.
     *
     * @throws IOException if there is a problem deserializing certificates
     */
    @Test
    public void testGetters() throws IOException {
        SupplyChainValidation validation = getTestSupplyChainValidation();
        Assert.assertEquals(
                validation.getValidationType(),
                SupplyChainValidation.ValidationType.ENDORSEMENT_CREDENTIAL
        );
        Assert.assertEquals(
                validation.getCertificatesUsed(),
                CertificateTest.getAllTestCertificates()
        );
        Assert.assertEquals(validation.getMessage(), MESSAGE);
    }

    /**
     * Test that a SupplyChainValidation can't be instantiated with a null validation type.
     *
     * @throws IOException if there is a problem deserializing certificates
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullValidationType() throws IOException {
        new SupplyChainValidation(
                null,
                AppraisalStatus.Status.PASS,
                CertificateTest.getAllTestCertificates(),
                MESSAGE
        );
    }

    /**
     * Test that a SupplyChainValidation can't be instantiated with a null certificate list.
     *
     * @throws IOException if there is a problem deserializing certificates
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullCertificates() throws IOException {
        new SupplyChainValidation(
                SupplyChainValidation.ValidationType.ENDORSEMENT_CREDENTIAL,
                AppraisalStatus.Status.PASS,
                null,
                MESSAGE
        );
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
                CertificateTest.getAllTestCertificates(),
                MESSAGE
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
                CertificateTest.getAllTestCertificates()
        );
    }

    /**
     * Construct a SupplyChainValidation for use in tests according to the provided parameters.
     *
     * @param type the type of validation
     * @param result the appraisal result
     * @param certificates the certificates related to this validation
     * @return the resulting SupplyChainValidation object
     */
    public static SupplyChainValidation getTestSupplyChainValidation(
            final SupplyChainValidation.ValidationType type,
            final AppraisalStatus.Status result,
            final List<Certificate> certificates) {
        return new SupplyChainValidation(
                type,
                result,
                certificates,
                MESSAGE
        );
    }
}
