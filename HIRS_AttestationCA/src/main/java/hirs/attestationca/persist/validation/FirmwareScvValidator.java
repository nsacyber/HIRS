package hirs.attestationca.persist.validation;

import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.EventLogMeasurements;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.exceptions.SupplyChainValidatorException;
import hirs.utils.SwidResource;
import hirs.utils.rim.ReferenceManifestValidator;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static hirs.attestationca.persist.enums.AppraisalStatus.Status.ERROR;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.FAIL;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.PASS;

/**
 * Validator class responsible for validating firmware-related information.
 */
@Log4j2
public class FirmwareScvValidator extends SupplyChainCredentialValidator {

    private ReferenceManifest supportReferenceManifest;

    /**
     * Validates the firmware and returns an {@link AppraisalStatus}.
     *
     * @param device                         device
     * @param policySettings                 policy settings
     * @param referenceManifestRepository    reference manifest repository
     * @param referenceDigestValueRepository reference digest value repository
     * @param caCredentialRepository         CA Credential repository
     * @return an appraisal status
     */
    @SuppressWarnings("methodlength")
    public static AppraisalStatus validateFirmware(
            final Device device, final PolicySettings policySettings,
            final ReferenceManifestRepository referenceManifestRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository,
            final CACredentialRepository caCredentialRepository) {
        boolean passed = true;
        AppraisalStatus fwStatus = null;
        String hostName = device.getDeviceInfo().getNetworkInfo().getHostname();
        BaseReferenceManifest baseReferenceManifest = null;
        EventLogMeasurements measurement = null;
        log.info("Validating firmware...");

        // This block was looking for a base RIM matching the device name
        // The base rim might not have a device name associated with it- i.e. if it's uploaded to the ACA
        // prior to provisioning In this case, try to look up the event log associated with the device,
        // then get the base rim associated by event log hash
        List<ReferenceManifest> deviceRims = referenceManifestRepository.findByDeviceName(hostName);
        for (ReferenceManifest deviceRim : deviceRims) {
            if (deviceRim instanceof BaseReferenceManifest && !deviceRim.isSwidSupplemental()
                    && !deviceRim.isSwidPatch()) {
                baseReferenceManifest = (BaseReferenceManifest) deviceRim;
            }

            if (deviceRim instanceof EventLogMeasurements && !deviceRim.isArchived()) {
                measurement = (EventLogMeasurements) deviceRim;
            }
        }

        // Attempt to get an event log from the database matching the expected hash
        if (baseReferenceManifest == null && measurement != null) {
            baseReferenceManifest =
                    (BaseReferenceManifest) referenceManifestRepository.findByEventLogHashAndRimType(
                            measurement.getHexDecHash(), ReferenceManifest.BASE_RIM);
        }

        String failedString = "";
        if (baseReferenceManifest == null) {
            failedString = "Base Reference Integrity Manifest not found for " + hostName + "\n";
            passed = false;
            fwStatus = new AppraisalStatus(ERROR, failedString);
        } else if (measurement == null) {
            measurement = (EventLogMeasurements) referenceManifestRepository
                    .findByHexDecHashAndRimTypeUnarchived(baseReferenceManifest.getEventLogHash(),
                            ReferenceManifest.MEASUREMENT_RIM);

            if (measurement == null) {
                measurement = referenceManifestRepository.byMeasurementDeviceNameUnarchived(
                        baseReferenceManifest.getDeviceName());
            }
        }

        if (measurement == null) {
            failedString += "Bios measurement not found for " + hostName;
            passed = false;
            fwStatus = new AppraisalStatus(ERROR, failedString);
        }

        // Reject another device's log before updating its validation result.
        if (passed && (measurement.getDeviceName() == null || !measurement.getDeviceName().equals(hostName))) {
            return new AppraisalStatus(FAIL,
                    "Firmware validation failed: measurement log is not associated with " + hostName,
                    ReferenceManifest.MEASUREMENT_RIM);
        }

        if (passed) {
            // Keep resolved RIM state isolated to this validation.
            FirmwareScvValidator validator = new FirmwareScvValidator();
            AppraisalStatus rimSignatureStatus = validator.validateRimSignature(baseReferenceManifest,
                    caCredentialRepository, referenceManifestRepository);
            fwStatus = rimSignatureStatus;
            if (rimSignatureStatus.getAppStatus() == PASS) {
                AppraisalStatus pcrStatus = validator.validatePcrValues(device, hostName,
                        measurement, referenceDigestValueRepository, policySettings);
                fwStatus = pcrStatus;
                if (pcrStatus.getAppStatus() == PASS) {
                    EventLogMeasurements eventLog = measurement;
                    eventLog.setOverallValidationResult(PASS);
                    referenceManifestRepository.save(eventLog);
                    fwStatus = new AppraisalStatus(PASS, SupplyChainCredentialValidator.FIRMWARE_VALID);
                } else {
                    failedString = pcrStatus.getMessage();
                    log.warn("PCR value validation failed: " + failedString);
                    passed = false;
                }
            } else {
                failedString = rimSignatureStatus.getMessage();
                log.warn("RIM signature validation failed: " + failedString);
                passed = false;
            }
        }
        if (!passed) {
            if (measurement != null) {
                measurement.setOverallValidationResult(fwStatus.getAppStatus());
                referenceManifestRepository.save(measurement);
            }
        }
        return fwStatus;
    }

