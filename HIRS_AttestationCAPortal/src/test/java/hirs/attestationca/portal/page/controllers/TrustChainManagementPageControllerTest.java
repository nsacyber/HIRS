package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.portal.page.PageControllerTest;
import hirs.attestationca.portal.page.PageMessages;
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
import java.security.cert.X509Certificate;
import java.util.List;

import static hirs.attestationca.portal.page.Page.TRUST_CHAIN;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of TrustChainManagementPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TrustChainManagementPageControllerTest extends PageControllerTest {

    // Location of test certs
    private static final String NONCACERT = "certificates/fakeIntelIntermediateCA.pem";
    private static final String BADCERT = "certificates/badCert.pem";
    // Base path for the page
    private final String pagePath;
    // Repository manager to handle data access between certificate entity and data storage in db
    @Autowired
    private CertificateRepository certificateRepository;
    @Autowired
    private X509Certificate acaCert;
    // A file that contains a cert that is not an UTC Cert. Should be parsable as a general
    // cert, but should (eventually) not be stored as an UTC because it isn't one.
    private MockMultipartFile nonCaCertFile;

    // A file that is not a cert at all, and just contains garbage text.
    private MockMultipartFile badCertFile;


    /**
     * Constructor providing the Page's display and routing specification.
     */
    public TrustChainManagementPageControllerTest() {
        super(TRUST_CHAIN);
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
        String[] pathTokens = NONCACERT.split("/");
        nonCaCertFile = new MockMultipartFile("file", pathTokens[1], "",
                new ClassPathResource(NONCACERT).getInputStream());

        pathTokens = BADCERT.split("/");
        badCertFile = new MockMultipartFile("file", pathTokens[1], "",
                new ClassPathResource(BADCERT).getInputStream());
    }

    /**
     * Checks that the page initializes correctly.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testInitPage() throws Exception {

        // verify page is initialized with ACA cert properties
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(TrustChainCertificatePageController.ACA_CERT_DATA))
                .andExpect(model().attribute(
                        TrustChainCertificatePageController.ACA_CERT_DATA,
                        hasEntry("issuer", "CN=Fake Root CA"))
                );
    }

    /**
     * Tests downloading the aca cert.
     *
     * @throws Exception when getting raw report
     */
    @Test
    @Rollback
    public void testDownloadAcaCert() throws Exception {

        // verify cert file attachment and content
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(
                        pagePath + "/download-aca-cert"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"hirs-aca-cert.cer\""))
                .andExpect(content().bytes(acaCert.getEncoded()));
    }

    /**
     * Tests downloading the certificate.
     *
     * @throws Exception when getting raw report
     */
    @Test
    @Rollback
    public void testDownloadCert() throws Exception {

        Certificate cert = uploadTestCert();

        String fileName = "attachment;filename=\"" + "CertificateAuthorityCredential_"
                + cert.getSerialNumber()
                + ".cer\"";

        // verify cert file attachment and content
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(
                                pagePath + "/download")
                        .param("id", cert.getId().toString())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition",
                        fileName))
                .andExpect(content().bytes(cert.getRawBytes()));

    }

    /**
     * Tests uploading a cert that is a valid CA Credential that can be used in a trust chain.
     * Currently this test may pass certs that meet some, but not all requirements
     * However the underlying code is looking for the basic elements of a CA certificate
     * generic credential successfully.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadAndArchiveCaTrustCert() throws Exception {
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

        String[] pathTokens = NONCACERT.split("/");

        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(nonCaCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
                pageMessages.getSuccess()
                        .get(0));
        assertEquals(0, pageMessages.getError().size());

        // verify the cert was actually stored
        List<Certificate> records =
                certificateRepository.findAll();
        assertEquals(1, records.size());

        //Check the cert is not already in the archive
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

        List<Certificate> records = certificateRepository.findAll();
        assertEquals(1, records.size());

        assertTrue(records.iterator().next().isArchived());
    }

    /**
     * Tests that uploading a certificate when an identical certificate is archived will cause
     * the existing certificate to be unarchived and updated.
     *
     * @throws Exception if an exception occurs
     */
//    @Test
    @Rollback
    public void uploadCausesUnarchive() throws Exception {

        String[] pathTokens = NONCACERT.split("/");

        Certificate cert = uploadTestCert();
        archiveTestCert(cert);

        // upload the same certificate again
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(nonCaCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals(1, pageMessages.getSuccess().size());
        assertEquals(0, pageMessages.getError().size());
        assertEquals("Pre-existing certificate found and unarchived (" + pathTokens[1] + "): ",
                pageMessages.getSuccess().get(0));

        // verify the cert can be retrieved and that there is only 1 cert in db
        List<Certificate> records = certificateRepository.findAll();
        assertEquals(1, records.size());
        Certificate newCert = records.iterator().next();

        // verify that the cert is now unarchived
        assertFalse(newCert.isArchived());
        // verify that the createTime was updated
        assertTrue(newCert.getCreateTime().getTime() > cert.getCreateTime().getTime());
    }

    /**
     * Tests that uploading something that is not a cert at all results in an error returned
     * to the web client.
     *
     * @throws Exception an exception occurs
     */
    @Test
    @Rollback
    public void uploadBadCaCert() throws Exception {
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
        List<Certificate> records = certificateRepository.findAll();
        assertEquals(0, records.size());
    }

}
