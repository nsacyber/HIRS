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

        boolean ec = policy.isEcValidationEnabled();
        boolean pc = policy.isPcValidationEnabled();
        boolean fm = policy.isFirmwareValidationEnabled();

        // perform test
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath))
                .andExpect(status().isOk())
                // Test that the two boolean policy values sent to the page match
                // the actual policy values.
                .andExpect(model().attribute(PolicyPageController.INITIAL_DATA,
                        hasProperty("enableEcValidation", is(ec))))
                .andExpect(model().attribute(PolicyPageController.INITIAL_DATA,
                        hasProperty("enablePcCertificateValidation", is(pc))))
                .andExpect(model().attribute(PolicyPageController.INITIAL_DATA,
                        hasProperty("enableFirmwareValidation", is(fm))));
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
                        .param("ecValidate", "checked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Endorsement credential validation enabled"))));

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
                        .param("ecValidate", "unchecked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Endorsement credential validation disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setEcValidationEnabled(true);
        policy.setPcValidationEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-ec-validation")
                        .param("ecValidate", "unchecked"));

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
                        .param("pcValidate", "checked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Platform certificate validation enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isPcValidationEnabled());

        //reset database for invalid policy test
        policy.setEcValidationEnabled(false);
        policy.setPcValidationEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pc-validation")
                        .param("pcValidate", "checked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Unable to change Platform Validation setting,"
                                        + "  invalid policy configuration."))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isPcValidationEnabled());

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
                        .param("pcValidate", "unchecked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Platform certificate validation disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isPcValidationEnabled());

        //reset database for invalid policy test
        policy.setPcValidationEnabled(true);
        policy.setPcAttributeValidationEnabled(true);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pc-validation")
                        .param("pcValidate", "unchecked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("errorMessages",
                                hasItem("Unable to change Platform Validation setting,"
                                        + "  invalid policy configuration."))));

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
                        .param("pcAttributeValidate", "checked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Platform certificate attribute validation enabled"))));

        policy = policyRepository.findByName("Default");
        assertTrue(policy.isPcAttributeValidationEnabled());

        //reset database for invalid policy test
        policy.setPcValidationEnabled(false);
        policy.setPcAttributeValidationEnabled(false);
        policyRepository.save(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(pagePath + "/update-pc-attribute-validation")
                        .param("pcAttributeValidate", "checked"));

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
                        .param("pcAttributeValidate", "unchecked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("successMessages",
                                hasItem("Platform certificate attribute validation disabled"))));

        policy = policyRepository.findByName("Default");
        assertFalse(policy.isPcAttributeValidationEnabled());
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
}
