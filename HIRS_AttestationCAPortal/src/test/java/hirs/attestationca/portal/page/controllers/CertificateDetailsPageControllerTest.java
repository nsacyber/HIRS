package hirs.attestationca.portal.page.controllers;

import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.IssuedAttestationCertificate;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.persist.CertificateManager;
import hirs.persist.DeviceGroupManager;
import hirs.persist.DeviceManager;
import hirs.attestationca.portal.page.PageControllerTest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.security.Security;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasItem;

import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import java.util.List;

/**
 * Integration tests that test the URL End Points of CertificateDetailsPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CertificateDetailsPageControllerTest extends PageControllerTest {

    @Autowired
    private CertificateManager certificateManager;

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private DeviceGroupManager deviceGroupManager;

    private CertificateAuthorityCredential caCertificate;
    private CertificateAuthorityCredential caRootCertificate;
    private PlatformCredential platofrmCredential;
    private PlatformCredential platofrmCredential2;
    private EndorsementCredential endorsementCredential;
    private IssuedAttestationCertificate issuedCredential;

    // Random UUID for certificate search.
    private static final String ID = "046b6c7f-0b8a-43b9-b35d-6489e6daee91";
    private static final String TEST_ENDORSEMENT_CREDENTIAL
            = "/endorsement_credentials/tpmcert.pem";
    private static final String TEST_PLATFORM_CREDENTIAL
            = "/platform_credentials/Intel_pc.cer";
    private static final String TEST_PLATFORM_CREDENTIAL_2
            = "/platform_credentials/basic_plat_cert_2-0.pem";
    private static final String TEST_CA_CERTIFICATE
            = "/certificates/fakestmtpmekint02.pem";
    private static final String TEST_ROOT_CA_CERTIFICATE
            = "/certificates/fakeCA.pem";
    private static final String ISSUED_CLIENT_CERT
            = "/certificates/sample_identity_cert.cer";

    /**
     * Constructor providing the Page's display and routing specification.
     */
    public CertificateDetailsPageControllerTest() {
        super(Page.CERTIFICATE_DETAILS);
    }

    /**
     * Prepares tests.
     * @throws IOException if test resources are not found
     */
    @BeforeClass
    public void prepareTests() throws IOException {
        Security.addProvider(new BouncyCastleProvider());

        Set<PlatformCredential> pcCertSet = new HashSet<>();

        //Create new device grup
        DeviceGroup group = new DeviceGroup("default");
        group = deviceGroupManager.saveDeviceGroup(group);

        //Create new device and save it
        Device device = new Device("Test");
        device.setDeviceGroup(group);
        device = deviceManager.saveDevice(device);

         //Upload and save EK Cert
         endorsementCredential = (EndorsementCredential)
                    getTestCertificate(
                            EndorsementCredential.class,
                            TEST_ENDORSEMENT_CREDENTIAL,
                            null,
                            null);
        certificateManager.save(endorsementCredential);

        //Upload and save CA Cert
        caCertificate = (CertificateAuthorityCredential)
                    getTestCertificate(
                            CertificateAuthorityCredential.class,
                            TEST_CA_CERTIFICATE,
                            null,
                            null);
        certificateManager.save(caCertificate);

        //Upload and save root Cert
        caRootCertificate = (CertificateAuthorityCredential)
                    getTestCertificate(
                            CertificateAuthorityCredential.class,
                            TEST_ROOT_CA_CERTIFICATE,
                            null,
                            null);
        certificateManager.save(caRootCertificate);

        //Upload and save Platform Cert
        platofrmCredential = (PlatformCredential)
                    getTestCertificate(
                            PlatformCredential.class,
                            TEST_PLATFORM_CREDENTIAL,
                            null,
                            null);
        certificateManager.save(platofrmCredential);

        pcCertSet.add(platofrmCredential);

        //Upload and save Platform Cert 2.0
        platofrmCredential2 = (PlatformCredential)
                    getTestCertificate(
                            PlatformCredential.class,
                            TEST_PLATFORM_CREDENTIAL_2,
                            null,
                            null);
        certificateManager.save(platofrmCredential2);

        pcCertSet.add(platofrmCredential);

        //Upload and save Issued Attestation Cert
        issuedCredential = (IssuedAttestationCertificate)
                    getTestCertificate(
                            IssuedAttestationCertificate.class,
                            ISSUED_CLIENT_CERT,
                            endorsementCredential,
                            pcCertSet);
        issuedCredential.setDevice(device);
        certificateManager.save(issuedCredential);
    }

    /**
     * Tests initial page when the certificate
     * was not found.
     * @throws Exception if an exception occurs
     */
    @Test
    public void testInitPage() throws Exception {
        // Get error message
        getMockMvc()
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                .param("id", ID)
                .param("type", "certificateauthority"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(PageController.MESSAGES_ATTRIBUTE, hasProperty("error",
                        hasItem("Unable to find certificate with ID: " + ID))))
                .andReturn();
    }

     /**
     * Tests initial page when invalid type.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    public void testInitPageInvalidType() throws Exception {
        // Get error message
        getMockMvc()
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                .param("id", ID)
                .param("type", "invalid"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(PageController.MESSAGES_ATTRIBUTE, hasProperty("error",
                        hasItem("Invalid certificate type: invalid"))))
                .andReturn();
    }

    /**
     * Tests initial page when missing a parameter.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    public void testInitPageMissingParam() throws Exception {
        // Get error message
        getMockMvc()
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                .param("id", ID))
                .andExpect(status().isOk())
                .andExpect(model().attribute(PageController.MESSAGES_ATTRIBUTE, hasProperty("error",
                        hasItem("Type was not provided"))))
                .andReturn();
    }

    /**
     * Tests initial page when the certificate type is
     * a Certificate Authority.
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPageCertificateAuthority() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                .param("id", caCertificate.getId().toString())
                .param("type", "certificateauthority"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initalData HashMap
        Map<String, String> initalData = (Map<String, String>) result
                                    .getModelAndView()
                                    .getModel()
                                    .get(PolicyPageController.INITIAL_DATA);
        Assert.assertEquals(initalData.get("issuer"), caCertificate.getIssuer());

    }

    /**
     * Tests initial page when the certificate type is
     * an Platform Certificate.
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPagePlatform() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                .param("id", platofrmCredential.getId().toString())
                .param("type", "platform"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initalData HashMap
        Map<String, String> initalData = (Map<String, String>) result
                                    .getModelAndView()
                                    .getModel()
                                    .get(PolicyPageController.INITIAL_DATA);
        Assert.assertEquals(initalData.get("issuer"), platofrmCredential.getIssuer());
        Assert.assertEquals(initalData.get("credentialType"),
                            ((PlatformCredential) platofrmCredential).getCredentialType());

    }

    /**
     * Tests initial page when the certificate type is
     * an Platform Certificate 2.0.
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPagePlatform20() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                .param("id", platofrmCredential2.getId().toString())
                .param("type", "platform"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initalData HashMap
        Map<String, Object> initalData = (Map<String, Object>) result
                                    .getModelAndView()
                                    .getModel()
                                    .get(PolicyPageController.INITIAL_DATA);
        Assert.assertEquals(initalData.get("issuer"), platofrmCredential2.getIssuer());
        Assert.assertEquals(initalData.get("credentialType"),
                            ((PlatformCredential) platofrmCredential2).getCredentialType());
        // Check component identifier
        Assert.assertNotNull(initalData.get("componentsIdentifier"));
        List<?> obj = (List<?>) initalData.get("componentsIdentifier");
        Assert.assertEquals(obj.size(), 7);

        // Check platform properties
        Assert.assertNotNull(initalData.get("platformProperties"));
        obj = (List<?>) initalData.get("platformProperties");
        Assert.assertEquals(obj.size(), 2);

    }

    /**
     * Tests initial page when the certificate type is
     * an Endorsement Certificate.
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPageEndorsement() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                .param("id", endorsementCredential.getId().toString())
                .param("type", "endorsement"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initalData HashMap
        Map<String, String> initalData = (Map<String, String>) result
                                    .getModelAndView()
                                    .getModel()
                                    .get(PolicyPageController.INITIAL_DATA);
        Assert.assertEquals(initalData.get("issuer"), endorsementCredential.getIssuer());
        Assert.assertEquals(initalData.get("manufacturer"),
                            ((EndorsementCredential) endorsementCredential).getManufacturer());
    }

    /**
     * Tests initial page for issuer ID.
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPageID() throws Exception {

        MvcResult result = getMockMvc()
                            .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                            .param("id", caCertificate.getId().toString())
                            .param("type", "certificateauthority"))
                            .andExpect(model().attributeExists(
                                    CertificateDetailsPageController.INITIAL_DATA))

                            .andReturn();

        // Obtain initalData HashMap
        Map<String, String> initalData = (Map<String, String>) result
                            .getModelAndView()
                            .getModel()
                            .get(PolicyPageController.INITIAL_DATA);

        Assert.assertEquals(initalData.get("issuer"), caCertificate.getIssuer());
        Assert.assertEquals(initalData.get("issuerID"),
                caRootCertificate.getId().toString());
    }

    /**
     * Tests initial page when the certificate type is
     * an Issued Attestation Certificate.
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPageIssuedAttestation() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                .param("id", issuedCredential.getId().toString())
                .param("type", "issued"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initalData HashMap
        Map<String, String> initalData = (Map<String, String>) result
                                    .getModelAndView()
                                    .getModel()
                                    .get(PolicyPageController.INITIAL_DATA);
        Assert.assertEquals(initalData.get("issuer"), issuedCredential.getIssuer());
        Assert.assertEquals(initalData.get("endorsementID"),
                            issuedCredential.getEndorsementCredential().getId().toString());


    }

    /**
     * Construct a test certificate from the given parameters.
     * @param <T> the type of Certificate that will be created
     * @param certificateClass the class of certificate to generate
     * @param filename the location of the certificate to be used
     * @param ekCert the endorsement credential
     * @param pcCert the platform credentials
     * @return the newly-constructed Certificate
     * @throws IOException if there is a problem constructing the test certificate
     */
    public <T extends Certificate> Certificate getTestCertificate(
            final Class<T> certificateClass,
            final String filename,
            final EndorsementCredential ekCert,
            final Set<PlatformCredential> pcCert)
            throws IOException {

        Path fPath;
        try {
            fPath = Paths.get(this.getClass().getResource(filename).toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Could not resolve path URI", e);
        }

        switch (certificateClass.getSimpleName()) {
            case "EndorsementCredential":
                return new EndorsementCredential(fPath);
            case "PlatformCredential":
                return new PlatformCredential(fPath);
            case "CertificateAuthorityCredential":
                return new CertificateAuthorityCredential(fPath);
            case "IssuedAttestationCertificate":
                return new IssuedAttestationCertificate(fPath, ekCert, pcCert);
            default:
                throw new IllegalArgumentException(
                        String.format("Unknown certificate class %s", certificateClass.getName())
                );
        }
    }
}
