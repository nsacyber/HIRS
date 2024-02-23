package hirs.attestationca.persist.entity.userdefined.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.TPMSecurityAssertions;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.TPMSpecification;

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

    /**
     * Tests the successful parsing of an EC using a test cert from STM.
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParse() throws IOException {
        String path = this.getClass().getResource(TEST_ENDORSEMENT_CREDENTIAL).
                getPath();
        Path fPath = Paths.get(path);
        EndorsementCredential ec = new EndorsementCredential(fPath);
        assertNotNull(ec);

        //test the fields
        assertEquals(ec.getManufacturer(), "id:53544D20");
        assertEquals(ec.getModel(), "ST33ZP24PVSP");
        assertEquals(ec.getVersion(), "id:0D0C");

        TPMSpecification spec = ec.getTpmSpecification();
        assertEquals(spec.getFamily(), "1.2");
        assertEquals(spec.getLevel(), BigInteger.valueOf(2));
        assertEquals(spec.getRevision(), BigInteger.valueOf(116));

        TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        assertEquals(asserts.getTpmSecAssertsVersion(), BigInteger.valueOf(0));
        assertTrue(asserts.isFieldUpgradeable());
        assertEquals(asserts.getEkGenType(),
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        assertEquals(asserts.getEkGenerationLocation(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        assertEquals(asserts.getEkCertificateGenerationLocation(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
    }

    /**
     * Tests the successful parsing of an EC using a test cert from NUC 1.
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParseNuc1() throws IOException {
        String path = this.getClass().getResource(
                TEST_ENDORSEMENT_CREDENTIAL_NUC1).getPath();
        Path fPath = Paths.get(path);
        EndorsementCredential ec = new EndorsementCredential(fPath);
        assertNotNull(ec);

        //test the fields
        assertEquals(ec.getManufacturer(), "id:53544D20");
        assertEquals(ec.getModel(), "ST33ZP24PVSP");
        assertEquals(ec.getVersion(), "id:0D0C");

        TPMSpecification spec = ec.getTpmSpecification();
        assertEquals(spec.getFamily(), "1.2");
        assertEquals(spec.getLevel(), BigInteger.valueOf(2));
        assertEquals(spec.getRevision(), BigInteger.valueOf(116));

        TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        assertEquals(asserts.getTpmSecAssertsVersion(), BigInteger.valueOf(0));
        assertTrue(asserts.isFieldUpgradeable());
        assertEquals(asserts.getEkGenType(),
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        assertEquals(asserts.getEkGenerationLocation(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        assertEquals(asserts.getEkCertificateGenerationLocation(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
    }

    /**
     * Tests the successful parsing of an EC using a test cert from NUC 1,
     * using the static builder method.
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParseNuc1BuilderMethod() throws IOException {
        String path = this.getClass().getResource(
                TEST_ENDORSEMENT_CREDENTIAL_NUC1).getPath();
        Path fPath = Paths.get(path);
        byte[] ecBytes = Files.readAllBytes(fPath);

        EndorsementCredential ec = EndorsementCredential.parseWithPossibleHeader(ecBytes);
        assertNotNull(ec);

        //test the fields
        assertEquals(ec.getManufacturer(), "id:53544D20");
        assertEquals(ec.getModel(), "ST33ZP24PVSP");
        assertEquals(ec.getVersion(), "id:0D0C");

        TPMSpecification spec = ec.getTpmSpecification();
        assertEquals(spec.getFamily(), "1.2");
        assertEquals(spec.getLevel(), BigInteger.valueOf(2));
        assertEquals(spec.getRevision(), BigInteger.valueOf(116));

        TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        assertEquals(asserts.getTpmSecAssertsVersion(), BigInteger.valueOf(0));
        assertTrue(asserts.isFieldUpgradeable());
        assertEquals(asserts.getEkGenType(),
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        assertEquals(asserts.getEkGenerationLocation(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        assertEquals(asserts.getEkCertificateGenerationLocation(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
    }

    /**
     * Tests the successful parsing of an EC using a test cert from NUC 2.
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParseNuc2() throws IOException {
        String path = this.getClass().getResource(
                TEST_ENDORSEMENT_CREDENTIAL_NUC2).getPath();
        Path fPath = Paths.get(path);
        EndorsementCredential ec = new EndorsementCredential(fPath);
        assertNotNull(ec);

        //test the fields
        assertEquals(ec.getManufacturer(), "id:53544D20");
        assertEquals(ec.getModel(), "ST33ZP24PVSP");
        assertEquals(ec.getVersion(), "id:0D0C");

        TPMSpecification spec = ec.getTpmSpecification();
        assertEquals(spec.getFamily(), "1.2");
        assertEquals(spec.getLevel(), BigInteger.valueOf(2));
        assertEquals(spec.getRevision(), BigInteger.valueOf(116));

        TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        assertEquals(asserts.getTpmSecAssertsVersion(), BigInteger.valueOf(0));
        assertTrue(asserts.isFieldUpgradeable());
        assertEquals(asserts.getEkGenType(),
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        assertEquals(asserts.getEkGenerationLocation(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        assertEquals(asserts.getEkCertificateGenerationLocation(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
    }

    /**
     * Tests that different EC certificates aren't the same, even if their attributes are the same.
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testCertsNotEqual() throws IOException {
        String path = this.getClass().getResource(TEST_ENDORSEMENT_CREDENTIAL).getPath();
        Path fPath = Paths.get(path);
        EndorsementCredential ec1 = new EndorsementCredential(fPath);
        assertNotNull(ec1);

        path = this.getClass().getResource(TEST_ENDORSEMENT_CREDENTIAL_NUC1).getPath();
        fPath = Paths.get(path);
        EndorsementCredential ec2 = new EndorsementCredential(fPath);
        assertNotNull(ec2);

        path = this.getClass().getResource(TEST_ENDORSEMENT_CREDENTIAL_NUC2).getPath();
        fPath = Paths.get(path);
        EndorsementCredential ec3 = new EndorsementCredential(fPath);
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
    public void testTpmSecurityAssertionsParsing() throws IOException {
        Path fPath = Paths.get(this.getClass()
                .getResource(EK_CERT_WITH_SECURITY_ASSERTIONS).getPath());
        EndorsementCredential ec = new EndorsementCredential(fPath);

        TPMSecurityAssertions securityAssertions = ec.getTpmSecurityAssertions();
        assertEquals(securityAssertions.getTpmSecAssertsVersion(), BigInteger.ONE);
        assertTrue(securityAssertions.isFieldUpgradeable());
        assertEquals(securityAssertions.getEkGenType(),
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        assertEquals(securityAssertions.getEkGenerationLocation(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        assertEquals(securityAssertions.getEkCertificateGenerationLocation(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
    }

}
