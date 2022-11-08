package hirs.attestationca.portal.page.controllers;

import hirs.appraiser.Appraiser;
import hirs.appraiser.SupplyChainAppraiser;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageControllerTest;
import hirs.data.persist.policy.SupplyChainPolicy;
import hirs.persist.AppraiserManager;
import hirs.persist.PolicyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static hirs.attestationca.portal.page.Page.POLICY;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of PolicyPageController.
 */
public class PolicyPageControllerTest extends PageControllerTest {

    @Autowired
    private PolicyManager policyManager;

    @Autowired
    private AppraiserManager appraiserManager;

    private SupplyChainPolicy policy;

    /**
     * Constructor requiring the Page's display and routing specification.
     *
     */
    public PolicyPageControllerTest() {
        super(POLICY);
    }

    /**
     * Constructor providing the Page's display and routing specification.
     */
    @BeforeClass
    public void setUpPolicy() {
        appraiserManager.saveAppraiser(new SupplyChainAppraiser());
        final Appraiser supplyChainAppraiser = appraiserManager.getAppraiser(
                SupplyChainAppraiser.NAME);

        policy = new SupplyChainPolicy("DEFAULT SCP", "a default policy");
        policyManager.savePolicy(policy);
        policyManager.setDefaultPolicy(supplyChainAppraiser, policy);

        policy = (SupplyChainPolicy) policyManager.getDefaultPolicy(
                supplyChainAppraiser);
    }

    /**
     * Verifies that spring is initialized properly by checking that an autowired bean
     * is populated.
     */
    @Test
    public void verifySpringInitialized() {
        Assert.assertNotNull(policyManager);
        Assert.assertNotNull(appraiserManager);
        Assert.assertNotNull(policy);
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
                .perform(MockMvcRequestBuilders.get("/" + getPage().getViewName()))
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
    public void testUpdateEcValEnable() throws Exception {

        final String baseURL = "/" + POLICY.getViewName();
        ResultActions actions;

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(baseURL + "/update-ec-validation")
                        .param("ecValidate", "checked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                    hasProperty("success",
                        hasItem("Endorsement credential validation enabled"))));

