package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.portal.page.PageControllerTest;
import hirs.attestationca.portal.page.PageMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.FlashMap;

import java.io.IOException;
import java.util.List;

import static hirs.attestationca.portal.page.Page.PLATFORM_CREDENTIALS;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of PlatformCredentialsPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PlatformCredentialsPageControllerTest extends PageControllerTest {

    // Location of test certs
    private static final String REALPCCERT = "platform_credentials/Intel_pc.cer";

    private static final String NONPCCERT = "certificates/fakeIntelIntermediateCA.pem";

    private static final String BADPCCERT = "certificates/badCert.pem";

    // Base path for the page
    private final String pagePath;

    // Repository manager to handle data access between certificate entity and data storage in db
    @Autowired
    private CertificateRepository certificateRepository;

    // A cert that is an actual PC cert file and should be parsable.
    private MockMultipartFile realPcCertFile;

    // A file that contains a cert that is not an PC Cert. Should be parsable as a general cert,
    // but should (eventually) not be stored as an PC because it isn't one.
    private MockMultipartFile nonPcCertFile;

    // A file that is not a cert at all, and just contains garbage text.
    private MockMultipartFile badCertFile;

    /**
     * Constructor providing the Page's display and routing specification.
     */
    public PlatformCredentialsPageControllerTest() {
        super(PLATFORM_CREDENTIALS);
        pagePath = getPagePath();
    }

    /**
     * Prepares tests.
     *
     * @throws IOException if test resources are not found
     */
    @BeforeAll
    public void prepareTests() throws IOException {

        // create a multi part file for the controller upload
        String[] pathTokens = REALPCCERT.split("/");
        realPcCertFile = new MockMultipartFile("file", pathTokens[1], "",
                new ClassPathResource(REALPCCERT)
                        .getInputStream());

        pathTokens = NONPCCERT.split("/");
        nonPcCertFile = new MockMultipartFile("file", pathTokens[1], "",
                new ClassPathResource(NONPCCERT).getInputStream());

        pathTokens = BADPCCERT.split("/");
        badCertFile = new MockMultipartFile("file", pathTokens[1], "",
                new ClassPathResource(BADPCCERT).getInputStream());

    }

    /**
     * Clears the database after each test run.
     */
    @AfterEach
    public void afterEachTest() {
        this.certificateRepository.deleteAll();
    }

    /**
     * Tests uploading a cert that is a Platform Credential, and archiving it.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadAndArchiveValidPlatformCert() throws Exception {
        Certificate cert = uploadTestCert();
        archiveTestCert(cert);
    }

    /**
     * Uploads test cert to db.
     *
     * @return the cert that was uploaded
     * @throws Exception if an exception occurs
     */
    private Certificate uploadTestCert() throws Exception {

        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(realPcCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals(1, pageMessages.getSuccessMessages().size());
        assertEquals(0, pageMessages.getErrorMessages().size());

        // verify the cert was actually stored
        List<Certificate> records =
                certificateRepository.findAll();
        assertEquals(1, records.size());

        // verify the cert is not yet archived
        Certificate cert = records.iterator().next();
        assertFalse(cert.isArchived());

        return cert;
    }

    /**
     * Archives test cert that is in db by setting the archive flag.
     *
     * @param cert certificate.
     * @throws Exception if an exception occurs
     */
    private void archiveTestCert(final Certificate cert) throws Exception {

        // now, archive the record
        getMockMvc().perform(MockMvcRequestBuilders
                        .post(pagePath + "/delete")
                        .param("id", cert.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        List<Certificate> records =
                certificateRepository.findAll();
        assertEquals(1, records.size());

        assertTrue(records.iterator().next().isArchived());
    }

    /**
     * Tests that uploading a certificate when an identical certificate is archived will cause
     * the existing certificate to be unarchived and updated.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadCausesUnarchive() throws Exception {

        String[] pathTokens = REALPCCERT.split("/");

        Certificate cert = uploadTestCert();
        archiveTestCert(cert);

        // upload the same cert again
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(realPcCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals(1, pageMessages.getSuccessMessages().size());
        assertEquals(0, pageMessages.getErrorMessages().size());
        assertEquals("Pre-existing certificate found and unarchived ("
                        + pathTokens[1] + "): ",
                pageMessages.getSuccessMessages().get(0));

        // verify there is still only one cert in db
        List<Certificate> records = certificateRepository.findAll();
        assertEquals(1, records.size());

        Certificate newCert = records.iterator().next();

        // verify that the cert was unarchived
        assertFalse(newCert.isArchived());

        // verify that the createTime was updated
        assertTrue(newCert.getCreateTime().getTime() > cert.getCreateTime().getTime());
    }

    /**
     * Tests uploading a cert that is not a Platform Credential, which results in failure.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadNonPlatformCert() throws Exception {

        // verify there are initially no certs in db
        List<Certificate> originalRecords =
                certificateRepository.findAll();
        assertEquals(0, originalRecords.size());

        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(nonPcCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals(0, pageMessages.getSuccessMessages().size());
        assertEquals(1, pageMessages.getErrorMessages().size());

        // verify the cert was not actually stored
        List<Certificate> records =
                certificateRepository.findAll();
        assertEquals(0, records.size());
    }

    /**
     * Tests that uploading something that is not a cert at all results in an error returned
     * to the web client.
     *
     * @throws Exception an exception occurs
     */
    @Test
    public void uploadBadPlatformCert() throws Exception {
        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(badCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals(1, pageMessages.getErrorMessages().size());
        assertEquals(0, pageMessages.getSuccessMessages().size());

        // verify the cert was not actually stored
        List<Certificate> records =
                certificateRepository.findAll();
        assertEquals(0, records.size());
    }

    /**
     * Tests the delete REST endpoint on the Platform Credential page controller.
     *
     * @throws Exception if any issues arise from performing this test.
     */
    @Test
    @Rollback
    public void testDeletePlatformCredential() throws Exception {
        final String[] pathTokens = REALPCCERT.split("/");

        // Upload the fake platform certificate to the ACA and confirm you get a 300 redirection status
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(realPcCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Verify that the platform certificate has been uploaded to the ACA
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
                pageMessages.getSuccessMessages().get(0));
        assertEquals(0, pageMessages.getErrorMessages().size());

        // Verify the platform cert has been stored
        List<Certificate> records = certificateRepository.findAll();
        assertEquals(1, records.size());

        Certificate cert = records.iterator().next();
        final String PLATFORM_CERT_ID = cert.getId().toString();

        // Now attempt to delete an platform certificate
        getMockMvc()
                .perform(MockMvcRequestBuilders
                        .post(pagePath + "/delete")
                        .param("id", PLATFORM_CERT_ID))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Since deletion doesn't fully remove the item from the repository but instead archives it for potential future use,
        // ensure that when the delete REST endpoint is triggered, it correctly redirects to the Platform Certificate page
        // and no platform certificates are displayed on the page.
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()))
                .andReturn();
    }

    /**
     * Tests the bulk-delete REST endpoint on the Platform Credential page controller.
     *
     * @throws Exception if any issues arise from performing this test.
     */
    @Test
    @Rollback
    public void testDeleteMultiplePlatformCredentials() throws Exception {
        final String[] pathTokens = REALPCCERT.split("/");

        // Upload multiple fake platform certificates to the ACA and confirm you get a 300 redirection status for
        // each upload
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(realPcCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Verify that the platform certificates have been uploaded to the ACA
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
                pageMessages.getSuccessMessages().get(0));
        assertEquals(0, pageMessages.getErrorMessages().size());

        // Verify one platform certificate has been stored
        List<Certificate> records = certificateRepository.findAll();
        assertEquals(1, records.size());

        Certificate cert = records.iterator().next();

        // Convert the list of platform cert ids to a string of comma separated ids
        final String PLATFORM_CERT_IDS = String.join(",", List.of(cert.getId().toString()));

        // Now attempt to delete multiple platform certificates
        getMockMvc()
                .perform(MockMvcRequestBuilders
                        .post(pagePath + "/bulk-delete")
                        .param("ids", PLATFORM_CERT_IDS))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Since bulk deletion doesn't fully remove the items from the repository but instead archives them for potential future use,
        // ensure that when the bulk-delete REST endpoint is triggered, it correctly redirects to the platform certificate page
        // and no platform certificates are displayed on the page.
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()))
                .andReturn();
    }
}
