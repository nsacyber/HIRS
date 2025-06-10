package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.service.PolicyPageService;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.PolicyPageModel;
import hirs.attestationca.portal.page.params.NoPageParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
     * Represents a web request indicating to enable a setting (based on radio
     * buttons from a web form).
     */
    private static final String ENABLED_CHECKED_PARAMETER_VALUE = "checked";
    private static final String ENABLED_EXPIRES_PARAMETER_VALUE = "expires";

    private final PolicyPageService policyPageService;

    /**
     * Constructor for the Policy Page Controller.
     *
     * @param policyPageService policy page service
     */
    @Autowired
    public PolicyPageController(
            final PolicyPageService policyPageService) {
        super(Page.POLICY);
        this.policyPageService = policyPageService;
    }

    /**
     * Returns the path for the view and the data model for the Policy Setting page.
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

        PolicySettings policySettings = this.policyPageService.getDefaultPolicy();
        log.debug(policySettings);
        PolicyPageModel pageModel = new PolicyPageModel(policySettings);
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
    @PostMapping("update-pc-validation")
    public RedirectView updatePCValidationPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                 final RedirectAttributes attr) throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            final boolean isPcValidationOptionEnabled
                    = ppModel.getPcValidate().equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

            final boolean isPCValidationPolicyUpdateSuccessful =
                    this.policyPageService.updatePCValidationPolicy(isPcValidationOptionEnabled);

            // if the pc validation update was not successful
            if (!isPCValidationPolicyUpdateSuccessful) {
                messages.addErrorMessage("Unable to updating ACA Platform Validation setting,"
                        + "  invalid policy configuration.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, attr);
            }

            // if the pc validation policy update was successful
            if (isPcValidationOptionEnabled) {
                messages.addSuccessMessage("Platform certificate validation enabled");
            } else {
                messages.addSuccessMessage("Platform certificate validation disabled");
            }
            model.put(MESSAGES_ATTRIBUTE, messages);

        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA platform validation Policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
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
    @PostMapping("update-pc-attribute-validation")
    public RedirectView updatePCAttributeValidationPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                          final RedirectAttributes attr)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            final boolean pcAttributeValidationOptionEnabled = ppModel.getPcAttributeValidate()
                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

            final boolean isPCAttributeValPolicyUpdateSuccessful =
                    this.policyPageService.updatePCAttributeValidationPolicy(
                            pcAttributeValidationOptionEnabled);

            // If PC Attribute Validation is enabled without PC Validation, disallow change
            if (!isPCAttributeValPolicyUpdateSuccessful) {
                messages.addErrorMessage(
                        "To enable Platform Attribute Validation, Platform Credential Validation"
                                + " must also be enabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, attr);
            }

            // if the pc attribute validation policy update was successful
            if (pcAttributeValidationOptionEnabled) {
                messages.addSuccessMessage("Platform certificate attribute validation enabled");
            } else {
                messages.addSuccessMessage("Platform certificate attribute validation disabled");
            }
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA platform attribute validation"
                            + " policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
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
    @PostMapping("update-revision-ignore")
    public RedirectView updateIgnoreRevisionAttribute(@ModelAttribute final PolicyPageModel ppModel,
                                                      final RedirectAttributes attr)
            throws URISyntaxException {
        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            final boolean isIgnoreRevisionAttributeOptionEnabled = ppModel.getIgnoreRevisionAttribute()
                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

            final boolean isIgnoreRevisionPolicyUpdateSuccessful =
                    this.policyPageService.updateIgnoreRevisionAttributePolicy(
                            isIgnoreRevisionAttributeOptionEnabled);

            if (!isIgnoreRevisionPolicyUpdateSuccessful) {
                messages.addErrorMessage("Ignore Component Revision Attribute cannot be "
                        + "enabled without PC Attribute validation policy enabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, attr);
            }

            // set the policy option and create success message
            if (isIgnoreRevisionAttributeOptionEnabled) {
                messages.addSuccessMessage("Ignore Component Revision enabled");
            } else {
                messages.addSuccessMessage("Ignore Component Revision disabled");
            }
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA Component Revision Attribute policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
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
    @PostMapping("update-issue-attestation")
    public RedirectView updateAttestationValidationPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                          final RedirectAttributes attr)
            throws URISyntaxException {
        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            final boolean isIssuedAttestationOptionEnabled = ppModel.getAttestationCertificateIssued()
                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

            this.policyPageService.updateAttestationValidationPolicy(isIssuedAttestationOptionEnabled);

            if (isIssuedAttestationOptionEnabled) {
                messages.addSuccessMessage("Attestation Certificate generation enabled.");
            } else {
                messages.addSuccessMessage("Attestation Certificate generation disabled.");
            }
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA Attestation Certificate" +
                            " generation policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
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
    @PostMapping("update-issue-devid")
    public RedirectView updateDevIdVal(@ModelAttribute final PolicyPageModel ppModel,
                                       final RedirectAttributes attr)
            throws URISyntaxException {
        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        try {
            final boolean isIssuedDevIdOptionEnabled
                    = ppModel.getDevIdCertificateIssued()
                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

            this.policyPageService.updateDevIdValidationPolicy(isIssuedDevIdOptionEnabled);

            if (isIssuedDevIdOptionEnabled) {
                messages.addSuccessMessage("DevID Certificate generation enabled.");
            } else {
                messages.addSuccessMessage("DevID Certificate generation disabled.");
            }
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA DevID Certificate" +
                            " generation policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

//    /**
//     * Updates the state of the policy setting that indicates that the generation
//     * will occur in a set time frame and redirects
//     * back to the original page.
//     *
//     * @param ppModel The data posted by the form mapped into an object.
//     * @param attr    RedirectAttributes used to forward data back to the original page.
//     * @return View containing the url and parameters
//     * @throws URISyntaxException if malformed URI
//     */
//    @PostMapping("update-expire-on")
//    public RedirectView updateExpireOnVal(@ModelAttribute final PolicyPageModel ppModel,
//                                          final RedirectAttributes attr)
//            throws URISyntaxException {
//
//        // set the data received to be populated back into the form
//        Map<String, Object> model = new HashMap<>();
//        PageMessages messages = new PageMessages();
//        String successMessage;
//        String numOfDays;
//
//        boolean generateCertificateEnabled = false;
//        // because this is just one option, there is not 'unchecked' value, so it is either
//        // 'checked' or null
//        if (ppModel.getGenerationExpirationOn() != null) {
//            generateCertificateEnabled
//                    = ppModel.getGenerationExpirationOn()
//                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
//        }
//
//        try {
//            boolean issuedAttestationOptionEnabled
//                    = policySettings.isIssueAttestationCertificate();
//
//            if (issuedAttestationOptionEnabled) {
//                if (generateCertificateEnabled) {
//                    successMessage = "Attestation Certificate generation expiration time enabled.";
//                } else {
//                    successMessage = "Attestation Certificate generation expiration time disabled.";
//                }
//
//                if (generateCertificateEnabled) {
//                    numOfDays = ppModel.getExpirationValue();
//                    if (numOfDays == null) {
//                        numOfDays = PolicySettings.TEN_YEARS;
//                    }
//                } else {
//                    numOfDays = policySettings.getValidityDays();
//                }
//
//                policySettings.setValidityDays(numOfDays);
//            } else {
//                generateCertificateEnabled = false;
//                successMessage = "Attestation Certificate generation is disabled, "
//                        + "can not set time expiration";
//            }
//
//            policySettings.setGenerateOnExpiration(generateCertificateEnabled);
//            
//        } catch (PolicyManagerException pmEx) {
//            handlePolicyManagerUpdateError(model, messages, pmEx,
//                    "Error changing ACA Attestation Certificate generation policy",
//                    "Error updating policy. \n" + pmEx.getMessage());
//        }
//
//        // return the redirect
//        return redirectToSelf(new NoPageParams(), model, attr);
//    }
//
//    /**
//     * Updates the state of the policy setting that indicates that the generation
//     * will occur in a set time frame and redirects
//     * back to the original page.
//     *
//     * @param ppModel The data posted by the form mapped into an object.
//     * @param attr    RedirectAttributes used to forward data back to the original page.
//     * @return View containing the url and parameters
//     * @throws URISyntaxException if malformed URI
//     */
//    @PostMapping("update-devid-expire-on")
//    public RedirectView updateDevIdExpireOnVal(@ModelAttribute final PolicyPageModel ppModel,
//                                               final RedirectAttributes attr)
//            throws URISyntaxException {
//
//        // set the data received to be populated back into the form
//        Map<String, Object> model = new HashMap<>();
//        PageMessages messages = new PageMessages();
//        String successMessage;
//        String numOfDays;
//
//        boolean generateDevIdCertificateEnabled = false;
//        // because this is just one option, there is not 'unchecked' value, so it is either
//        // 'checked' or null
//        if (ppModel.getDevIdExpirationChecked() != null) {
//            generateDevIdCertificateEnabled
//                    = ppModel.getDevIdExpirationChecked()
//                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
//        }
//
//        try {
//
//            boolean issuedDevIdOptionEnabled
//                    = policySettings.isIssueDevIdCertificate();
//
//            if (issuedDevIdOptionEnabled) {
//                if (generateDevIdCertificateEnabled) {
//                    successMessage = "DevID Certificate generation expiration time enabled.";
//                } else {
//                    successMessage = "DevID Certificate generation expiration time disabled.";
//                }
//
//                if (generateDevIdCertificateEnabled) {
//                    numOfDays = ppModel.getDevIdExpirationValue();
//                    if (numOfDays == null) {
//                        numOfDays = PolicySettings.TEN_YEARS;
//                    }
//                } else {
//                    numOfDays = policySettings.getDevIdValidityDays();
//                }
//
//                policySettings.setDevIdValidityDays(numOfDays);
//            } else {
//                generateDevIdCertificateEnabled = false;
//                successMessage = "DevID Certificate generation is disabled, "
//                        + "can not set time expiration";
//            }
//
//            policySettings.setDevIdExpirationFlag(generateDevIdCertificateEnabled);
//            
//        } catch (PolicyManagerException pmEx) {
//            handlePolicyManagerUpdateError(model, messages, pmEx,
//                    "Error changing ACA DevID Certificate generation policy",
//                    "Error updating policy. \n" + pmEx.getMessage());
//        }
//
//        // return the redirect
//        return redirectToSelf(new NoPageParams(), model, attr);
//    }
//
//    /**
//     * Updates the state of the policy setting that indicates that the generation
//     * will occur in a set time frame from the end validity date and redirects
//     * back to the original page.
//     *
//     * @param ppModel The data posted by the form mapped into an object.
//     * @param attr    RedirectAttributes used to forward data back to the original page.
//     * @return View containing the url and parameters
//     * @throws URISyntaxException if malformed URI
//     */
//    @PostMapping("update-threshold")
//    public RedirectView updateThresholdVal(@ModelAttribute final PolicyPageModel ppModel,
//                                           final RedirectAttributes attr)
//            throws URISyntaxException {
//
//        // set the data received to be populated back into the form
//        Map<String, Object> model = new HashMap<>();
//        PageMessages messages = new PageMessages();
//        String successMessage;
//        String threshold;
//
//        boolean generateCertificateEnabled = false;
//        // because this is just one option, there is not 'unchecked' value, so it is either
//        // 'checked' or null
//        if (ppModel.getGenerationExpirationOn() != null) {
//            generateCertificateEnabled
//                    = ppModel.getGenerationExpirationOn()
//                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
//        }
//
//        try {
//
//            boolean issuedAttestationOptionEnabled
//                    = policy.isIssueAttestationCertificate();
//
//            if (issuedAttestationOptionEnabled) {
//                if (generateCertificateEnabled) {
//                    successMessage = "Attestation Certificate generation threshold time enabled.";
//                } else {
//                    successMessage = "Attestation Certificate generation threshold time disabled.";
//                }
//
//                if (generateCertificateEnabled) {
//                    threshold = ppModel.getThresholdValue();
//                } else {
//                    threshold = ppModel.getReissueThreshold();
//                }
//
//                if (threshold == null || threshold.isEmpty()) {
//                    threshold = PolicySettings.YEAR;
//                }
//
//                policy.setReissueThreshold(threshold);
//            } else {
//                generateCertificateEnabled = false;
//                successMessage = "Attestation Certificate generation is disabled, "
//                        + "can not set time expiration";
//            }
//
//            policy.setGenerateOnExpiration(generateCertificateEnabled);
//            
//        } catch (PolicyManagerException pmEx) {
//            handlePolicyManagerUpdateError(model, messages, pmEx,
//                    "Error changing ACA Attestation Certificate generation policy",
//                    "Error updating policy. \n" + pmEx.getMessage());
//        }
//
//        // return the redirect
//        return redirectToSelf(new NoPageParams(), model, attr);
//    }
//
//    /**
//     * Updates the state of the policy setting that indicates that the generation
//     * will occur in a set time frame from the end validity date and redirects
//     * back to the original page.
//     *
//     * @param ppModel The data posted by the form mapped into an object.
//     * @param attr    RedirectAttributes used to forward data back to the original page.
//     * @return View containing the url and parameters
//     * @throws URISyntaxException if malformed URI
//     */
//    @PostMapping("update-devid-threshold")
//    public RedirectView updateDevIdThresholdVal(@ModelAttribute final PolicyPageModel ppModel,
//                                                final RedirectAttributes attr)
//            throws URISyntaxException {
//        // set the data received to be populated back into the form
//        Map<String, Object> model = new HashMap<>();
//        PageMessages messages = new PageMessages();
//        String successMessage;
//        String threshold;
//
//        boolean generateDevIdCertificateEnabled = false;
//        // because this is just one option, there is not 'unchecked' value, so it is either
//        // 'checked' or null
//        if (ppModel.getDevIdExpirationChecked() != null) {
//            generateDevIdCertificateEnabled
//                    = ppModel.getDevIdExpirationChecked()
//                    .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
//        }
//
//        try {
//
//            boolean issuedDevIdOptionEnabled
//                    = policy.isIssueDevIdCertificate();
//
//            if (issuedDevIdOptionEnabled) {
//                if (generateDevIdCertificateEnabled) {
//                    successMessage = "DevID Certificate generation threshold time enabled.";
//                } else {
//                    successMessage = "DevID Certificate generation threshold time disabled.";
//                }
//
//                if (generateDevIdCertificateEnabled) {
//                    threshold = ppModel.getDevIdThresholdValue();
//                } else {
//                    threshold = ppModel.getDevIdReissueThreshold();
//                }
//
//                if (threshold == null || threshold.isEmpty()) {
//                    threshold = PolicySettings.YEAR;
//                }
//
//                policy.setDevIdReissueThreshold(threshold);
//            } else {
//                generateDevIdCertificateEnabled = false;
//                successMessage = "DevID Certificate generation is disabled, "
//                        + "can not set time expiration";
//            }
//
//            policy.setDevIdExpirationFlag(generateDevIdCertificateEnabled);
//            
//        } catch (PolicyManagerException pmEx) {
//            handlePolicyManagerUpdateError(model, messages, pmEx,
//                    "Error changing ACA DevID Certificate generation policy",
//                    "Error updating policy. \n" + pmEx.getMessage());
//        }
//
//        // return the redirect
//        return redirectToSelf(new NoPageParams(), model, attr);
//    }
//

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
    @PostMapping("update-ec-validation")
    public RedirectView updateEcVal(@ModelAttribute final PolicyPageModel ppModel,
                                    final RedirectAttributes attr) throws URISyntaxException {
        // set the data received to be populated back into the form
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            final boolean ecValidationOptionEnabled
                    = ppModel.getEcValidate().equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);

            final boolean isECValPolicyUpdateSuccessful =
                    this.policyPageService.updateECValidationPolicy(ecValidationOptionEnabled);

