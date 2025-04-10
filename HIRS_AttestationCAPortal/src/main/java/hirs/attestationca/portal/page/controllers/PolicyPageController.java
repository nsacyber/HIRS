package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.PolicyPageModel;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.utils.exception.PolicyManagerException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the Policy page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/policy")
public class PolicyPageController extends PageController<NoPageParams> {

    /**
     * Model attribute name used by initPage for the initial data passed to the
     * page.
     */
    public static final String INITIAL_DATA = "initialData";
    /**
     * Flash attribute name used by initPage and post for the data forwarded
     * during the redirect from the POST operation back to the page.
     */
    public static final String RESULT_DATA = "resultData";
    /**
     * Represents a web request indicating to enable a setting (based on radio
     * buttons from a web form).
     */
    private static final String ENABLED_CHECKED_PARAMETER_VALUE = "checked";
    private static final String ENABLED_EXPIRES_PARAMETER_VALUE = "expires";
    private final PolicyRepository policyRepository;

    /**
     * Constructor.
     *
     * @param policyRepository the policy service
     */
    @Autowired
    public PolicyPageController(final PolicyRepository policyRepository) {
        super(Page.POLICY);
        this.policyRepository = policyRepository;

        if (this.policyRepository.findByName("Default") == null) {
            this.policyRepository.saveAndFlush(new PolicySettings("Default",
                    "Settings are configured for no validation flags set."));
        }
    }

