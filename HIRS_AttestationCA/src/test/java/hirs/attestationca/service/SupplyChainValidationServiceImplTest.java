package hirs.attestationca.service;

import hirs.appraiser.SupplyChainAppraiser;
import hirs.data.persist.AppraisalStatus;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.SpringPersistenceTest;
import hirs.data.persist.SupplyChainPolicy;
import hirs.data.persist.SupplyChainValidation;
import hirs.data.persist.SupplyChainValidationSummary;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
import hirs.data.persist.certificate.DeviceAssociatedCertificate;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.persist.AppraiserManager;
import hirs.persist.CertificateManager;
import hirs.persist.CrudManager;
import hirs.persist.DBCertificateManager;
import hirs.persist.DBDeviceGroupManager;
import hirs.persist.DBDeviceManager;
import hirs.persist.DeviceGroupManager;
import hirs.persist.DeviceManager;
import hirs.persist.PolicyManager;
import hirs.validation.CredentialValidator;
import hirs.validation.SupplyChainCredentialValidator;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static hirs.data.persist.AppraisalStatus.Status.FAIL;
import static hirs.data.persist.AppraisalStatus.Status.PASS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@see SupplyChainValidationServiceImpl}.
 */
public class SupplyChainValidationServiceImplTest extends SpringPersistenceTest {
    private static final String NUC1_EC = "/certificates/nuc-1/tpmcert.pem";
    private static final String STM_ROOT_CA = "/certificates/stMicroCaCerts/stmtpmekroot.crt";
    private static final String GS_ROOT_CA = "/certificates/stMicroCaCerts/gstpmroot.crt";
    private static final String INTEL_CA = "/certificates/IntelSigningKey_20April2017.pem";
    private static final String NUC_PC = "/certificates/platform_certs_2/Intel_pc.pem";
    private static final String STM_TPM_EK_INTERMEDIATE_CA_02 =
            "/certificates/STM TPM EK Intermediate CA.CER";
    private static final String NUC_EC = "/certificates/nuc_ec.pem";

    @Mock
    private PolicyManager policyManager;

    @Mock
    private AppraiserManager appraiserManager;

    @Mock
    private CertificateManager certificateManager;

    @Mock
    private CredentialValidator supplyChainCredentialValidator;

    @Mock
    private CrudManager<SupplyChainValidationSummary> supplyChainValidationSummaryDBManager;

    @InjectMocks
    private SupplyChainValidationServiceImpl service;

    // mocked
    private SupplyChainPolicy policy;
    private PlatformCredential pc;
    private PlatformCredential delta;
    private EndorsementCredential ec;
    private HashSet<PlatformCredential> pcs;
    private Device device;

