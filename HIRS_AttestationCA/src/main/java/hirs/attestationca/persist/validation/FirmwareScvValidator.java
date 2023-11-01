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
import hirs.utils.rim.ReferenceManifestValidator;
import hirs.utils.SwidResource;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static hirs.attestationca.persist.enums.AppraisalStatus.Status.FAIL;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.PASS;

@Log4j2
public class FirmwareScvValidator extends SupplyChainCredentialValidator {

    private static PcrValidator pcrValidator;

    @SuppressWarnings("methodlength")
    public static AppraisalStatus validateFirmware(
            final Device device, final PolicySettings policySettings,
            final ReferenceManifestRepository referenceManifestRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository,
            final CACredentialRepository caCredentialRepository) {
        boolean passed = true;
        String[] baseline = new String[Integer.SIZE];
        AppraisalStatus fwStatus = null;
        String hostName = device.getDeviceInfo().getNetworkInfo().getHostname();
        String manufacturer = device.getDeviceInfo()
                .getHardwareInfo().getManufacturer();
        ReferenceManifest validationObject;
        List<BaseReferenceManifest> baseReferenceManifests = null;
        BaseReferenceManifest baseReferenceManifest = null;
        ReferenceManifest supportReferenceManifest = null;
        EventLogMeasurements measurement = null;

        baseReferenceManifests = referenceManifestRepository.findAllBaseRims();

        for (BaseReferenceManifest bRim : baseReferenceManifests) {
            if (bRim.getDeviceName().equals(hostName)
                    && !bRim.isSwidSupplemental() && !bRim.isSwidPatch()) {
                baseReferenceManifest = bRim;
            }
        }

        String failedString = "";
        if (baseReferenceManifest == null) {
            failedString = "Base Reference Integrity Manifest\n";
            passed = false;
        } else {
            measurement = (EventLogMeasurements) referenceManifestRepository.findByHexDecHash(
                    baseReferenceManifest.getEventLogHash());

            if (measurement == null) {
                measurement = referenceManifestRepository.byMeasurementDeviceName(
                        baseReferenceManifest.getDeviceName());
            }
        }

        if (measurement == null) {
            failedString += "Bios measurement";
            passed = false;
        }
        validationObject = measurement;

        if (passed) {
            List<SwidResource> resources =
                    ((BaseReferenceManifest) baseReferenceManifest).getFileResources();
            fwStatus = new AppraisalStatus(PASS,
                    SupplyChainCredentialValidator.FIRMWARE_VALID);

            // verify signatures
            ReferenceManifestValidator referenceManifestValidator =
                    new ReferenceManifestValidator();
            referenceManifestValidator.setRim(baseReferenceManifest.getRimBytes());

            //Validate signing cert
            List<CertificateAuthorityCredential> allCerts = caCredentialRepository.findAll();
            CertificateAuthorityCredential signingCert = null;
            for (CertificateAuthorityCredential cert : allCerts) {
                signingCert = cert;
                KeyStore keyStore = ValidationService.getCaChain(signingCert,
                        caCredentialRepository);
                try {
                    if (referenceManifestValidator.validateXmlSignature(signingCert.getX509Certificate().getPublicKey(),
                        signingCert.getSubjectKeyIdString(), signingCert.getEncodedPublicKey())) {
                        try {
                            if (!SupplyChainCredentialValidator.verifyCertificate(
                                signingCert.getX509Certificate(), keyStore)) {
                                passed = false;
                                fwStatus = new AppraisalStatus(FAIL,
                                    "Firmware validation failed: invalid certificate path.");
                                validationObject = baseReferenceManifest;
                            }
                        } catch (IOException ioEx) {
                            log.error("Error getting X509 cert from manager: " + ioEx.getMessage());
                        } catch (SupplyChainValidatorException scvEx) {
                            log.error("Error validating cert against keystore: " + scvEx.getMessage());
                            fwStatus = new AppraisalStatus(FAIL,
                                    "Firmware validation failed: invalid certificate path.");
                        }
                        break;
                    }
                } catch (IOException ioEx) {
                    log.error("Error getting X509 cert from manager: " + ioEx.getMessage());
                }
            }

            for (SwidResource swidRes : resources) {
                supportReferenceManifest = referenceManifestRepository.findByHexDecHash(
                        swidRes.getHashValue());
                if (supportReferenceManifest != null) {
                    // Removed the filename check from this if statement
                    referenceManifestValidator.validateSupportRimHash(
                            supportReferenceManifest.getRimBytes(), swidRes.getHashValue());
                }
            }

            if (passed && signingCert == null) {
                passed = false;
                fwStatus = new AppraisalStatus(FAIL,
                        "Firmware validation failed: signing cert not found.");
            }

            if (passed && supportReferenceManifest == null) {
                fwStatus = new AppraisalStatus(FAIL,
                        "Support Reference Integrity Manifest can not be found");
                passed = false;
            }

            if (passed && !referenceManifestValidator.isSignatureValid()) {
                passed = false;
                fwStatus = new AppraisalStatus(FAIL,
                        "Firmware validation failed: Signature validation "
                                + "failed for Base RIM.");
            }

            if (passed && !referenceManifestValidator.isSupportRimValid()) {
                passed = false;
                fwStatus = new AppraisalStatus(FAIL,
                        "Firmware validation failed: Hash validation "
                                + "failed for Support RIM.");
            }

            if (passed) {
                TCGEventLog logProcessor;
                try {
                    logProcessor = new TCGEventLog(supportReferenceManifest.getRimBytes());
                    baseline = logProcessor.getExpectedPCRValues();
                } catch (CertificateException cEx) {
                    log.error(cEx);
                } catch (NoSuchAlgorithmException noSaEx) {
                    log.error(noSaEx);
                } catch (IOException ioEx) {
                    log.error(ioEx);
                }

                // part 1 of firmware validation check: PCR baseline match
                pcrValidator = new PcrValidator(baseline);

                if (baseline.length > 0) {
                    String pcrContent = "";
                    pcrContent = new String(device.getDeviceInfo().getTpmInfo().getPcrValues());

                    if (pcrContent.isEmpty()) {
                        fwStatus = new AppraisalStatus(FAIL,
                                "Firmware validation failed: Client did not "
                                        + "provide pcr values.");
                        log.warn(String.format(
                                "Firmware validation failed: Client (%s) did not "
                                        + "provide pcr values.", device.getName()));
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
                        } catch (CertificateException cEx) {
                            log.error(cEx);
                        } catch (NoSuchAlgorithmException noSaEx) {
                            log.error(noSaEx);
                        } catch (IOException ioEx) {
                            log.error(ioEx);
                        }

                        if (!tpmPcrEvents.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            validationObject = measurement;
                            sb.append(String.format("%d digest(s) were not found:%n",
                                    tpmPcrEvents.size()));
                            for (TpmPcrEvent tpe : tpmPcrEvents) {
                                sb.append(String.format("PCR Index %d - %s%n",
                                        tpe.getPcrIndex(),
                                        tpe.getEventTypeStr()));
                            }
                            if (fwStatus.getAppStatus().equals(FAIL)) {
                                fwStatus = new AppraisalStatus(FAIL, String.format("%s%n%s",
                                        fwStatus.getMessage(), sb.toString()));
                            } else {
                                fwStatus = new AppraisalStatus(FAIL, sb.toString());
                            }
                        }
                    }
                } else {
                    fwStatus = new AppraisalStatus(FAIL, "The RIM baseline could not be found.");
                }
            }

            EventLogMeasurements eventLog = measurement;
            eventLog.setOverallValidationResult(fwStatus.getAppStatus());
            referenceManifestRepository.save(eventLog);
        } else {
            fwStatus = new AppraisalStatus(FAIL, String.format("Firmware Validation failed: "
                    + "%s for %s can not be found", failedString, hostName));
            if (measurement != null) {
                measurement.setOverallValidationResult(fwStatus.getAppStatus());
                referenceManifestRepository.save(measurement);
            }
        }

        return fwStatus;
    }
}
