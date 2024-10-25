package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.enums.HealthStatus;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageControllerTest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.security.Security;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of CertificateDetailsPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CertificateDetailsPageControllerTest extends PageControllerTest {

    // Random UUID for certificate search.
    private static final String ID = "046b6c7f-0b8a-43b9-b35d-6489e6daee91";
    private static final String TEST_CA_CERTIFICATE
            = "/certificates/fakestmtpmekint02.pem";
    private static final String TEST_ROOT_CA_CERTIFICATE
            = "/certificates/fakeCA.pem";
    private static final String ISSUED_CLIENT_CERT
            = "/certificates/sample_identity_cert.cer";
    private static final String TEST_ENDORSEMENT_CREDENTIAL
            = "/endorsement_credentials/tpmcert.pem";
    private static final String TEST_PLATFORM_CREDENTIAL
            = "/platform_credentials/Intel_pc.cer";
    private static final String TEST_PLATFORM_CREDENTIAL_2
            = "/platform_credentials/basic_plat_cert_2-0.pem";
    private static final String TEST_PLATFORM_CREDENTIAL_2_PCI
            = "/platform_credentials/pciids_plat_cert_2-0.pem";
    // Base path for the page
    private final String pagePath;
    // Repository manager to handle data access between device entity and data storage in db
    @Autowired
    private DeviceRepository deviceRepository;
    // Repository manager to handle data access between certificate entity and data storage in db
    @Autowired
    private CertificateRepository certificateRepository;
    private CertificateAuthorityCredential caCertificate;
    private CertificateAuthorityCredential caRootCertificate;
    private PlatformCredential platformCredential;
    private PlatformCredential platformCredential2;
    private PlatformCredential platformCertificatePCI;
    private EndorsementCredential endorsementCredential;
    private IssuedAttestationCertificate issuedCredential;

    /**
     * Constructor providing the Page's display and routing specification.
     */
    public CertificateDetailsPageControllerTest() {
        super(Page.CERTIFICATE_DETAILS);
        pagePath = getPagePath();
    }

    /**
     * Prepares tests.
     *
     * @throws IOException if test resources are not found
     */
    @BeforeAll
    public void prepareTests() throws IOException {

        // Fake device to store in db for test
        Device device;

        Security.addProvider(new BouncyCastleProvider());

        // list of platformCredentials that have been saved in the db
        List<PlatformCredential> platformCredentialsList = new LinkedList<>();

        // Create new device to be used in test and save it to db
        device = new Device("Test Device", null, HealthStatus.TRUSTED, AppraisalStatus.Status.PASS,
                null, false, "tmp_overrideReason", "tmp_summId");
        device = deviceRepository.save(device);

        //Upload and save EK Cert
        endorsementCredential = (EndorsementCredential)
                getTestCertificate(
                        EndorsementCredential.class,
                        TEST_ENDORSEMENT_CREDENTIAL);
        certificateRepository.save(endorsementCredential);

        //Upload and save CA Cert
        caCertificate = (CertificateAuthorityCredential)
                getTestCertificate(
                        CertificateAuthorityCredential.class,
                        TEST_CA_CERTIFICATE);
        certificateRepository.save(caCertificate);

        //Upload and save root Cert
        caRootCertificate = (CertificateAuthorityCredential)
                getTestCertificate(
                        CertificateAuthorityCredential.class,
                        TEST_ROOT_CA_CERTIFICATE);
        certificateRepository.save(caRootCertificate);

        //Upload and save Platform Cert, add it to platformCredentials list
        platformCredential = (PlatformCredential)
                getTestCertificate(
                        PlatformCredential.class,
                        TEST_PLATFORM_CREDENTIAL);
        certificateRepository.save(platformCredential);
        platformCredentialsList.add(platformCredential);

        //Upload and save Platform Cert 2.0, add it to platformCredentials list
        platformCredential2 = (PlatformCredential)
                getTestCertificate(
                        PlatformCredential.class,
                        TEST_PLATFORM_CREDENTIAL_2);
        certificateRepository.save(platformCredential2);
        platformCredentialsList.add(platformCredential);

        //Upload and save Platform Cert 2.0 PCI, add it to platformCredentials list
        platformCertificatePCI = (PlatformCredential)
                getTestCertificate(
                        PlatformCredential.class,
                        TEST_PLATFORM_CREDENTIAL_2_PCI);
        certificateRepository.save(platformCertificatePCI);
        platformCredentialsList.add(platformCertificatePCI);

        //Upload and save Issued Attestation Cert
        issuedCredential = (IssuedAttestationCertificate)
                getTestCertificate(
                        IssuedAttestationCertificate.class,
                        ISSUED_CLIENT_CERT,
                        endorsementCredential,
                        platformCredentialsList);
        issuedCredential.setDeviceId(device.getId());
        certificateRepository.save(issuedCredential);
    }

    /**
     * Tests initial page when the certificate was not found
     * (uses ID that doesn't correspond to anything).
     *
     * @throws Exception if an exception occurs
     */
    @Test
    public void testInitPage() throws Exception {
        // Get error message
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath)
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
                .perform(MockMvcRequestBuilders.get(pagePath)
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
                .perform(MockMvcRequestBuilders.get(pagePath)
                        .param("id", ID))
                .andExpect(status().isOk())
                .andExpect(model().attribute(PageController.MESSAGES_ATTRIBUTE, hasProperty("error",
                        hasItem("Type was not provided"))))
                .andReturn();
    }

    /**
     * Tests initial page when the certificate type is
     * a Certificate Authority.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPageCertificateAuthority() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath)
                        .param("id", caCertificate.getId().toString())
                        .param("type", "certificateauthority"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initialData HashMap
        Map<String, String> initialData = (Map<String, String>) result
                .getModelAndView()
                .getModel()
                .get(PolicyPageController.INITIAL_DATA);
        assertEquals(caCertificate.getIssuer(), initialData.get("issuer"));

    }

    /**
     * Tests initial page when the certificate type is
     * a Platform Certificate.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPagePlatform() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath)
                        .param("id", platformCredential.getId().toString())
                        .param("type", "platform"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initialData HashMap
        Map<String, String> initialData = (Map<String, String>) result
                .getModelAndView()
                .getModel()
                .get(PolicyPageController.INITIAL_DATA);
        assertEquals(platformCredential.getIssuer(), initialData.get("issuer"));
        assertEquals(platformCredential.getCredentialType(),
                initialData.get("credentialType"));

    }

    /**
     * Tests initial page when the certificate type is
     * a Platform Certificate 2.0.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPagePlatform20() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath)
                        .param("id", platformCredential2.getId().toString())
                        .param("type", "platform"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initialData HashMap
        Map<String, Object> initialData = (Map<String, Object>) result
                .getModelAndView()
                .getModel()
                .get(PolicyPageController.INITIAL_DATA);
        assertEquals(platformCredential2.getIssuer(), initialData.get("issuer"));
        assertEquals(platformCredential2.getCredentialType(),
                initialData.get("credentialType"));
        // Check component identifier
        assertNotNull(initialData.get("componentsIdentifier"));
        List<?> obj = (List<?>) initialData.get("componentsIdentifier");
        assertEquals(7, obj.size());

        // Check platform properties
        assertNotNull(initialData.get("platformProperties"));
        obj = (List<?>) initialData.get("platformProperties");
        assertEquals(2, obj.size());

    }

    /**
     * Tests initial page when the certificate type is
     * a Platform Certificate 2.0 with PCI IDs.
     *
     * @throws Exception if an exception occurs
     */
//    @Test
//    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPagePlatform20PCI() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath)
                        .param("id", platformCertificatePCI.getId().toString())
                        .param("type", "platform"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initialData HashMap
        Map<String, Object> initialData = (Map<String, Object>) result
                .getModelAndView()
                .getModel()
                .get(PolicyPageController.INITIAL_DATA);
        assertEquals(platformCertificatePCI.getIssuer(), initialData.get("issuer"));
        assertEquals(platformCertificatePCI.getCredentialType(),
                initialData.get("credentialType"));
        // Check component identifier
        assertNotNull(initialData.get("componentsIdentifier"));
        List<?> obj = (List<?>) initialData.get("componentsIdentifier");
        assertEquals(14, obj.size());

        // Check platform properties
        assertNotNull(initialData.get("platformProperties"));
        obj = (List<?>) initialData.get("platformProperties");
        assertEquals(0, obj.size());

    }

    /**
     * Tests initial page when the certificate type is
     * an Endorsement Certificate.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPageEndorsement() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath)
                        .param("id", endorsementCredential.getId().toString())
                        .param("type", "endorsement"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initialData HashMap
        Map<String, String> initialData = (Map<String, String>) result
                .getModelAndView()
                .getModel()
                .get(PolicyPageController.INITIAL_DATA);
        assertEquals(endorsementCredential.getIssuer(), initialData.get("issuer"));
        assertEquals(endorsementCredential.getManufacturer(),
                initialData.get("manufacturer"));
    }

    /**
     * Tests initial page for issuer ID.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPageID() throws Exception {

        // use mock MVC to access a cert from Certificate Details Page path;
        // send it some parameters, including the ID of the cert you want, and
        // a ‘type’ (which is different than the ‘credential type’);
        // check that the certificate details Model attributes exist,
        // and return the MVC data call into a result object
        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath)
                        .param("id", caCertificate.getId().toString())
                        .param("type", "certificateauthority"))
                .andExpect(model().attributeExists(
                        CertificateDetailsPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initialData HashMap
        Map<String, String> initialData = (Map<String, String>) result
                .getModelAndView()
                .getModel()
                .get(PolicyPageController.INITIAL_DATA);

        assertEquals(caCertificate.getIssuer(), initialData.get("issuer"));
        assertEquals(caRootCertificate.getId().toString(),
                initialData.get("issuerID"));
    }

    /**
     * Tests initial page when the certificate type is
     * an Issued Attestation Certificate.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPageIssuedAttestation() throws Exception {

        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath)
                        .param("id", issuedCredential.getId().toString())
                        .param("type", "issued"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initialData HashMap
        Map<String, String> initialData = (Map<String, String>) result
                .getModelAndView()
                .getModel()
                .get(PolicyPageController.INITIAL_DATA);
        assertEquals(issuedCredential.getIssuer(), initialData.get("issuer"));
        //assertEquals(issuedCredential.getEndorsementCredential().getId().toString(),
        //        initialData.get("endorsementID"));
    }
}