    private AppraisalStatus validateRimSignature(
            final BaseReferenceManifest baseReferenceManifest,
            final CACredentialRepository caCredentialRepository,
            final ReferenceManifestRepository referenceManifestRepository) {
        List<SwidResource> resources =
                baseReferenceManifest.getFileResources();
        log.info("Validating RIM signature...");

        // verify signatures
        ReferenceManifestValidator referenceManifestValidator =
                new ReferenceManifestValidator();
        try {
            referenceManifestValidator.setRim(baseReferenceManifest.getRimBytes());
        } catch (IOException e) {
            return new AppraisalStatus(FAIL,
                    "RIM signature validation failed: can't set base RIM.");
        }

        //Validate signing cert
        List<CertificateAuthorityCredential> allCerts = caCredentialRepository.findAll();
        boolean trustedSignatureFound = false;
        for (CertificateAuthorityCredential cert : allCerts) {
            KeyStore keyStore;
            Set<CertificateAuthorityCredential> set = ValidationService.getCaChainRec(cert,
                    Collections.emptySet(),
                    caCredentialRepository);
            try {
                keyStore = ValidationService.caCertSetToKeystore(set);
            } catch (IOException | KeyStoreException e) {
                log.error("Error building CA chain for "
                        + Arrays.toString(cert.getSubjectKeyIdentifier()) + ": "
                        + e.getMessage());
                continue;
            }

            ArrayList<X509Certificate> certs = new ArrayList<>(set.size());
            boolean chainReadable = true;
            for (CertificateAuthorityCredential cac : set) {
                try {
                    certs.add(cac.getX509Certificate());
                } catch (IOException e) {
                    log.error(
                            "Error building CA chain for "
                                    + Arrays.toString(cert.getSubjectKeyIdentifier()) + ": "
                                    + e.getMessage());
                    chainReadable = false;
                    break;
                }
            }
            if (!chainReadable) {
                continue;
            }
            referenceManifestValidator.setTrustStore(certs);
            try {
                X509Certificate candidateCertificate = cert.getX509Certificate();
                if (referenceManifestValidator.validateXmlSignature(
                        candidateCertificate.getPublicKey(), cert.getSubjectKeyIdString())) {
                    try {
                        if (!SupplyChainCredentialValidator.verifyCertificate(
                                candidateCertificate, keyStore)) {
                            return new AppraisalStatus(FAIL,
                                    "RIM signature validation failed: invalid certificate path.");
                        }
                    } catch (SupplyChainValidatorException scvEx) {
                        log.error("Error validating cert against keystore: " + scvEx.getMessage());
                        return new AppraisalStatus(FAIL,
                                "RIM signature validation failed: invalid certificate path.");
                    }
                    trustedSignatureFound = true;
                    break;
                }
            } catch (IOException ioEx) {
                log.error("Error getting X509 cert from manager: " + ioEx.getMessage());
            }
        }

        if (!trustedSignatureFound) {
            return new AppraisalStatus(FAIL,
                    "RIM signature validation failed: signing cert not found.");
        }

        for (SwidResource swidRes : resources) {
            supportReferenceManifest = referenceManifestRepository.findByHexDecHashAndRimType(
                    swidRes.getHashValue(), ReferenceManifest.SUPPORT_RIM);
            if (supportReferenceManifest == null) {
                return new AppraisalStatus(FAIL,
                        "Support Reference Integrity Manifest can not be found: " + swidRes.getHashValue());
            }
            referenceManifestValidator.validateSupportRimHash(
                    supportReferenceManifest.getRimBytes(), swidRes.getHashValue());
            if (!referenceManifestValidator.isSupportRimValid()) {
                return new AppraisalStatus(FAIL,
                        "RIM signature validation failed: Hash validation failed for Support RIM.");
            }
        }

        if (supportReferenceManifest == null) {
            return new AppraisalStatus(FAIL,
                    "Support Reference Integrity Manifest can not be found");
        }

        return new AppraisalStatus(PASS, "RIM signature valid.");
    }

