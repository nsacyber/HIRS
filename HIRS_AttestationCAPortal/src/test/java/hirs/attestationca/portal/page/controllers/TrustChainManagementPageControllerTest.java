package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.PageControllerTest;
import hirs.attestationca.portal.page.PageMessages;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
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
import java.security.cert.X509Certificate;
import java.util.Set;

import static hirs.attestationca.portal.page.Page.TRUST_CHAIN;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of TrustChainManagementPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TrustChainManagementPageControllerTest extends PageControllerTest {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private X509Certificate acaCert;

    // A file that contains a cert that is not an UTC Cert. Should be parsable as a general
    // cert, but should (eventually) not be stored as an UTC because it isn't one.
    private MockMultipartFile nonCaCertFile;
    // A file that is not a cert at all, and just contains garbage text.
    private MockMultipartFile badCertFile;

    private static final String NONCACERT = "fakeIntelIntermediateCA.pem";
    private static final String BADCERT = "badCert.pem";

    /**
     * Constructor providing the Page's display and routing specification.
     */
    public TrustChainManagementPageControllerTest() {
        super(TRUST_CHAIN);
    }


    /**
     * Prepares tests.
     * @throws IOException if test resources are not found
     */
    @BeforeClass
    public void prepareTests() throws IOException {
        // create a multi part file for the controller upload
        nonCaCertFile = new MockMultipartFile("file", NONCACERT, "",
                new ClassPathResource("certificates/" + NONCACERT).getInputStream());

        badCertFile = new MockMultipartFile("file", BADCERT, "",
                new ClassPathResource("certificates/" + BADCERT).getInputStream());
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
            .perform(MockMvcRequestBuilders.get("/certificate-request/trust-chain"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists(CertificateRequestPageController.ACA_CERT_DATA))
            .andExpect(model().attribute(
                    CertificateRequestPageController.ACA_CERT_DATA,
                        hasEntry("issuer", "CN=Fake Root CA"))
            );
    }

    /**
     * Tests downloading the aca cert.
     *
     * @throws java.lang.Exception when getting raw report
     */
    @Test
    @Rollback
    public void testDownloadAcaCert() throws Exception {

        // verify cert file attachment and content
        getMockMvc()
            .perform(MockMvcRequestBuilders.get(
                    "/certificate-request/trust-chain/download-aca-cert"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/octet-stream"))
            .andExpect(header().string("Content-Disposition",
                    "attachment; filename=\"hirs-aca-cert.cer\""))
                .andExpect(content().bytes(acaCert.getEncoded()));
    }

    /**
     * Tests downloading the certificate.
     * @throws java.lang.Exception when getting raw report
     */
    @Test
    @Rollback
    public void testDownloadCert() throws Exception {

        Certificate cert = uploadTestCert();

        StringBuilder fileName = new StringBuilder("attachment;filename=\"");
        fileName.append("CertificateAuthorityCredential_");
        fileName.append(cert.getSerialNumber());
        fileName.append(".cer\"");

        // verify cert file attachment and content
        getMockMvc()
            .perform(MockMvcRequestBuilders.get(
                            "/certificate-request/trust-chain/download")
                .param("id", cert.getId().toString())
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/octet-stream"))
            .andExpect(header().string("Content-Disposition",
                    fileName.toString()))
            .andExpect(content().bytes(cert.getRawBytes()));

    }

    /**
     * Tests uploading a cert that is a valid CA Credential that can be used in a trust chain.
     * Currently this test may pass certs that meet some, but not all requirements
     * However the underlying code is looking for the basic elements of a CA certificate
     * generic credential successfully.
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadAndArchiveCaTrustCert() throws Exception {
        Certificate cert = uploadTestCert();
        archiveTestCert(cert);
    }

    private Certificate uploadTestCert() throws Exception {
        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .multipart("/certificate-request/trust-chain/upload")
                .file(nonCaCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        Assert.assertEquals(pageMessages.getSuccess()
                .get(0), "New certificate successfully uploaded (" + NONCACERT + "): ");
        Assert.assertEquals(pageMessages.getError().size(), 0);

        // verify the cert was actually stored
        Set<Certificate> records =
                certificateService.getCertificate(
                        CertificateAuthorityCredential.select(certificateService));
        Assert.assertEquals(records.size(), 1);

        //Check the cert is not already in the archive
        Certificate cert = records.iterator().next();
        Assert.assertFalse(cert.isArchived());

        return cert;
     }

     private void archiveTestCert(final Certificate cert) throws Exception {
        // now, archive the record
        getMockMvc().perform(MockMvcRequestBuilders
                .post("/certificate-request/trust-chain/delete")
                .param("id", cert.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Set<Certificate> records = certificateService.getCertificate(CertificateAuthorityCredential
                .select(certificateService).includeArchived());
        Assert.assertEquals(records.size(), 1);

        Assert.assertTrue(records.iterator().next().isArchived());
    }

    /**
     * Tests that uploading a certificate when an identical certificate is archived will cause
     * the existing certificate to be unarchived and updated.
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadCausesUnarchive() throws Exception {
        Certificate cert = uploadTestCert();
        archiveTestCert(cert);

        // upload the same certificate again
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .multipart("/certificate-request/trust-chain/upload")
                .file(nonCaCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        Assert.assertEquals(pageMessages.getSuccess().size(), 1);
        Assert.assertEquals(pageMessages.getError().size(), 0);
        Assert.assertEquals(pageMessages.getSuccess().get(0),
                "Pre-existing certificate found and unarchived (" + NONCACERT + "): ");

        // verify the cert can be retrieved without looking at archived certs
        Set<Certificate> records = certificateService.getCertificate(CertificateAuthorityCredential
                .select(certificateService).includeArchived());
        Assert.assertEquals(records.size(), 1);
        Certificate newCert = records.iterator().next();

        // verify that the cert really is unarchived
        Assert.assertFalse(newCert.isArchived());
        // verify that the createTime was updated
        Assert.assertTrue(newCert.getCreateTime().getTime() > cert.getCreateTime().getTime());
    }

    /**
     * Tests that uploading something that is not a cert at all results in an error returned
     * to the web client.
     * @throws Exception an exception occurs
     */
    @Test
    @Rollback
    public void uploadBadCaCert() throws Exception {
        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .multipart("/certificate-request/trust-chain/upload")
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
                        CertificateAuthorityCredential.select(certificateService));
        Assert.assertEquals(records.size(), 0);
    }

}
