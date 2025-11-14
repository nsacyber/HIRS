package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageControllerTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static hirs.attestationca.portal.page.Page.POLICY;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of PolicyPageController.
 */
public class PolicyPageControllerTest extends PageControllerTest {

    // Base path for the page
    private final String pagePath;

    // Repository manager to handle data access between policy entity and data storage in db
    @Autowired
    private PolicyRepository policyRepository;

    // Policy refers to the settings such as whether to validate endorsement credentials, platform credentials
    // , etc
    private PolicySettings policy;

    /**
     * Constructor requiring the Page's display and routing specification.
     */
    public PolicyPageControllerTest() {
        super(POLICY);
        pagePath = getPagePath();
    }

    /**
     * Sets up policy.
     */
    @BeforeAll
    public void setUpPolicy() {

        // create the supply chain policy
        policy = policyRepository.findByName("Default");
    }

    /**
     * Verifies that spring is initialized properly by checking that an autowired bean
     * is populated.
     */
    @Test
    public void verifySpringInitialized() {
        assertNotNull(policyRepository);
        assertNotNull(policy);
    }

    /**
     * Checks that the page initializes correctly.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testInitPage() throws Exception {
        final boolean ec = policy.isEcValidationEnabled();
        final boolean pc = policy.isPcValidationEnabled();
        final boolean fm = policy.isFirmwareValidationEnabled();

        // perform test
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath))
                .andExpect(status().isOk())
                // Test that the two boolean policy values sent to the page match
                // the actual policy values.
                .andExpect(model().attribute(PolicyPageController.INITIAL_DATA,
                        hasProperty("ecValidationEnabled", is(ec))))
                .andExpect(model().attribute(PolicyPageController.INITIAL_DATA,
                        hasProperty("pcValidationEnabled", is(pc))))
                .andExpect(model().attribute(PolicyPageController.INITIAL_DATA,
                        hasProperty("firmwareValidationEnabled", is(fm))));
    }

    /**
     * Verifies the rest call for enabling the EC Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateEndorsementCredentialValidationPolicyEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-ec-validation")
                        .param("ecValidationEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Endorsement Credential Validation enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isEcValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the EC Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateEndorsementCredentialValidationPolicyDisable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        policy.setEcValidationEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-ec-validation")
                        .param("ecValidationEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Endorsement Credential Validation disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setEcValidationEnabled(true);
        policy.setPcValidationEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-ec-validation")
                        .param("ecValidationEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("To disable Endorsement Credential Validation, Platform Validation"
                                        + " must also be disabled."))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isEcValidationEnabled());

    }

    /**
     * Verifies the rest call for enabling the PC Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdatePcValEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        policy.setEcValidationEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pc-validation")
                        .param("pcValidationEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Platform Certificate Validation enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isPcValidationEnabled());
        assertTrue(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setEcValidationEnabled(false);
        policy.setPcValidationEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pc-validation")
                        .param("pcValidationEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Unable to update ACA Platform Validation setting due to the current "
                                        + "policy configuration."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isPcValidationEnabled());
        assertFalse(policy.isEcValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the PC Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdatePcValDisable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        setPolicyPcToTrue();
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pc-validation")
                        .param("pcValidationEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Platform Certificate Validation disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isPcValidationEnabled());

        //reset database for invalid policy test
        policy.setPcValidationEnabled(true);
        policy.setPcAttributeValidationEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pc-validation")
                        .param("pcValidationEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Unable to update ACA Platform Validation setting due to the current"
                                        + " policy configuration."))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isPcValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the PC attribute Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdatePcAttributeValEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        setPolicyPcToTrue();
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pc-attribute-validation")
                        .param("pcAttributeValidationEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Platform Certificate Attribute validation enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isPcAttributeValidationEnabled());
        assertTrue(policy.isPcValidationEnabled());
        assertTrue(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setPcValidationEnabled(false);
        policy.setPcAttributeValidationEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pc-attribute-validation")
                        .param("pcAttributeValidationEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("To enable Platform Attribute Validation,"
                                        + " Platform Credential Validation must also be enabled."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isPcAttributeValidationEnabled());
        assertFalse(policy.isPcValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the PC attribute validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdatePcAttributeValDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyPcAttributeToTrue();
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pc-attribute-validation")
                        .param("pcValidationEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Platform Certificate Attribute validation disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the ignore component revision policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreComponentRevisionEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        setPolicyPcAttributeToTrue();
        policy.setIgnoreRevisionEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-revision-ignore")
                        .param("ignoreRevisionAttributeEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore Component Revision enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isIgnoreRevisionEnabled());
        assertTrue(policy.isPcAttributeValidationEnabled());
        assertTrue(policy.isPcValidationEnabled());
        assertTrue(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setPcAttributeValidationEnabled(false);
        policy.setIgnoreRevisionEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-revision-ignore")
                        .param("ignoreRevisionAttributeEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Ignore Component Revision Attribute cannot be " +
                                        "enabled without PC Attribute validation policy enabled."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnoreRevisionEnabled());
        assertFalse(policy.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the component revision policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreComponentRevisionDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyPcAttributeToTrue();
        policy.setIgnoreRevisionEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-revision-ignore")
                        .param("ignoreRevisionAttributeEnabled", "false"));
        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore Component Revision disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnoreRevisionEnabled());
        assertTrue(policy.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the ignore pcie vpd policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnorePcieVpdEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        setPolicyPcAttributeToTrue();
        policy.setIgnorePcieVpdEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pcie-vpd-ignore")
                        .param("ignorePcieVpdAttributeEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore PCIE VPD Attribute enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isIgnorePcieVpdEnabled());
        assertTrue(policy.isPcAttributeValidationEnabled());
        assertTrue(policy.isPcValidationEnabled());
        assertTrue(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setPcAttributeValidationEnabled(false);
        policy.setIgnorePcieVpdEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pcie-vpd-ignore")
                        .param("ignorePcieVpdAttributeEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Ignore PCIE VPD Attribute Policy cannot be enabled without PC Attribute"
                                        + " validation policy enabled."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnorePcieVpdEnabled());
        assertFalse(policy.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the ignore pcie vpd policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreIgnorePcieVpdDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyPcAttributeToTrue();
        policy.setIgnorePcieVpdEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pcie-vpd-ignore")
                        .param("ignorePcieVpdAttributeEnabled", "false"));
        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore PCIE VPD Attribute disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnorePcieVpdEnabled());
        assertTrue(policy.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the firmware validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateFirmwareValEnable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-firmware-validation")
                        .param("firmwareValidationEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Firmware Validation enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isFirmwareValidationEnabled());
        assertTrue(policy.isPcAttributeValidationEnabled());
        assertTrue(policy.isPcValidationEnabled());
        assertTrue(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setPcAttributeValidationEnabled(false);
        policy.setFirmwareValidationEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-firmware-validation")
                        .param("firmwareValidationEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Firmware validation cannot be enabled without PC Attributes policy enabled."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isFirmwareValidationEnabled());
        assertFalse(policy.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the firmware validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateFirmwareValDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-firmware-validation")
                        .param("firmwareValidationEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Firmware Validation disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the ignore ima policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreIMAEnable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policy.setIgnoreImaEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-ima-ignore")
                        .param("ignoreImaEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore IMA enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isIgnoreImaEnabled());
        assertTrue(policy.isFirmwareValidationEnabled());
        assertTrue(policy.isPcAttributeValidationEnabled());
        assertTrue(policy.isPcValidationEnabled());
        assertTrue(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setFirmwareValidationEnabled(false);
        policy.setIgnoreImaEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-ima-ignore")
                        .param("ignoreImaEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Ignore IMA cannot be enabled without Firmware Validation policy enabled."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnoreImaEnabled());
        assertFalse(policy.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the ignore ima policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreIMADisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policy.setIgnoreImaEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-ima-ignore")
                        .param("ignoreImaEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore IMA disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnoreImaEnabled());
        assertTrue(policy.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the ignore tboot validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreTbootEnable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policy.setIgnoretBootEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-tboot-ignore")
                        .param("ignoreTbootEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore TBoot enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isIgnoretBootEnabled());
        assertTrue(policy.isFirmwareValidationEnabled());
        assertTrue(policy.isPcAttributeValidationEnabled());
        assertTrue(policy.isPcValidationEnabled());
        assertTrue(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setFirmwareValidationEnabled(false);
        policy.setIgnoretBootEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-tboot-ignore")
                        .param("ignoreTbootEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Ignore TBoot cannot be enabled without Firmware Validation policy enabled."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnoretBootEnabled());
        assertFalse(policy.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the ignore tboot policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreTbootDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policy.setIgnoretBootEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-tboot-ignore")
                        .param("ignoreTbootEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore TBoot disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnoretBootEnabled());
        assertTrue(policy.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the ignore gpt policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreGptEnable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policy.setIgnoreGptEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-gpt-ignore")
                        .param("ignoreGptEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore GPT enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isIgnoreGptEnabled());
        assertTrue(policy.isFirmwareValidationEnabled());
        assertTrue(policy.isPcAttributeValidationEnabled());
        assertTrue(policy.isPcValidationEnabled());
        assertTrue(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setFirmwareValidationEnabled(false);
        policy.setIgnoreGptEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-gpt-ignore")
                        .param("ignoreGptEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Ignore GPT Events cannot be enabled without Firmware Validation policy enabled."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnoreGptEnabled());
        assertFalse(policy.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the ignore gpt policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreGPTDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policy.setIgnoreGptEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-gpt-ignore")
                        .param("ignoreTbootEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore GPT disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnoreGptEnabled());
        assertTrue(policy.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the ignore os events policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreOsEvtsEnable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policy.setIgnoreOsEvtEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-os-events-ignore")
                        .param("ignoreOsEvtEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore OS Events enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isIgnoreOsEvtEnabled());
        assertTrue(policy.isFirmwareValidationEnabled());
        assertTrue(policy.isPcAttributeValidationEnabled());
        assertTrue(policy.isPcValidationEnabled());
        assertTrue(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setFirmwareValidationEnabled(false);
        policy.setIgnoreOsEvtEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-os-events-ignore")
                        .param("ignoreOsEvtEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Ignore OS Events cannot be enabled without Firmware Validation policy enabled."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnoreOsEvtEnabled());
        assertFalse(policy.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the ignore os events policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreOsEvtsDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policy.setIgnoreOsEvtEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-os-events-ignore")
                        .param("ignoreOsEvtEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Ignore OS Events disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIgnoreOsEvtEnabled());
        assertTrue(policy.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling generate attestation certificate policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateGenerateAttestationCertificateEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        policy.setIssueAttestationCertificateEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-issued-attestation-generation")
                        .param("issueAttestationCertificateEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Attestation Certificate Generation enabled."))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isIssueAttestationCertificateEnabled());
    }

    /**
     * Verifies the rest call for disabling the generate attestation certificate policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateGenerateAttestationCertificateDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        policy.setIssueAttestationCertificateEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-issued-attestation-generation")
                        .param("issueAttestationCertificateEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Attestation Certificate Generation disabled."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIssueAttestationCertificateEnabled());
    }

    /**
     * Verifies the rest call for enabling generate attestation certificate policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateGenerateLDevIdCertificateEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        policy.setIssueDevIdCertificateEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-issued-ldevid-generation")
                        .param("issueDevIdCertificateEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("LDevId Certificate Generation enabled."))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isIssueDevIdCertificateEnabled());
    }

    /**
     * Verifies the rest call for disabling the generate ldevid certificate policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateGenerateLDevIdCertificateDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        policy.setIssueDevIdCertificateEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-issued-ldevid-generation")
                        .param("issueDevIdCertificateEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("LDevId Certificate Generation disabled."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isIssueDevIdCertificateEnabled());
    }

    /**
     * Helper function to set policy member variable back to all false.
     * After this function, can set specific values to true and then need to save policy.
     */
    private void setPolicyAllToFalse() {
        policy.setEcValidationEnabled(false);
        policy.setPcValidationEnabled(false);
        policy.setPcAttributeValidationEnabled(false);
        policy.setFirmwareValidationEnabled(false);
    }

    /**
     * Helper function to set policy member variable - PC Validation to True
     * Note: to set PC Validation to true, EC Validation must also be true.
     */
    private void setPolicyPcToTrue() {
        policy.setEcValidationEnabled(true);
        policy.setPcValidationEnabled(true);
    }

    /**
     * Helper function to set policy member variable - PC Attribute Validation to True
     * Note: to set PC Attribute Validation to true, PC Validation must also be true.
     */
    private void setPolicyPcAttributeToTrue() {
        setPolicyPcToTrue();
        policy.setPcAttributeValidationEnabled(true);
    }

    /**
     * Helper function to set policy member variable - PC Attribute Validation to True
     * Note: to set PC Attribute Validation to true, PC Validation must also be true.
     */
    private void setPolicyFirmwareToTrue() {
        setPolicyPcAttributeToTrue();
        policy.setFirmwareValidationEnabled(true);
    }
}
