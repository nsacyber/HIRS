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
import hirs.attestationca.persist.service.ValidationService;
import hirs.utils.SwidResource;
import hirs.utils.rim.ReferenceManifestValidator;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static hirs.attestationca.persist.enums.AppraisalStatus.Status.FAIL;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.PASS;

@Log4j2
public class FirmwareScvValidator extends SupplyChainCredentialValidator {

    private static PcrValidator pcrValidator;
    private static ReferenceManifest supportReferenceManifest;

    /**
     * @param device                         device
     * @param policySettings                 policy settings
     * @param referenceManifestRepository    reference manifest repository
     * @param referenceDigestValueRepository reference digest value repository
     * @param caCredentialRepository         CA Credential repository
     * @return an appraisal status
     */
    public static AppraisalStatus validateFirmware(
            final Device device,
            final PolicySettings policySettings,
            final ReferenceManifestRepository referenceManifestRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository,
            final CACredentialRepository caCredentialRepository) {
        final String hostName = device.getDeviceInfo().getNetworkInfo().getHostname();
        boolean passed = true;
        AppraisalStatus fwStatus = null;
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
        }

        if (passed) {
            AppraisalStatus rimSignatureStatus = validateRimSignature(baseReferenceManifest,
                    caCredentialRepository, referenceManifestRepository);
            fwStatus = rimSignatureStatus;
            if (rimSignatureStatus.getAppStatus() == PASS) {
                AppraisalStatus pcrStatus = validatePcrValues(device, hostName, baseReferenceManifest,
                        measurement, referenceDigestValueRepository, policySettings);
                fwStatus = pcrStatus;
                if (pcrStatus.getAppStatus() == PASS) {
                    measurement.setOverallValidationResult(PASS);
                    referenceManifestRepository.save(measurement);
                    fwStatus = new AppraisalStatus(PASS, SupplyChainCredentialValidator.FIRMWARE_VALID);
                } else {
                    failedString = pcrStatus.getMessage();
                    log.warn("PCR value validation failed: {}", failedString);
                    passed = false;
                }
            } else {
                failedString = rimSignatureStatus.getMessage();
                log.warn("RIM signature validation failed: {}", failedString);
                passed = false;
            }
        }

        if (!passed && measurement != null) {
            fwStatus = new AppraisalStatus(FAIL, failedString);
            measurement.setOverallValidationResult(fwStatus.getAppStatus());
            referenceManifestRepository.save(measurement);
        }