        policy = getDefaultPolicy();
        Assert.assertTrue(policy.isEcValidationEnabled());
    }

    /**
     * Verifies the rest call for disabling the EC Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdateEcValDisable() throws Exception {

        final String baseURL = "/" + POLICY.getViewName();
        ResultActions actions;

        //init the database
        policy = getDefaultPolicy();
        policy.setPcValidationEnabled(false);
        policy.setEcValidationEnabled(true);
        policy.setFirmwareValidationEnabled(false);
        policyManager.updatePolicy(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(baseURL + "/update-ec-validation")
                        .param("ecValidate", "unchecked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                    hasProperty("success",
                        hasItem("Endorsement credential validation disabled"))));

        policy = getDefaultPolicy();
        Assert.assertFalse(policy.isEcValidationEnabled());

        //reset database for invalid policy test
        policy.setEcValidationEnabled(true);
        policy.setPcValidationEnabled(true);
        policy.setFirmwareValidationEnabled(false);
        policyManager.updatePolicy(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(baseURL + "/update-ec-validation")
                        .param("ecValidate", "unchecked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                    hasProperty("error",
                        hasItem("To disable Endorsement Credential Validation, Platform Validation"
                            + " must also be disabled."))));

        policy = getDefaultPolicy();
        Assert.assertTrue(policy.isEcValidationEnabled());

    }

    /**
     * Verifies the rest call for enabling the PC Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdatePcValEnable() throws Exception {

        final String baseURL = "/" + POLICY.getViewName();
        ResultActions actions;

        //init the database
        policy = getDefaultPolicy();
        policy.setEcValidationEnabled(true);
        policy.setPcValidationEnabled(false);
        policy.setFirmwareValidationEnabled(false);
        policyManager.updatePolicy(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(baseURL + "/update-pc-validation")
                        .param("pcValidate", "checked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                    hasProperty("success",
                        hasItem("Platform certificate validation enabled"))));

        policy = getDefaultPolicy();
        Assert.assertTrue(policy.isPcValidationEnabled());

        //reset database for invalid policy test
        policy.setEcValidationEnabled(false);
        policy.setPcValidationEnabled(false);
        policy.setFirmwareValidationEnabled(false);
        policyManager.updatePolicy(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(baseURL + "/update-pc-validation")
                        .param("pcValidate", "checked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                    hasProperty("error",
                        hasItem("Unable to change Platform Validation setting,"
                            + "  invalid policy configuration."))));

        policy = getDefaultPolicy();
        Assert.assertFalse(policy.isPcValidationEnabled());

    }

    /**
     * Verifies the rest call for disabling the PC Validation policy setting.
     * @throws Exception if test fails
     */
    @Test
    public void testUpdatePcValDisable() throws Exception {

        final String baseURL = "/" + POLICY.getViewName();
        ResultActions actions;

        //init the database
        policy = getDefaultPolicy();
        policy.setPcValidationEnabled(true);
        policy.setPcAttributeValidationEnabled(false);
        policy.setFirmwareValidationEnabled(false);
        policyManager.updatePolicy(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(baseURL + "/update-pc-validation")
                        .param("pcValidate", "unchecked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                    hasProperty("success",
                        hasItem("Platform certificate validation disabled"))));

        policy = getDefaultPolicy();
        Assert.assertFalse(policy.isPcValidationEnabled());

        //reset database for invalid policy test
        policy.setPcAttributeValidationEnabled(true);
        policy.setPcValidationEnabled(true);
        policy.setFirmwareValidationEnabled(false);
        policyManager.updatePolicy(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(baseURL + "/update-pc-validation")
                        .param("pcValidate", "unchecked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                    hasProperty("error",
                        hasItem("Unable to change Platform Validation setting,"
                            + "  invalid policy configuration."))));

        policy = getDefaultPolicy();
        Assert.assertTrue(policy.isPcValidationEnabled());

    }

    /**
     * Verifies the rest call for enabling the PC attribute Validation policy setting.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testUpdatePcAttributeValEnable() throws Exception {

        final String baseURL = "/" + POLICY.getViewName();
        ResultActions actions;

        //init the database
        policy = getDefaultPolicy();
        policy.setPcAttributeValidationEnabled(false);
        policy.setPcValidationEnabled(true);
        policy.setFirmwareValidationEnabled(false);
        policyManager.updatePolicy(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(baseURL + "/update-pc-attribute-validation")
                        .param("pcAttributeValidate", "checked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("success",
                                hasItem("Platform certificate attribute validation enabled"))));

        policy = getDefaultPolicy();
        Assert.assertTrue(policy.isPcAttributeValidationEnabled());

        //reset database for invalid policy test
        policy.setPcAttributeValidationEnabled(false);
        policy.setPcValidationEnabled(false);
        policyManager.updatePolicy(policy);

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(baseURL + "/update-pc-attribute-validation")
                        .param("pcAttributeValidate", "checked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                    hasProperty("error",
                        hasItem("To enable Platform Attribute Validation,"
                            + " Platform Credential Validation must also be enabled."))));

        policy = getDefaultPolicy();
        Assert.assertFalse(policy.isPcAttributeValidationEnabled());

    }

    /**
     * Verifies the rest call for disabling the PC attribute validation policy setting.
     * @throws Exception if test fails
     */
    @Test
    public void testUpdatePcAttributeValDisable() throws Exception {

        final String baseURL = "/" + POLICY.getViewName();
        ResultActions actions;

        // perform the mock request
        actions = getMockMvc()
                .perform(MockMvcRequestBuilders.post(baseURL + "/update-pc-attribute-validation")
                        .param("pcAttributeValidate", "unchecked"));

        actions
                // check HTTP status
                .andExpect(status().is3xxRedirection())
                // check the messages forwarded to the redirected page
                .andExpect(flash().attribute(PageController.MESSAGES_ATTRIBUTE,
                        hasProperty("success",
                                hasItem("Platform certificate attribute validation disabled"))));

        policy = getDefaultPolicy();
        Assert.assertFalse(policy.isPcAttributeValidationEnabled());
    }

    /**
     * Helper function to get a fresh load of the default policy from the DB.
     *
     * @return The default Supply Chain Policy
     */
    private SupplyChainPolicy getDefaultPolicy() {
        final Appraiser supplyChainAppraiser = appraiserManager.getAppraiser(
                SupplyChainAppraiser.NAME);
        return (SupplyChainPolicy) policyManager.getDefaultPolicy(
                supplyChainAppraiser);
    }

}
