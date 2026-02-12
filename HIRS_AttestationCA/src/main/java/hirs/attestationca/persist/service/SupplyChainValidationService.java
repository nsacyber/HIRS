package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentAttributeRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.manager.SupplyChainValidationRepository;
import hirs.attestationca.persist.entity.manager.SupplyChainValidationSummaryRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentAttributeResult;
import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.EventLogMeasurements;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.exceptions.DBManagerException;
import hirs.attestationca.persist.validation.PcrValidator;
import hirs.attestationca.persist.validation.SupplyChainCredentialValidator;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static hirs.attestationca.persist.enums.AppraisalStatus.Status.FAIL;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.PASS;

/**
 * A service layer class responsible for validating the supply chain based on the policy settings.
 */
@Log4j2
@Service
public class SupplyChainValidationService {

    private final CACredentialRepository caCredentialRepository;
    private final PolicyRepository policyRepository;
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final ComponentResultRepository componentResultRepository;
    private final ComponentAttributeRepository componentAttributeRepository;
    private final CertificateRepository certificateRepository;
    private final SupplyChainValidationRepository supplyChainValidationRepository;
    private final SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository;
    private UUID provisionSessionId;

    /**
     * Constructor.
     *
     * @param caCredentialRepository                 ca credential repository
     * @param policyRepository                       the policy manager
     * @param certificateRepository                  the cert manager
     * @param componentResultRepository              the comp result manager
     * @param componentAttributeRepository           component attribute repository
     * @param referenceManifestRepository            the RIM manager
     * @param supplyChainValidationRepository        the scv manager
     * @param supplyChainValidationSummaryRepository the summary manager
     * @param referenceDigestValueRepository         the even manager
     */
    @Autowired
    public SupplyChainValidationService(
            final CACredentialRepository caCredentialRepository,
            final PolicyRepository policyRepository,
            final CertificateRepository certificateRepository,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final ReferenceManifestRepository referenceManifestRepository,
            final SupplyChainValidationRepository supplyChainValidationRepository,
            final SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository) {
        this.caCredentialRepository = caCredentialRepository;
        this.policyRepository = policyRepository;
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
        this.componentAttributeRepository = componentAttributeRepository;
        this.referenceManifestRepository = referenceManifestRepository;
        this.supplyChainValidationRepository = supplyChainValidationRepository;
        this.supplyChainValidationSummaryRepository = supplyChainValidationSummaryRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
    }

