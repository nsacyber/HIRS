package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.EndorsementCredentialRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.portal.page.PageControllerTest;
import hirs.attestationca.portal.page.PageMessages;
import java.io.IOException;
import java.util.List;
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
import static hirs.attestationca.portal.page.Page.ENDORSEMENT_KEY_CREDENTIALS;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of EndorsementKeyCredentialsPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EndorsementKeyCredentialsPageControllerTest extends PageControllerTest {

    // Base path for the page
    private String pagePath;

    // Repository manager to handle data access between certificate entity and data storage in db
    @Autowired
    private CertificateRepository certificateRepository;

    // Repository manager to handle data access between endorsement certificate entity and data storage in db
    @Autowired
    private EndorsementCredentialRepository endorsementCredentialRepository;

    // Location of test certs
    private static final String EKCERT = "certificates/fakeIntelIntermediateCA.pem";
    private static final String BADEKCERT = "certificates/badCert.pem";

    // A file that contains a cert that is not an EK Cert. Should be parsable as a general cert,
    // but should (eventually) not be stored as an EK because it isn't one.
    private MockMultipartFile nonEkCertFile;
    // A file that is not a cert at all, and just contains garbage text.
    private MockMultipartFile badCertFile;

    /**
     * Constructor providing the Page's display and routing specification.
     */
    public EndorsementKeyCredentialsPageControllerTest() {
        super(ENDORSEMENT_KEY_CREDENTIALS);
        pagePath = getPagePath();
    }

    /**
     * Prepares tests.
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
     * Tests uploading a cert that is not an Endorsement Credential. Eventually, this
     * should indicate a failure, but for now, EndorsementCredential just parses it as a
     * generic credential successfully.
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
                pageMessages.getSuccess().get(0));
        assertEquals(0, pageMessages.getError().size());

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
     * @throws Exception an exception occurs
     */
    @Test
    @Rollback
    @DirtiesContext(methodMode = BEFORE_METHOD)     // clear endorsement cert from db
    public void uploadBadEndorsementCert() throws Exception {
        
        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(badCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals(1, pageMessages.getError().size());
        assertEquals(0, pageMessages.getSuccess().size());

        // verify the cert was not actually stored
        List<EndorsementCredential> records =
                endorsementCredentialRepository.findAll();
        assertEquals(0, records.size());
    }
}
