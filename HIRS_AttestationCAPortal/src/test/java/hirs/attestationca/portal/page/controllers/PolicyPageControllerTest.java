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

    // Policy refers to the settings such as whether to validate endorsement certificates, platform certificates
    // , etc
    private PolicySettings policySetting;

    /**
     * Constructor requiring the Page's display and routing specification.
     */
    public PolicyPageControllerTest() {
        super(POLICY);
        pagePath = getPagePath();
    }

    /**
     * Sets up policySetting.
     */
    @BeforeAll
    public void setUpPolicy() {

        // create the supply chain policy
        policySetting = policyRepository.findByName("Default");
    }

    /**
     * Verifies that spring is initialized properly by checking that an autowired bean
     * is populated.
     */
    @Test
    public void verifySpringInitialized() {
        assertNotNull(policyRepository);
        assertNotNull(policySetting);
    }

    /**
     * Checks that the page initializes correctly.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testInitPage() throws Exception {
        final boolean ec = policySetting.isEcValidationEnabled();
        final boolean pc = policySetting.isPcValidationEnabled();
        final boolean fm = policySetting.isFirmwareValidationEnabled();

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
    public void testUpdateEndorsementCertificateValidationPolicyEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        policyRepository.save(policySetting);

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
                                hasItem("Endorsement Certificate Validation enabled"))));

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isEcValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the EC Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateEndorsementCertificateValidationPolicyDisable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        policySetting.setEcValidationEnabled(true);
        policyRepository.save(policySetting);

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
                                hasItem("Endorsement Certificate Validation disabled"))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isEcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setEcValidationEnabled(true);
        policySetting.setPcValidationEnabled(true);
        policyRepository.save(policySetting);

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
                                hasItem("To disable Endorsement Certificate Validation, Platform Certificate Validation"
                                        + " must also be disabled."))));

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isEcValidationEnabled());

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
        policySetting.setEcValidationEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isPcValidationEnabled());
        assertTrue(policySetting.isEcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setEcValidationEnabled(false);
        policySetting.setPcValidationEnabled(false);
        policyRepository.save(policySetting);

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
                                hasItem("Unable to update ACA Platform Certificate Validation setting due to the"
                                        + " current policy configuration."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isPcValidationEnabled());
        assertFalse(policySetting.isEcValidationEnabled());
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
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isPcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setPcValidationEnabled(true);
        policySetting.setPcAttributeValidationEnabled(true);
        policyRepository.save(policySetting);

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
                                hasItem("Unable to update ACA Platform Certificate Validation setting due to the "
                                        + "current policy configuration."))));

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isPcValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the Platform Certificate Attribute Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdatePlatformCertificateAttributeValEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        setPolicyPcToTrue();
        policyRepository.save(policySetting);

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
                                hasItem("Platform Certificate Attribute Validation enabled"))));

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isPcAttributeValidationEnabled());
        assertTrue(policySetting.isPcValidationEnabled());
        assertTrue(policySetting.isEcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setPcValidationEnabled(false);
        policySetting.setPcAttributeValidationEnabled(false);
        policyRepository.save(policySetting);

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
                                hasItem("To enable Platform Certificate Attribute Validation,"
                                        + " Platform Certificate Validation must also be enabled."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isPcAttributeValidationEnabled());
        assertFalse(policySetting.isPcValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the Platform Certificate Attribute Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdatePlatformCertificateAttributeValDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyPlatformCertificateAttributeToTrue();
        policyRepository.save(policySetting);

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
                                hasItem("Platform Certificate Attribute Validation disabled"))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the Ignore Component Revision policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreComponentRevisionEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        setPolicyPlatformCertificateAttributeToTrue();
        policySetting.setIgnoreRevisionEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isIgnoreRevisionEnabled());
        assertTrue(policySetting.isPcAttributeValidationEnabled());
        assertTrue(policySetting.isPcValidationEnabled());
        assertTrue(policySetting.isEcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setPcAttributeValidationEnabled(false);
        policySetting.setIgnoreRevisionEnabled(false);
        policyRepository.save(policySetting);

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
                                hasItem("Ignore Component Revision Attribute cannot be enabled without Platform "
                                        + "Certificate Attribute Validation policy enabled."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnoreRevisionEnabled());
        assertFalse(policySetting.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the Component Revision policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreComponentRevisionDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyPlatformCertificateAttributeToTrue();
        policySetting.setIgnoreRevisionEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnoreRevisionEnabled());
        assertTrue(policySetting.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the Ignore pcie vpd policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnorePcieVpdEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        setPolicyPlatformCertificateAttributeToTrue();
        policySetting.setIgnorePcieVpdEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isIgnorePcieVpdEnabled());
        assertTrue(policySetting.isPcAttributeValidationEnabled());
        assertTrue(policySetting.isPcValidationEnabled());
        assertTrue(policySetting.isEcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setPcAttributeValidationEnabled(false);
        policySetting.setIgnorePcieVpdEnabled(false);
        policyRepository.save(policySetting);

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
                                hasItem("Ignore PCIE VPD Attribute Policy cannot be enabled without Platform "
                                        + "Certificate Attribute Validation policy enabled."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnorePcieVpdEnabled());
        assertFalse(policySetting.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the Ignore pcie vpd policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreIgnorePcieVpdDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyPlatformCertificateAttributeToTrue();
        policySetting.setIgnorePcieVpdEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnorePcieVpdEnabled());
        assertTrue(policySetting.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the Firmware Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateFirmwareValEnable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isFirmwareValidationEnabled());
        assertTrue(policySetting.isPcAttributeValidationEnabled());
        assertTrue(policySetting.isPcValidationEnabled());
        assertTrue(policySetting.isEcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setPcAttributeValidationEnabled(false);
        policySetting.setFirmwareValidationEnabled(false);
        policyRepository.save(policySetting);

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
                                hasItem("Firmware Validation cannot be enabled without Platform Certificate Attributes "
                                        + "policy enabled."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isFirmwareValidationEnabled());
        assertFalse(policySetting.isPcAttributeValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the Firmware Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateFirmwareValDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the Ignore ima policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreIMAEnable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policySetting.setIgnoreImaEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isIgnoreImaEnabled());
        assertTrue(policySetting.isFirmwareValidationEnabled());
        assertTrue(policySetting.isPcAttributeValidationEnabled());
        assertTrue(policySetting.isPcValidationEnabled());
        assertTrue(policySetting.isEcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setFirmwareValidationEnabled(false);
        policySetting.setIgnoreImaEnabled(false);
        policyRepository.save(policySetting);

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
                                hasItem("Ignore IMA cannot be enabled without Firmware Validation policy "
                                        + "enabled."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnoreImaEnabled());
        assertFalse(policySetting.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the Ignore ima policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreIMADisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policySetting.setIgnoreImaEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnoreImaEnabled());
        assertTrue(policySetting.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the Ignore tboot Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreTbootEnable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policySetting.setIgnoretBootEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isIgnoretBootEnabled());
        assertTrue(policySetting.isFirmwareValidationEnabled());
        assertTrue(policySetting.isPcAttributeValidationEnabled());
        assertTrue(policySetting.isPcValidationEnabled());
        assertTrue(policySetting.isEcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setFirmwareValidationEnabled(false);
        policySetting.setIgnoretBootEnabled(false);
        policyRepository.save(policySetting);

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
                                hasItem("Ignore TBoot cannot be enabled without Firmware Validation policy "
                                        + "enabled."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnoretBootEnabled());
        assertFalse(policySetting.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the Ignore tboot policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreTbootDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policySetting.setIgnoretBootEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnoretBootEnabled());
        assertTrue(policySetting.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the Ignore gpt policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreGptEnable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policySetting.setIgnoreGptEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isIgnoreGptEnabled());
        assertTrue(policySetting.isFirmwareValidationEnabled());
        assertTrue(policySetting.isPcAttributeValidationEnabled());
        assertTrue(policySetting.isPcValidationEnabled());
        assertTrue(policySetting.isEcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setFirmwareValidationEnabled(false);
        policySetting.setIgnoreGptEnabled(false);
        policyRepository.save(policySetting);

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
                                hasItem("Ignore GPT Events cannot be enabled without Firmware Validation "
                                        + "policy enabled."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnoreGptEnabled());
        assertFalse(policySetting.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the Ignore gpt policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreGPTDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policySetting.setIgnoreGptEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnoreGptEnabled());
        assertTrue(policySetting.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for enabling the Ignore os events policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreOsEvtsEnable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policySetting.setIgnoreOsEvtEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isIgnoreOsEvtEnabled());
        assertTrue(policySetting.isFirmwareValidationEnabled());
        assertTrue(policySetting.isPcAttributeValidationEnabled());
        assertTrue(policySetting.isPcValidationEnabled());
        assertTrue(policySetting.isEcValidationEnabled());

        //reset database for invalid policy test
        policySetting.setFirmwareValidationEnabled(false);
        policySetting.setIgnoreOsEvtEnabled(false);
        policyRepository.save(policySetting);

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
                                hasItem("Ignore OS Events cannot be enabled without Firmware Validation "
                                        + "policy enabled."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnoreOsEvtEnabled());
        assertFalse(policySetting.isFirmwareValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the Ignore os events policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateIgnoreOsEvtsDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setPolicyFirmwareToTrue();
        policySetting.setIgnoreOsEvtEnabled(true);
        policyRepository.save(policySetting);

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

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIgnoreOsEvtEnabled());
        assertTrue(policySetting.isFirmwareValidationEnabled());
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
        policySetting.setIssueAttestationCertificateEnabled(false);
        policyRepository.save(policySetting);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(
                                pagePath + "/update-issued-attestation-generation")
                        .param("issueAttestationCertificateEnabled", "true"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Attestation Certificate Generation enabled."))));

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isIssueAttestationCertificateEnabled());
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
        policySetting.setIssueAttestationCertificateEnabled(true);
        policyRepository.save(policySetting);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(
                                pagePath + "/update-issued-attestation-generation")
                        .param("issueAttestationCertificateEnabled", "false"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Attestation Certificate Generation disabled."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIssueAttestationCertificateEnabled());
    }

    /**
     * Verifies the rest call for enabling generate LDevId certificate policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateGenerateLDevIDCertificateEnable() throws Exception {
        ResultActions actions;

        //init the database
        setPolicyAllToFalse();
        policySetting.setIssueDevIdCertificateEnabled(false);
        policyRepository.save(policySetting);

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
                                hasItem("LDevID Certificate Generation enabled."))));

        policySetting = policyRepository.findByName("Default");
        assertTrue(policySetting.isIssueDevIdCertificateEnabled());
    }

    /**
     * Verifies the rest call for disabling the generate LDevID certificate policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateGenerateLDevIdCertificateDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        policySetting.setIssueDevIdCertificateEnabled(true);
        policyRepository.save(policySetting);

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
                                hasItem("LDevID Certificate Generation disabled."))));

        policySetting = policyRepository.findByName("Default");
        assertFalse(policySetting.isIssueDevIdCertificateEnabled());
    }

    /**
     * Verifies the rest call for enabling/disabling the saving protobuf to aca log policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateSaveProtobufToLogEnableDisable() throws Exception {
        ResultActions actions;
        setPolicyAllToFalse();
        setSaveProtobufLogSettingsToFalse();

        // set the policy setting to save protobuf data always
        policySetting.setSaveProtobufToLogAlwaysEnabled(true);
        policyRepository.save(policySetting);

        // perform the mock request to save protobuf data only on failed validations
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-save-protobuf-data-to-log")
                        .param("saveProtobufToLogOption", "log-protobuf-on-fail-val"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Save Protobuf Data To ACA Log only on failed validation"
                                        + " has been enabled"))));

        policySetting = policyRepository.findByName("Default");

        // verify that the other save protobuf data policy settings are disabled now
        assertFalse(policySetting.isSaveProtobufToLogAlwaysEnabled());
        assertFalse(policySetting.isSaveProtobufToLogNeverEnabled());

        // verify that the save protobuf data on failed validations is enabled now
        assertTrue(policySetting.isSaveProtobufToLogOnFailedValEnabled());

        // perform the mock request to save protobuf data always
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-save-protobuf-data-to-log")
                        .param("saveProtobufToLogOption", "always-log-protobuf"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Save Protobuf Data To ACA Log always has been enabled"))));

        policySetting = policyRepository.findByName("Default");

        // verify that the other save protobuf data policy settings are disabled now
        assertFalse(policySetting.isSaveProtobufToLogOnFailedValEnabled());
        assertFalse(policySetting.isSaveProtobufToLogNeverEnabled());

        // verify that the save protobuf data always is enabled now
        assertTrue(policySetting.isSaveProtobufToLogAlwaysEnabled());

        // perform the mock request to save protobuf data never
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-save-protobuf-data-to-log")
                        .param("saveProtobufToLogOption", "never-log-protobuf"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Save Protobuf Data To ACA Log never has been enabled"))));

        policySetting = policyRepository.findByName("Default");

        // verify that the other save protobuf data policy settings are disabled now
        assertFalse(policySetting.isSaveProtobufToLogOnFailedValEnabled());
        assertFalse(policySetting.isSaveProtobufToLogAlwaysEnabled());

        // verify that the save protobuf data never is enabled now
        assertTrue(policySetting.isSaveProtobufToLogNeverEnabled());
    }

    /**
     * Helper function to set policy member variable back to all false.
     */
    private void setPolicyAllToFalse() {
        policySetting.setEcValidationEnabled(false);
        policySetting.setPcValidationEnabled(false);
        policySetting.setPcAttributeValidationEnabled(false);
        policySetting.setFirmwareValidationEnabled(false);
    }

    /**
     * Helper function to set policy member variable - PC Validation to True.
     * Note: to set PC Validation to true, EC Validation must also be set to true.
     */
    private void setPolicyPcToTrue() {
        policySetting.setEcValidationEnabled(true);
        policySetting.setPcValidationEnabled(true);
    }

    /**
     * Helper function to set policy member variable - Platform Certificate Attribute Validation to True.
     * Note: to set Platform Certificate Attribute Validation to true, PC Validation must also be set to true.
     */
    private void setPolicyPlatformCertificateAttributeToTrue() {
        setPolicyPcToTrue();
        policySetting.setPcAttributeValidationEnabled(true);
    }

    /**
     * Helper function to set policy member variable - Firmware Validation to True.
     * Note: to set Firmware Validation to true, Platform Certificate Attribute Validation must also be set to true.
     */
    private void setPolicyFirmwareToTrue() {
        setPolicyPlatformCertificateAttributeToTrue();
        policySetting.setFirmwareValidationEnabled(true);
    }

    /**
     * Helper function to set policy member variables - Save Protobuf Data To Log to False.
     */
    private void setSaveProtobufLogSettingsToFalse() {
        policySetting.setSaveProtobufToLogOnFailedValEnabled(false);
        policySetting.setSaveProtobufToLogNeverEnabled(false);
        policySetting.setSaveProtobufToLogAlwaysEnabled(false);
    }
}
