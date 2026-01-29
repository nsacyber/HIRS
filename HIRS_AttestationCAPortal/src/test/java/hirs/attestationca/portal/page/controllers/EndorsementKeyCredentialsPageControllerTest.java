package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.EndorsementCredentialRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
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

import static hirs.attestationca.portal.page.Page.ENDORSEMENT_KEY_CREDENTIALS;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of Endorsement Key Credentials PageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EndorsementKeyCredentialsPageControllerTest extends PageControllerTest {

    // Location of test certs
    private static final String EKCERT = "certificates/fakeIntelIntermediateCA.pem";
    private static final String BADEKCERT = "certificates/badCert.pem";

    // Base path for the page
    private final String pagePath;

    // Repository manager to handle data access between endorsement certificate entity and data storage in db
    @Autowired
    private EndorsementCredentialRepository endorsementCredentialRepository;

    // A file that contains a cert that is not an EK Cert. Should be parsable as a general cert,
    // but should (eventually) not be stored as an EK because it isn't one.
    private MockMultipartFile nonEkCertFile;

    // A file that is not a cert at all, and just contains garbage text.
    private MockMultipartFile badCertFile;

    /**
     * Constructor providing the Endorsement Key Credential Page's display and routing specification.
     */
    public EndorsementKeyCredentialsPageControllerTest() {
        super(ENDORSEMENT_KEY_CREDENTIALS);
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
        String[] pathTokens = EKCERT.split("/");
        nonEkCertFile = new MockMultipartFile("file", pathTokens[1], "",
                new ClassPathResource(EKCERT).getInputStream());

        pathTokens = BADEKCERT.split("/");
        badCertFile = new MockMultipartFile("file", pathTokens[1], "",
                new ClassPathResource(BADEKCERT).getInputStream());
    }

    /**
     * Clears the database after each test run.
     */
    @AfterEach
    public void afterEachTest() {
        this.endorsementCredentialRepository.deleteAll();
    }

    /**
     * Tests uploading a cert that is not an Endorsement Credential. Eventually, this
     * should indicate a failure, but for now, EndorsementCredential just parses it as a
     * generic credential successfully.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadAndArchiveNonEndorsementCert() throws Exception {

        String[] pathTokens = EKCERT.split("/");

        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(nonEkCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
                pageMessages.getSuccessMessages().get(0));
        assertEquals(0, pageMessages.getErrorMessages().size());

        // verify the cert was actually stored
        List<EndorsementCredential> records =
                endorsementCredentialRepository.findAll();
        assertEquals(1, records.size());

        // verify the cert is not yet archived
        Certificate cert = records.iterator().next();
        assertFalse(cert.isArchived());

        // now, archive the record
        getMockMvc().perform(MockMvcRequestBuilders
                        .post(pagePath + "/delete")
                        .param("id", cert.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        records = endorsementCredentialRepository.findAll();
        assertEquals(1, records.size());

        // verify the cert is now archived
        cert = records.iterator().next();
        assertTrue(cert.isArchived());

    }

    /**
     * Tests that uploading something that is not a cert at all results in an error returned
     * to the web client.
     *
     * @throws Exception an exception occurs
     */
    @Test
    @Rollback
    @DirtiesContext(methodMode = BEFORE_METHOD)     // clear endorsement cert from db
    public void testUploadBadEndorsementCert() throws Exception {

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
        List<EndorsementCredential> records =
                endorsementCredentialRepository.findAll();
        assertEquals(0, records.size());
    }

    /**
     * Tests the delete REST endpoint on the Endorsement Key Credential page controller.
     *
     * @throws Exception if any issues arise from performing this test.
     */
    @Test
    @Rollback
    public void testDeleteEndorsementKeyCredential() throws Exception {

        final String[] pathTokens = EKCERT.split("/");

        // Upload the fake EK certificate to the ACA and confirm you get a 300 redirection status
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(nonEkCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Verify that the EK certificate has been uploaded to the ACA
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
                pageMessages.getSuccessMessages().get(0));
        assertEquals(0, pageMessages.getErrorMessages().size());

        // Verify the EK cert has been stored
        List<EndorsementCredential> records = endorsementCredentialRepository.findAll();
        assertEquals(1, records.size());

        Certificate cert = records.iterator().next();
        final String EK_ID = cert.getId().toString();

        // Now attempt to delete an Endorsement Credential
        getMockMvc()
                .perform(MockMvcRequestBuilders
                        .post(pagePath + "/delete")
                        .param("id", EK_ID))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Since deletion doesn't fully remove the item from the repository but instead archives it for potential future use,
        // ensure that when the delete REST endpoint is triggered, it correctly redirects to the Endorsement Credential page
        // and no Endorsement credentials are displayed on the page.
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()))
                .andReturn();
    }

    /**
     * Tests the bulk-delete REST endpoint on the Endorsement Key Credential page controller.
     *
     * @throws Exception if any issues arise from performing this test.
     */
    @Test
    @Rollback
    public void testDeleteMultipleEndorsementKeyCredentials() throws Exception {
        final String[] pathTokens = EKCERT.split("/");

        // Upload multiple fake EK certificates to the ACA and confirm you get a 300 redirection status for
        // each upload
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(nonEkCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Verify that the EK certificates have been uploaded to the ACA
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
                pageMessages.getSuccessMessages().get(0));
        assertEquals(0, pageMessages.getErrorMessages().size());

        // Verify one EK certificate has been stored
        List<EndorsementCredential> records = endorsementCredentialRepository.findAll();
        assertEquals(1, records.size());

        Certificate cert = records.iterator().next();

        // Convert the list of ek cert ids to a string of comma separated ids
        final String EK_IDS = String.join(",", List.of(cert.getId().toString()));

        // Now attempt to delete multiple Endorsement Credentials
        getMockMvc()
                .perform(MockMvcRequestBuilders
                        .post(pagePath + "/bulk-delete")
                        .param("ids", EK_IDS))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Since bulk deletion doesn't fully remove the items from the repository but instead archives them for potential future use,
        // ensure that when the bulk-delete REST endpoint is triggered, it correctly redirects to the Endorsement Credential page
        // and no Endorsement credentials are displayed on the page.
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()))
                .andReturn();
    }
}
