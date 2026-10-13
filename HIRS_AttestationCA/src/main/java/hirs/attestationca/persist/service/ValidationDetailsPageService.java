package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.manager.SupplyChainValidationSummaryRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.EventLogMeasurements;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.enums.AppraisalStatus;
import jdk.jfr.Event;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service class that encapsulates the business logic for displaying Supply Chain Validation Summary data.
 */
@Log4j2
@Service
public class ValidationDetailsPageService {
    private final SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository;
    private final ReferenceManifestRepository referenceManifestRepository;
    private final CertificateRepository certificateRepository;

    public ValidationDetailsPageService(SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository,
                                        ReferenceManifestRepository referenceManifestRepository,
                                        CertificateRepository certificateRepository) {
        this.supplyChainValidationSummaryRepository = supplyChainValidationSummaryRepository;
        this.referenceManifestRepository = referenceManifestRepository;
        this.certificateRepository = certificateRepository;
    }

    /**
     * This method builds the data map of all validation details to return to the controller.
     *
     * @param uuid SupplyChainValidationSummary id
     * @return Map of parsed details
     */
    public Map<String, Object> getValidationDetails(final UUID uuid) {
        HashMap<String, Object> data = new HashMap<>();
        SupplyChainValidationSummary summary =
                supplyChainValidationSummaryRepository.findSupplyChainValidationSummaryById(uuid);
        data.put("reportTime", summary.getCreateTime());
        data.putAll(parseValidationsAndCerts(summary));
        data.putAll(parseDeviceDetails(summary.getDevice()));
        return data;
    }

    private Map<String, Object> parseValidationsAndCerts(final SupplyChainValidationSummary summary) {
        HashMap<String, Object> data = new HashMap<>();

        Set<SupplyChainValidation> validations = summary.getValidations();
        for(SupplyChainValidation validation : validations) {
            String keyPrefix = "";
            String provisionSessionId;
            if(summary.getProvisionSessionId() == null) {
                provisionSessionId = "";
            } else {
                provisionSessionId = summary.getProvisionSessionId().toString();
            }

            switch(validation.getValidationType()) {
                case SupplyChainValidation.ValidationType.ENDORSEMENT_CERTIFICATE:
                    keyPrefix = "endorsement";
                    data.putAll(parseEndorsementPlatformValidationDetails(validation,
                            keyPrefix, provisionSessionId));
                    break;
                case SupplyChainValidation.ValidationType.PLATFORM_CERTIFICATE:
                    keyPrefix = "platform";
                    data.putAll(parseEndorsementPlatformValidationDetails(validation,
                            keyPrefix, provisionSessionId));
                    break;
                case SupplyChainValidation.ValidationType.FIRMWARE:
                    keyPrefix = "firmware";
                    data.putAll(parseFirmwareValidationDetails(validation,
                            summary.getCreateTime().toString()));
                    break;
                default:
                    log.warn("Encountered unknown validation type.");
            }

            //key of the form, "is(Endorsement|Platform|Firmware)Valid"
            String key = "is" + keyPrefix.substring(0, 1).toUpperCase() + keyPrefix.substring(1);
            if (validation.getValidationResult().toString().equals("PASS")) {
               data.put(key + "Valid", true);
            } else {
               data.put(key + "Valid", false);
            }
        }

        return data;
    }

    /**
     * This method parses details specific to tpm and hardware validation.
     *
     * @param validation the validation object to parse
     * @param keyPrefix "endorsement" or "platform"
     * @param provisionSessionId session id from backend
     * @return mapping of validation details
     */
    private Map<String, Object> parseEndorsementPlatformValidationDetails(SupplyChainValidation validation,
                                                                          String keyPrefix,
                                                                          String provisionSessionId) {
        HashMap<String, Object> data = new HashMap<>();

        if (!validation.getCertificatesUsed().isEmpty()) {
            data.put(keyPrefix + "CertificateId",
                    validation.getCertificatesUsed().getFirst().getId());
        }
        data.put("provisionSessionId", provisionSessionId);//is this needed?

        return data;
    }

    /**
     * This method parses details specific to firmware validation.
     *
     * @param fwValidation the validation object to parse
     * @return mapping of validation details
     */
    private Map<String, Object> parseFirmwareValidationDetails(SupplyChainValidation fwValidation, String createTime) {
        HashMap<String, Object> data = new HashMap<>();
        BaseReferenceManifest baseRim =
                (BaseReferenceManifest) referenceManifestRepository.findByTypeBeforeTime(
                                createTime, ReferenceManifest.BASE_RIM);
        SupportReferenceManifest supportRim =
                (SupportReferenceManifest) referenceManifestRepository.findByTypeBeforeTime(
                                createTime, ReferenceManifest.SUPPORT_RIM);
        EventLogMeasurements eventLogMeasurements =
                (EventLogMeasurements) referenceManifestRepository.findByTypeBeforeTime(
                                createTime, ReferenceManifest.MEASUREMENT_RIM);
        data.put("baseRim", baseRim);
        data.put("supportRim", supportRim);
        data.put("eventLog", eventLogMeasurements);
        data.put("validationMessage", fwValidation.getMessage());

        return data;
    }

    private Map<String, Object> parseDeviceDetails(Device device) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("deviceName", device.getName());

        return data;
    }

}
