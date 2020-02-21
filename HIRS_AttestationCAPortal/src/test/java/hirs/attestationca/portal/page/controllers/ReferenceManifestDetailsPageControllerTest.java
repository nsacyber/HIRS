package hirs.attestationca.portal.page.controllers;

import hirs.data.persist.ReferenceManifest;
import hirs.persist.DBReferenceManifestManager;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageControllerTest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Map;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration tests that test the URL End Points of
 * EndorsementKeyCredentialsPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ReferenceManifestDetailsPageControllerTest extends PageControllerTest {

    private static final String GOOD_RIM_FILE = "/rims/generated_good.swidtag";
    private static final String ID = "046b6c7f-0b8a-43b9-b35d-6489e6daee91";

    @Autowired
    private DBReferenceManifestManager referenceManifestManager;
    private ReferenceManifest referenceManifest;


    /**
     * Prepares tests.
     *
     * @throws IOException if test resources are not found
     */
    @BeforeClass
    public void prepareTests() throws IOException {
        Path fPath;
        try {
            fPath = Paths.get(this.getClass().getResource(GOOD_RIM_FILE).toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Could not resolve path URI", e);
        }
        referenceManifest = new ReferenceManifest(Files.readAllBytes(fPath));
        referenceManifestManager.save(referenceManifest);
    }

    /**
     * Constructor.
     */
    public ReferenceManifestDetailsPageControllerTest() {
        super(Page.RIM_DETAILS);
    }

    /**
     * Tests initial page when the Reference Integrity Manifest
     * was not found.
     * @throws Exception if an exception occurs
     */
    @Test
    public void testInitPage() throws Exception {
        // Get error message
        getMockMvc()
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                        .param("id", ID))
                .andExpect(status().isOk())
                .andExpect(model().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("error", hasItem("Unable to find RIM with ID: " + ID))))
                .andReturn();
    }

    /**
     * Tests initial page for an Reference Integrity Manifest.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    @SuppressWarnings("unchecked")
    public void testInitPageRim() throws Exception {
        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName())
                .param("id", referenceManifest.getId().toString())
                .param("swidTagId", referenceManifest.getTagId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(PolicyPageController.INITIAL_DATA))
                .andReturn();

        // Obtain initialData HashMap
        Map<String, String> initialData = (Map<String, String>) result
                .getModelAndView()
                .getModel()
                .get(PolicyPageController.INITIAL_DATA);
        Assert.assertEquals(initialData.get("swidTagId"), referenceManifest.getTagId());
    }
}