//            !isPolicyValid(ecValidationOptionEnabled, policy.isPcValidationEnabled(),
//                    policy.isPcAttributeValidationEnabled())

            //If PC Validation is enabled without EC Validation, disallow change
            if (!isECValPolicyUpdateSuccessful) {
                messages.addErrorMessage(
                        "To disable Endorsement Credential Validation, Platform Validation"
                                + " must also be disabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, attr);
            }
            // set the policy option and create success message
            if (ecValidationOptionEnabled) {
                //policy.setEcValidationEnabled(true); todo
                messages.addSuccessMessage("Endorsement credential validation enabled");
            } else {
                //policy.setEcValidationEnabled(false); todo
                messages.addSuccessMessage("Endorsement credential validation disabled");
            }
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA endorsement validation policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        // return the redirect
        return redirectToSelf(new NoPageParams(), model, attr);
    }

//    /**
//     * Updates the Firmware Validation policy setting and
//     * redirects back to the original page.
//     *
//     * @param ppModel The data posted by the form mapped into an object.
//     * @param attr    RedirectAttributes used to forward data back to the original
//     *                page.
//     * @return View containing the url and parameters
//     * @throws URISyntaxException if malformed URI
//     */
//    @PostMapping("update-firmware-validation")
//    public RedirectView updateFirmwareVal(@ModelAttribute final PolicyPageModel ppModel,
//                                          final RedirectAttributes attr) throws URISyntaxException {
//
//        // set the data received to be populated back into the form
//        Map<String, Object> model = new HashMap<>();
//        PageMessages messages = new PageMessages();
//        String successMessage;
//        boolean firmwareValidationOptionEnabled = ppModel.getFmValidate()
//                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
//
//        try {
//
//
//            //If firmware is enabled without PC attributes, disallow change
//            if (firmwareValidationOptionEnabled && !policy.isPcAttributeValidationEnabled()) {
//                handleUserError(model, messages,
//                        "Firmware validation can not be "
//                                + "enabled without PC Attributes policy enabled.");
//                return redirectToSelf(new NoPageParams(), model, attr);
//            }
//
//            // set the policy option and create success message
//            if (firmwareValidationOptionEnabled) {
//                policy.setFirmwareValidationEnabled(true);
//                policy.setIgnoreGptEnabled(true);
//                successMessage = "Firmware validation enabled";
//            } else {
//                policy.setFirmwareValidationEnabled(false);
//                policy.setIgnoreImaEnabled(false);
//                policy.setIgnoretBootEnabled(false);
//                policy.setIgnoreOsEvtEnabled(false);
//                successMessage = "Firmware validation disabled";
//            }
//
//            
//        } catch (PolicyManagerException pmEx) {
//            handlePolicyManagerUpdateError(model, messages, pmEx,
//                    "Error changing ACA firmware validation policy",
//                    "Error updating policy. \n" + pmEx.getMessage());
//
//        }
//
//        // return the redirect
//        return redirectToSelf(new NoPageParams(), model, attr);
//    }
//
//    /**
//     * Updates the ignore IMA policy setting and
//     * redirects back to the original page.
//     *
//     * @param ppModel The data posted by the form mapped into an object.
//     * @param attr    RedirectAttributes used to forward data back to the original
//     *                page.
//     * @return View containing the url and parameters
//     * @throws URISyntaxException if malformed URI
//     */
//    @PostMapping("update-ima-ignore")
//    public RedirectView updateIgnoreIma(@ModelAttribute final PolicyPageModel ppModel,
//                                        final RedirectAttributes attr) throws URISyntaxException {
//        // set the data received to be populated back into the form
//        Map<String, Object> model = new HashMap<>();
//        PageMessages messages = new PageMessages();
//        String successMessage;
//        boolean ignoreImaOptionEnabled = ppModel.getIgnoreIma()
//                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
//
//        try {
//
//
//            //If Ignore IMA is enabled without firmware, disallow change
//            if (ignoreImaOptionEnabled && !policy.isFirmwareValidationEnabled()) {
//                handleUserError(model, messages,
//                        "Ignore IMA can not be "
//                                + "enabled without Firmware Validation policy enabled.");
//                return redirectToSelf(new NoPageParams(), model, attr);
//            }
//
//            // set the policy option and create success message
//            if (ignoreImaOptionEnabled) {
//                policy.setIgnoreImaEnabled(true);
//                successMessage = "Ignore IMA enabled";
//            } else {
//                policy.setIgnoreImaEnabled(false);
//                successMessage = "Ignore IMA disabled";
//            }
//
//            
//        } catch (PolicyManagerException pmEx) {
//            handlePolicyManagerUpdateError(model, messages, pmEx,
//                    "Error changing ACA IMA ignore policy",
//                    "Error updating policy. \n" + pmEx.getMessage());
//        }
//
//        // return the redirect
//        return redirectToSelf(new NoPageParams(), model, attr);
//    }
//
//    /**
//     * Updates the ignore TBoot policy setting and
//     * redirects back to the original page.
//     *
//     * @param ppModel The data posted by the form mapped into an object.
//     * @param attr    RedirectAttributes used to forward data back to the original
//     *                page.
//     * @return View containing the url and parameters
//     * @throws URISyntaxException if malformed URI
//     */
//    @PostMapping("update-tboot-ignore")
//    public RedirectView updateIgnoreTboot(@ModelAttribute final PolicyPageModel ppModel,
//                                          final RedirectAttributes attr) throws URISyntaxException {
//        // set the data received to be populated back into the form
//        Map<String, Object> model = new HashMap<>();
//        PageMessages messages = new PageMessages();
//        String successMessage;
//        boolean ignoreTbootOptionEnabled = ppModel.getIgnoretBoot()
//                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
//
//        try {
//
//
//            //If Ignore TBoot is enabled without firmware, disallow change
//            if (ignoreTbootOptionEnabled && !policy.isFirmwareValidationEnabled()) {
//                handleUserError(model, messages,
//                        "Ignore TBoot can not be "
//                                + "enabled without Firmware Validation policy enabled.");
//                return redirectToSelf(new NoPageParams(), model, attr);
//            }
//
//            // set the policy option and create success message
//            if (ignoreTbootOptionEnabled) {
//                policy.setIgnoretBootEnabled(true);
//                successMessage = "Ignore TBoot enabled";
//            } else {
//                policy.setIgnoretBootEnabled(false);
//                successMessage = "Ignore TBoot disabled";
//            }
//
//            
//        } catch (PolicyManagerException pmEx) {
//            handlePolicyManagerUpdateError(model, messages, pmEx,
//                    "Error changing ACA TBoot ignore policy",
//                    "Error updating policy. \n" + pmEx.getMessage());
//        }
//
//        // return the redirect
//        return redirectToSelf(new NoPageParams(), model, attr);
//    }
//
//    /**
//     * Updates the ignore GPT policy setting and
//     * redirects back to the original page.
//     *
//     * @param ppModel The data posted by the form mapped into an object.
//     * @param attr    RedirectAttributes used to forward data back to the original
//     *                page.
//     * @return View containing the url and parameters
//     * @throws URISyntaxException if malformed URI
//     */
//    @PostMapping("update-gpt-ignore")
//    public RedirectView updateIgnoreGptEvents(@ModelAttribute final PolicyPageModel ppModel,
//                                              final RedirectAttributes attr) throws URISyntaxException {
//        // set the data received to be populated back into the form
//        Map<String, Object> model = new HashMap<>();
//        PageMessages messages = new PageMessages();
//        String successMessage;
//        boolean ignoreGptOptionEnabled = ppModel.getIgnoreGpt()
//                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
//
//        try {
//
//
//            //If Ignore TBoot is enabled without firmware, disallow change
//            if (ignoreGptOptionEnabled && !policy.isFirmwareValidationEnabled()) {
//                handleUserError(model, messages,
//                        "Ignore GPT Events can not be "
//                                + "enabled without Firmware Validation policy enabled.");
//                return redirectToSelf(new NoPageParams(), model, attr);
//            }
//
//            // set the policy option and create success message
//            if (ignoreGptOptionEnabled) {
//                policy.setIgnoreGptEnabled(true);
//                successMessage = "Ignore GPT enabled";
//            } else {
//                policy.setIgnoreGptEnabled(false);
//                successMessage = "Ignore GPT disabled";
//            }
//
//            
//        } catch (PolicyManagerException pmEx) {
//            handlePolicyManagerUpdateError(model, messages, pmEx,
//                    "Error changing ACA GPT ignore policy",
//                    "Error updating policy. \n" + pmEx.getMessage());
//        }
//
//        // return the redirect
//        return redirectToSelf(new NoPageParams(), model, attr);
//    }
//
//    /**
//     * Updates the ignore Os Events policy setting and
//     * redirects back to the original page.
//     *
//     * @param ppModel The data posted by the form mapped into an object.
//     * @param attr    RedirectAttributes used to forward data back to the original
//     *                page.
//     * @return View containing the url and parameters
//     * @throws URISyntaxException if malformed URI
//     */
//    @PostMapping("update-os-evt-ignore")
//    public RedirectView updateIgnoreOsEvents(
//            @ModelAttribute final PolicyPageModel ppModel,
//            final RedirectAttributes attr)
//            throws URISyntaxException {
//        // set the data received to be populated back into the form
//        Map<String, Object> model = new HashMap<>();
//        PageMessages messages = new PageMessages();
//        String successMessage;
//        boolean ignoreOsEvtOptionEnabled = ppModel.getIgnoreOsEvt()
//                .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
//
//        try {
//
//
//            //If Ignore TBoot is enabled without firmware, disallow change
//            if (ignoreOsEvtOptionEnabled && !policy.isFirmwareValidationEnabled()) {
//                handleUserError(model, messages,
//                        "Ignore Os Events can not be "
//                                + "enabled without Firmware Validation policy enabled.");
//                return redirectToSelf(new NoPageParams(), model, attr);
//            }
//
//            // set the policy option and create success message
//            if (ignoreOsEvtOptionEnabled) {
//                policy.setIgnoreOsEvtEnabled(true);
//                policy.setIgnoreGptEnabled(true);
//                successMessage = "Ignore OS Events enabled";
//            } else {
//                policy.setIgnoreOsEvtEnabled(false);
//                successMessage = "Ignore OS Events disabled";
//            }
//
//            
//        } catch (PolicyManagerException pmEx) {
//            handlePolicyManagerUpdateError(model, messages, pmEx,
//                    "Error changing ACA OS Events ignore policy",
//                    "Error updating policy. \n" + pmEx.getMessage());
//        }
//
//        // return the redirect
//        return redirectToSelf(new NoPageParams(), model, attr);
//    }
}
