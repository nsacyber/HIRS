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

    private final PolicyPageService policyPageService;

    /**
     * Constructor for the Policy Page Controller.
     *
     * @param policyPageService policy page service
     */
    @Autowired
    public PolicyPageController(final PolicyPageService policyPageService) {
        super(Page.POLICY);
        this.policyPageService = policyPageService;
    }

    /**
     * Returns the path for the view and the data model for the Policy Settings page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        ModelAndView mav = getBaseModelAndView();

        PolicySettings policySettings = this.policyPageService.getDefaultPolicy();
        log.debug("Policy Page Settings: {}", policySettings);

        PolicyPageModel pageModel = new PolicyPageModel(policySettings);
        mav.addObject(INITIAL_DATA, pageModel);
        log.debug("Policy Page Model: {}", pageModel);
        return mav;
    }

    /**
     * Updates the Platform Cert Validation policy setting and redirects the user back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-pc-validation")
    public RedirectView updatePCValidationPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                 final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the platform credential validation policy setting");

        try {
            final boolean isPcValidationOptionEnabled = ppModel.isPcValidationEnabled();

            final boolean isPCValidationPolicyUpdateSuccessful =
                    this.policyPageService.updatePCValidationPolicy(isPcValidationOptionEnabled);

            if (!isPCValidationPolicyUpdateSuccessful) {
                messages.addErrorMessage("Unable to update ACA Platform Validation setting due to the current"
                        + " policy configuration.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, redirectAttributes);
            }

            // if the pc validation policy update was successful
            messages.addSuccessMessage("Platform Certificate Validation "
                    + (isPcValidationOptionEnabled ? "enabled" : "disabled"));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA platform validation Policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }
        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the Platform Cert Attribute Validation policy setting and redirects the user back to the
     * Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-pc-attribute-validation")
    public RedirectView updatePCAttributeValPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                   final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the platform credential attribute validation policy setting");

        try {
            final boolean isPCAttributeValidationOptionEnabled =
                    ppModel.isPcAttributeValidationEnabled();

            final boolean isPCAttributeValPolicyUpdateSuccessful =
                    this.policyPageService.updatePCAttributeValidationPolicy(
                            isPCAttributeValidationOptionEnabled);

            if (!isPCAttributeValPolicyUpdateSuccessful) {
                messages.addErrorMessage(
                        "To enable Platform Attribute Validation, Platform Credential Validation"
                                + " must also be enabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, redirectAttributes);
            }

            // if the pc attribute validation policy update was successful
            messages.addSuccessMessage("Platform Certificate Attribute validation "
                    + (isPCAttributeValidationOptionEnabled ? "enabled" : "disabled"));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA platform attribute validation policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates Ignore PCIE VPD Attribute policy under the Platform Cert Attribute Validation policy setting
     * and redirects the user back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-pcie-vpd-ignore")
    public RedirectView updateIgnorePCIEVpdAttributePolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                           final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the Ignore PCIE VPD Attribute policy under "
                + " the platform credential attribute validation policy setting");

        try {
            final boolean isIgnorePcieVpdOptionEnabled = ppModel.isIgnorePcieVpdAttributeEnabled();

            final boolean isIgnorePcieVpdPolicyUpdateSuccessful =
                    this.policyPageService.updateIgnorePCIEVpdPolicy(isIgnorePcieVpdOptionEnabled);

            if (!isIgnorePcieVpdPolicyUpdateSuccessful) {
                messages.addErrorMessage(
                        "Ignore PCIE VPD Attribute Policy cannot be enabled without PC Attribute"
                                + " validation policy enabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, redirectAttributes);
            }

            // if the Ignore PCIE VPD Attribute update was successful
            messages.addSuccessMessage("Ignore PCIE VPD Attribute  "
                    + (isIgnorePcieVpdOptionEnabled ? "enabled" : "disabled"));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA Ignore PCIE VPD Attribute policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the Ignore Component Revision Attribute Policy under Platform Cert Attribute Validation policy
     * setting and redirects the user back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-revision-ignore")
    public RedirectView updateIgnoreRevisionAttributePolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                            final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the ignore component revision attribute policy under "
                + " the platform credential attribute validation policy setting");

        try {
            final boolean isIgnoreRevisionAttributeOptionEnabled = ppModel.isIgnoreRevisionAttributeEnabled();

            final boolean isIgnoreRevisionPolicyUpdateSuccessful =
                    this.policyPageService.updateIgnoreRevisionAttributePolicy(
                            isIgnoreRevisionAttributeOptionEnabled);

            if (!isIgnoreRevisionPolicyUpdateSuccessful) {
                messages.addErrorMessage("Ignore Component Revision Attribute cannot be "
                        + "enabled without PC Attribute validation policy enabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, redirectAttributes);
            }

            // if the ignore revision policy update was successful
            messages.addSuccessMessage("Ignore Component Revision "
                    + (isIgnoreRevisionAttributeOptionEnabled ? "enabled" : "disabled"));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA Component Revision Attribute policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the Attestation Certificate generation policy setting and redirects the user
     * back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the Policy Settings page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-issued-attestation-generation")
    public RedirectView updateAttestationCertGenerationPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                              final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the issue attestation certificate generation policy setting");

        try {
            final boolean isIssuedAttestationOptionEnabled = ppModel.isIssueAttestationCertificateEnabled();

            this.policyPageService.updateIssuedAttestationGenerationPolicy(isIssuedAttestationOptionEnabled);

            messages.addSuccessMessage("Attestation Certificate Generation "
                    + (isIssuedAttestationOptionEnabled ? "enabled." : "disabled."));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA Attestation Certificate generation policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the DevID Certificate generation policy setting and redirects the user
     * back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the Policy Settings page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-issued-ldevid-generation")
    public RedirectView updateLDevIdGenerationPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                     final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the ldevid certificate generation policy setting");

        try {
            final boolean isIssuedLDevIdOptionEnabled = ppModel.isIssueDevIdCertificateEnabled();
            this.policyPageService.updateLDevIdGenerationPolicy(isIssuedLDevIdOptionEnabled);

            // if the ldevid certificate generation policy update was successful
            messages.addSuccessMessage("LDevID Certificate Generation "
                    + (isIssuedLDevIdOptionEnabled ? "enabled." : "disabled."));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA LDevID Certificate generation policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the attestation certificate generation expiration policy setting and redirects the user
     * back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the Policy Settings page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-attestation-certificate-expiration")
    public RedirectView updateAttestationCertExpirationPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                              final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the attestation certificate expiration policy under the"
                + "generate attestation certificate policy setting");

        try {
            boolean isGenerateCertificateEnabled = ppModel.isGenerateAttestationCertOnExpirationEnabled();

            final String successMessage = this.policyPageService.updateAttestationCertExpirationPolicy(
                    ppModel.getGenerateAttestCertExpirationValue(),
                    isGenerateCertificateEnabled);
            messages.addSuccessMessage(successMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA Attestation Certificate generation policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the ldevid certification generation expiration policy setting and redirects the user
     * back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the Policy Settings page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-ldevid-certificate-expiration")
    public RedirectView updateLDevIdCertExpirationPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                         final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the ldevid certificate expiration policy under "
                + "the ldevid certificate generation policy setting");

        try {
            boolean isGenerateDevIdCertificateEnabled = false;
            // because this is just one option, there is not 'unchecked' value, so it is either
            // 'checked' or null
            if (ppModel.getDevIdExpirationChecked() != null) {
//                isGenerateDevIdCertificateEnabled
//                        = ppModel.getDevIdExpirationChecked()
//                        .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
            }

            final String successMessage =
                    this.policyPageService.updateLDevIdExpirationPolicy(
                            ppModel.getGenerateDevIdCertExpirationValue(),
                            isGenerateDevIdCertificateEnabled);
            messages.addSuccessMessage(successMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA LDevID Certificate expiration policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the attestation certificate generation threshold policy setting and redirects the user
     * back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the Policy Settings page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-issued-cert-threshold")
    public RedirectView updateAttestationCertThresholdPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                             final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the attestation certificate threshold policy under "
                + "the attestation certificate generation policy setting");

        try {
            boolean generateCertificateEnabled = ppModel.isGenerateAttestationCertOnExpirationEnabled();

            final String successMessage =
                    this.policyPageService.updateAttestationCertThresholdPolicy(
                            ppModel.getGenerateAttestCertThresholdValue(),
                            ppModel.getReissueThreshold(),
                            generateCertificateEnabled);
            messages.addSuccessMessage(successMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA Attestation Certificate threshold value";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the ldevid certificate generation threshold policy setting and redirects the user
     * back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the Policy Settings page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-ldevid-threshold")
    public RedirectView updateLDevIdThresholdValPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                       final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the ldevid certificate threshold policy under "
                + "the ldevid certificate generation policy setting");

        try {
            boolean generateDevIdCertificateEnabled = false;
            // because this is just one option, there is not 'unchecked' value, so it is either
            // 'checked' or null
            if (ppModel.getDevIdExpirationChecked() != null) {
//                generateDevIdCertificateEnabled
//                        = ppModel.getDevIdExpirationChecked()
//                        .equalsIgnoreCase(ENABLED_CHECKED_PARAMETER_VALUE);
            }

            final String successMessage =
                    this.policyPageService.updateLDevIdThresholdPolicy(
                            ppModel.getGenerateDevIdCertThresholdValue(),
                            ppModel.getDevIdReissueThreshold(),
                            generateDevIdCertificateEnabled);
            messages.addSuccessMessage(successMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA DevID Certificate generation policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the Endorsement Credential Validation policy setting and redirects the user back to
     * the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-ec-validation")
    public RedirectView updateECValidationPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                 final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the endorsement credential validation policy setting");

        try {
            final boolean isECValidationOptionEnabled = ppModel.isEcValidationEnabled();

            final boolean isECValPolicyUpdateSuccessful =
                    this.policyPageService.updateECValidationPolicy(isECValidationOptionEnabled);

            if (!isECValPolicyUpdateSuccessful) {
                messages.addErrorMessage(
                        "To disable Endorsement Credential Validation, Platform Validation"
                                + " must also be disabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, redirectAttributes);
            }

            // if the EC validation policy update was successful
            messages.addSuccessMessage("Endorsement Credential Validation "
                    + (isECValidationOptionEnabled ? "enabled" : "disabled"));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA endorsement validation policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the Firmware Validation policy setting and redirects the user back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-firmware-validation")
    public RedirectView updateFirmwareValidationPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                       final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the firmware validation policy setting");

        try {
            final boolean isFirmwareValidationOptionEnabled = ppModel.isFirmwareValidationEnabled();

            final boolean isFirmwareValPolicyUpdateSuccessful =
                    this.policyPageService.updateFirmwareValidationPolicy(isFirmwareValidationOptionEnabled);

            if (!isFirmwareValPolicyUpdateSuccessful) {
                messages.addErrorMessage(
                        "Firmware validation cannot be enabled without PC Attributes policy enabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, redirectAttributes);
            }

            // if the firmware validation policy update was successful
            messages.addSuccessMessage(
                    "Firmware Validation " + (isFirmwareValidationOptionEnabled ? "enabled" : "disabled"));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA firmware validation policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the Ignore IMA policy under the firmware validation policy setting and redirects the
     * user back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-ima-ignore")
    public RedirectView updateIgnoreImaPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                              final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the ignore IMA policy under the firmware"
                + " validation policy setting");

        try {
            final boolean isIgnoreImaOptionEnabled = ppModel.isIgnoreImaEnabled();

            final boolean isIgnoreImaPolicyUpdateSuccessful =
                    this.policyPageService.updateIgnoreImaPolicy(isIgnoreImaOptionEnabled);

            if (!isIgnoreImaPolicyUpdateSuccessful) {
                messages.addErrorMessage(
                        "Ignore IMA cannot be enabled without Firmware Validation policy enabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, redirectAttributes);
            }

            // if the ignore IMA policy update was successful
            messages.addSuccessMessage("Ignore IMA " + (isIgnoreImaOptionEnabled ? "enabled" : "disabled"));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while updating ACA IMA ignore policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the Ignore TBoot policy under the firmware validation policy setting and redirects the user
     * back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-tboot-ignore")
    public RedirectView updateIgnoreTbootPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the ignore TBoot policy under the firmware"
                + " validation policy setting");

        try {
            final boolean ignoreTbootOptionEnabled = ppModel.isIgnoreTbootEnabled();

            final boolean isIgnoreTbootPolicyUpdateSuccessful =
                    this.policyPageService.updateIgnoreTBootPolicy(ignoreTbootOptionEnabled);

            if (!isIgnoreTbootPolicyUpdateSuccessful) {
                messages.addErrorMessage(
                        "Ignore TBoot cannot be enabled without Firmware Validation policy enabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, redirectAttributes);
            }

            // if the ignore TBoot policy update was successful
            messages.addSuccessMessage("Ignore TBoot " + (ignoreTbootOptionEnabled ? "enabled" : "disabled"));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while updating ACA TBoot Ignore policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the Ignore GPT Events policy under the firmware validation policy setting and redirects the
     * user back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-gpt-ignore")
    public RedirectView updateIgnoreGptEventsPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                    final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the ignore GPT events policy under the firmware"
                + " validation policy setting");

        try {
            final boolean isIgnoreGptOptionEnabled = ppModel.isIgnoreGptEnabled();

            final boolean isIgnoreGptOptionPolicyUpdateSuccessful =
                    this.policyPageService.updateIgnoreGptEventsPolicy(isIgnoreGptOptionEnabled);

            if (!isIgnoreGptOptionPolicyUpdateSuccessful) {
                messages.addErrorMessage(
                        "Ignore GPT Events cannot be enabled without Firmware Validation policy enabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, redirectAttributes);
            }

            // if the ignore GPT events policy update was successful
            messages.addSuccessMessage("Ignore GPT " + (isIgnoreGptOptionEnabled ? "enabled" : "disabled"));
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while updating ACA ignore GPT events policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the Ignore Os Events policy under the firmware validation policy setting and redirects the user
     * back to the Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-os-evt-ignore")
    public RedirectView updateIgnoreOsEventsPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                   final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the ignore OS events policy under the firmware"
                + " validation policy setting");

        try {
            final boolean isIgnoreOsEvtOptionEnabled = ppModel.isIgnoreOsEvtEnabled();
            final boolean isIgnoreOSPolicyUpdateSuccessful =
                    this.policyPageService.updateIgnoreOSEventsPolicy(isIgnoreOsEvtOptionEnabled);

            if (!isIgnoreOSPolicyUpdateSuccessful) {
                messages.addErrorMessage(
                        "Ignore Os Events cannot be enabled without Firmware Validation policy enabled.");
                model.put(MESSAGES_ATTRIBUTE, messages);
                return redirectToSelf(new NoPageParams(), model, redirectAttributes);
            }

            // if the ignore OS events policy update was successful
            messages.addSuccessMessage(
                    isIgnoreOsEvtOptionEnabled ? "Ignore OS Events enabled" : "Ignore OS Events disabled");
            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while updating ACA OS Events ignore policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Updates the save protobuf data to ACA log policy setting and redirects the user back to the
     * Policy Settings page.
     *
     * @param ppModel            The data posted by the form mapped into an object.
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return View containing the url and parameters
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("update-save-protobuf-data-to-log")
    public RedirectView updateSaveProtobufDataToLogPolicy(@ModelAttribute final PolicyPageModel ppModel,
                                                          final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        log.info("Received request to update the save protobuf data to the ACA log policy setting");

        try {
            final String saveProtobufToLogVal = ppModel.getSaveProtobufToLogOption();

            this.policyPageService.updateSaveProtobufDataToLogPolicy(saveProtobufToLogVal);

            switch (saveProtobufToLogVal) {
                case "always-log-protobuf" ->
                        messages.addSuccessMessage("Save Protobuf Data To ACA Log always has been enabled");
                case "log-protobuf-on-fail-val" ->
                        messages.addSuccessMessage("Save Protobuf Data To ACA Log only on failed validation"
                                + " has been enabled");
                case "never-log-protobuf" ->
                        messages.addSuccessMessage("Save Protobuf Data To ACA Log never has been enabled");
                default -> throw new IllegalArgumentException("There must be exactly three valid options for "
                        + "setting the policy to save protobuf data to the ACA log.");
            }

            model.put(MESSAGES_ATTRIBUTE, messages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while updating ACA save of protobuf data to "
                    + "ACA log policy";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
            model.put(MESSAGES_ATTRIBUTE, messages);
        }

        return redirectToSelf(new NoPageParams(), model, redirectAttributes);
    }
}