    /**
     * Sets up the mocks.
     *
      @throws IOException won't actually throw, the method is being mocked instead of actually
     *                     called
     */
    @BeforeMethod
    public void beforeClass() throws IOException {
        MockitoAnnotations.initMocks(this);

        device = mock(Device.class);

        SupplyChainAppraiser appraiser = mock(SupplyChainAppraiser.class);
        policy = mock(SupplyChainPolicy.class);

        when(appraiserManager.getAppraiser(SupplyChainAppraiser.NAME)).thenReturn(appraiser);
        when(policyManager.getDefaultPolicy(appraiser)).thenReturn(policy);

        // mock endorsement credential
        ec = mock(EndorsementCredential.class);
        when(ec.getEncodedPublicKey()).thenReturn(new byte[] {0x0});
        when(ec.getIssuerOrganization()).thenReturn("STMicroelectronics NV");

        Set<Certificate> resultEcs = new HashSet<>();
        resultEcs.add(ec);

        // mock platform credential
        X509Certificate cert = mock(X509Certificate.class);
        pc = mock(PlatformCredential.class);
        when(pc.getId()).thenReturn(UUID.randomUUID());
        when(pc.getX509Certificate()).thenReturn(cert);
        when(pc.getSerialNumber()).thenReturn(BigInteger.ONE);
        when(pc.getPlatformSerial()).thenReturn(String.valueOf(Integer.MIN_VALUE));
        when(pc.getIssuerOrganization()).thenReturn("STMicroelectronics NV");
        when(ec.getSubjectOrganization()).thenReturn("STMicroelectronics NV");
        pcs = new HashSet<PlatformCredential>();
        pcs.add(pc);

        //Mock delta platform credential
        X509Certificate deltaCert = mock(X509Certificate.class);
        delta = mock(PlatformCredential.class);
        when(delta.getId()).thenReturn(UUID.randomUUID());
        when(delta.getX509Certificate()).thenReturn(deltaCert);
        //when(delta.getSerialNumber()).thenReturn(BigInteger.ONE);
        when(delta.getIssuerOrganization()).thenReturn("STMicroelectronics NV");
        when(delta.getSubjectOrganization()).thenReturn("STMicroelectronics NV");

        Set<Certificate> resultPcs = new HashSet<>();
        resultPcs.add(pc);
        //resultPcs.add(delta);

        // mock credential retrieval
        when(certificateManager.get(any(EndorsementCredential.Selector.class)))
                .thenReturn(resultEcs);
        when(certificateManager.get(any(PlatformCredential.Selector.class)))
                .thenReturn(resultPcs);
        when(certificateManager.get(any(CertificateAuthorityCredential.Selector.class)))
                .thenReturn(Collections.emptySet());
    }

    /**
     * Remove test certificates and close the session factory.
     */
    @AfterMethod
    public void teardown() {
        DBCertificateManager certMan = new DBCertificateManager(sessionFactory);
        DBDeviceManager deviceMan = new DBDeviceManager(sessionFactory);
        DBDeviceGroupManager groupMan = new DBDeviceGroupManager(sessionFactory);

        certMan.deleteAll();
        deviceMan.deleteAll();
        groupMan.deleteAll();
    }
    /**
     * All validations enabled, all pass.
     */
    @Test
    public final void testFullSuccessfulValidation() {
        when(policy.isEcValidationEnabled()).thenReturn(true);
        when(policy.isPcValidationEnabled()).thenReturn(true);
        when(policy.isPcAttributeValidationEnabled()).thenReturn(true);
        when(policy.isExpiredCertificateValidationEnabled()).thenReturn(true);

        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator).
                validateEndorsementCredential(eq(ec), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator)
                .validatePlatformCredential(eq(pc), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator)
                .validatePlatformCredentialAttributes(eq(pc), any(DeviceInfoReport.class),
                        any(EndorsementCredential.class));

        Assert.assertEquals(service.validateSupplyChain(ec, pcs,
                device).getOverallValidationResult(), PASS);
        verify(supplyChainValidationSummaryDBManager).save(any(SupplyChainValidationSummary.class));

        // verify the certs were updated with the test device object and saved in the cert man
        ArgumentCaptor<DeviceAssociatedCertificate> certificatesCaptor
                = ArgumentCaptor.forClass(DeviceAssociatedCertificate.class);
        verify(certificateManager, times(3)).update(certificatesCaptor.capture());