    private AppraisalStatus validatePcrValues(
            final Device device,
            final String hostName,
            final EventLogMeasurements measurement,
            final ReferenceDigestValueRepository referenceDigestValueRepository,
            final PolicySettings policySettings) {

        String[] baseline;
        TCGEventLog logProcessor;
        AppraisalStatus pcrAppraisalStatus = new AppraisalStatus(PASS, "Firmware event digests validated.");
        log.info("Validating PCR values...");

        try {
            logProcessor = new TCGEventLog(supportReferenceManifest.getRimBytes());
            baseline = logProcessor.getExpectedPCRValues();
        } catch (IOException | RuntimeException e) {
            log.error("Unable to parse support RIM event log for " + hostName, e);
            return new AppraisalStatus(ERROR, "Firmware validation failed: support RIM event log could not be parsed.");
        }

        PcrValidator pcrValidator = new PcrValidator(baseline);

        if (baseline.length > 0) {
            String pcrContent = "";
            pcrContent = new String(device.getDeviceInfo().getTpmInfo().getPcrValues(),
                    StandardCharsets.UTF_8);

            if (pcrContent.isEmpty()) {
                pcrAppraisalStatus = new AppraisalStatus(FAIL,
                        "Firmware validation failed: Client did not "
                                + "provide pcr values.");
                log.warn(String.format(
                        "Firmware validation failed: Client (%s) did not "
                                + "provide pcr values.", device.getName()));
            } else {
                // Check measured event digests against the RIM database.
                TCGEventLog tcgMeasurementLog;
                LinkedList<TpmPcrEvent> tpmPcrEvents = new LinkedList<>();
                List<ReferenceDigestValue> eventValue;
                HashMap<String, ReferenceDigestValue> eventValueMap = new HashMap<>();
                try {
                    tcgMeasurementLog = new TCGEventLog(measurement.getRimBytes());
                } catch (IOException | RuntimeException e) {
                    log.error("Unable to parse measurement event log for " + hostName, e);
                    return new AppraisalStatus(ERROR,
                            "Firmware validation failed: measurement event log could not be parsed.",
                            ReferenceManifest.MEASUREMENT_RIM);
                }
                eventValue = referenceDigestValueRepository.findByManufacturerAndModel(
                        device.getDeviceInfo().getHardwareInfo().getManufacturer(),
                        device.getDeviceInfo().getHardwareInfo().getProductName());
                for (ReferenceDigestValue rdv : eventValue) {
                    eventValueMap.put(rdv.getDigestValue(), rdv);
                }

                tpmPcrEvents.addAll(pcrValidator.validateTpmEvents(
                        tcgMeasurementLog, eventValueMap, policySettings));

                if (!tpmPcrEvents.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("%d digest(s) were not found:%n",
                            tpmPcrEvents.size()));
                    for (TpmPcrEvent tpe : tpmPcrEvents) {
                        sb.append(String.format("PCR Index %d - %s%n",
                                tpe.getPcrIndex(),
                                tpe.getEventTypeStr()));
                    }
                    if (pcrAppraisalStatus.getAppStatus().equals(FAIL)) {
                        pcrAppraisalStatus = new AppraisalStatus(FAIL, String.format("%s%n%s",
                                pcrAppraisalStatus.getMessage(), sb));
                    } else {
                        pcrAppraisalStatus = new AppraisalStatus(FAIL,
                                sb.toString(), ReferenceManifest.MEASUREMENT_RIM);
                    }
                }
            }
        } else {
            pcrAppraisalStatus = new AppraisalStatus(FAIL, "The RIM baseline could not be found.");
        }

        return pcrAppraisalStatus;
    }

    private static void logAndReportError(final AppraisalStatus status, final String errorString) {
        status.setMessage(errorString);
        log.error(errorString);
    }
}
