package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageControllerTest;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 *
 */
public class ReferenceManifestPageControllerTest extends PageControllerTest {
    // Base path for the page
    private final String pagePath;

    /**
     * Constructor providing the Reference Manifest Page's display and routing specification.
     */
    public ReferenceManifestPageControllerTest() {
        super(Page.REFERENCE_MANIFESTS);
        pagePath = getPagePath();
    }

    /**
     * Tests the list REST endpoint on the Reference Manifest page controller.
     */
    @Test
    public void testGetAllRIMs() throws Exception {

    }

    /**
     * Tests the delete REST endpoint on the Reference Manifest page controller.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    public void testDeleteRIM() throws Exception {

        final String RIM_ID = "";

        // perform test todo
//        getMockMvc()
//                .perform(MockMvcRequestBuilders
//                        .post(pagePath + "/delete")
//                        .param("id", RIM_ID))
//                .andExpect(status().isOk())
//                .andReturn();
    }

    /**
     * @throws Exception
     */
    @Test
    public void testDeleteInvalidRIM() throws Exception {

    }

    /**
     * Tests the bulk-delete REST endpoint on the Reference Manifest page controller.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    public void testDeleteMultipleRIMS() throws Exception {
        final List<String> RIM_IDS = List.of("", "");
        final String RIM_IDS_STRINGIFIED = String.join(",", RIM_IDS);

        // perform test todo
//        getMockMvc()
//                .perform(MockMvcRequestBuilders
//                        .post(pagePath + "/bulk-delete")
//                        .param("ids", RIM_IDS_STRINGIFIED))
//                .andExpect(status().isOk())
//                .andReturn();

    }

    /**
     * @throws Exception
     */
    @Test
    public void testDeleteMultipleInvalidRIMs() throws Exception {

    }
}
