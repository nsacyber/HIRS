package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.model.PolicyPageModel;
import static hirs.attestationca.portal.page.Page.POLICY;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import static org.apache.logging.log4j.LogManager.getLogger;
import org.apache.logging.log4j.Logger;
import hirs.appraiser.Appraiser;
import hirs.appraiser.SupplyChainAppraiser;
import hirs.data.persist.SupplyChainPolicy;
import hirs.persist.AppraiserManager;
import hirs.persist.PolicyManager;
import hirs.persist.PolicyManagerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the Policy page.
 */
@Controller
@RequestMapping("/policy")
public class PolicyPageController extends PageController<NoPageParams> {

    private static final Logger LOGGER = getLogger(PolicyPageController.class);

    /**
     * Represents a web request indicating to enable a setting (based on radio buttons from a
     * web form).
     */
    private static final String ENABLED_PARAMETER_VALUE = "checked";


    private PolicyManager policyManager;

    private AppraiserManager appraiserManager;

    /**
     * Model attribute name used by initPage for the initial data passed to the page.
     */
    public static final String INITIAL_DATA = "initialData";

    /**
     * Flash attribute name used by initPage and post for the data forwarded during the redirect
     * from the POST operation back to the page.
     */
    public static final String RESULT_DATA = "resultData";

    /**
     * Constructor.
     * @param policyManager the policy manager
     * @param appraiserManager the appraiser manager
     */
    @Autowired
    public PolicyPageController(final PolicyManager policyManager,
                                final AppraiserManager appraiserManager) {
        super(POLICY);

        this.policyManager = policyManager;
        this.appraiserManager = appraiserManager;
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {

        // get the basic information to render the page
        ModelAndView mav = getBaseModelAndView();

        SupplyChainPolicy policy = getDefaultPolicy();

        PolicyPageModel pageModel = new PolicyPageModel(policy);
        mav.addObject(INITIAL_DATA, pageModel);

        LOGGER.debug(pageModel);

        return mav;
    }

    /**
     * Updates the Platform Cert Validation policy setting and redirects back
     * to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr RedirectAttributes used to forward data back to the original page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-pc-validation", method = RequestMethod.POST)
    public RedirectView updatePcVal(@ModelAttribute final PolicyPageModel ppModel,
            final RedirectAttributes attr) throws URISyntaxException {


        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean pcValidationOptionEnabled =
                ppModel.getPcValidate().equalsIgnoreCase(ENABLED_PARAMETER_VALUE);

        try {
            SupplyChainPolicy policy = getDefaultPolicyAndSetInModel(ppModel, model);

            // If PC policy setting change results in invalid policy, inform user
            if (!isPolicyValid(policy.isEcValidationEnabled(), pcValidationOptionEnabled,
                    policy.isPcAttributeValidationEnabled())) {
                handleUserError(model, messages,
                    "Unable to change Platform Validation setting,"
                            + "  invalid policy configuration.");
                return redirectToSelf(new NoPageParams(), model, attr);
            }
            // set the policy option and create display message
            if (pcValidationOptionEnabled) {
                policy.setPcValidationEnabled(true);
                successMessage = "Platform certificate validation enabled";
            } else {
                policy.setPcValidationEnabled(false);
                successMessage = "Platform certificate validation disabled";
            }
            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);


        } catch (PolicyManagerException e) {
            // Log and return any error messages to the user
            handlePolicyManagerUpdateError(model, messages, e,
                    "Error changing ACA platform validation Policy",
                    "Error updating policy. \n" + e.getMessage());
        }
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the Platform Cert Attribute Validation policy setting and redirects back
     * to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr RedirectAttributes used to forward data back to the original page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-pc-attribute-validation", method = RequestMethod.POST)
    public RedirectView updatePcAttributeVal(@ModelAttribute final PolicyPageModel ppModel,
                                             final RedirectAttributes attr)
    throws URISyntaxException {


        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean pcAttributeValidationOptionEnabled = ppModel.getPcAttributeValidate()
                .equalsIgnoreCase(ENABLED_PARAMETER_VALUE);

        try {
            SupplyChainPolicy policy = getDefaultPolicyAndSetInModel(ppModel, model);


            // If PC Attribute Validation is enabled without PC Validation, disallow change
            if (!isPolicyValid(policy.isEcValidationEnabled(),
                    policy.isPcValidationEnabled(), pcAttributeValidationOptionEnabled)) {

                handleUserError(model, messages,
                    "To enable Platform Attribute Validation, Platform Credential Validation"
                            + " must also be enabled.");
                return redirectToSelf(new NoPageParams(), model, attr);
            }
            // set the policy option and create display message
            if (pcAttributeValidationOptionEnabled) {
                policy.setPcAttributeValidationEnabled(true);
                successMessage = "Platform certificate attribute validation enabled";
            } else {
                policy.setPcAttributeValidationEnabled(false);
                successMessage = "Platform certificate attribute validation disabled";
            }
            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);


        } catch (PolicyManagerException e) {
            // Log and return any error messages to the user
            handlePolicyManagerUpdateError(model, messages, e,
                    "Error changing ACA platform certificate attribute validation policy",
                    "Error updating policy. \n" + e.getMessage());
        }
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the Endorsement Credential Validation policy setting and redirects back
     * to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr RedirectAttributes used to forward data back to the original page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-ec-validation", method = RequestMethod.POST)
    public RedirectView updateEcVal(@ModelAttribute final PolicyPageModel ppModel,
            final RedirectAttributes attr) throws URISyntaxException {

        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean ecValidationOptionEnabled =
                ppModel.getEcValidate().equalsIgnoreCase(ENABLED_PARAMETER_VALUE);

        try {
            SupplyChainPolicy policy = getDefaultPolicyAndSetInModel(ppModel, model);

            //If PC Validation is enabled without EC Validation, disallow change
            if (!isPolicyValid(ecValidationOptionEnabled, policy.isPcValidationEnabled(),
                    policy.isPcAttributeValidationEnabled())) {
                handleUserError(model, messages,
                    "To disable Endorsement Credential Validation, Platform Validation"
                            + " must also be disabled.");
                return redirectToSelf(new NoPageParams(), model, attr);
            }
            // set the policy option and create success message
            if (ecValidationOptionEnabled) {
                policy.setEcValidationEnabled(true);
                successMessage = "Endorsement credential validation enabled";
            } else {
                policy.setEcValidationEnabled(false);
                successMessage = "Endorsement credential validation disabled";
            }

            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);

        } catch (PolicyManagerException e) {
            handlePolicyManagerUpdateError(model, messages, e,
                    "Error changing ACA endorsement validation policy",
                    "Error updating policy. \n" + e.getMessage());

        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the Endorsement Credential Validation policy setting and redirects back
     * to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr RedirectAttributes used to forward data back to the original page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-firmware-validation", method = RequestMethod.POST)
    public RedirectView updateFirmwareVal(@ModelAttribute final PolicyPageModel ppModel,
            final RedirectAttributes attr) throws URISyntaxException {

        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean firmwareValidationOptionEnabled = ppModel.getFmValidate()
                .equalsIgnoreCase(ENABLED_PARAMETER_VALUE);

        try {
            SupplyChainPolicy policy = getDefaultPolicyAndSetInModel(ppModel, model);

            //If firmware is enabled without PC attributes, disallow change
            if (firmwareValidationOptionEnabled && !policy.isPcAttributeValidationEnabled()) {
                handleUserError(model, messages,
                    "Firmware validation can not be "
                            + "enabled without PC Attributes policy enabled.");
                return redirectToSelf(new NoPageParams(), model, attr);
            }

            // set the policy option and create success message
            if (firmwareValidationOptionEnabled) {
                policy.setFirmwareValidationEnabled(true);
                successMessage = "Firmware validation enabled";
            } else {
                policy.setFirmwareValidationEnabled(false);
                successMessage = "Firmware validation disabled";
            }

            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException e) {
            handlePolicyManagerUpdateError(model, messages, e,
                    "Error changing ACA endorsement validation policy",
                    "Error updating policy. \n" + e.getMessage());

        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    private void handlePolicyManagerUpdateError(final Map<String, Object> model,
                                                final PageMessages messages,
                                                final PolicyManagerException e,
                                                final String message, final String error) {
        LOGGER.error(message, e);
        messages.addError(error);
        model.put(MESSAGES_ATTRIBUTE, messages);
    }

    private void handleUserError(final Map<String, Object> model,
                                                final PageMessages messages,
                                                final String errorMessage) {
        messages.addError(errorMessage);
        model.put(MESSAGES_ATTRIBUTE, messages);
    }

    /**
     * Takes in policy setting states and determines if policy configuration is valid or not.
     * PC Attribute Validation must have PC Validation Enabled
     * PC Validation must have EC Validation enabled
     *
     * @param isEcEnable EC Validation Policy State
     * @param isPcEnable PC Validation Policy State
     * @param isPcAttEnable PC Attribute Validation Policy State
     * @return True if policy combination is valid
     */
    private static boolean isPolicyValid(final boolean isEcEnable, final boolean isPcEnable,
            final boolean isPcAttEnable) {

        if (isPcAttEnable && !isPcEnable) {
            return false;
        } else {
            return !isPcEnable || isEcEnable;
        }
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


    /**
     * Gets the default policy and applies the current values in to the page model.
     * @param ppModel the page model
     * @param model the map of string messages to be displayed on the view
     * @return The default Supply Chain Policy
     */
    private SupplyChainPolicy getDefaultPolicyAndSetInModel(
            final PolicyPageModel ppModel, final Map<String, Object> model) {
        // load the current default policy from the DB
        SupplyChainPolicy policy = getDefaultPolicy();

        // set the data received to be populated back into the form
        model.put(RESULT_DATA, ppModel);
        return policy;
    }

    private void savePolicyAndApplySuccessMessage(final PolicyPageModel ppModel,
                                                  final Map<String, Object> model,
                                                  final PageMessages messages,
                                                  final String successMessage,
                                                  final SupplyChainPolicy policy) {
        // save the policy to the DB
        policyManager.updatePolicy(policy);

        // Log and set the success message
        messages.addSuccess(successMessage);
        LOGGER.debug("ACA Policy set to: " + ppModel.toString());

        model.put(MESSAGES_ATTRIBUTE, messages);
    }

}