        return fwStatus;
    }

    private static AppraisalStatus validateRimSignature(
            final BaseReferenceManifest baseReferenceManifest,
            final CACredentialRepository caCredentialRepository,
            final ReferenceManifestRepository referenceManifestRepository) {
        List<SwidResource> resources =
                baseReferenceManifest.getFileResources();
        AppraisalStatus rimSignatureStatus = new AppraisalStatus(PASS, "RIM signature valid.");
        boolean passed = true;
        log.info("Validating RIM signature...");

        // verify signatures
        ReferenceManifestValidator referenceManifestValidator =
                new ReferenceManifestValidator();
        referenceManifestValidator.setRim(baseReferenceManifest.getRimBytes());

        //Validate signing cert
        List<CertificateAuthorityCredential> allCerts = caCredentialRepository.findAll();
        CertificateAuthorityCredential signingCert = null;
        for (CertificateAuthorityCredential cert : allCerts) {
            signingCert = cert;
            KeyStore keyStore = null;
            Set<CertificateAuthorityCredential> set = ValidationService.getCaChainRec(signingCert,
                    Collections.emptySet(),
                    caCredentialRepository);
            try {
                keyStore = ValidationService.caCertSetToKeystore(set);
            } catch (Exception e) {
                log.error("Error building CA chain for {}: {}",
                        signingCert.getSubjectKeyIdentifier(),
                        e.getMessage());
            }

            ArrayList<X509Certificate> certs = new ArrayList<>(set.size());
            for (CertificateAuthorityCredential cac : set) {
                try {
                    certs.add(cac.getX509Certificate());
                } catch (IOException e) {
                    log.error("Error building CA chain for {}: {}",
                            signingCert.getSubjectKeyIdentifier(),
                            e.getMessage());
                }
            }
            referenceManifestValidator.setTrustStore(certs);
            try {
                if (referenceManifestValidator.validateXmlSignature(
                        signingCert.getX509Certificate().getPublicKey(),
                        signingCert.getSubjectKeyIdString())) {
                    try {
                        if (!SupplyChainCredentialValidator.verifyCertificate(
                                signingCert.getX509Certificate(), keyStore)) {
                            passed = false;
                            rimSignatureStatus = new AppraisalStatus(FAIL,
                                    "RIM signature validation failed: invalid certificate path.");
                        }
                    } catch (IOException ioEx) {
                        log.error("Error getting X509 cert from manager: {}", ioEx.getMessage());
                    } catch (SupplyChainValidatorException scvEx) {
                        log.error("Error validating cert against keystore: {}", scvEx.getMessage());
                        rimSignatureStatus = new AppraisalStatus(FAIL,
                                "RIM signature validation failed: invalid certificate path.");
                    }
                    break;
                }
            } catch (IOException ioEx) {
                log.error("Error getting X509 cert from manager: {}", ioEx.getMessage());
            }
        }

        for (SwidResource swidRes : resources) {
            supportReferenceManifest = referenceManifestRepository.findByHexDecHashAndRimType(
                    swidRes.getHashValue(), ReferenceManifest.SUPPORT_RIM);
            if (supportReferenceManifest != null) {
                // Removed the filename check from this if statement
                referenceManifestValidator.validateSupportRimHash(
                        supportReferenceManifest.getRimBytes(), swidRes.getHashValue());
            }
        }

        if (passed && signingCert == null) {
            passed = false;
            rimSignatureStatus = new AppraisalStatus(FAIL,
                    "RIM signature validation failed: signing cert not found.");
        }

        if (passed && supportReferenceManifest == null) {
            rimSignatureStatus = new AppraisalStatus(FAIL,
                    "Support Reference Integrity Manifest can not be found");
            passed = false;
        }

        if (passed && !referenceManifestValidator.isSignatureValid()) {
            passed = false;
            String validationErrorMessage = referenceManifestValidator.getValidationErrorMessage();
            if (!validationErrorMessage.isEmpty()) {
                rimSignatureStatus = new AppraisalStatus(FAIL, validationErrorMessage);
            } else {
                rimSignatureStatus = new AppraisalStatus(FAIL, "Base RIM signature invalid.");
            }
        }

        if (passed && !referenceManifestValidator.isSupportRimValid()) {
            rimSignatureStatus = new AppraisalStatus(FAIL,
                    "RIM signature validation failed: Hash validation "
                            + "failed for Support RIM.");
        }

        return rimSignatureStatus;
    }

    private static AppraisalStatus validatePcrValues(
            final Device device,
            final String hostName,
            final ReferenceManifest baseReferenceManifest,
            final EventLogMeasurements measurement,
            final ReferenceDigestValueRepository referenceDigestValueRepository,
            final PolicySettings policySettings) {

        String[] baseline = new String[Integer.SIZE];
        TCGEventLog logProcessor;
        AppraisalStatus pcrAppraisalStatus = new AppraisalStatus(PASS, "PCR values validated.");
        log.info("Validating PCR values...");

        try {
            logProcessor = new TCGEventLog(supportReferenceManifest.getRimBytes());
            baseline = logProcessor.getExpectedPCRValues();
        } catch (CertificateException | NoSuchAlgorithmException | IOException exception) {
            log.error(exception);
        }

        // part 1 of firmware validation check: PCR baseline match
        pcrValidator = new PcrValidator(baseline);

        if (baseline.length > 0) {
            String pcrContent = "";
            pcrContent = new String(device.getDeviceInfo().getTpmInfo().getPcrValues(),
                    StandardCharsets.UTF_8);

            if (pcrContent.isEmpty()) {
                pcrAppraisalStatus = new AppraisalStatus(FAIL,
                        "Firmware validation failed: Client did not "
                                + "provide pcr values.");
                log.warn("Firmware validation failed: Client ({}) did not "
                        + "provide pcr values.", device.getName());
            } else {
                // we have a full set of PCR values
                //int algorithmLength = baseline[0].length();
                //String[] storedPcrs = buildStoredPcrs(pcrContent, algorithmLength);
                //pcrPolicy.validatePcrs(storedPcrs);

                // part 2 of firmware validation check: bios measurements
                // vs baseline tcg event log
                // find the measurement
                TCGEventLog tcgMeasurementLog;
                LinkedList<TpmPcrEvent> tpmPcrEvents = new LinkedList<>();
                List<ReferenceDigestValue> eventValue;
                HashMap<String, ReferenceDigestValue> eventValueMap = new HashMap<>();
                try {
                    if (measurement.getDeviceName().equals(hostName)) {
                        tcgMeasurementLog = new TCGEventLog(measurement.getRimBytes());
                        eventValue = referenceDigestValueRepository
                                .findValuesByBaseRimId(baseReferenceManifest.getId());
                        for (ReferenceDigestValue rdv : eventValue) {
                            eventValueMap.put(rdv.getDigestValue(), rdv);
                        }

                        tpmPcrEvents.addAll(pcrValidator.validateTpmEvents(
                                tcgMeasurementLog, eventValueMap, policySettings));
                    }
                } catch (NoSuchAlgorithmException | CertificateException | IOException exception) {
                    log.error(exception);
                }

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
}
