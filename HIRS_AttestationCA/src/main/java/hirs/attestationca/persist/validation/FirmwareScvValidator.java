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

    /**
     * @param device
     * @param policySettings
     * @param referenceManifestRepository
     * @param referenceDigestValueRepository
     * @param caCredentialRepository
     * @return
     */
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
//        ReferenceManifest validationObject;
        List<BaseReferenceManifest> baseReferenceManifests = null;
        BaseReferenceManifest baseReferenceManifest = null;
        ReferenceManifest supportReferenceManifest = null;
        EventLogMeasurements measurement = null;

        //baseReferenceManifests = referenceManifestRepository.findAllBaseRims();

        // This block was looking for a base RIM matching the device name
        // The base rim might not have a device name associated with it- i.e. if it's uploaded to the ACA prior to provisioning
        // In this case, try to look up the event log associated with the device, then get the base rim associated by event log hash
        List<ReferenceManifest> deviceRims = referenceManifestRepository.findByDeviceName(hostName);
        for (ReferenceManifest deviceRim : deviceRims) {
            if (deviceRim instanceof BaseReferenceManifest && !deviceRim.isSwidSupplemental() &&
                    !deviceRim.isSwidPatch()) {
                baseReferenceManifest = (BaseReferenceManifest) deviceRim;
            }

            if (deviceRim instanceof EventLogMeasurements) {
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
            failedString = "Base Reference Integrity Manifest\n";
            passed = false;
        } else if (measurement == null) {
            measurement = (EventLogMeasurements) referenceManifestRepository.findByHexDecHashAndRimType(
                    baseReferenceManifest.getEventLogHash(), ReferenceManifest.MEASUREMENT_RIM);

            if (measurement == null) {
                measurement = referenceManifestRepository.byMeasurementDeviceName(
                        baseReferenceManifest.getDeviceName());
            }
        }

        if (measurement == null) {
            failedString += "Bios measurement";
            passed = false;
        }

        if (passed) {
            List<SwidResource> resources =
                    baseReferenceManifest.getFileResources();
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
                KeyStore keyStore = null;
                Set<CertificateAuthorityCredential> set = ValidationService.getCaChainRec(signingCert,
                        Collections.emptySet(),
                        caCredentialRepository);
                try {
                    keyStore = ValidationService.caCertSetToKeystore(set);
                } catch (Exception e) {
                    log.error("Error building CA chain for " + signingCert.getSubjectKeyIdentifier() + ": "
                            + e.getMessage());
                }

                ArrayList<X509Certificate> certs = new ArrayList<>(set.size());
                for (CertificateAuthorityCredential cac : set) {
                    try {
                        certs.add(cac.getX509Certificate());
                    } catch (IOException e) {
                        log.error(
                                "Error building CA chain for " + signingCert.getSubjectKeyIdentifier() + ": "
                                        + e.getMessage());
                    }
                }
                referenceManifestValidator.setTrustStore(certs);
                try {
                    if (referenceManifestValidator.validateXmlSignature(
                            signingCert.getX509Certificate().getPublicKey(),
                            signingCert.getSubjectKeyIdString(), signingCert.getEncodedPublicKey())) {
                        try {
                            if (!SupplyChainCredentialValidator.verifyCertificate(
                                    signingCert.getX509Certificate(), keyStore)) {
                                passed = false;
                                fwStatus = new AppraisalStatus(FAIL,
                                        "Firmware validation failed: invalid certificate path.");
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
                TCGEventLog expectedEventLog;
                try {
                    expectedEventLog = new TCGEventLog(supportReferenceManifest.getRimBytes());
                    baseline = expectedEventLog.getExpectedPCRValues();
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
                    pcrContent = new String(device.getDeviceInfo().getTpmInfo().getPcrValues(),
                            StandardCharsets.UTF_8);

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
                        TCGEventLog actualEventLog;
                        LinkedList<TpmPcrEvent> failedPcrValues = new LinkedList<>();
                        List<ReferenceDigestValue> rimIntegrityMeasurements;
                        HashMap<String, ReferenceDigestValue> expectedEventLogRecords = new HashMap<>();
                        try {
                            if (measurement.getDeviceName().equals(hostName)) {
                                actualEventLog = new TCGEventLog(measurement.getRimBytes());
                                rimIntegrityMeasurements = referenceDigestValueRepository
                                        .findValuesByBaseRimId(baseReferenceManifest.getId());
                                for (ReferenceDigestValue rdv : rimIntegrityMeasurements) {
                                    expectedEventLogRecords.put(rdv.getDigestValue(), rdv);
                                }

                                failedPcrValues.addAll(pcrValidator.validateTpmEvents(
                                        actualEventLog, expectedEventLogRecords, policySettings));
                            }
                        } catch (CertificateException cEx) {
                            log.error(cEx);
                        } catch (NoSuchAlgorithmException noSaEx) {
                            log.error(noSaEx);
                        } catch (IOException ioEx) {
                            log.error(ioEx);
                        }

                        if (!failedPcrValues.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("%d digest(s) were not found:%n",
                                    failedPcrValues.size()));
                            for (TpmPcrEvent tpe : failedPcrValues) {
                                sb.append(String.format("PCR Index %d - %s%n",
                                        tpe.getPcrIndex(),
                                        tpe.getEventTypeStr()));
                            }
                            if (fwStatus.getAppStatus().equals(FAIL)) {
                                fwStatus = new AppraisalStatus(FAIL, String.format("%s%n%s",
                                        fwStatus.getMessage(), sb.toString()));
                            } else {
                                fwStatus = new AppraisalStatus(FAIL,
                                        sb.toString(), ReferenceManifest.MEASUREMENT_RIM);
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
