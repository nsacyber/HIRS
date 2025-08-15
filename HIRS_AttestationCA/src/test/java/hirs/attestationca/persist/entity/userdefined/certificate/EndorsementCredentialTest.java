package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.certificate.attributes.TPMSecurityAssertions;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.TPMSpecification;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the EndorsementCredential class.
 */
public class EndorsementCredentialTest {
    private static final String TEST_ENDORSEMENT_CREDENTIAL
            = "/certificates/ab21ccf2-tpmcert.pem";
    private static final String TEST_ENDORSEMENT_CREDENTIAL_NUC1
            = "/certificates/nuc-1/tpmcert.pem";
    private static final String TEST_ENDORSEMENT_CREDENTIAL_NUC2
            = "/certificates/nuc-2/tpmcert.pem";
    private static final String EK_CERT_WITH_SECURITY_ASSERTIONS =
            "/certificates/ek_cert_with_security_assertions.cer";

    private static final int TPM_SPEC_REVISION_NUM = 116;

    /**
     * Tests the successful parsing of an EC using a test cert from STM.
     *
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParse() throws IOException, URISyntaxException {
        final URI path = Objects.requireNonNull(
                this.getClass().getResource(TEST_ENDORSEMENT_CREDENTIAL)).toURI();
        final Path fPath = Paths.get(path);
        final EndorsementCredential ec = new EndorsementCredential(fPath);
        assertNotNull(ec);

        //test the fields
        assertEquals("id:53544D20", ec.getManufacturer());
        assertEquals("ST33ZP24PVSP", ec.getModel());
        assertEquals("id:0D0C", ec.getVersion());

        final TPMSpecification spec = ec.getTpmSpecification();
        assertEquals("1.2", spec.getFamily());
        assertEquals(spec.getLevel(), BigInteger.valueOf(2));
        assertEquals(spec.getRevision(), BigInteger.valueOf(TPM_SPEC_REVISION_NUM));

        final TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        assertEquals(BigInteger.valueOf(0), asserts.getTpmSecAssertsVersion());
        assertTrue(asserts.isFieldUpgradeable());
        assertEquals(TPMSecurityAssertions.EkGenerationType.INJECTED,
                asserts.getEkGenType());
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER,
                asserts.getEkGenerationLocation());
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER,
                asserts.getEkCertificateGenerationLocation());
    }

    /**
     * Tests the successful parsing of an EC using a test cert from NUC 1.
     *
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParseNuc1() throws IOException, URISyntaxException {
        final URI path = Objects.requireNonNull(this.getClass().getResource(
                TEST_ENDORSEMENT_CREDENTIAL_NUC1)).toURI();
        final Path fPath = Paths.get(path);
        final EndorsementCredential ec = new EndorsementCredential(fPath);
        assertNotNull(ec);

        //test the fields
        assertEquals("id:53544D20", ec.getManufacturer());
        assertEquals("ST33ZP24PVSP", ec.getModel());
        assertEquals("id:0D0C", ec.getVersion());

        final TPMSpecification spec = ec.getTpmSpecification();
        assertEquals("1.2", spec.getFamily());
        assertEquals(BigInteger.valueOf(2), spec.getLevel());
        assertEquals(BigInteger.valueOf(TPM_SPEC_REVISION_NUM), spec.getRevision());

        final TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        assertEquals(BigInteger.valueOf(0), asserts.getTpmSecAssertsVersion());
        assertTrue(asserts.isFieldUpgradeable());
        assertEquals(TPMSecurityAssertions.EkGenerationType.INJECTED,
                asserts.getEkGenType());
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER,
                asserts.getEkGenerationLocation());
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER,
                asserts.getEkCertificateGenerationLocation());
    }

    /**
     * Tests the successful parsing of an EC using a test cert from NUC 1,
     * using the static builder method.
     *
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParseNuc1BuilderMethod() throws IOException, URISyntaxException {
        final URI path = Objects.requireNonNull(this.getClass().getResource(
                TEST_ENDORSEMENT_CREDENTIAL_NUC1)).toURI();
        final Path fPath = Paths.get(path);
        byte[] ecBytes = Files.readAllBytes(fPath);

        final EndorsementCredential ec = EndorsementCredential.parseWithPossibleHeader(ecBytes);
        assertNotNull(ec);

        //test the fields
        assertEquals("id:53544D20", ec.getManufacturer());
        assertEquals("ST33ZP24PVSP", ec.getModel());
        assertEquals("id:0D0C", ec.getVersion());

        final TPMSpecification spec = ec.getTpmSpecification();
        assertEquals("1.2", spec.getFamily());
        assertEquals(BigInteger.valueOf(2), spec.getLevel());
        assertEquals(BigInteger.valueOf(TPM_SPEC_REVISION_NUM), spec.getRevision());

        final TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        assertEquals(BigInteger.valueOf(0), asserts.getTpmSecAssertsVersion());
        assertTrue(asserts.isFieldUpgradeable());
        assertEquals(TPMSecurityAssertions.EkGenerationType.INJECTED,
                asserts.getEkGenType());
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER,
                asserts.getEkGenerationLocation());
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER,
                asserts.getEkCertificateGenerationLocation());
    }

    /**
     * Tests the successful parsing of an EC using a test cert from NUC 2.
     *
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParseNuc2() throws IOException, URISyntaxException {
        final URI path = Objects.requireNonNull(this.getClass().getResource(
                TEST_ENDORSEMENT_CREDENTIAL_NUC2)).toURI();
        final Path fPath = Paths.get(path);
        final EndorsementCredential ec = new EndorsementCredential(fPath);
        assertNotNull(ec);

        //test the fields
        assertEquals("id:53544D20", ec.getManufacturer());
        assertEquals("ST33ZP24PVSP", ec.getModel());
        assertEquals("id:0D0C", ec.getVersion());

        final TPMSpecification spec = ec.getTpmSpecification();
        assertEquals("1.2", spec.getFamily());
        assertEquals(BigInteger.valueOf(2), spec.getLevel());
        assertEquals(BigInteger.valueOf(TPM_SPEC_REVISION_NUM), spec.getRevision());

        final TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        assertEquals(BigInteger.valueOf(0), asserts.getTpmSecAssertsVersion());
        assertTrue(asserts.isFieldUpgradeable());
        assertEquals(TPMSecurityAssertions.EkGenerationType.INJECTED,
                asserts.getEkGenType());
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER,
                asserts.getEkGenerationLocation());
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER,
                asserts.getEkCertificateGenerationLocation());
    }

    /**
     * Tests that different EC certificates aren't the same, even if their attributes are the same.
     *
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testCertsNotEqual() throws IOException, URISyntaxException {
        URI path = Objects.requireNonNull(this.getClass().getResource(TEST_ENDORSEMENT_CREDENTIAL)).toURI();
        Path fPath = Paths.get(path);
        final EndorsementCredential ec1 = new EndorsementCredential(fPath);
        assertNotNull(ec1);

        path = Objects.requireNonNull(this.getClass().getResource(TEST_ENDORSEMENT_CREDENTIAL_NUC1)).toURI();
        fPath = Paths.get(path);
        final EndorsementCredential ec2 = new EndorsementCredential(fPath);
        assertNotNull(ec2);

        path = Objects.requireNonNull(this.getClass().getResource(TEST_ENDORSEMENT_CREDENTIAL_NUC2)).toURI();
        fPath = Paths.get(path);
        final EndorsementCredential ec3 = new EndorsementCredential(fPath);
        assertNotNull(ec3);

        assertNotEquals(ec1, ec2);
        assertNotEquals(ec2, ec3);
    }

    /**
     * Tests that EndorsementCredential correctly parses out TPM Security Assertions from a
     * provided TPM EK Certificate.
     *
     * @throws IOException if there is a problem reading the cert file at the given path
     */
    @Test
    public void testTpmSecurityAssertionsParsing() throws IOException, URISyntaxException {
        final Path fPath = Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(EK_CERT_WITH_SECURITY_ASSERTIONS)).toURI());
        final EndorsementCredential ec = new EndorsementCredential(fPath);

        final TPMSecurityAssertions securityAssertions = ec.getTpmSecurityAssertions();
        assertEquals(BigInteger.ONE, securityAssertions.getTpmSecAssertsVersion());
        assertTrue(securityAssertions.isFieldUpgradeable());
        assertEquals(TPMSecurityAssertions.EkGenerationType.INJECTED,
                securityAssertions.getEkGenType());
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER,
                securityAssertions.getEkGenerationLocation());
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER,
                securityAssertions.getEkCertificateGenerationLocation());
    }
}
