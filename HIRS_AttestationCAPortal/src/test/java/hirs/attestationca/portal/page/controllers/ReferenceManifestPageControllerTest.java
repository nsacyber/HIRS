package hirs.attestationca.portal.page.controllers;

import hirs.data.persist.BaseReferenceManifest;
import hirs.data.persist.ReferenceManifest;
import hirs.persist.ReferenceManifestManager;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageControllerTest;
import hirs.attestationca.portal.page.PageMessages;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.servlet.FlashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration tests that test the URL End Points of
 * ReferenceManifestPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReferenceManifestPageControllerTest extends PageControllerTest {

    private static final String GOOD_RIM_FILE = "generated_good.swidtag";
    private static final String BAD_RIM_FILE = "generated_bad.swidtag";

    @Autowired
    private ReferenceManifestManager referenceManifestManager;
    private MockMultipartFile validRimFile;
    private MockMultipartFile nonValidRimFile;

    /**
     * Constructor.
     */
    public ReferenceManifestPageControllerTest() {
        super(Page.REFERENCE_MANIFESTS);
    }

    /**
     * Prepares tests.
     *
     * @throws IOException if test resources are not found
     */
    @BeforeMethod
    public void prepareTests() throws IOException {
        // create a multi part file for the controller upload
        validRimFile = new MockMultipartFile("file", GOOD_RIM_FILE, "",
                new ClassPathResource("rims/" + GOOD_RIM_FILE).getInputStream());
        nonValidRimFile = new MockMultipartFile("file", BAD_RIM_FILE, "",
                new ClassPathResource("rims/" + BAD_RIM_FILE).getInputStream());
    }

    private void archiveTestCert(final ReferenceManifest referenceManifest) throws Exception {
        // now, archive the record
        getMockMvc().perform(MockMvcRequestBuilders
                .post("/reference-manifests/delete")
                .param("id", referenceManifest.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        Set<ReferenceManifest> records
                = referenceManifestManager.get(BaseReferenceManifest
                        .select(referenceManifestManager).includeArchived());
        Assert.assertEquals(records.size(), 1);

        Assert.assertTrue(records.iterator().next().isArchived());
    }

    /**
     * Tests uploading a RIM that is a Reference Integrity Manifest, and
     * archiving it.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadAndArchiveValidRim() throws Exception {
        ReferenceManifest rim = uploadTestRim();
        archiveTestRim(rim);
    }

    /**
     * Tests uploading a rim that is not a valid Reference Integrity Manifest,
     * which results in failure.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadNonValidRim() throws Exception {
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .fileUpload("/reference-manifests/upload")
                .file(nonValidRimFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        Assert.assertEquals(pageMessages.getSuccess().size(), 0);
        Assert.assertEquals(pageMessages.getError().size(), 1);
    }

    /**
     * Tests that uploading a RIM when an identical RIM is archived will cause
     * the existing RIM to be unarchived and updated.
     *
     * @throws Exception if an exception occurs
     */
    @Test
    @Rollback
    public void uploadCausesUnarchive() throws Exception {
        ReferenceManifest rim = uploadTestRim();
        archiveTestCert(rim);

        // upload the same cert again
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .fileUpload("/reference-manifests/upload")
                .file(validRimFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        Assert.assertEquals(pageMessages.getSuccess().size(), 1);
        Assert.assertEquals(pageMessages.getError().size(), 0);
        Assert.assertEquals(pageMessages.getSuccess().get(0),
                "Pre-existing RIM found and unarchived (generated_good.swidtag): ");

        // verify the cert was actually stored
        Set<ReferenceManifest> records = referenceManifestManager.get(BaseReferenceManifest.select(
                referenceManifestManager));
        Assert.assertEquals(records.size(), 1);

        ReferenceManifest newRim = records.iterator().next();

        // verify that the rim was unarchived
        Assert.assertFalse(newRim.isArchived());
        // verify that the createTime was updated
        Assert.assertTrue(newRim.getCreateTime().getTime() > rim.getCreateTime().getTime());
    }

    private ReferenceManifest uploadTestRim() throws Exception {
        MvcResult result = getMockMvc().perform(MockMvcRequestBuilders
                .fileUpload("/reference-manifests/upload")
                .file(validRimFile))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // verify redirection messages
        FlashMap flashMap = result.getFlashMap();
        PageMessages pageMessages = (PageMessages) flashMap.get("messages");
        Assert.assertEquals(pageMessages.getSuccess().size(), 1);
        Assert.assertEquals(pageMessages.getError().size(), 0);

        // verify the cert was actually stored
        Set<ReferenceManifest> records
                = referenceManifestManager.get(BaseReferenceManifest
                .select(referenceManifestManager));
        Assert.assertEquals(records.size(), 1);

        ReferenceManifest rim = records.iterator().next();
        Assert.assertFalse(rim.isArchived());

        return rim;
    }

    private void archiveTestRim(final ReferenceManifest rim) throws Exception {
        // now, archive the record
        getMockMvc().perform(MockMvcRequestBuilders
                .post("/reference-manifests/delete")
                .param("id", rim.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        Set<ReferenceManifest> records
                = referenceManifestManager.get(BaseReferenceManifest
                        .select(referenceManifestManager).includeArchived());
        Assert.assertEquals(records.size(), 1);

        Assert.assertTrue(records.iterator().next().isArchived());
    }
}
