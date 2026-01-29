package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.IDevIDCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
import hirs.attestationca.portal.page.Page;
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of IDevId Page Controller.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IDevIdCertificatePageControllerTest extends PageControllerTest {

    // Location of test certs
    private static final String IDEVID_CERT = "certificates/fakeIntelIntermediateCA.pem";
    private static final String BAD_IDEVID_CERT = "certificates/badCert.pem";

    // Base path for the page
    private final String pagePath;

    // Repository manager for IDevId certificates
    @Autowired
    private IDevIDCertificateRepository iDevIDCertificateRepository;

    // A file that contains a cert that is not an IDEVID Cert. Should be parsable as a general cert,
    // but should (eventually) not be stored as an IDEVID because it isn't one.
    private MockMultipartFile nonIDevIdCertFile;

    // A file that is not a cert at all, and just contains garbage text.
    private MockMultipartFile badCertFile;

    /**
     * Constructor providing the IDevId Certificate Page's display and routing specification.
     */
    public IDevIdCertificatePageControllerTest() {
        super(Page.IDEVID_CERTIFICATES);
        pagePath = getPagePath();
    }

    /**
     * Setups the certificates before any tests are run.
     *
     * @throws IOException if test resources are not found
     */
    @BeforeAll
    public void prepareTests() throws IOException {

        // create a multi part file for the controller upload
        String[] pathTokens = IDEVID_CERT.split("/");
        nonIDevIdCertFile = new MockMultipartFile("file", pathTokens[1], "",
                new ClassPathResource(IDEVID_CERT).getInputStream());

        pathTokens = BAD_IDEVID_CERT.split("/");
        badCertFile = new MockMultipartFile("file", pathTokens[1], "",
                new ClassPathResource(BAD_IDEVID_CERT).getInputStream());
    }

    /**
     * Clears the database after each test run.
     */
    @AfterEach
    public void afterEachTest() {
        this.iDevIDCertificateRepository.deleteAll();
    }

    /**
     * Tests the list REST endpoints on the IDevID Certificate page controller.
     *
     * @throws Exception if any issues arise from performing this test.
     */
    @Test
    @Rollback
    public void testGetAllIDevIdCertificates() throws Exception {
        final String[] pathTokens = IDEVID_CERT.split("/");

        // Upload the fake IDevId certificate to the ACA and confirm you get a 300 redirection status
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(nonIDevIdCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Verify that the IDevId certificate has been uploaded to the ACA
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
                pageMessages.getSuccessMessages().get(0));
        assertEquals(0, pageMessages.getErrorMessages().size());

        // Verify that one IDevId has been listed on the page
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andReturn();
    }

    /**
     * Tests the delete REST endpoint on the IDevID Certificate page controller.
     *
     * @throws Exception if any issues arise from performing this test.
     */
    @Test
    @Rollback
    public void testDeleteIDevIDCertificate() throws Exception {
        final String[] pathTokens = IDEVID_CERT.split("/");

        // Upload the fake IDevId certificate to the ACA and confirm you get a 300 redirection status
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(nonIDevIdCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Verify that the IDevId certificate has been uploaded to the ACA
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
                pageMessages.getSuccessMessages().get(0));
        assertEquals(0, pageMessages.getErrorMessages().size());

        // Verify the IDevId cert has been stored
        List<IDevIDCertificate> records = iDevIDCertificateRepository.findAll();
        assertEquals(1, records.size());

        Certificate cert = records.iterator().next();
        final String IDEVID_ID = cert.getId().toString();

        // Now attempt to delete an IDevId certificate
        getMockMvc()
                .perform(MockMvcRequestBuilders
                        .post(pagePath + "/delete")
                        .param("id", IDEVID_ID))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Since deletion doesn't fully remove the item from the repository but instead archives it for potential future use,
        // ensure that when the delete REST endpoint is triggered, it correctly redirects to the IDevId certificate page
        // and no IDevId certificates are displayed on the page.
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()))
                .andReturn();
    }

    /**
     * Tests the bulk-delete REST endpoint on the IDevID Certificate page controller.
     *
     * @throws Exception if any issues arise from performing this test.
     */
    @Test
    @Rollback
    public void testDeleteMultipleIDevIDCertificates() throws Exception {
        final String[] pathTokens = IDEVID_CERT.split("/");

        // Upload multiple fake IDevId certificates to the ACA and confirm you get a 300 redirection status for
        // each upload
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                        .multipart(pagePath + "/upload")
                        .file(nonIDevIdCertFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Verify that the IDevId  certificates have been uploaded to the ACA
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
                pageMessages.getSuccessMessages().get(0));
        assertEquals(0, pageMessages.getErrorMessages().size());

        // Verify one IDevId certificate has been stored
        List<IDevIDCertificate> records = iDevIDCertificateRepository.findAll();
        assertEquals(1, records.size());

        Certificate cert = records.iterator().next();

        // Convert the list of IDevId cert ids to a string of comma separated ids
        final String IDEVID_IDS = String.join(",", List.of(cert.getId().toString()));

        // Now attempt to delete multiple IDevId certificates
        getMockMvc()
                .perform(MockMvcRequestBuilders
                        .post(pagePath + "/bulk-delete")
                        .param("ids", IDEVID_IDS))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Since bulk deletion doesn't fully remove the items from the repository but instead archives them for potential future use,
        // ensure that when the bulk-delete REST endpoint is triggered, it correctly redirects to the IDevId certificate page
        // and no IDevId certificates are displayed on the page.
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()))
                .andReturn();
    }
}
