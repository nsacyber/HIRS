package hirs.persist;

import hirs.data.persist.SpringPersistenceTest;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
import hirs.data.persist.certificate.ConformanceCredential;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.IssuedAttestationCertificate;
import hirs.data.persist.certificate.PlatformCredential;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import hirs.data.persist.certificate.CertificateTest;
import hirs.data.persist.certificate.PlatformCredentialTest;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * This class tests the storage, retrieval, and deletion of {@link Certificate}s.
 */
public class DBCertificateManagerTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(DBCertificateManagerTest.class);

    private Certificate rootCert;
    private Certificate intelIntermediateCert;
    private Certificate sgiIntermediateCert;
    private Certificate anotherSelfSignedCert;
    private Certificate intelPlatformCert;
    private Certificate stmEkCert;
    private Certificate stmRootCaCert;
    private Certificate gsTpmRootCaCert;
    private Certificate hirsClientCert;

    private Map<Class<? extends Certificate>, Certificate> testCertificates = new HashMap<>();

    /**
     * Set up a session factory for the tests.
     */
    @BeforeClass
    public void setup() {
        LOGGER.debug("retrieving session factory");
    }

    /**
     * Release resources associated with the test.
     */
    @AfterClass
    public void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * This method (re)instantiates some common objects used by the tests below.
     *
     * @throws IOException if there is a problem creating the certificates
     */
    @BeforeMethod
    public void setupTestObjects() throws IOException {
        // create indivdual test certificates
        rootCert = CertificateTest.getTestCertificate(CertificateTest.FAKE_ROOT_CA_FILE);
        intelIntermediateCert = CertificateTest.getTestCertificate(
                CertificateTest.FAKE_INTEL_INT_CA_FILE
        );
        sgiIntermediateCert = CertificateTest.getTestCertificate(
                CertificateTest.FAKE_SGI_INT_CA_FILE
        );
        intelPlatformCert = CertificateTest.getTestCertificate(
                PlatformCredential.class,
                PlatformCredentialTest.TEST_PLATFORM_CERT_2
        );
        anotherSelfSignedCert = CertificateTest.getTestCertificate(
                CertificateTest.ANOTHER_SELF_SIGNED_FILE
        );

        stmEkCert = CertificateTest.getTestCertificate(EndorsementCredential.class,
                CertificateTest.STM_NUC1_EC);

        stmRootCaCert = CertificateTest.getTestCertificate(CertificateTest.STM_ROOT_CA);

        gsTpmRootCaCert = CertificateTest.getTestCertificate(CertificateTest.GS_ROOT_CA);

        // create a collection of test certificates, one of each type
        testCertificates.put(
                CertificateAuthorityCredential.class,
                CertificateTest.getTestCertificate(
                        CertificateAuthorityCredential.class,
                        CertificateTest.FAKE_ROOT_CA_FILE
                )
        );

        testCertificates.put(
                ConformanceCredential.class,
                CertificateTest.getTestCertificate(
                        ConformanceCredential.class,
                        CertificateTest.FAKE_INTEL_INT_CA_FILE
                )
        );

        EndorsementCredential endorsementCredential =
                (EndorsementCredential) CertificateTest.getTestCertificate(
                        EndorsementCredential.class,
                        CertificateTest.TEST_EC
                );
        testCertificates.put(EndorsementCredential.class, endorsementCredential);

        PlatformCredential platformCredential =
                (PlatformCredential) CertificateTest.getTestCertificate(
                        PlatformCredential.class,
                        PlatformCredentialTest.TEST_PLATFORM_CERT_2
                );

        testCertificates.put(PlatformCredential.class, platformCredential);

        //Set up multi-platform cert Attestation Cert
        Set<PlatformCredential> platformCredentials = new HashSet<>();
        platformCredentials.add(platformCredential);

        IssuedAttestationCertificate issuedCert =
                (IssuedAttestationCertificate)
                        CertificateTest.getTestCertificate(IssuedAttestationCertificate.class,
                CertificateTest.ISSUED_CLIENT_CERT, endorsementCredential, platformCredentials);

        testCertificates.put(IssuedAttestationCertificate.class, issuedCert);
    }

    /**
     * Resets the test state to a known good state. This resets
     * the database by removing all {@link Certificate} objects.
     */
    @AfterMethod
    public void resetTestState() {
        LOGGER.debug("reset test state");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final Class<?>[] clazzes =
                {Certificate.class};
        for (Class<?> clazz : clazzes) {
            final List<?> objects = session.createCriteria(clazz).list();
            for (Object o : objects) {
                LOGGER.debug("deleting object: {}", o);
                session.delete(o);
            }
            LOGGER.debug("all {} removed", clazz);
        }
        session.getTransaction().commit();
    }

    /**
     * Tests that a Certificate can be stored in the database.
     *
     * @throws IOException if there is a problem creating the certificate
     */
    @Test
    public void testSave() throws IOException {
        DBCertificateManager certMan = new DBCertificateManager(sessionFactory);
        saveTestCertsToDb(certMan);
    }

    /**
     * Tests that a Certificate can be stored in and subsequently retrieved from the database.
     *
     * @throws IOException if there is a problem creating the certificate
     */
    @Test
    public void testGet() throws IOException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);
        saveTestCertsToDb(certMan);

        Assert.assertEquals(
                testCertificates.get(CertificateAuthorityCredential.class),
                CertificateAuthorityCredential.select(certMan).getCertificate()
        );

        Assert.assertEquals(
                testCertificates.get(ConformanceCredential.class),
                ConformanceCredential.select(certMan).getCertificate()
        );

        Assert.assertEquals(
                testCertificates.get(EndorsementCredential.class),
                EndorsementCredential.select(certMan).getCertificate()
        );

        Assert.assertEquals(
                testCertificates.get(PlatformCredential.class),
                PlatformCredential.select(certMan).getCertificate()
        );

        IssuedAttestationCertificate issuedAttestationCertificate =
                IssuedAttestationCertificate.select(certMan).getCertificate();
        Assert.assertEquals(
                testCertificates.get(IssuedAttestationCertificate.class),
                issuedAttestationCertificate);

        // verify issued cert's references
        Assert.assertTrue(issuedAttestationCertificate.getEndorsementCredential()
                .equals(EndorsementCredential.select(certMan).getCertificate()));

        Assert.assertTrue(issuedAttestationCertificate.getPlatformCredentials()
                .containsAll(PlatformCredential.select(certMan).getCertificates()));

    }

    private void saveTestCertsToDb(final CertificateManager certMan) {
        saveTestCertsToDb(certMan, false);
    }

    private void saveTestCertsToDb(final CertificateManager certMan, final boolean isArchived) {
        // save IssuedCerts last, as they reference other certs which aren't stored yet.
        // Ensure that the dependent certs are stored prior to storing Issued certs.
        List<Certificate> secondaryCertsToSave = new ArrayList<>();

        for (Map.Entry<Class<? extends Certificate>, Certificate> entry
                : testCertificates.entrySet()) {
            if (isArchived) {
                entry.getValue().archive();
            }
            if (entry.getKey() != IssuedAttestationCertificate.class) {
                Certificate savedCert = certMan.save(entry.getValue());
                Assert.assertEquals(entry.getValue(), savedCert);
                Assert.assertNotNull(savedCert.getId());
            } else {
                secondaryCertsToSave.add(entry.getValue());
            }
        }

        for (Certificate cert : secondaryCertsToSave) {
            Certificate savedCert = certMan.save(cert);
            Assert.assertEquals(cert, savedCert);
            Assert.assertNotNull(savedCert.getId());
        }
    }


    /**
     * Verifies that by default, the selector excludes certs that have been archived, but that
     * they can be retrieved when includeArchived() is called.
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testGetArchivedCert() throws IOException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        saveTestCertsToDb(certMan, true);

        CertificateAuthorityCredential cert =
                CertificateAuthorityCredential.select(certMan).getCertificate();

        Assert.assertNull(cert);

        cert = CertificateAuthorityCredential.select(certMan).includeArchived().getCertificate();

        Assert.assertNotNull(cert);
    }

    /**
     * Tests that multiple Certificates can retrieved by their common type.
     *
     * @throws IOException if there is a problem creating the certificate
     */
    @Test
    public void testGetAllByType() throws IOException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        Certificate savedRootCert = certMan.save(rootCert);
        Certificate savedIntelIntermediateCert = certMan.save(intelIntermediateCert);
        Certificate savedSGIIntermediateCert  = certMan.save(sgiIntermediateCert);

        Set<CertificateAuthorityCredential> retrievedCerts =
                CertificateAuthorityCredential.select(certMan)
                .getCertificates();

        Assert.assertEquals(retrievedCerts, new HashSet<>(
                Arrays.asList(savedRootCert, savedIntelIntermediateCert, savedSGIIntermediateCert))
        );
    }

    /**
     * Tests that multiple Certificates can retrieved by their common type and issuer.
     * @throws IOException if there is a problem creating the certificate
     * @throws CertificateException if there is a problem deserializing the original X509Certificate
     */
    @Test
    public void testGetAllByIssuer() throws IOException, CertificateException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        Certificate savedRootCert = certMan.save(rootCert);
        Certificate savedIntelIntermediateCert = certMan.save(intelIntermediateCert);
        Certificate savedSGIIntermediateCert  = certMan.save(sgiIntermediateCert);

        Set<CertificateAuthorityCredential> retrievedCerts =
                CertificateAuthorityCredential.select(certMan)
                .byIssuer(intelIntermediateCert.getX509Certificate().getIssuerDN().getName())
                .getCertificates();

        Assert.assertEquals(
                retrievedCerts,
                new HashSet<>(Arrays.asList(
                        savedRootCert, savedIntelIntermediateCert, savedSGIIntermediateCert
                ))
        );
    }

    /**
     * Tests that multiple Certificates can retrieved by their common type and subject organization.
     * @throws IOException if there is a problem creating the certificate
     * @throws CertificateException if there is a problem deserializing the original X509Certificate
     */
    @Test
    public void testGetAllBySubjectOrganization() throws IOException, CertificateException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        Certificate savedStmRootCert = certMan.save(stmRootCaCert);
        certMan.save(stmEkCert);
        Certificate savedGsRootCa = certMan.save(gsTpmRootCaCert);

        Set<CertificateAuthorityCredential> retrievedCerts =
                CertificateAuthorityCredential.select(certMan)
                        .bySubjectOrganization(stmEkCert.getIssuerOrganization())
                        .getCertificates();

        Assert.assertEquals(
                retrievedCerts,
                new HashSet<>(Arrays.asList(
                        savedStmRootCert))
        );

        Set<CertificateAuthorityCredential> secondRetrievedCerts =
                CertificateAuthorityCredential.select(certMan)
                        .bySubjectOrganization(stmRootCaCert.getIssuerOrganization())
                        .getCertificates();

        Assert.assertEquals(
                secondRetrievedCerts,
                new HashSet<>(Arrays.asList(
                        savedGsRootCa))
        );
    }

    /**
     * Tests that multiple Certificates can retrieved by their common type and issuer organization.
     * @throws IOException if there is a problem creating the certificate
     * @throws CertificateException if there is a problem deserializing the original X509Certificate
     */
    @Test
    public void testGetAllByIssuerOrganization() throws IOException, CertificateException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        Certificate savedStmRootCert = certMan.save(stmRootCaCert);
        certMan.save(stmEkCert);
        certMan.save(gsTpmRootCaCert);

        Set<CertificateAuthorityCredential> retrievedCerts =
                CertificateAuthorityCredential.select(certMan)
                        .byIssuerOrganization(stmRootCaCert.getIssuerOrganization())
                        .getCertificates();

        Assert.assertEquals(
                retrievedCerts,
                new HashSet<>(Arrays.asList(
                        savedStmRootCert, gsTpmRootCaCert))
        );
    }

    /**
     * Tests that a single Certificate can be retrieved amongst many stored Certificates according
     * to its type and subject.
     * @throws IOException if there is a problem creating the certificate
     * @throws CertificateException if there is a problem deserializing the original X509Certificate
     */
    @Test
    public void testGetBySubject() throws IOException, CertificateException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        certMan.save(rootCert);
        certMan.save(sgiIntermediateCert);
        Certificate savedIntelIntermediateCert = certMan.save(intelIntermediateCert);

        Set<CertificateAuthorityCredential> retrievedCerts =
                CertificateAuthorityCredential.select(certMan)
                .bySubject(intelIntermediateCert.getX509Certificate().getSubjectDN().getName())
                .getCertificates();
        Assert.assertEquals(retrievedCerts, Collections.singleton(savedIntelIntermediateCert));
    }

    /**
     * Tests that a single Certificate can be retrieved amongst many stored Certificates according
     * to its type and serial number.
     *
     * @throws IOException if there is a problem creating the certificate
     * @throws CertificateException if there is a problem deserializing the original X509Certificate
     */
    @Test
    public void testGetSingleBySerialNumber() throws IOException, CertificateException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        certMan.save(rootCert);
        certMan.save(sgiIntermediateCert);
        Certificate savedIntelIntermediateCert = certMan.save(intelIntermediateCert);

        Set<CertificateAuthorityCredential> retrievedCerts =
                CertificateAuthorityCredential.select(certMan)
                .bySerialNumber(
                        intelIntermediateCert.getX509Certificate().getSerialNumber()
                )
                .getCertificates();
        Assert.assertEquals(retrievedCerts, Collections.singleton(savedIntelIntermediateCert));
    }

    /**
     * Tests that a single Certificate can be retrieved amongst many stored Certificates according
     * to its type and encoded public key.
     *
     * @throws IOException if there is a problem creating the certificate
     * @throws CertificateException if there is a problem deserializing the original X509Certificate
     */
    @Test
    public void testGetSingleByPublicKey() throws IOException, CertificateException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        certMan.save(rootCert);
        certMan.save(sgiIntermediateCert);
        Certificate savedIntelIntermediateCert = certMan.save(intelIntermediateCert);

        Set<CertificateAuthorityCredential> retrievedCerts =
                CertificateAuthorityCredential.select(certMan)
                .byEncodedPublicKey(
                        intelIntermediateCert.getX509Certificate().getPublicKey().getEncoded()
                )
                .getCertificates();
        Assert.assertEquals(retrievedCerts, Collections.singleton(savedIntelIntermediateCert));
    }

    /**
     * Tests that a single Certificate can be retrieved amongst many stored Certificates according
     * to its type and public key modulus.
     *
     * @throws IOException if there is a problem creating the certificate
     */
    @Test
    public void testGetSingleByPublicKeyModulus() throws IOException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        certMan.save(rootCert);
        certMan.save(sgiIntermediateCert);
        Certificate savedIntelIntermediateCert = certMan.save(intelIntermediateCert);

        Set<CertificateAuthorityCredential> retrievedCerts =
                CertificateAuthorityCredential.select(certMan)
                        .byPublicKeyModulus(Certificate.getPublicKeyModulus(
                                savedIntelIntermediateCert.getX509Certificate()
                        ))
                        .getCertificates();
        Assert.assertEquals(retrievedCerts, Collections.singleton(savedIntelIntermediateCert));
    }

    /**
     * Tests that a single certificate can be retrieved amongst many other stored certificates, only
     * according to its type.
     *
     * @throws IOException if there is a problem creating the certificate
     */
    @Test
    public void testGetSingleByType() throws IOException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        Certificate platformSgiCert = CertificateTest.getTestCertificate(
                PlatformCredential.class, PlatformCredentialTest.TEST_PLATFORM_CERT_2
        );

        certMan.save(rootCert);
        certMan.save(intelIntermediateCert);
        certMan.save(platformSgiCert);

        Set<PlatformCredential> retrievedCerts = PlatformCredential.select(certMan)
                .getCertificates();
        Assert.assertEquals(retrievedCerts, Collections.singleton(platformSgiCert));
    }

    /**
     * Tests that a single Certificate can be retrieved amongst many stored Certificates according
     * to its type and subject key identifier.
     *
     * @throws IOException if there is a problem creating the certificate
     * @throws CertificateException if there is a problem deserializing the original X509Certificate
     */
    @Test
    public void testGetCertAuthByCriteria() throws IOException, CertificateException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        CertificateAuthorityCredential savedRootCert = (CertificateAuthorityCredential)
                certMan.save(rootCert);
        certMan.save(sgiIntermediateCert);
        certMan.save(intelIntermediateCert);

        byte[] subjectKeyIdentifier = savedRootCert.getSubjectKeyIdentifier();

        Set<CertificateAuthorityCredential> retrievedCerts =
                CertificateAuthorityCredential.select(certMan)
                .bySubjectKeyIdentifier(subjectKeyIdentifier)
                .getCertificates();
        Assert.assertEquals(retrievedCerts, Collections.singleton(savedRootCert));

        String issuer = savedRootCert.getX509Certificate().getIssuerDN().getName();
        BigInteger serialNumber = savedRootCert.getX509Certificate().getSerialNumber();
        byte[] encodedPublicKey = savedRootCert.getX509Certificate().getPublicKey()
                .getEncoded();

        retrievedCerts = CertificateAuthorityCredential.select(certMan)
                        .bySubjectKeyIdentifier(subjectKeyIdentifier)
                        .byIssuer(issuer)
                        .bySerialNumber(serialNumber)
                        .byEncodedPublicKey(encodedPublicKey)
                        .getCertificates();

        Assert.assertEquals(retrievedCerts, Collections.singleton(savedRootCert));
    }

    /**
     * Tests that a PlatformCredential can be retrieved by its fields.
     *
     * @throws IOException if there is a problem constructing test certificates
     */
    @Test
    public void testGetPlatformByCriteria() throws IOException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);
        saveTestCertsToDb(certMan);

        PlatformCredential platformCredentialByManufacturer
                = PlatformCredential
                        .select(certMan)
                        .byManufacturer("Intel")
                        .getCertificate();

        Assert.assertEquals(
                testCertificates.get(PlatformCredential.class),
                platformCredentialByManufacturer
        );

        Assert.assertEquals(
                testCertificates.get(PlatformCredential.class),
                PlatformCredential.select(certMan).byModel("DE3815TYKH")
                        .getCertificate()
        );

        Assert.assertEquals(
                testCertificates.get(PlatformCredential.class),
                PlatformCredential.select(certMan).byVersion("H26998-402")
                        .getCertificate()
        );

        Assert.assertEquals(
                testCertificates.get(PlatformCredential.class),
                PlatformCredential.select(certMan).byIssuer(
                        "C=US,ST=CA,L=Santa Clara,O=Intel Corporation,"
                                + "OU=Transparent Supply Chain,CN=www.intel.com"
                ).getCertificate()
        );


        UUID uuid = platformCredentialByManufacturer.getId();

        Assert.assertEquals(
                testCertificates.get(PlatformCredential.class),
                PlatformCredential.select(certMan).byEntityId(uuid).getCertificate()
        );

        Assert.assertEquals(
                testCertificates.get(PlatformCredential.class),
                PlatformCredential.select(certMan)
                        .byManufacturer("Intel")
                        .byModel("DE3815TYKH")
                        .byVersion("H26998-402")
                        .byIssuer(
                        "C=US,ST=CA,L=Santa Clara,O=Intel Corporation,"
                                + "OU=Transparent Supply Chain,CN=www.intel.com"
                ).getCertificate()
        );

        Assert.assertNull(
                PlatformCredential.select(certMan).byBoardSerialNumber(
                        "BQKP52840678"
                ).getCertificate()
        );
    }

    /**
     * Tests that a {@link CertificateSelector} can be used to retrieve certificates in various
     * forms, including {@link Certificate}, {@link X509Certificate}, and {@link KeyStore}.
     *
     * @throws IOException if there is a problem creating the certificate
     * @throws KeyStoreException if there is a problem constructing the resultant KeyStore
     * @throws CertificateException if there is a problem deserializing the original X509Certificate
     */
    @Test
    public void testGetSingleInDifferentForms()
            throws IOException, KeyStoreException, CertificateException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        certMan.save(rootCert);
        certMan.save(intelIntermediateCert);
        certMan.save(sgiIntermediateCert);

        String issuer = intelIntermediateCert.getX509Certificate().getIssuerDN().getName();
        BigInteger serialNumber = intelIntermediateCert.getX509Certificate().getSerialNumber();
        byte[] encodedPublicKey = intelIntermediateCert.getX509Certificate().getPublicKey()
                .getEncoded();
        byte[] subjectKeyIdentifier =
                ((CertificateAuthorityCredential) intelIntermediateCert).getSubjectKeyIdentifier();
        UUID certId = intelIntermediateCert.getId();

        CertificateSelector certSelector = CertificateAuthorityCredential.select(certMan)
                .bySubjectKeyIdentifier(subjectKeyIdentifier)
                .byIssuer(issuer)
                .bySerialNumber(serialNumber)
                .byEncodedPublicKey(encodedPublicKey)
                .byEntityId(certId);

        Assert.assertEquals(
                certSelector.getCertificate(),
                intelIntermediateCert
        );

        Assert.assertEquals(
                certSelector.getCertificates(),
                Collections.singleton(intelIntermediateCert)
        );

        Assert.assertEquals(
                certSelector.getX509Certificate(),
                intelIntermediateCert.getX509Certificate()
        );

        Assert.assertEquals(
                certSelector.getX509Certificates(),
                Collections.singleton(intelIntermediateCert.getX509Certificate())
        );

        Assert.assertNotNull(
                certSelector.getKeyStore().getCertificateAlias(
                        intelIntermediateCert.getX509Certificate()
                )
        );
    }

    /**
     * Tests that selecting on criteria that no stored Certificates match returns either null,
     * an empty set, or an empty KeyStore, as appropriate.
     *
     * @throws IOException if there is a problem creating the certificate
     * @throws KeyStoreException if there is a problem constructing the resultant KeyStore
     */
    @Test
    public void testGetNone() throws IOException, KeyStoreException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        certMan.save(rootCert);
        certMan.save(intelIntermediateCert);
        certMan.save(sgiIntermediateCert);

        CertificateSelector certSelector = CertificateAuthorityCredential.select(certMan)
                .byIssuer("An issuer that doesn't exist");

        Assert.assertEquals(certSelector.getCertificates(), Collections.EMPTY_SET);

        Assert.assertEquals(certSelector.getCertificate(), null);

        Assert.assertEquals(certSelector.getX509Certificates(), Collections.EMPTY_SET);

        Assert.assertEquals(certSelector.getX509Certificate(), null);

        Assert.assertEquals(certSelector.getKeyStore().size(), 0);
    }

    /**
     * Tests that a certificate cannot be stored twice in the database.
     */
    @Test(expectedExceptions = DBManagerException.class)
    public void testStoreDuplicate() {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);
        certMan.save(rootCert);
        certMan.save(rootCert);
    }

    /**
     * Tests that a certificate can be stored twice under different roles in the database.
     * @throws IOException if there is a problem creating the certificate
     */
    @Test
    public void testStoreSameBinaryCertDifferentCertClass() throws IOException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        Certificate caCert = CertificateTest.getTestCertificate(
                CertificateAuthorityCredential.class, CertificateTest.FAKE_ROOT_CA_FILE
        );
        certMan.save(caCert);

        Certificate ecCert = CertificateTest.getTestCertificate(
                EndorsementCredential.class, CertificateTest.FAKE_ROOT_CA_FILE
        );
        certMan.save(ecCert);

        Assert.assertEquals(caCert.getCertificateHash(), ecCert.getCertificateHash());

        Assert.assertEquals(
                CertificateAuthorityCredential
                        .select(certMan)
                        .byHashCode(caCert.getCertificateHash())
                        .getCertificate(),
                caCert
        );

        Assert.assertEquals(
                EndorsementCredential
                        .select(certMan)
                        .byHashCode(ecCert.getCertificateHash())
                        .getCertificate(),
                ecCert
        );
    }

    /**
     * Tests that a certificate can be deleted from the database.
     *
     * @throws IOException if there is a problem creating the certificate
     * @throws CertificateException if there is a problem deserializing the original X509Certificate
     */
    @Test
    public void testDelete() throws IOException, CertificateException {
        CertificateManager certMan = new DBCertificateManager(sessionFactory);

        certMan.save(rootCert);
        certMan.save(sgiIntermediateCert);
        Certificate savedIntelIntermediateCert = certMan.save(intelIntermediateCert);

        Assert.assertTrue(certMan.delete(savedIntelIntermediateCert));

        Certificate retrievedIntelCert = CertificateAuthorityCredential.select(certMan)
                .bySerialNumber(
                        intelIntermediateCert.getX509Certificate().getSerialNumber()
                )
                .getCertificate();
        Assert.assertNull(retrievedIntelCert);

        Certificate retrievedRootCert = CertificateAuthorityCredential.select(certMan)
                .bySerialNumber(
                        rootCert.getX509Certificate().getSerialNumber()
                )
                .getCertificate();
        Assert.assertEquals(retrievedRootCert, rootCert);

        Certificate retrievedSGICert = CertificateAuthorityCredential.select(certMan)
                .bySerialNumber(
                        sgiIntermediateCert.getX509Certificate().getSerialNumber()
                )
                .getCertificate();
        Assert.assertEquals(retrievedSGICert, sgiIntermediateCert);
    }
}
