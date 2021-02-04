package hirs.persist;

import hirs.data.persist.SpringPersistenceTest;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
import org.springframework.util.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class contains simple tests some of the functionality of {@link CertificateSelector}.
 * For actual functional tests that test certificate retrieval using the class, see
 * {@link DBCertificateManagerTest}.
 */
public class CertificateSelectorTest extends SpringPersistenceTest {
    private CertificateManager certMan;

    /**
     * Sets up a certificate manager for this test.
     */
    @BeforeClass
    public void setUp() {
        certMan = new DBCertificateManager(sessionFactory);
    }

    /**
     * Test that a new CertificateSelector can be constructed.
     */
    @Test
    public void testConstruction() {
        Assert.notNull(CertificateAuthorityCredential.select(certMan));
    }

    /**
     * Test that a new CertificateSelector cannot be constructed
     * with a null CertificateManager.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConstructionNullManager() {
        CertificateAuthorityCredential.select(null);
    }

    /**
     * Test that a null issuer cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullByIssuer() {
        CertificateAuthorityCredential.select(certMan).byIssuer(null);
    }

    /**
     * Test that an empty issuer cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyByIssuer() {
        CertificateAuthorityCredential.select(certMan).byIssuer("");
    }

    /**
     * Test that a null subject cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullBySubject() {
        CertificateAuthorityCredential.select(certMan).bySubject(null);
    }

    /**
     * Test that an empty subject cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyBySubject() {
        CertificateAuthorityCredential.select(certMan).bySubject("");
    }

    /**
     * Test that a null encoded public key cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullByEncodedPublicKey() {
        CertificateAuthorityCredential.select(certMan).byEncodedPublicKey(null);
    }

    /**
     * Test that an empty encoded public key cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyByEncodedPublicKey() {
        CertificateAuthorityCredential.select(certMan).byEncodedPublicKey(new byte[]{});
    }

    /**
     * Test that an empty public key modulus cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullByPublicKeyModulus() {
        CertificateAuthorityCredential.select(certMan).byPublicKeyModulus(null);
    }

    /**
     * Test that an empty organization cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullByIssuerOrganization() {
        CertificateAuthorityCredential.select(certMan).byIssuerSorted(null);
    }

    /**
     * Test that an empty organization cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullBySubjectOrganization() {
        CertificateAuthorityCredential.select(certMan).bySubjectSorted(null);
    }
}
