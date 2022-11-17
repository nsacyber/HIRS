package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.PageControllerTest;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.entity.certificate.Certificate;
import hirs.attestationca.entity.certificate.PlatformCredential;
import hirs.attestationca.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.FlashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Set;

import static hirs.attestationca.portal.page.Page.PLATFORM_CREDENTIALS;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of PlatformCredentialsPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PlatformCredentialsPageControllerTest extends PageControllerTest {
    @Autowired
    private CertificateService certificateService;

    // A cert that is an actual PC cert file and should be parsable.
    private MockMultipartFile realPcCertFile;
    // A file that contains a cert that is not an PC Cert. Should be parsable as a general cert,
    // but should (eventually) not be stored as an PC because it isn't one.
    private MockMultipartFile nonPcCertFile;
    // A file that is not a cert at all, and just contains garbage text.
    private MockMultipartFile badCertFile;

    private static final String NONPCCERT = "fakeIntelIntermediateCA.pem";
    private static final String BADPCCERT = "badCert.pem";
    private static final String REALPCCERT = "Intel_pc.cer";

    /**
     * Constructor providing the Page's display and routing specification.
     */
    public PlatformCredentialsPageControllerTest() {
        super(PLATFORM_CREDENTIALS);
    }

    /**
     * Prepares tests.
     * @throws IOException if test resources are not found
     */
    @BeforeMethod
    public void prepareTests() throws IOException {
        // create a multi part file for the controller upload
        nonPcCertFile = new MockMultipartFile("file", NONPCCERT, "",
                new ClassPathResource("certificates/" + NONPCCERT).getInputStream());

        badCertFile = new MockMultipartFile("file", BADPCCERT, "",
                new ClassPathResource("certificates/" + BADPCCERT).getInputStream());

        realPcCertFile = new MockMultipartFile("file", REALPCCERT, "",
                new ClassPathResource("platform_credentials/" + REALPCCERT)
                        .getInputStream());
    }

    /**
     * Tests uploading a cert that is a Platform Credential, and archiving it.
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadAndArchiveValidPlatformCert() throws Exception {
        Certificate cert = uploadTestCert();
        archiveTestCert(cert);
    }

    private Certificate uploadTestCert() throws Exception {
        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .multipart("/certificate-request/platform-credentials/upload")
                .file(realPcCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        Assert.assertEquals(pageMessages.getSuccess().size(), 1);
        Assert.assertEquals(pageMessages.getError().size(), 0);

        // verify the cert was actually stored
        Set<Certificate> records =
                certificateService.getCertificate(PlatformCredential.select(certificateService));
        Assert.assertEquals(records.size(), 1);

        Certificate cert = records.iterator().next();
        Assert.assertFalse(cert.isArchived());

        return cert;
    }

    private void archiveTestCert(final Certificate cert) throws Exception {
        // now, archive the record
        getMockMvc().perform(MockMvcRequestBuilders
            .post("/certificate-request/platform-credentials/delete")
            .param("id", cert.getId().toString()))
            .andExpect(status().is3xxRedirection())
            .andReturn();

        Set<Certificate> records =
                certificateService.getCertificate(PlatformCredential
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

        // upload the same cert again
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .multipart("/certificate-request/platform-credentials/upload")
                .file(realPcCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        Assert.assertEquals(pageMessages.getSuccess().size(), 1);
        Assert.assertEquals(pageMessages.getError().size(), 0);
        Assert.assertEquals(pageMessages.getSuccess().get(0),
                "Pre-existing certificate found and unarchived (" + REALPCCERT + "): ");

        // verify the cert was actually stored
        Set<Certificate> records = certificateService.getCertificate(PlatformCredential.select(
                certificateService));
        Assert.assertEquals(records.size(), 1);

        Certificate newCert = records.iterator().next();

        // verify that the cert was unarchived
        Assert.assertFalse(newCert.isArchived());
        // verify that the createTime was updated
        Assert.assertTrue(newCert.getCreateTime().getTime() > cert.getCreateTime().getTime());
    }

    /**
     * Tests uploading a cert that is not a Platform Credential, which results in failure.
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadNonPlatformCert() throws Exception {
        // verify the cert was not actually stored
        Set<Certificate> originalRecords =
                certificateService.getCertificate(PlatformCredential.select(certificateService));
        Assert.assertEquals(originalRecords.size(), 0);

        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .multipart("/certificate-request/platform-credentials/upload")
                .file(nonPcCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        Assert.assertEquals(pageMessages.getSuccess().size(), 0);
        Assert.assertEquals(pageMessages.getError().size(), 1);

        // verify the cert was not actually stored
        Set<Certificate> records =
                certificateService.getCertificate(PlatformCredential.select(certificateService));
        Assert.assertEquals(records.size(), 0);
    }

    /**
     * Tests that uploading something that is not a cert at all results in an error returned
     * to the web client.
     * @throws Exception an exception occurs
     */
    @Test
    public void uploadBadPlatformCert() throws Exception {
        // perform upload. Attach csv file and add HTTP parameters for the baseline name and type.
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .multipart("/certificate-request/platform-credentials/upload")
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
                certificateService.getCertificate(PlatformCredential.select(certificateService));
        Assert.assertEquals(records.size(), 0);
    }
}