    /**
     * Takes in policy setting states and determines if policy configuration is
     * valid or not. PC Attribute Validation must have PC Validation Enabled PC
     * Validation must have EC Validation enabled
     *
     * @param isEcEnable    EC Validation Policy State
     * @param isPcEnable    PC Validation Policy State
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
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        // get the basic information to render the page
        ModelAndView mav = getBaseModelAndView();

        PolicySettings policy = getDefaultPolicy();
        log.debug(policy);
        PolicyPageModel pageModel = new PolicyPageModel(policy);
        mav.addObject(INITIAL_DATA, pageModel);

        log.debug(pageModel);

        return mav;
    }

    /**
     * Updates the Platform Cert Validation policy setting and redirects back to
     * the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original
     *                page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-pc-validation", method = RequestMethod.POST)
    public RedirectView updatePcVal(@ModelAttribute final PolicyPageModel ppModel,
                                    final RedirectAttributes attr) throws URISyntaxException {

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean pcValidationOptionEnabled
                = ppModel.getPcValidate().equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

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
                policy.setPcAttributeValidationEnabled(false);
                successMessage = "Platform certificate validation disabled";
            }
            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);

        } catch (PolicyManagerException pmEx) {
            // Log and return any error messages to the user
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA platform validation Policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the Platform Cert Attribute Validation policy setting and
     * redirects back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original
     *                page.
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
                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

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
        } catch (PolicyManagerException pmEx) {
            // Log and return any error messages to the user
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA platform certificate attribute validation policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the ignore component revision attribute setting and
     * redirects back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original
     *                page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-revision-ignore", method = RequestMethod.POST)
    public RedirectView updateIgnoreRevisionAttribute(@ModelAttribute final PolicyPageModel ppModel,
                                                      final RedirectAttributes attr)
            throws URISyntaxException {
        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean ignoreRevisionAttributeOptionEnabled = ppModel.getIgnoreRevisionAttribute()
                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

            //If Ignore Revision is enabled without PC Attributes, disallow change
            if (ignoreRevisionAttributeOptionEnabled && !policy.isPcAttributeValidationEnabled()) {
                handleUserError(model, messages,
                        "Ignore Component Revision Attribute can not be "
                                + "enabled without PC Attribute validation policy enabled.");
                return redirectToSelf(new NoPageParams(), model, attr);
            }

            // set the policy option and create success message
            if (ignoreRevisionAttributeOptionEnabled) {
                policy.setIgnoreRevisionEnabled(true);
                successMessage = "Ignore Component Revision enabled";
            } else {
                policy.setIgnoreRevisionEnabled(false);
                successMessage = "Ignore Component Revision disabled";
            }

            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA Component Revision Attribute policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the Attestation Certificate generation policy setting and redirects
     * back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-issue-attestation", method = RequestMethod.POST)
    public RedirectView updateAttestationVal(@ModelAttribute final PolicyPageModel ppModel,
                                             final RedirectAttributes attr)
            throws URISyntaxException {

        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean issuedAttestationOptionEnabled
                = ppModel.getAttestationCertificateIssued()
                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

            if (issuedAttestationOptionEnabled) {
                successMessage = "Attestation Certificate generation enabled.";
            } else {
                successMessage = "Attestation Certificate generation disabled.";
                policy.setGenerateOnExpiration(false);
            }

            policy.setIssueAttestationCertificate(issuedAttestationOptionEnabled);
            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA Attestation Certificate generation policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the DevID Certificate generation policy setting and redirects
     * back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-issue-devid", method = RequestMethod.POST)
    public RedirectView updateDevIdVal(@ModelAttribute final PolicyPageModel ppModel,
                                       final RedirectAttributes attr)
            throws URISyntaxException {

        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean issuedDevIdOptionEnabled
                = ppModel.getDevIdCertificateIssued()
                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

            if (issuedDevIdOptionEnabled) {
                successMessage = "DevID Certificate generation enabled.";
            } else {
                successMessage = "DevID Certificate generation disabled.";
                policy.setDevIdExpirationFlag(false);
            }

            policy.setIssueDevIdCertificate(issuedDevIdOptionEnabled);
            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA DevID Certificate generation policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the state of the policy setting that indicates that the generation
     * will occur in a set time frame and redirects
     * back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-expire-on", method = RequestMethod.POST)
    public RedirectView updateExpireOnVal(@ModelAttribute final PolicyPageModel ppModel,
                                          final RedirectAttributes attr)
            throws URISyntaxException {

        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        String numOfDays;

        boolean generateCertificateEnabled = false;
        // because this is just one option, there is not 'unchecked' value, so it is either
        // 'checked' or null
        if (ppModel.getGenerationExpirationOn() != null) {
            generateCertificateEnabled
                    = ppModel.getGenerationExpirationOn()
                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
        }

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);
            boolean issuedAttestationOptionEnabled
                    = policy.isIssueAttestationCertificate();

            if (issuedAttestationOptionEnabled) {
                if (generateCertificateEnabled) {
                    successMessage = "Attestation Certificate generation expiration time enabled.";
                } else {
                    successMessage = "Attestation Certificate generation expiration time disabled.";
                }

                if (generateCertificateEnabled) {
                    numOfDays = ppModel.getExpirationValue();
                    if (numOfDays == null) {
                        numOfDays = PolicySettings.TEN_YEARS;
                    }
                } else {
                    numOfDays = policy.getValidityDays();
                }

                policy.setValidityDays(numOfDays);
            } else {
                generateCertificateEnabled = false;
                successMessage = "Attestation Certificate generation is disabled, "
                        + "can not set time expiration";
            }

            policy.setGenerateOnExpiration(generateCertificateEnabled);
            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA Attestation Certificate generation policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the state of the policy setting that indicates that the generation
     * will occur in a set time frame and redirects
     * back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-devid-expire-on", method = RequestMethod.POST)
    public RedirectView updateDevIdExpireOnVal(@ModelAttribute final PolicyPageModel ppModel,
                                               final RedirectAttributes attr)
            throws URISyntaxException {

        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        String numOfDays;

        boolean generateDevIdCertificateEnabled = false;
        // because this is just one option, there is not 'unchecked' value, so it is either
        // 'checked' or null
        if (ppModel.getDevIdExpirationChecked() != null) {
            generateDevIdCertificateEnabled
                    = ppModel.getDevIdExpirationChecked()
                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
        }

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);
            boolean issuedDevIdOptionEnabled
                    = policy.isIssueDevIdCertificate();

            if (issuedDevIdOptionEnabled) {
                if (generateDevIdCertificateEnabled) {
                    successMessage = "DevID Certificate generation expiration time enabled.";
                } else {
                    successMessage = "DevID Certificate generation expiration time disabled.";
                }

                if (generateDevIdCertificateEnabled) {
                    numOfDays = ppModel.getDevIdExpirationValue();
                    if (numOfDays == null) {
                        numOfDays = PolicySettings.TEN_YEARS;
                    }
                } else {
                    numOfDays = policy.getDevIdValidityDays();
                }

                policy.setDevIdValidityDays(numOfDays);
            } else {
                generateDevIdCertificateEnabled = false;
                successMessage = "DevID Certificate generation is disabled, "
                        + "can not set time expiration";
            }

            policy.setDevIdExpirationFlag(generateDevIdCertificateEnabled);
            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA DevID Certificate generation policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the state of the policy setting that indicates that the generation
     * will occur in a set time frame from the end validity date and redirects
     * back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-threshold", method = RequestMethod.POST)
    public RedirectView updateThresholdVal(@ModelAttribute final PolicyPageModel ppModel,
                                           final RedirectAttributes attr)
            throws URISyntaxException {

        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        String threshold;

        boolean generateCertificateEnabled = false;
        // because this is just one option, there is not 'unchecked' value, so it is either
        // 'checked' or null
        if (ppModel.getGenerationExpirationOn() != null) {
            generateCertificateEnabled
                    = ppModel.getGenerationExpirationOn()
                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
        }

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);
            boolean issuedAttestationOptionEnabled
                    = policy.isIssueAttestationCertificate();

            if (issuedAttestationOptionEnabled) {
                if (generateCertificateEnabled) {
                    successMessage = "Attestation Certificate generation threshold time enabled.";
                } else {
                    successMessage = "Attestation Certificate generation threshold time disabled.";
                }

                if (generateCertificateEnabled) {
                    threshold = ppModel.getThresholdValue();
                } else {
                    threshold = ppModel.getReissueThreshold();
                }

                if (threshold == null || threshold.isEmpty()) {
                    threshold = PolicySettings.YEAR;
                }

                policy.setReissueThreshold(threshold);
            } else {
                generateCertificateEnabled = false;
                successMessage = "Attestation Certificate generation is disabled, "
                        + "can not set time expiration";
            }

            policy.setGenerateOnExpiration(generateCertificateEnabled);
            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA Attestation Certificate generation policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the state of the policy setting that indicates that the generation
     * will occur in a set time frame from the end validity date and redirects
     * back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-devid-threshold", method = RequestMethod.POST)
    public RedirectView updateDevIdThresholdVal(@ModelAttribute final PolicyPageModel ppModel,
                                                final RedirectAttributes attr)
            throws URISyntaxException {
        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        String threshold;

        boolean generateDevIdCertificateEnabled = false;
        // because this is just one option, there is not 'unchecked' value, so it is either
        // 'checked' or null
        if (ppModel.getDevIdExpirationChecked() != null) {
            generateDevIdCertificateEnabled
                    = ppModel.getDevIdExpirationChecked()
                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
        }

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);
            boolean issuedDevIdOptionEnabled
                    = policy.isIssueDevIdCertificate();

            if (issuedDevIdOptionEnabled) {
                if (generateDevIdCertificateEnabled) {
                    successMessage = "DevID Certificate generation threshold time enabled.";
                } else {
                    successMessage = "DevID Certificate generation threshold time disabled.";
                }

                if (generateDevIdCertificateEnabled) {
                    threshold = ppModel.getDevIdThresholdValue();
                } else {
                    threshold = ppModel.getDevIdReissueThreshold();
                }

                if (threshold == null || threshold.isEmpty()) {
                    threshold = PolicySettings.YEAR;
                }

                policy.setDevIdReissueThreshold(threshold);
            } else {
                generateDevIdCertificateEnabled = false;
                successMessage = "DevID Certificate generation is disabled, "
                        + "can not set time expiration";
            }

            policy.setDevIdExpirationFlag(generateDevIdCertificateEnabled);
            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA DevID Certificate generation policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the Endorsement Credential Validation policy setting and
     * redirects back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original
     *                page.
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
        boolean ecValidationOptionEnabled
                = ppModel.getEcValidate().equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

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
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA endorsement validation policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the Firmware Validation policy setting and
     * redirects back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original
     *                page.
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
                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

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
                policy.setIgnoreGptEnabled(true);
                successMessage = "Firmware validation enabled";
            } else {
                policy.setFirmwareValidationEnabled(false);
                policy.setIgnoreImaEnabled(false);
                policy.setIgnoretBootEnabled(false);
                policy.setIgnoreOsEvtEnabled(false);
                successMessage = "Firmware validation disabled";
            }

            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA firmware validation policy",
                    "Error updating policy. \n" + pmEx.getMessage());

        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the ignore IMA policy setting and
     * redirects back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original
     *                page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-ima-ignore", method = RequestMethod.POST)
    public RedirectView updateIgnoreIma(@ModelAttribute final PolicyPageModel ppModel,
                                        final RedirectAttributes attr) throws URISyntaxException {
        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean ignoreImaOptionEnabled = ppModel.getIgnoreIma()
                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

            //If Ignore IMA is enabled without firmware, disallow change
            if (ignoreImaOptionEnabled && !policy.isFirmwareValidationEnabled()) {
                handleUserError(model, messages,
                        "Ignore IMA can not be "
                                + "enabled without Firmware Validation policy enabled.");
                return redirectToSelf(new NoPageParams(), model, attr);
            }

            // set the policy option and create success message
            if (ignoreImaOptionEnabled) {
                policy.setIgnoreImaEnabled(true);
                successMessage = "Ignore IMA enabled";
            } else {
                policy.setIgnoreImaEnabled(false);
                successMessage = "Ignore IMA disabled";
            }

            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA IMA ignore policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the ignore TBoot policy setting and
     * redirects back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original
     *                page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-tboot-ignore", method = RequestMethod.POST)
    public RedirectView updateIgnoreTboot(@ModelAttribute final PolicyPageModel ppModel,
                                          final RedirectAttributes attr) throws URISyntaxException {
        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean ignoreTbootOptionEnabled = ppModel.getIgnoretBoot()
                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

            //If Ignore TBoot is enabled without firmware, disallow change
            if (ignoreTbootOptionEnabled && !policy.isFirmwareValidationEnabled()) {
                handleUserError(model, messages,
                        "Ignore TBoot can not be "
                                + "enabled without Firmware Validation policy enabled.");
                return redirectToSelf(new NoPageParams(), model, attr);
            }

            // set the policy option and create success message
            if (ignoreTbootOptionEnabled) {
                policy.setIgnoretBootEnabled(true);
                successMessage = "Ignore TBoot enabled";
            } else {
                policy.setIgnoretBootEnabled(false);
                successMessage = "Ignore TBoot disabled";
            }

            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA TBoot ignore policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the ignore GPT policy setting and
     * redirects back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original
     *                page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-gpt-ignore", method = RequestMethod.POST)
    public RedirectView updateIgnoreGptEvents(@ModelAttribute final PolicyPageModel ppModel,
                                              final RedirectAttributes attr) throws URISyntaxException {
        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean ignoreGptOptionEnabled = ppModel.getIgnoreGpt()
                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

            //If Ignore TBoot is enabled without firmware, disallow change
            if (ignoreGptOptionEnabled && !policy.isFirmwareValidationEnabled()) {
                handleUserError(model, messages,
                        "Ignore GPT Events can not be "
                                + "enabled without Firmware Validation policy enabled.");
                return redirectToSelf(new NoPageParams(), model, attr);
            }

            // set the policy option and create success message
            if (ignoreGptOptionEnabled) {
                policy.setIgnoreGptEnabled(true);
                successMessage = "Ignore GPT enabled";
            } else {
                policy.setIgnoreGptEnabled(false);
                successMessage = "Ignore GPT disabled";
            }

            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA GPT ignore policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    /**
     * Updates the ignore Os Events policy setting and
     * redirects back to the original page.
     *
     * @param ppModel The data posted by the form mapped into an object.
     * @param attr    RedirectAttributes used to forward data back to the original
     *                page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "update-os-evt-ignore", method = RequestMethod.POST)
    public RedirectView updateIgnoreOsEvents(
            @ModelAttribute final PolicyPageModel ppModel,
            final RedirectAttributes attr)
            throws URISyntaxException {
        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String successMessage;
        boolean ignoreOsEvtOptionEnabled = ppModel.getIgnoreOsEvt()
                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

        try {
            PolicySettings policy = getDefaultPolicyAndSetInModel(ppModel, model);

            //If Ignore TBoot is enabled without firmware, disallow change
            if (ignoreOsEvtOptionEnabled && !policy.isFirmwareValidationEnabled()) {
                handleUserError(model, messages,
                        "Ignore Os Events can not be "
                                + "enabled without Firmware Validation policy enabled.");
                return redirectToSelf(new NoPageParams(), model, attr);
            }

            // set the policy option and create success message
            if (ignoreOsEvtOptionEnabled) {
                policy.setIgnoreOsEvtEnabled(true);
                policy.setIgnoreGptEnabled(true);
                successMessage = "Ignore OS Events enabled";
            } else {
                policy.setIgnoreOsEvtEnabled(false);
                successMessage = "Ignore OS Events disabled";
            }

            savePolicyAndApplySuccessMessage(ppModel, model, messages, successMessage, policy);
        } catch (PolicyManagerException pmEx) {
            handlePolicyManagerUpdateError(model, messages, pmEx,
                    "Error changing ACA OS Events ignore policy",
                    "Error updating policy. \n" + pmEx.getMessage());
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

    private void handlePolicyManagerUpdateError(final Map<String, Object> model,
                                                final PageMessages messages,
                                                final PolicyManagerException pmEx,
                                                final String message, final String error) {
        log.error(message, pmEx);
        messages.addErrorMessage(error);
        model.put(MESSAGES_ATTRIBUTE, messages);
    }

    private void handleUserError(final Map<String, Object> model,
                                 final PageMessages messages,
                                 final String errorMessage) {
        messages.addErrorMessage(errorMessage);
        model.put(MESSAGES_ATTRIBUTE, messages);
    }

    /**
     * Helper function to get a fresh load of the default policy from the DB.
     *
     * @return The default Supply Chain Policy
     */
    private PolicySettings getDefaultPolicy() {
        PolicySettings defaultSettings = this.policyRepository.findByName("Default");

        if (defaultSettings == null) {
            defaultSettings = new PolicySettings("Default",
                    "Settings are configured for no validation flags set.");
        }
        return defaultSettings;
    }

    /**
     * Gets the default policy and applies the current values in to the page
     * model.
     *
     * @param ppModel the page model
     * @param model   the map of string messages to be displayed on the view
     * @return The default Supply Chain Policy
     */
    private PolicySettings getDefaultPolicyAndSetInModel(
            final PolicyPageModel ppModel, final Map<String, Object> model) {
        // load the current default policy from the DB
        PolicySettings policy = getDefaultPolicy();

        // set the data received to be populated back into the form
        model.put(RESULT_DATA, ppModel);
        return policy;
    }

    private void savePolicyAndApplySuccessMessage(
            final PolicyPageModel ppModel, final Map<String, Object> model,
            final PageMessages messages, final String successMessage,
            final PolicySettings settings) {
        // save the policy to the DB
        policyRepository.saveAndFlush(settings);

        // Log and set the success message
        messages.addSuccessMessage(successMessage);
        log.debug("ACA Policy set to: " + ppModel.toString());

        model.put(MESSAGES_ATTRIBUTE, messages);
    }
}