    /**
     * The "main" method of supply chain validation. Takes the credentials from
     * an identity request and validates the supply chain in accordance to the
     * current supply chain policy.
     *
     * @param ec             The endorsement credential from the identity request.
     * @param pcs            The platform credentials from the identity request.
     * @param device         The device to be validated.
     * @param componentInfos list of components from the device
     * @return A summary of the validation results.
     * @throws IOException if any issues arise from validating the supply chain
     */
    public SupplyChainValidationSummary validateSupplyChain(final EndorsementCredential ec,
                                                            final List<PlatformCredential> pcs,
                                                            final Device device,
                                                            final List<ComponentInfo> componentInfos)
            throws IOException {
        final PolicySettings policySettings = getPolicySettings();
        boolean acceptExpiredCerts = policySettings.isExpiredCertificateValidationEnabled();
        provisionSessionId = UUID.randomUUID();
        PlatformCredential baseCredential = null;
        SupplyChainValidation platformScv = null;
        SupplyChainValidation basePlatformScv = null;
        boolean chkDeltas = false;
        String pcErrorMessage = "";
        List<SupplyChainValidation> validations = new LinkedList<>();
        Map<PlatformCredential, SupplyChainValidation> deltaMapping = new HashMap<>();

        log.info("Beginning Supply Chain Validation...");

        // Validate the Endorsement Credential
        if (policySettings.isEcValidationEnabled()) {
            log.info("Beginning Endorsement Credential Validation...");
            validations.add(ValidationService.evaluateEndorsementCredentialStatus(ec,
                    this.caCredentialRepository, acceptExpiredCerts));
            // store the device with the credential
            if (ec != null) {
                ec.setDeviceId(device.getId());
                ec.setDeviceName(device.getDeviceInfo().getNetworkInfo().getHostname());
                this.certificateRepository.save(ec);
            }
        }

        // Validate Platform Credential signatures
        if (policySettings.isPcValidationEnabled()) {
            log.info("Beginning Platform Credential Validation...");
            // Ensure there are platform credentials to validate
            if (pcs == null || pcs.isEmpty()) {
                log.error("There were no Platform Credentials to validate.");
                pcErrorMessage = "Platform credential(s) missing\n";
            } else {
                for (PlatformCredential pc : pcs) {
                    KeyStore trustedCa = ValidationService.getCaChain(pc, caCredentialRepository);
                    platformScv = ValidationService.evaluatePlatformCredentialStatus(
                            pc, trustedCa, acceptExpiredCerts);

                    if (platformScv.getValidationResult() == AppraisalStatus.Status.FAIL) {
                        pcErrorMessage = String.format("%s%s%n", pcErrorMessage,
                                platformScv.getMessage());
                    }

                    // set the base credential
                    if (pc.isPlatformBase()) {
                        baseCredential = pc;
                        basePlatformScv = platformScv;
                    } else {
                        chkDeltas = true;
                        deltaMapping.put(pc, null);
                    }
                    pc.setEndorsementCredential(ec);
                    pc.setDeviceId(device.getId());
                    pc.setDeviceName(device.getDeviceInfo().getNetworkInfo().getHostname());
                    this.certificateRepository.save(pc);
                }

                // check that the delta certificates validity date is after
                // the base
                if (baseCredential != null) {
                    for (PlatformCredential pc : pcs) {
                        int result = baseCredential.getBeginValidity()
                                .compareTo(pc.getBeginValidity());
                        if (!pc.isPlatformBase() && (result > 0)) {
                            pcErrorMessage = String.format("%s%s%n", pcErrorMessage,
                                    "Delta Certificate's validity "
                                            + "date is not after Base");
                            break;
                        }
                    }
                } else {
                    // we don't have a base cert, fail
                    pcErrorMessage = String.format("%s%s%n", pcErrorMessage,
                            "Base Platform credential missing");
                }
            }

            if (pcErrorMessage.isEmpty()) {
                validations.add(platformScv);
            } else {
                List<ArchivableEntity> pcsList = (pcs == null) ? new ArrayList<>() : new ArrayList<>(pcs);
                validations.add(
                        new SupplyChainValidation(SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                                AppraisalStatus.Status.FAIL,
                                pcsList,
                                pcErrorMessage));
            }
        }

        // Validate Platform Credential attributes
        if (policySettings.isPcAttributeValidationEnabled() && pcErrorMessage.isEmpty()) {
            log.info("Beginning Platform Attributes Validation...");

            // Ensure there are platform credentials to validate
            SupplyChainValidation attributeScv = null;
            String attrErrorMessage = "";
            List<ArchivableEntity> achievableEntities = new ArrayList<>();
            // need to check if there are deltas, if not then just verify
            // components of the base
            if (baseCredential == null) {
                validations.add(ValidationService.buildValidationRecord(
                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                        AppraisalStatus.Status.FAIL,
                        "Base Platform credential missing. Cannot validate attributes",
                        null, Level.ERROR));
            } else {
                if (chkDeltas) {
                    // There are delta certificates, so the code need to build a new list of
                    // certificate components to then compare against the device component list
                    achievableEntities.addAll(basePlatformScv.getCertificatesUsed());

                    attributeScv = ValidationService.evaluateDeltaAttributesStatus(
                            device.getDeviceInfo(),
                            baseCredential, deltaMapping, certificateRepository,
                            componentResultRepository,
                            componentAttributeRepository,
                            componentInfos, provisionSessionId,
                            policySettings.isIgnoreRevisionEnabled(),
                            policySettings.isIgnorePcieVpdEnabled());
                    if (attributeScv.getValidationResult() == AppraisalStatus.Status.FAIL) {
                        attrErrorMessage = String.format("%s%s%n", attrErrorMessage,
                                attributeScv.getMessage());
                    }
                } else {
                    // validate attributes for a single base platform certificate
                    achievableEntities.add(baseCredential);
                    validations.remove(platformScv);
                    // if there are no deltas, just check base credential
                    platformScv = ValidationService.evaluatePCAttributesStatus(
                            baseCredential, device.getDeviceInfo(), ec,
                            certificateRepository, componentResultRepository,
                            componentAttributeRepository, componentInfos, provisionSessionId,
                            policySettings.isIgnoreRevisionEnabled(),
                            policySettings.isIgnorePcieVpdEnabled());
                    validations.add(new SupplyChainValidation(
                            SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                            platformScv.getValidationResult(), achievableEntities, platformScv.getMessage()));
                }

                updateComponentStatus(componentResultRepository
                        .findByCertificateSerialNumberAndBoardSerialNumber(
                                baseCredential.getSerialNumber().toString(),
                                baseCredential.getPlatformSerial()));
            }

            if (!attrErrorMessage.isEmpty()) {
                //combine platform and platform attributes
                validations.remove(platformScv);
                validations.add(new SupplyChainValidation(
                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                        attributeScv.getValidationResult(), achievableEntities, attributeScv.getMessage()));
            }
        }

        if (policySettings.isFirmwareValidationEnabled()) {
            log.info("Beginning Firmware Validation...");
            // may need to associated with device to pull the correct info
            // compare tpm quote with what is pulled from RIM associated file
            validations.add(ValidationService.evaluateFirmwareStatus(device, policySettings,
                    referenceManifestRepository, referenceDigestValueRepository,
                    caCredentialRepository));
        }

        log.info("The validation finished, summarizing...");
        // Generate validation summary, save it, and return it.
        SupplyChainValidationSummary summary =
                new SupplyChainValidationSummary(device, validations, provisionSessionId);
        try {
            supplyChainValidationSummaryRepository.save(summary);
        } catch (DBManagerException dbMEx) {
            log.error("Failed to save Supply Chain Summary");
        }

        return summary;
    }

