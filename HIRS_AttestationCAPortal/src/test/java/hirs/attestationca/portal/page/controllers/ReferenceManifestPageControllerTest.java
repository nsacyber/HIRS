package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageControllerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

/**
 * Integration tests that test the URL End Points of Reference Manifest Page Controller.
 */
public class ReferenceManifestPageControllerTest extends PageControllerTest {

    // Location of test RIMs
    private static final String FAKE_RIM = "";

    // Base path for the page
    private final String pagePath;

    // RIM Multipart file
    private MockMultipartFile rimMultiPartFile;

    /**
     * Constructor providing the Reference Manifest Page's display and routing specification.
     */
    public ReferenceManifestPageControllerTest() {
        super(Page.REFERENCE_MANIFESTS);
        pagePath = getPagePath();
    }

    /**
     * Setups the RIMS before any tests are run.
     *
     * @throws IOException if test resources are not found
     */
    @BeforeAll
    public void prepareTests() throws IOException {

    }

    /**
     * Clears the database after each test run.
     */
    @AfterEach
    public void afterEachTest() {
    }

    /**
     * Tests the list REST endpoint on the Reference Manifest page controller.
     * todo Finish writing up tests
     *
     * @throws Exception if any issues arise from performing this test.
     */
    @Test
    public void testGetAllRIMs() throws Exception {
//        final String[] pathTokens = FAKE_RIM.split("/");
//
//        // Upload the fake RIM to the ACA and confirm you get a 300 redirection status
//        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
//                        .multipart(pagePath + "/upload")
//                        .file(rimMultiPartFile))
//                .andExpect(status().is3xxRedirection())
//                .andReturn();
//
//        // Verify that the RIM has been uploaded to the ACA
//        FlashMap flashMap = result.getFlashMap();
//        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
//        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
//                pageMessages.getSuccessMessages().get(0));
//        assertEquals(0, pageMessages.getErrorMessages().size());
//
//        // Verify that one RIM has been listed on the page
//        getMockMvc()
//                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data", hasSize(1)))
//                .andReturn();
    }

    /**
     * Tests the delete REST endpoint on the Reference Manifest page controller.
     * todo Finish writing up tests
     *
     * @throws Exception if an exception occurs
     */
    @Test
    public void testDeleteRIM() throws Exception {

//        // Upload the fake RIM to the ACA and confirm you get a 300 redirection status
//        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
//                        .multipart(pagePath + "/upload")
//                        .file(rimMultiPartFile))
//                .andExpect(status().is3xxRedirection())
//                .andReturn();
//
//        // Verify that the RIM has been uploaded to the ACA
//        FlashMap flashMap = result.getFlashMap();
//        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
//        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
//                pageMessages.getSuccessMessages().get(0));
//        assertEquals(0, pageMessages.getErrorMessages().size());
//
//        // Verify the IDevId cert has been stored
//        List<ReferenceManifest> records = referenceManifestRepository.findAll();
//        assertEquals(1, records.size());
//
//        ReferenceManifest rim = records.iterator().next();
//        final String RIM_ID = rim.getId().toString();
//
//        // Now attempt to delete an IDevId certificate
//        getMockMvc()
//                .perform(MockMvcRequestBuilders
//                        .post(pagePath + "/delete")
//                        .param("id", RIM_ID))
//                .andExpect(status().is3xxRedirection())
//                .andReturn();
//
//        getMockMvc()
//                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data", empty()))
//                .andReturn();
    }

    /**
     * Tests the bulk-delete REST endpoint on the Reference Manifest page controller.
     * todo Finish writing up tests
     *
     * @throws Exception if an exception occurs
     */
    @Test
    public void testDeleteMultipleRIMS() throws Exception {
//        final String[] pathTokens = FAKE_RIM.split("/");
//
//        // Upload multiple fake RIMS to the ACA and confirm you get a 300 redirection status for
//        // each upload
//        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
//                        .multipart(pagePath + "/upload")
//                        .file(rimMultiPartFile))
//                .andExpect(status().is3xxRedirection())
//                .andReturn();
//
//        // Verify that the RIMS have been uploaded to the ACA
//        FlashMap flashMap = result.getFlashMap();
//        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
//        assertEquals("New certificate successfully uploaded (" + pathTokens[1] + "): ",
//                pageMessages.getSuccessMessages().get(0));
//        assertEquals(0, pageMessages.getErrorMessages().size());
//
//        // Verify one rim has been stored
//        List<ReferenceManifest> records = referenceManifestRepository.findAll();
//        assertEquals(1, records.size());
//
//        ReferenceManifest referenceManifest = records.iterator().next();
//
//        // Convert the list of rims ids to a string of comma separated ids
//        final String RIM_IDS = String.join(",", List.of(referenceManifest.getId().toString()));
//
//        // Now attempt to delete multiple RIMs
//        getMockMvc()
//                .perform(MockMvcRequestBuilders
//                        .post(pagePath + "/bulk-delete")
//                        .param("ids", RIM_IDS))
//                .andExpect(status().is3xxRedirection())
//                .andReturn();
//
//        getMockMvc()
//                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data", empty()))
//                .andReturn();
    }
}
