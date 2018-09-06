package hirs.data.persist.certificate;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import hirs.data.persist.certificate.attributes.TPMSecurityAssertions;
import hirs.data.persist.certificate.attributes.TPMSpecification;

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

    /**
     * Tests the successful parsing of an EC using a test cert from STM.
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParse() throws IOException {
        String path = CertificateTest.class.getResource(TEST_ENDORSEMENT_CREDENTIAL).
                getPath();
        Path fPath = Paths.get(path);
        EndorsementCredential ec = new EndorsementCredential(fPath);
        Assert.assertNotNull(ec);

        //test the fields
        Assert.assertEquals(ec.getManufacturer(), "id:53544D20");
        Assert.assertEquals(ec.getModel(), "ST33ZP24PVSP");
        Assert.assertEquals(ec.getVersion(), "id:0D0C");

        TPMSpecification spec = ec.getTpmSpecification();
        Assert.assertEquals(spec.getFamily(), "1.2");
        Assert.assertEquals(spec.getLevel(), BigInteger.valueOf(2));
        Assert.assertEquals(spec.getRevision(), BigInteger.valueOf(116));

        TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        Assert.assertEquals(asserts.getVersion(), BigInteger.valueOf(0));
        Assert.assertTrue(asserts.isFieldUpgradeable());
        Assert.assertEquals(asserts.getEkGenType(),
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        Assert.assertEquals(asserts.getEkGenLoc(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        Assert.assertEquals(asserts.getEkCertGenLoc(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
    }

    /**
     * Tests the successful parsing of an EC using a test cert from NUC 1.
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParseNuc1() throws IOException {
        String path = CertificateTest.class.getResource(
                TEST_ENDORSEMENT_CREDENTIAL_NUC1).getPath();
        Path fPath = Paths.get(path);
        EndorsementCredential ec = new EndorsementCredential(fPath);
        Assert.assertNotNull(ec);

        //test the fields
        Assert.assertEquals(ec.getManufacturer(), "id:53544D20");
        Assert.assertEquals(ec.getModel(), "ST33ZP24PVSP");
        Assert.assertEquals(ec.getVersion(), "id:0D0C");

        TPMSpecification spec = ec.getTpmSpecification();
        Assert.assertEquals(spec.getFamily(), "1.2");
        Assert.assertEquals(spec.getLevel(), BigInteger.valueOf(2));
        Assert.assertEquals(spec.getRevision(), BigInteger.valueOf(116));

        TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        Assert.assertEquals(asserts.getVersion(), BigInteger.valueOf(0));
        Assert.assertTrue(asserts.isFieldUpgradeable());
        Assert.assertEquals(asserts.getEkGenType(),
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        Assert.assertEquals(asserts.getEkGenLoc(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        Assert.assertEquals(asserts.getEkCertGenLoc(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
    }

    /**
     * Tests the successful parsing of an EC using a test cert from NUC 1,
     * using the static builder method.
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParseNuc1BuilderMethod() throws IOException {
        String path = CertificateTest.class.getResource(
                TEST_ENDORSEMENT_CREDENTIAL_NUC1).getPath();
        Path fPath = Paths.get(path);
        byte[] ecBytes = Files.readAllBytes(fPath);

        EndorsementCredential ec = EndorsementCredential.parseWithPossibleHeader(ecBytes);
        Assert.assertNotNull(ec);

        //test the fields
        Assert.assertEquals(ec.getManufacturer(), "id:53544D20");
        Assert.assertEquals(ec.getModel(), "ST33ZP24PVSP");
        Assert.assertEquals(ec.getVersion(), "id:0D0C");

        TPMSpecification spec = ec.getTpmSpecification();
        Assert.assertEquals(spec.getFamily(), "1.2");
        Assert.assertEquals(spec.getLevel(), BigInteger.valueOf(2));
        Assert.assertEquals(spec.getRevision(), BigInteger.valueOf(116));

        TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        Assert.assertEquals(asserts.getVersion(), BigInteger.valueOf(0));
        Assert.assertTrue(asserts.isFieldUpgradeable());
        Assert.assertEquals(asserts.getEkGenType(),
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        Assert.assertEquals(asserts.getEkGenLoc(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        Assert.assertEquals(asserts.getEkCertGenLoc(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
    }

    /**
     * Tests the successful parsing of an EC using a test cert from NUC 2.
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testParseNuc2() throws IOException {
        String path = CertificateTest.class.getResource(
                TEST_ENDORSEMENT_CREDENTIAL_NUC2).getPath();
        Path fPath = Paths.get(path);
        EndorsementCredential ec = new EndorsementCredential(fPath);
        Assert.assertNotNull(ec);

        //test the fields
        Assert.assertEquals(ec.getManufacturer(), "id:53544D20");
        Assert.assertEquals(ec.getModel(), "ST33ZP24PVSP");
        Assert.assertEquals(ec.getVersion(), "id:0D0C");

        TPMSpecification spec = ec.getTpmSpecification();
        Assert.assertEquals(spec.getFamily(), "1.2");
        Assert.assertEquals(spec.getLevel(), BigInteger.valueOf(2));
        Assert.assertEquals(spec.getRevision(), BigInteger.valueOf(116));

        TPMSecurityAssertions asserts = ec.getTpmSecurityAssertions();
        Assert.assertEquals(asserts.getVersion(), BigInteger.valueOf(0));
        Assert.assertTrue(asserts.isFieldUpgradeable());
        Assert.assertEquals(asserts.getEkGenType(),
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        Assert.assertEquals(asserts.getEkGenLoc(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        Assert.assertEquals(asserts.getEkCertGenLoc(),
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
    }

    /**
     * Tests that different EC certificates aren't the same, even if their attributes are the same.
     * @throws IOException test failed due to invalid certificate parsing
     */
    @Test
    public void testCertsNotEqual() throws IOException {
        String path = CertificateTest.class.getResource(TEST_ENDORSEMENT_CREDENTIAL).getPath();
        Path fPath = Paths.get(path);
        EndorsementCredential ec1 = new EndorsementCredential(fPath);
        Assert.assertNotNull(ec1);

        path = CertificateTest.class.getResource(TEST_ENDORSEMENT_CREDENTIAL_NUC1).getPath();
        fPath = Paths.get(path);
        EndorsementCredential ec2 = new EndorsementCredential(fPath);
        Assert.assertNotNull(ec2);

        path = CertificateTest.class.getResource(TEST_ENDORSEMENT_CREDENTIAL_NUC2).getPath();
        fPath = Paths.get(path);
        EndorsementCredential ec3 = new EndorsementCredential(fPath);
        Assert.assertNotNull(ec3);

        Assert.assertNotEquals(ec1, ec2);
        Assert.assertNotEquals(ec2, ec3);
    }

}