    /**
     * A supplemental method that handles validating just the quote post main validation.
     *
     * @param device the associated device.
     * @return True if validation is successful, false otherwise.
     */
    public SupplyChainValidationSummary validateQuote(final Device device) {
        SupplyChainValidation quoteScv = null;
        SupplyChainValidationSummary summary = null;
        Level level = Level.ERROR;
        AppraisalStatus fwStatus = new AppraisalStatus(FAIL,
                "Unknown exception caught during quote validation.");
        SupportReferenceManifest sRim = null;
        EventLogMeasurements eventLog = null;

        final PolicySettings policySettings = getPolicySettings();

        // check if the policy is enabled
        if (policySettings.isFirmwareValidationEnabled()) {
            String deviceName = device.getDeviceInfo()
                    .getNetworkInfo().getHostname();
            try {
                List<SupportReferenceManifest> supportRims = referenceManifestRepository
                        .getSupportByManufacturerModel(
                                device.getDeviceInfo().getHardwareInfo().getManufacturer(),
                                device.getDeviceInfo().getHardwareInfo().getProductName());
                for (SupportReferenceManifest support : supportRims) {
                    if (support.isBaseSupport()) {
                        sRim = support;
                    }
                }

                if (sRim == null) {
                    fwStatus = new AppraisalStatus(FAIL,
                            String.format("Firmware Quote validation failed: "
                                            + "No associated Support RIM file "
                                            + "could be found for %s",
                                    deviceName));
                } else {
                    ReferenceManifest manifest = referenceManifestRepository
                            .findByHexDecHashAndRimTypeUnarchived(sRim.getEventLogHash(),
                                    ReferenceManifest.MEASUREMENT_RIM);
                    if (manifest instanceof EventLogMeasurements) {
                        eventLog = (EventLogMeasurements) manifest;
                    }
                }
                if (eventLog == null) {
                    fwStatus = new AppraisalStatus(FAIL,
                            String.format("Firmware Quote validation failed: "
                                            + "No associated Client Log file "
                                            + "could be found for %s",
                                    deviceName));
                } else {
                    String[] storedPcrs = eventLog.getExpectedPCRList();
                    PcrValidator pcrValidator = new PcrValidator(sRim.getExpectedPCRList());
                    // grab the quote
                    byte[] tpmQuoteHash = device.getDeviceInfo().getTpmInfo().getTpmQuoteHash();
                    if (pcrValidator.validateQuote(tpmQuoteHash, storedPcrs, policySettings)) {
                        level = Level.INFO;
                        fwStatus = new AppraisalStatus(PASS,
                                SupplyChainCredentialValidator.FIRMWARE_VALID);
                        fwStatus.setMessage("Firmware validation of TPM Quote successful.");
                    } else {
                        fwStatus.setMessage("Firmware validation of TPM Quote failed."
                                + "\nPCR hash and Quote hash do not match.");
                    }
                    eventLog.setOverallValidationResult(fwStatus.getAppStatus());
                    this.referenceManifestRepository.save(eventLog);
                }
            } catch (Exception ex) {
                log.error(ex);
            }

            BaseReferenceManifest bRim = null;
            if (sRim != null && sRim.getAssociatedRim() != null) {
                Optional<ReferenceManifest> oRim =
                        referenceManifestRepository.findById(sRim.getAssociatedRim());
                if (oRim.isPresent()) {
                    ReferenceManifest rim = oRim.get();
                    if (rim instanceof BaseReferenceManifest) {
                        bRim = (BaseReferenceManifest) rim;
                    }
                }
            }

            quoteScv = ValidationService.buildValidationRecord(SupplyChainValidation
                            .ValidationType.FIRMWARE,
                    fwStatus.getAppStatus(), fwStatus.getMessage(), bRim != null ? bRim : eventLog, level);

            // Generate validation summary, save it, and return it.
            List<SupplyChainValidation> validations = new ArrayList<>();
            Optional<SupplyChainValidationSummary> previousOpt
                    //= this.supplyChainValidationSummaryRepository.findByDevice(deviceName);
                    //= this.supplyChainValidationSummaryRepository.findByDevice(device);
                    = this.supplyChainValidationSummaryRepository.findById(
                    UUID.fromString(device.getSummaryId()));
            if (previousOpt.isPresent()) {
                SupplyChainValidationSummary previous = previousOpt.get();
                for (SupplyChainValidation scv : previous.getValidations()) {
                    if (scv.getValidationType() != SupplyChainValidation.ValidationType.FIRMWARE) {
                        validations.add(ValidationService.buildValidationRecord(scv.getValidationType(),
                                scv.getValidationResult(), scv.getMessage(),
                                scv.getCertificatesUsed().get(0), Level.INFO));
                    }
                }
                validations.add(quoteScv);
                previous.archive();
                supplyChainValidationSummaryRepository.save(previous);
                summary = new SupplyChainValidationSummary(device, validations);

                // try removing the supply chain validation as well and resaving that
                try {
                    supplyChainValidationSummaryRepository.save(summary);
                } catch (DBManagerException dbEx) {
                    log.error("Failed to save Supply Chain Summary", dbEx);
                }
            }
        }

        return summary;
    }

    /**
     * Helper function that retrieves the default policy settings from the database.
     *
     * @return The default Supply Chain Policy Settings
     */
    private PolicySettings getPolicySettings() {
        PolicySettings defaultSettings = this.policyRepository.findByName("Default");

        if (defaultSettings == null) {
            defaultSettings = new PolicySettings("Default",
                    "Settings are configured for no validation flags set.");
        }
        return defaultSettings;
    }

    /**
     * If the platform attributes policy is enabled, this method updates the matched
     * status for the component result.  This is done so that the details page for the
     * platform certificate highlights the title card red.
     *
     * @param componentResults list of associated component results
     */
    private void updateComponentStatus(final List<ComponentResult> componentResults) {
        List<ComponentAttributeResult> componentAttributeResults = componentAttributeRepository
                .findByProvisionSessionId(provisionSessionId);
        List<UUID> componentIdList = new ArrayList<>();

        for (ComponentAttributeResult componentAttributeResult : componentAttributeResults) {
            componentIdList.add(componentAttributeResult.getComponentId());
        }

        for (ComponentResult componentResult : componentResults) {
            componentResult.setFailedValidation(componentIdList.contains(componentResult.getId()));
            componentResultRepository.save(componentResult);
        }
    }
}
