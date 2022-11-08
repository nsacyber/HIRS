package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.PageControllerTest;
import hirs.attestationca.portal.page.PageMessages;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.persist.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.FlashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Set;

import static hirs.attestationca.portal.page.Page.ENDORSEMENT_KEY_CREDENTIALS;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of EndorsementKeyCredentialsPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EndorsementKeyCredentialsPageControllerTest extends PageControllerTest {

    @Autowired
    private CertificateService certificateService;

    private static final String EKCERT = "fakeIntelIntermediateCA.pem";
    private static final String BADEKCERT = "badCert.pem";

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
    }

    /**
     * Prepares tests.
     * @throws IOException if test resources are not found
     */
    @BeforeClass
    public void prepareTests() throws IOException {
        // create a multi part file for the controller upload
        nonEkCertFile = new MockMultipartFile("file", EKCERT, "",
                new ClassPathResource("certificates/" + EKCERT).getInputStream());

        badCertFile = new MockMultipartFile("file", BADEKCERT, "",
                new ClassPathResource("certificates/" + BADEKCERT).getInputStream());
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
        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .multipart("/certificate-request/endorsement-key-credentials/upload")
                .file(nonEkCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        Assert.assertEquals(pageMessages.getSuccess().get(0), "New certificate successfully "
                + "uploaded (" + EKCERT + "): ");
        Assert.assertEquals(pageMessages.getError().size(), 0);

        // verify the cert was actually stored
        Set<Certificate> records =
                certificateService.getCertificate(
                        EndorsementCredential.select(certificateService));
        Assert.assertEquals(records.size(), 1);

        Certificate cert = records.iterator().next();
        Assert.assertFalse(cert.isArchived());

        // now, archive the record
        getMockMvc().perform(MockMvcRequestBuilders
                .post("/certificate-request/endorsement-key-credentials/delete")
                .param("id", cert.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        records = certificateService.getCertificate(EndorsementCredential
                .select(certificateService).includeArchived());
        Assert.assertEquals(records.size(), 1);

        cert = records.iterator().next();
        Assert.assertTrue(cert.isArchived());
    }

    /**
     * Tests that uploading something that is not a cert at all results in an error returned
     * to the web client.
     * @throws Exception an exception occurs
     */
    @Test
    @Rollback
    public void uploadBadEndorsementCert() throws Exception {
        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .multipart("/certificate-request/endorsement-key-credentials/upload")
                .file(badCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        Assert.assertEquals(pageMessages.getError().size(), 1);
        Assert.assertEquals(pageMessages.getSuccess().size(), 0);

        // verify the cert was not actually stored
        Set<Certificate> records =
                certificateService.getCertificate(
                        EndorsementCredential.select(certificateService));
        Assert.assertEquals(records.size(), 0);
    }
}