        List<DeviceAssociatedCertificate> certificateArgs = certificatesCaptor.getAllValues();
        for (DeviceAssociatedCertificate certArg : certificateArgs) {
            verify(certArg, atLeast(1)).setDevice(device);
        }
    }

    /**
     * All validations enabled, fail EC.
     */
    @Test
    public final void testFailEcValidation() {
        when(policy.isEcValidationEnabled()).thenReturn(true);
        when(policy.isPcValidationEnabled()).thenReturn(true);
        when(policy.isPcAttributeValidationEnabled()).thenReturn(true);
        when(policy.isExpiredCertificateValidationEnabled()).thenReturn(true);

        doReturn(new AppraisalStatus(FAIL, "")).when(supplyChainCredentialValidator).
                validateEndorsementCredential(eq(ec), any(KeyStore.class), any(Boolean.class));
        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator)
                .validatePlatformCredential(eq(pc), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator)
                .validatePlatformCredentialAttributes(eq(pc), any(DeviceInfoReport.class),
                        any(EndorsementCredential.class));

        Assert.assertEquals(service.validateSupplyChain(ec, pcs,
                device).getOverallValidationResult(), FAIL);
        verify(supplyChainValidationSummaryDBManager).save(any(SupplyChainValidationSummary.class));
    }

    /**
     * All validations enabled, fail Pc Cert.
     */
    @Test
    public final void testFailPcValidation() {
        when(policy.isEcValidationEnabled()).thenReturn(true);
        when(policy.isPcValidationEnabled()).thenReturn(true);
        when(policy.isPcAttributeValidationEnabled()).thenReturn(true);
        when(policy.isExpiredCertificateValidationEnabled()).thenReturn(true);

        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator).
                validateEndorsementCredential(eq(ec), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(FAIL, "")).when(supplyChainCredentialValidator).
                validatePlatformCredential(eq(pc), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(FAIL, "")).when(supplyChainCredentialValidator)
                .validatePlatformCredentialAttributes(eq(pc), any(DeviceInfoReport.class),
                        any(EndorsementCredential.class));
        Assert.assertEquals(service.validateSupplyChain(ec, pcs,
                device).getOverallValidationResult(), FAIL);
        verify(supplyChainValidationSummaryDBManager).save(any(SupplyChainValidationSummary.class));
    }

    /**
     * All validations enabled, Pc Attrib. fails.
     */
    @Test
    public final void testFailPcAttributeValidation() {
        when(policy.isEcValidationEnabled()).thenReturn(true);
        when(policy.isPcValidationEnabled()).thenReturn(true);
        when(policy.isPcAttributeValidationEnabled()).thenReturn(true);
        when(policy.isExpiredCertificateValidationEnabled()).thenReturn(true);

        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator).
                validateEndorsementCredential(eq(ec), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator).
                validatePlatformCredential(eq(pc), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(FAIL, "")).when(supplyChainCredentialValidator).
                validatePlatformCredentialAttributes(eq(pc), any(DeviceInfoReport.class),
                        any(EndorsementCredential.class));

        Assert.assertEquals(service.validateSupplyChain(ec, pcs,
                device).getOverallValidationResult(), FAIL);
        verify(supplyChainValidationSummaryDBManager).save(any(SupplyChainValidationSummary.class));
    }

    /**
     * Ec not enabled, all others pass.
     */
    @Test
    public final void testNoEcValidation() {
        when(policy.isEcValidationEnabled()).thenReturn(false);
        when(policy.isPcValidationEnabled()).thenReturn(true);
        when(policy.isPcAttributeValidationEnabled()).thenReturn(true);
        when(policy.isExpiredCertificateValidationEnabled()).thenReturn(true);

        doReturn(new AppraisalStatus(FAIL, "")).when(supplyChainCredentialValidator).
                validateEndorsementCredential(eq(ec), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator).
                validatePlatformCredential(eq(pc), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator).
                validatePlatformCredentialAttributes(eq(pc), any(DeviceInfoReport.class),
                        any(EndorsementCredential.class));

        Assert.assertEquals(service.validateSupplyChain(ec, pcs,
                device).getOverallValidationResult(), PASS);
        verify(supplyChainValidationSummaryDBManager).save(any(SupplyChainValidationSummary.class));
    }

    /**
     * Pc cert not enabled, all others pass.
     */
    @Test
    public final void testNoPcValidation() {
        when(policy.isEcValidationEnabled()).thenReturn(true);
        when(policy.isPcValidationEnabled()).thenReturn(false);
        when(policy.isPcAttributeValidationEnabled()).thenReturn(true);
        when(policy.isExpiredCertificateValidationEnabled()).thenReturn(true);

        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator).
                validateEndorsementCredential(eq(ec), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(FAIL, "")).when(supplyChainCredentialValidator).
                validatePlatformCredential(eq(pc), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator).
                validatePlatformCredentialAttributes(eq(pc), any(DeviceInfoReport.class),
                        any(EndorsementCredential.class));

        Assert.assertEquals(service.validateSupplyChain(ec, pcs,
                device).getOverallValidationResult(), PASS);
        verify(supplyChainValidationSummaryDBManager).save(any(SupplyChainValidationSummary.class));
    }

    /**
     * Pc attrib not enabled, all others pass.
     */
    @Test
    public final void testNoPcAttributeValidation() {
        when(policy.isEcValidationEnabled()).thenReturn(true);
        when(policy.isPcValidationEnabled()).thenReturn(true);
        when(policy.isPcAttributeValidationEnabled()).thenReturn(false);
        when(policy.isExpiredCertificateValidationEnabled()).thenReturn(true);

        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator).
                validateEndorsementCredential(eq(ec), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(PASS, "")).when(supplyChainCredentialValidator).
                validatePlatformCredential(eq(pc), any(KeyStore.class), eq(true));
        doReturn(new AppraisalStatus(FAIL, "")).when(supplyChainCredentialValidator).
                validatePlatformCredentialAttributes(eq(pc), any(DeviceInfoReport.class),
                        any(EndorsementCredential.class));

        Assert.assertEquals(service.validateSupplyChain(ec, pcs,
                device).getOverallValidationResult(), PASS);
        verify(supplyChainValidationSummaryDBManager).save(any(SupplyChainValidationSummary.class));
    }



    /**
     * All enabled, EC is null.
     */
    @Test
    public final void testNullEcValidation() {
        when(policy.isEcValidationEnabled()).thenReturn(true);
        Assert.assertEquals(service.validateSupplyChain(null, pcs,
                device).getOverallValidationResult(), FAIL);
    }

    /**
     * All enabled, PC is null. Then PC set is empty.
     */
    @Test
    public final void testNullPcValidation() {
        when(policy.isEcValidationEnabled()).thenReturn(false);
        when(policy.isPcValidationEnabled()).thenReturn(true);
        Assert.assertEquals(service.validateSupplyChain(ec, null,
                device).getOverallValidationResult(), FAIL);
        final HashSet<PlatformCredential> emptySet = new HashSet<>();
        Assert.assertEquals(service.validateSupplyChain(ec, emptySet,
                device).getOverallValidationResult(), FAIL);
    }

    /**
     * All enabled, PC is null. Then PC set is empty.
     */
    @Test
    public final void testNullPcAttributeValidation() {
        when(policy.isEcValidationEnabled()).thenReturn(false);
        when(policy.isPcValidationEnabled()).thenReturn(false);
        when(policy.isPcAttributeValidationEnabled()).thenReturn(true);
        Assert.assertEquals(service.validateSupplyChain(ec, null,
                device).getOverallValidationResult(), FAIL);
        final HashSet<PlatformCredential> emptySet = new HashSet<>();
        Assert.assertEquals(service.validateSupplyChain(ec, emptySet,
                device).getOverallValidationResult(), FAIL);
    }

    /**
     * Puts an EC, STM CA, and GS CA in the DB, attempts to retrieve the CAs from the EC.
     * @throws URISyntaxException failed to parse certificate file location.
     * @throws IOException couldn't create certificates from file.
     * @throws KeyStoreException was unable to retrieve keystore.
     */
    @Test
    public final void testGetCaChain() throws URISyntaxException, IOException, KeyStoreException {
        CertificateManager realCertMan = new DBCertificateManager(sessionFactory);

        // the main service in this class only uses mocked managers, we need a real DB certificate
        // manager for this test, so we make a second service.
        SupplyChainValidationServiceImpl mostlyMockedService = new SupplyChainValidationServiceImpl(
                policyManager,
                appraiserManager,
                realCertMan,
                supplyChainValidationSummaryDBManager,
                supplyChainCredentialValidator
        );

        CertificateAuthorityCredential globalSignCaCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        GS_ROOT_CA).toURI())));

        CertificateAuthorityCredential rootCa = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        STM_ROOT_CA).toURI()))
        );

        EndorsementCredential endorsementCredential = new EndorsementCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        NUC1_EC).toURI())));

        realCertMan.save(endorsementCredential);
        realCertMan.save(rootCa);
        realCertMan.save(globalSignCaCert);

        KeyStore ks = mostlyMockedService.getCaChain(endorsementCredential);

        String stmCaAlias = rootCa.getId().toString();
        String gsCaAlias = globalSignCaCert.getId().toString();

        Assert.assertNotNull(ks.getCertificate(stmCaAlias));
        Assert.assertNotNull(ks.getCertificate(gsCaAlias));
        Assert.assertEquals(ks.size(), 2);

        realCertMan.delete(endorsementCredential);
        realCertMan.delete(rootCa);
        realCertMan.delete(globalSignCaCert);
    }

    /**
     * Puts an EC, and STM CA in the DB, attempts to retrieve the CAs from the EC. The STM CA
     * points to a GS CA that is not present.
     * @throws URISyntaxException failed to parse certificate file location.
     * @throws IOException couldn't create certificates from file.
     * @throws KeyStoreException was unable to retrieve keystore.
     */
    @Test
    public final void testGetNotFullCaChain() throws URISyntaxException, IOException,
            KeyStoreException {
        CertificateManager realCertMan = new DBCertificateManager(sessionFactory);

        // the main service in this class only uses mocked managers, we need a real DB certificate
        // manager for this test, so we make a second service.
        SupplyChainValidationServiceImpl mostlyMockedService = new SupplyChainValidationServiceImpl(
                policyManager,
                appraiserManager,
                realCertMan,
                supplyChainValidationSummaryDBManager,
                supplyChainCredentialValidator
        );

        CertificateAuthorityCredential rootCa = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        STM_ROOT_CA).toURI()))
        );

        EndorsementCredential endorsementCredential = new EndorsementCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        NUC1_EC).toURI())));

        realCertMan.save(endorsementCredential);
        realCertMan.save(rootCa);

        KeyStore ks = mostlyMockedService.getCaChain(endorsementCredential);

        String stmCaAlias = rootCa.getId().toString();

        Assert.assertNotNull(ks.getCertificate(stmCaAlias));
        Assert.assertEquals(ks.size(), 1);

        realCertMan.delete(endorsementCredential);
        realCertMan.delete(rootCa);
    }

    /**
     * Puts an EC in the DB, attempts to retrieve the CA from the EC.
     * @throws URISyntaxException failed to parse certificate file location.
     * @throws IOException couldn't create certificates from file.
     * @throws KeyStoreException was unable to retrieve keystore.
     */
    @Test
    public final void testGetEmptyCaChain() throws URISyntaxException, IOException,
            KeyStoreException {
        CertificateManager realCertMan = new DBCertificateManager(sessionFactory);

        // the main service in this class only uses mocked managers, we need a real DB certificate
        // manager for this test, so we make a second service.
        SupplyChainValidationServiceImpl mostlyMockedService = new SupplyChainValidationServiceImpl(
                policyManager,
                appraiserManager,
                realCertMan,
                supplyChainValidationSummaryDBManager,
                supplyChainCredentialValidator
        );

        EndorsementCredential endorsementCredential = new EndorsementCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        NUC1_EC).toURI())));

        realCertMan.save(endorsementCredential);

        KeyStore ks = mostlyMockedService.getCaChain(endorsementCredential);

        Assert.assertEquals(ks.size(), 0);

        realCertMan.delete(endorsementCredential);
    }

    /**
     * Puts an EC, STM CA, GS CA, and an Intel CA in the DB, attempts to retrieve the CAs
     * from the EC.
     * @throws URISyntaxException failed to parse certificate file location.
     * @throws IOException couldn't create certificates from file.
     * @throws KeyStoreException was unable to retrieve keystore.
     */
    @Test
    public final void testGetCaChainWithExtraCerts() throws URISyntaxException, IOException,
            KeyStoreException {
        CertificateManager realCertMan = new DBCertificateManager(sessionFactory);

        // the main service in this class only uses mocked managers, we need a real DB certificate
        // manager for this test, so we make a second service.
        SupplyChainValidationServiceImpl mostlyMockedService = new SupplyChainValidationServiceImpl(
                policyManager,
                appraiserManager,
                realCertMan,
                supplyChainValidationSummaryDBManager,
                supplyChainCredentialValidator
        );

        CertificateAuthorityCredential globalSignCaCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        GS_ROOT_CA).toURI())));

        CertificateAuthorityCredential rootCa = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        STM_ROOT_CA).toURI()))
        );

        CertificateAuthorityCredential intelCa = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        INTEL_CA).toURI()))
        );

        EndorsementCredential endorsementCredential = new EndorsementCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        NUC1_EC).toURI())));

        realCertMan.save(endorsementCredential);
        realCertMan.save(rootCa);
        realCertMan.save(globalSignCaCert);
        realCertMan.save(intelCa);

        KeyStore ks = mostlyMockedService.getCaChain(endorsementCredential);

        String stmCaAlias = rootCa.getId().toString();
        String gsCaAlias = globalSignCaCert.getId().toString();

        Assert.assertNotNull(ks.getCertificate(stmCaAlias));
        Assert.assertNotNull(ks.getCertificate(gsCaAlias));
        Assert.assertEquals(ks.size(), 2);

        realCertMan.delete(endorsementCredential);
        realCertMan.delete(rootCa);
        realCertMan.delete(globalSignCaCert);
        realCertMan.delete(intelCa);
    }

    /**
     * Puts an Intel PC and Intel CA in the DB, attempts to retrieve the CA from the PC.
     * @throws URISyntaxException failed to parse certificate file location.
     * @throws IOException couldn't create certificates from file.
     * @throws KeyStoreException was unable to retrieve keystore.
     */
    @Test
    public final void testGetPcCaChain() throws URISyntaxException, IOException, KeyStoreException {
        CertificateManager realCertMan = new DBCertificateManager(sessionFactory);

        // the main service in this class only uses mocked managers, we need a real DB certificate
        // manager for this test, so we make a second service.
        SupplyChainValidationServiceImpl mostlyMockedService = new SupplyChainValidationServiceImpl(
                policyManager,
                appraiserManager,
                realCertMan,
                supplyChainValidationSummaryDBManager,
                supplyChainCredentialValidator
        );

        CertificateAuthorityCredential intelCa = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        INTEL_CA).toURI()))
        );

        PlatformCredential platformCredential = new PlatformCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        NUC_PC).toURI())));

        realCertMan.save(platformCredential);
        realCertMan.save(intelCa);

        KeyStore ks = mostlyMockedService.getCaChain(platformCredential);

        String intelCaAlias = intelCa.getId().toString();

        Assert.assertNotNull(ks.getCertificate(intelCaAlias));
        Assert.assertEquals(ks.size(), 1);

        realCertMan.delete(platformCredential);
        realCertMan.delete(intelCa);
    }

    /**
     * Puts an Intel PC, STM CA, and GS CA in the DB, attempts to retrieve the CAs from the PC. None
     * should match.
     * @throws URISyntaxException failed to parse certificate file location.
     * @throws IOException couldn't create certificates from file.
     * @throws KeyStoreException was unable to retrieve keystore.
     */
    @Test
    public final void testGetPcCaChainNoMatches() throws URISyntaxException, IOException,
            KeyStoreException {
        CertificateManager realCertMan = new DBCertificateManager(sessionFactory);

        // the main service in this class only uses mocked managers, we need a real DB certificate
        // manager for this test, so we make a second service.
        SupplyChainValidationServiceImpl mostlyMockedService = new SupplyChainValidationServiceImpl(
                policyManager,
                appraiserManager,
                realCertMan,
                supplyChainValidationSummaryDBManager,
                supplyChainCredentialValidator
        );

        CertificateAuthorityCredential globalSignCaCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        GS_ROOT_CA).toURI())));

        CertificateAuthorityCredential rootCa = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        STM_ROOT_CA).toURI()))
        );

        PlatformCredential platformCredential = new PlatformCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        NUC_PC).toURI())));

        realCertMan.save(platformCredential);
        realCertMan.save(rootCa);
        realCertMan.save(globalSignCaCert);

        KeyStore ks = mostlyMockedService.getCaChain(platformCredential);

        Assert.assertEquals(ks.size(), 0);

        realCertMan.delete(platformCredential);
        realCertMan.delete(rootCa);
        realCertMan.delete(globalSignCaCert);
    }

    /**
     * Puts an STM intermediate CA, STM 'root' CA, and GlobalSign root CA into the in-memory
     * database, and then runs supply chain validation on a given endorsement credential.
     *
     * @throws URISyntaxException if building the path to a certificate resource fails
     * @throws IOException if there is a problem deserializing a certificate
     */
    @Test
    public void testVerifyEcAgainstCaChain() throws URISyntaxException, IOException {
        when(policy.isEcValidationEnabled()).thenReturn(true);
        when(policy.isPcValidationEnabled()).thenReturn(false);
        when(policy.isPcAttributeValidationEnabled()).thenReturn(false);

        CertificateManager realCertMan = new DBCertificateManager(sessionFactory);
        Device storedDevice = getStoredTestDevice();

        SupplyChainValidationServiceImpl mostlyMockedService = new SupplyChainValidationServiceImpl(
                policyManager,
                appraiserManager,
                realCertMan,
                supplyChainValidationSummaryDBManager,
                new SupplyChainCredentialValidator()
        );

        CertificateAuthorityCredential stmEkRootCa = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        STM_ROOT_CA).toURI())));

        CertificateAuthorityCredential stmTpmEkIntermediateCA = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        STM_TPM_EK_INTERMEDIATE_CA_02).toURI())));

        CertificateAuthorityCredential globalSignTpmRoot = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(
                        GS_ROOT_CA).toURI()))
        );

        realCertMan.save(stmTpmEkIntermediateCA);
        realCertMan.save(globalSignTpmRoot);
        realCertMan.save(stmEkRootCa);

        EndorsementCredential nucEc = new EndorsementCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(NUC_EC).toURI()))
        );

        realCertMan.save(nucEc);

        SupplyChainValidationSummary summary = mostlyMockedService.validateSupplyChain(
                nucEc, Collections.emptySet(), storedDevice
        );

        Assert.assertEquals(summary.getOverallValidationResult(), PASS);
        for (SupplyChainValidation validation : summary.getValidations()) {
            Assert.assertEquals(
                    validation.getValidationType(),
                    SupplyChainValidation.ValidationType.ENDORSEMENT_CREDENTIAL
            );
        }

        // verify the EC was updated with the test device object and saved in the cert man
        EndorsementCredential updatedStoredEc =
                EndorsementCredential.select(realCertMan).bySerialNumber(nucEc.getSerialNumber())
                .getCertificate();

        Assert.assertEquals(updatedStoredEc.getDevice().getId(), storedDevice.getId());

        realCertMan.delete(stmTpmEkIntermediateCA);
        realCertMan.delete(globalSignTpmRoot);
        realCertMan.delete(stmEkRootCa);
        realCertMan.delete(nucEc);
    }

    private Device getStoredTestDevice() {
        DeviceManager deviceManager = new DBDeviceManager(sessionFactory);
        DeviceGroupManager deviceGroupManager = new DBDeviceGroupManager(sessionFactory);

        DeviceGroup testGroup = new DeviceGroup("group1");
        Device testDevice = new Device("SCVSI-test");

        testDevice.setDeviceGroup(deviceGroupManager.saveDeviceGroup(testGroup));
        return deviceManager.saveDevice(testDevice);
    }
}
