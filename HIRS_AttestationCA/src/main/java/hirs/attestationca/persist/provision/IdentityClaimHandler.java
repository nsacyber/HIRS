package hirs.attestationca.persist.provision;

import com.google.protobuf.ByteString;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.manager.TPM2ProvisionerStateRepository;
import hirs.attestationca.persist.entity.tpm.TPM2ProvisionerState;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.info.FirmwareInfo;
import hirs.attestationca.persist.entity.userdefined.info.HardwareInfo;
import hirs.attestationca.persist.entity.userdefined.info.NetworkInfo;
import hirs.attestationca.persist.entity.userdefined.info.OSInfo;
import hirs.attestationca.persist.entity.userdefined.info.TPMInfo;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.EventLogMeasurements;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.exceptions.IdentityProcessingException;
import hirs.attestationca.persist.service.SupplyChainValidationService;
import hirs.utils.HexUtils;
import hirs.utils.SwidResource;
import hirs.utils.enums.DeviceInfoEnums;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class IdentityClaimHandler extends AbstractRequestHandler {
    private static final String PCR_QUOTE_MASK = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,"
            + "14,15,16,17,18,19,20,21,22,23";

    private static final int NUM_OF_VARIABLES = 5;
    /**
     * Number of bytes to include in the TPM2.0 nonce.
     */
    public static final int NONCE_LENGTH = 20;
    private static final int MAC_BYTES = 6;

    private SupplyChainValidationService supplyChainValidationService;
    private CertificateRepository certificateRepository;
    private ReferenceManifestRepository referenceManifestRepository;
    private ReferenceDigestValueRepository referenceDigestValueRepository;
    private DeviceRepository deviceRepository;
    private TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository;

    /**
     * Constructor
     */
    public IdentityClaimHandler(
            final SupplyChainValidationService supplyChainValidationService,
            final CertificateRepository certificateRepository,
            final ReferenceManifestRepository referenceManifestRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository,
            final DeviceRepository deviceRepository,
            final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository,
            final PolicyRepository policyRepository) {
        this.supplyChainValidationService = supplyChainValidationService;
        this.certificateRepository = certificateRepository;
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.deviceRepository = deviceRepository;
        this.tpm2ProvisionerStateRepository = tpm2ProvisionerStateRepository;
        setPolicyRepository(policyRepository);
    }

    /**
     * Basic implementation of the ACA processIdentityClaimTpm2 method. Parses the claim,
     * stores the device info, performs supply chain validation, generates a nonce,
     * and wraps that nonce with the make credential process before returning it to the client.
     *            attCert.setPcrValues(pcrValues);

     * @param identityClaim the request to process, cannot be null
     * @return an identity claim response for the specified request containing a wrapped blob
     */
    public byte[] processIdentityClaimTpm2(final byte[] identityClaim) {
        log.error("Identity Claim received...");

        if (ArrayUtils.isEmpty(identityClaim)) {
            log.error("Identity claim empty throwing exception.");
            throw new IllegalArgumentException("The IdentityClaim sent by the client"
                    + " cannot be null or empty.");
        }

        // attempt to deserialize Protobuf IdentityClaim
        ProvisionerTpm2.IdentityClaim claim = parseIdentityClaim(identityClaim);

        // parse the EK Public key from the IdentityClaim once for use in supply chain validation
        // and later tpm20MakeCredential function
        RSAPublicKey ekPub = parsePublicKey(claim.getEkPublicArea().toByteArray());
        AppraisalStatus.Status validationResult = AppraisalStatus.Status.FAIL;

        try {
            validationResult = doSupplyChainValidation(claim, ekPub);
        } catch (Exception ex) {
            for (StackTraceElement ste : ex.getStackTrace()) {
                log.error(ste.toString());
            }
        }

        ByteString blobStr = ByteString.copyFrom(new byte[]{});
        if (validationResult == AppraisalStatus.Status.PASS) {
            RSAPublicKey akPub = parsePublicKey(claim.getAkPublicArea().toByteArray());
            byte[] nonce = generateRandomBytes(NONCE_LENGTH);
            blobStr = tpm20MakeCredential(ekPub, akPub, nonce);
            PolicyRepository scp = this.getPolicyRepository();
            PolicySettings policySettings = scp.findByName("Default");
            String pcrQuoteMask = PCR_QUOTE_MASK;

            String strNonce = HexUtils.byteArrayToHexString(nonce);
            log.info("Sending nonce: " + strNonce);
            log.info("Persisting claim of length: " + identityClaim.length);

            tpm2ProvisionerStateRepository.save(new TPM2ProvisionerState(nonce, identityClaim));

            if (policySettings != null && policySettings.isIgnoreImaEnabled()) {
                pcrQuoteMask = PCR_QUOTE_MASK.replace("10,", "");
            }
            // Package response
            ProvisionerTpm2.IdentityClaimResponse response
                    = ProvisionerTpm2.IdentityClaimResponse.newBuilder()
                    .setCredentialBlob(blobStr).setPcrMask(pcrQuoteMask)
                    .setStatus(ProvisionerTpm2.ResponseStatus.PASS)
                    .build();
            return response.toByteArray();
        } else {
            log.error("Supply chain validation did not succeed. Result is: "
                    + validationResult);
            // empty response
            ProvisionerTpm2.IdentityClaimResponse response
                    = ProvisionerTpm2.IdentityClaimResponse.newBuilder()
                    .setCredentialBlob(blobStr)
                    .setStatus(ProvisionerTpm2.ResponseStatus.FAIL)
                    .build();
            return response.toByteArray();
        }
    }

    /**
     * Performs supply chain validation.
     *
     * @param claim the identity claim
     * @param ekPub the public endorsement key
     * @return the {@link AppraisalStatus} of the supply chain validation
     */
    private AppraisalStatus.Status doSupplyChainValidation(
            final ProvisionerTpm2.IdentityClaim claim, final PublicKey ekPub) {
        // attempt to find an endorsement credential to validate
        EndorsementCredential endorsementCredential = parseEcFromIdentityClaim(claim, ekPub, certificateRepository);

        // attempt to find platform credentials to validate
        List<PlatformCredential> platformCredentials = parsePcsFromIdentityClaim(claim,
                endorsementCredential, certificateRepository);

        // Parse and save device info
        Device device = processDeviceInfo(claim);

        // There are situations in which the claim is sent with no PCs
        // or a PC from the tpm which will be deprecated
        // this is to check what is in the platform object and pull
        // additional information from the DB if information exists
        if (platformCredentials.size() == 1) {
            for (PlatformCredential pc : platformCredentials) {
                if (pc != null && pc.getPlatformSerial() != null) {
                    platformCredentials.addAll(certificateRepository
                            .byBoardSerialNumber(pc.getPlatformSerial()));
                }
            }
        }
        // perform supply chain validation
        SupplyChainValidationSummary summary = supplyChainValidationService.validateSupplyChain(
                endorsementCredential, platformCredentials, device);
        device.setSummaryId(summary.getId().toString());
        // update the validation result in the device
        AppraisalStatus.Status validationResult = summary.getOverallValidationResult();
        device.setSupplyChainValidationStatus(validationResult);
        this.deviceRepository.save(device);
        return validationResult;
    }

    private Device processDeviceInfo(final ProvisionerTpm2.IdentityClaim claim) {
        DeviceInfoReport deviceInfoReport = null;
        String deviceName = deviceInfoReport.getNetworkInfo().getHostname();

        try {
            deviceInfoReport = parseDeviceInfo(claim);
        } catch (NoSuchAlgorithmException noSaEx) {
            log.error(noSaEx);
        }

        if (deviceInfoReport == null) {
            log.error("Failed to deserialize Device Info Report");
            throw new IdentityProcessingException("Device Info Report failed to deserialize "
                    + "from Identity Claim");
        }

        log.info("Processing Device Info Report");
        // store device and device info report.
        Device device = this.deviceRepository.findByName(deviceName);
        device.setDeviceInfo(deviceInfoReport);
        return this.deviceRepository.save(device);
    }

    /**
     * Converts a protobuf DeviceInfo object to a HIRS Utils DeviceInfoReport object.
     * @param claim the protobuf serialized identity claim containing the device info
     * @return a HIRS Utils DeviceInfoReport representation of device info
     */
    @SuppressWarnings("methodlength")
    private DeviceInfoReport parseDeviceInfo(final ProvisionerTpm2.IdentityClaim claim)
            throws NoSuchAlgorithmException {
        ProvisionerTpm2.DeviceInfo dv = claim.getDv();
        String pcrValues = "";

        // Get network info
        ProvisionerTpm2.NetworkInfo nwProto = dv.getNw();

        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(nwProto.getIpAddress());
        } catch (UnknownHostException e) {
            log.error("Unable to parse IP address: ", e);
        }
        String[] macAddressParts = nwProto.getMacAddress().split(":");

        // convert mac hex string to byte values
        byte[] macAddressBytes = new byte[MAC_BYTES];
        Integer hex;
        if (macAddressParts.length == MAC_BYTES) {
            for (int i = 0; i < MAC_BYTES; i++) {
                hex = HexUtils.hexToInt(macAddressParts[i]);
                macAddressBytes[i] = hex.byteValue();
            }
        }
        NetworkInfo nw = new NetworkInfo(nwProto.getHostname(), ip, macAddressBytes);

        // Get firmware info
        ProvisionerTpm2.FirmwareInfo fwProto = dv.getFw();
        FirmwareInfo fw = new FirmwareInfo(fwProto.getBiosVendor(), fwProto.getBiosVersion(),
                fwProto.getBiosReleaseDate());

        // Get OS info
        ProvisionerTpm2.OsInfo osProto = dv.getOs();
        OSInfo os = new OSInfo(osProto.getOsName(), osProto.getOsVersion(), osProto.getOsArch(),
                osProto.getDistribution(), osProto.getDistributionRelease());

        // Get hardware info
        ProvisionerTpm2.HardwareInfo hwProto = dv.getHw();
        // Make sure chassis info has at least one chassis
        String firstChassisSerialNumber = DeviceInfoEnums.NOT_SPECIFIED;
        if (hwProto.getChassisInfoCount() > 0) {
            firstChassisSerialNumber = hwProto.getChassisInfo(0).getSerialNumber();
        }
        // Make sure baseboard info has at least one baseboard
        String firstBaseboardSerialNumber = DeviceInfoEnums.NOT_SPECIFIED;
        if (hwProto.getBaseboardInfoCount() > 0) {
            firstBaseboardSerialNumber = hwProto.getBaseboardInfo(0).getSerialNumber();
        }
        HardwareInfo hw = new HardwareInfo(hwProto.getManufacturer(), hwProto.getProductName(),
                hwProto.getProductVersion(), hwProto.getSystemSerialNumber(),
                firstChassisSerialNumber, firstBaseboardSerialNumber);

        if (dv.hasPcrslist()) {
            pcrValues = dv.getPcrslist().toStringUtf8();
        }

        // check for RIM Base and Support files, if they don't exists in the database, load them
        String defaultClientName = String.format("%s_%s",
                dv.getHw().getManufacturer(),
                dv.getHw().getProductName());
        BaseReferenceManifest dbBaseRim = null;
        SupportReferenceManifest support;
        EventLogMeasurements measurements;
        String tagId = "";
        String fileName = "";
        Pattern pattern = Pattern.compile("([^\\s]+(\\.(?i)(rimpcr|rimel|bin|log))$)");
        Matcher matcher;
        MessageDigest messageDigest =  MessageDigest.getInstance("SHA-256");
        List<ReferenceManifest> listOfSavedRims = new LinkedList<>();

        if (dv.getLogfileCount() > 0) {
            for (ByteString logFile : dv.getLogfileList()) {
                try {
                    support = (SupportReferenceManifest) referenceManifestRepository.findByHexDecHash(
                                    Hex.encodeHexString(messageDigest.digest(logFile.toByteArray())));
                    if (support == null) {
                        support = new SupportReferenceManifest(
                                String.format("%s.rimel",
                                        defaultClientName),
                                logFile.toByteArray());
                        // this is a validity check
                        new TCGEventLog(support.getRimBytes());
                        // no issues, continue
                        support.setPlatformManufacturer(dv.getHw().getManufacturer());
                        support.setPlatformModel(dv.getHw().getProductName());
                        support.setFileName(String.format("%s_[%s].rimel", defaultClientName,
                                support.getHexDecHash().substring(
                                        support.getHexDecHash().length() - NUM_OF_VARIABLES)));
                        support.setDeviceName(dv.getNw().getHostname());
                        this.referenceManifestRepository.save(support);
                    } else {
                        log.info("Client provided Support RIM already loaded in database.");
                        if (support.isArchived()) {
                            support.restore();
                            support.resetCreateTime();
                            this.referenceManifestRepository.save(support);
                        }
                    }
                } catch (IOException ioEx) {
                    log.error(ioEx);
                } catch (Exception ex) {
                    log.error(String.format("Failed to load support rim: %s", messageDigest.digest(
                            logFile.toByteArray()).toString()));
                }
            }
        } else {
            log.warn(String.format("%s did not send support RIM file...",
                    dv.getNw().getHostname()));
        }

        if (dv.getSwidfileCount() > 0) {
            for (ByteString swidFile : dv.getSwidfileList()) {
                try {
                    dbBaseRim = (BaseReferenceManifest) referenceManifestRepository
                            .findByBase64Hash(Base64.getEncoder()
                            .encodeToString(messageDigest
                                    .digest(swidFile.toByteArray())));
                    if (dbBaseRim == null) {
                        dbBaseRim = new BaseReferenceManifest(
                                String.format("%s.swidtag",
                                        defaultClientName),
                                swidFile.toByteArray());
                        dbBaseRim.setDeviceName(dv.getNw().getHostname());
                        this.referenceManifestRepository.save(dbBaseRim);
                    } else {
                        log.info("Client provided Base RIM already loaded in database.");
                        /**
                         * Leaving this as is for now, however can there be a condition
                         * in which the provisioner sends swidtags without support rims?
                         */
                        if (dbBaseRim.isArchived()) {
                            dbBaseRim.restore();
                            dbBaseRim.resetCreateTime();
                            this.referenceManifestRepository.save(dbBaseRim);
                        }
                    }
                } catch (IOException ioEx) {
                    log.error(ioEx);
                }
            }
        } else {
            log.warn(String.format("%s did not send swid tag file...",
                    dv.getNw().getHostname()));
        }

        //update Support RIMs and Base RIMs.
        for (ByteString swidFile : dv.getSwidfileList()) {
            dbBaseRim = (BaseReferenceManifest) referenceManifestRepository
                    .findByBase64Hash(Base64.getEncoder().encodeToString(messageDigest.digest(
                            swidFile.toByteArray())));
            if (dbBaseRim != null) {
                // get file name to use
                for (SwidResource swid : dbBaseRim.getFileResources()) {
                    matcher = pattern.matcher(swid.getName());
                    if (matcher.matches()) {
                        //found the file name
                        int dotIndex = swid.getName().lastIndexOf(".");
                        fileName = swid.getName().substring(0, dotIndex);
                        dbBaseRim.setFileName(String.format("%s.swidtag",
                                fileName));
                    }

                    // now update support rim
                    SupportReferenceManifest dbSupport = (SupportReferenceManifest) referenceManifestRepository
                            .findByHexDecHash(swid.getHashValue());
                    if (dbSupport != null) {
                        dbSupport.setFileName(swid.getName());
                        dbSupport.setSwidTagVersion(dbBaseRim.getSwidTagVersion());
                        dbSupport.setTagId(dbBaseRim.getTagId());
                        dbSupport.setSwidTagVersion(dbBaseRim.getSwidTagVersion());
                        dbSupport.setSwidVersion(dbBaseRim.getSwidVersion());
                        dbSupport.setSwidPatch(dbBaseRim.isSwidPatch());
                        dbSupport.setSwidSupplemental(dbBaseRim.isSwidSupplemental());
                        dbBaseRim.setAssociatedRim(dbSupport.getId());
                        dbSupport.setUpdated(true);
                        dbSupport.setAssociatedRim(dbBaseRim.getId());
                        this.referenceManifestRepository.save(dbSupport);
                        listOfSavedRims.add(dbSupport);
                    }
                }
                this.referenceManifestRepository.save(dbBaseRim);
                listOfSavedRims.add(dbBaseRim);
            }
        }

        generateDigestRecords(hw.getManufacturer(), hw.getProductName());

        if (dv.hasLivelog()) {
            log.info("Device sent bios measurement log...");
            fileName = String.format("%s.measurement",
                    dv.getNw().getHostname());
            try {
                EventLogMeasurements temp = new EventLogMeasurements(fileName,
                        dv.getLivelog().toByteArray());
                // find previous version.
                measurements = referenceManifestRepository
                        .byMeasurementDeviceName(dv.getNw().getHostname());

                if (measurements != null) {
                    // Find previous log and delete it
                    referenceManifestRepository.delete(measurements);
                }

                BaseReferenceManifest baseRim = referenceManifestRepository
                        .getBaseByManufacturerModel(dv.getHw().getManufacturer(),
                                dv.getHw().getProductName());
                measurements = temp;
                measurements.setPlatformManufacturer(dv.getHw().getManufacturer());
                measurements.setPlatformModel(dv.getHw().getProductName());
                measurements.setTagId(tagId);
                measurements.setDeviceName(dv.getNw().getHostname());
                if (baseRim != null) {
                    measurements.setAssociatedRim(baseRim.getAssociatedRim());
                }
                this.referenceManifestRepository.save(measurements);

                if (baseRim != null) {
                    // pull the base versions of the swidtag and rimel and set the
                    // event log hash for use during provision
                    SupportReferenceManifest sBaseRim = (SupportReferenceManifest) referenceManifestRepository
                            .findByBase64Hash(baseRim.getBase64Hash());
                    baseRim.setEventLogHash(temp.getHexDecHash());
                    sBaseRim.setEventLogHash(temp.getHexDecHash());
                    referenceManifestRepository.save(baseRim);
                    referenceManifestRepository.save(sBaseRim);
                }
            } catch (IOException ioEx) {
                log.error(ioEx);
            }
        } else {
            log.warn(String.format("%s did not send bios measurement log...",
                    dv.getNw().getHostname()));
        }

         // Get TPM info, currently unimplemented
        TPMInfo tpm = createTpmInfo(pcrValues);

        // Create final report
        DeviceInfoReport dvReport = new DeviceInfoReport(nw, os, fw, hw, tpm,
                claim.getClientVersion());
        dvReport.setPaccorOutputString(claim.getPaccorOutput());

        return dvReport;
    }

    private boolean generateDigestRecords(final String manufacturer, final String model) {
        List<ReferenceDigestValue> rdValues = new LinkedList<>();
        SupportReferenceManifest baseSupportRim = null;
        List<SupportReferenceManifest> supplementalRims = new ArrayList<>();
        List<SupportReferenceManifest> patchRims = new ArrayList<>();
        List<SupportReferenceManifest> dbSupportRims = this.referenceManifestRepository
                .getSupportByManufacturerModel(manufacturer, model);
        List<ReferenceDigestValue> sourcedValues = referenceDigestValueRepository
                .findByManufacturerAndModel(manufacturer, model);

        Map<String, ReferenceDigestValue> digestValueMap = new HashMap<>();
        sourcedValues.stream().forEach((rdv) -> {
            digestValueMap.put(rdv.getDigestValue(), rdv);
        });

        for (SupportReferenceManifest dbSupport : dbSupportRims) {
            if (dbSupport.isSwidPatch()) {
                patchRims.add(dbSupport);
            } else if (dbSupport.isSwidSupplemental()) {
                supplementalRims.add(dbSupport);
            } else {
                // we have a base support rim (verify this is getting set)
                baseSupportRim = dbSupport;
            }
        }

        if (baseSupportRim != null
                && referenceDigestValueRepository.findBySupportRimHash(baseSupportRim.getHexDecHash()).isEmpty()) {
            try {
                TCGEventLog logProcessor = new TCGEventLog(baseSupportRim.getRimBytes());
                ReferenceDigestValue rdv;
                for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                    rdv = new ReferenceDigestValue(baseSupportRim.getAssociatedRim(),
                            baseSupportRim.getId(), manufacturer, model, tpe.getPcrIndex(),
                            tpe.getEventDigestStr(), baseSupportRim.getHexDecHash(),
                            tpe.getEventTypeStr(),
                            false, false, true, tpe.getEventContent());
                    rdValues.add(rdv);
                }

                // since I have the base already I don't have to care about the backward
                // linkage
                for (SupportReferenceManifest supplemental : supplementalRims) {
                    logProcessor = new TCGEventLog(supplemental.getRimBytes());
                    for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                        // all RDVs will have the same base rim
                        rdv = new ReferenceDigestValue(baseSupportRim.getAssociatedRim(),
                                supplemental.getId(), manufacturer, model, tpe.getPcrIndex(),
                                tpe.getEventDigestStr(), baseSupportRim.getHexDecHash(),
                                tpe.getEventTypeStr(),
                                false, false, true, tpe.getEventContent());
                        rdValues.add(rdv);
                    }
                }

                // Save all supplemental values
                ReferenceDigestValue tempRdv;
                for (ReferenceDigestValue subRdv : rdValues) {
                    // check if the value already exists
                    if (digestValueMap.containsKey(subRdv.getDigestValue())) {
                        tempRdv = digestValueMap.get(subRdv.getDigestValue());
                        if (tempRdv.getPcrIndex() != subRdv.getPcrIndex()
                                && !tempRdv.getEventType().equals(subRdv.getEventType())) {
                            referenceDigestValueRepository.save(subRdv);
                        } else {
                            // will this be a problem down the line?
                            referenceDigestValueRepository.save(subRdv);
                        }
                    } else {
                        referenceDigestValueRepository.save(subRdv);
                    }
                    digestValueMap.put(subRdv.getDigestValue(), subRdv);
                }

                // if a patch value doesn't exist, error?
                ReferenceDigestValue dbRdv;
                String patchedValue;
                for (SupportReferenceManifest patch : patchRims) {
                    logProcessor = new TCGEventLog(patch.getRimBytes());
                    for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                        patchedValue = tpe.getEventDigestStr();
                        dbRdv = digestValueMap.get(patchedValue);

                        if (dbRdv == null) {
                            log.error(String.format("Patching value does not exist (%s)",
                                    patchedValue));
                        } else {
                            /**
                             * Until we get patch examples, this is WIP
                             */
                            dbRdv.setPatched(true);
                        }
                    }
                }
            } catch (CertificateException cEx) {
                log.error(cEx);
            } catch (NoSuchAlgorithmException noSaEx) {
                log.error(noSaEx);
            } catch (IOException ioEx) {
                log.error(ioEx);
            }
        }

        return true;
    }

    /**
     * Performs the first step of the TPM 2.0 identity claim process. Takes an ek, ak, and secret
     * and then generates a seed that is used to generate AES and HMAC keys. Parses the ak name.
     * Encrypts the seed with the public ek. Uses the AES key to encrypt the secret. Uses the HMAC
     * key to generate an HMAC to cover the encrypted secret and the ak name. The output is an
     * encrypted blob that acts as the first part of a challenge-response authentication mechanism
     * to validate an identity claim.
     *
     * Equivalent to calling tpm2_makecredential using tpm2_tools.
     *
     * @param ek endorsement key in the identity claim
     * @param ak attestation key in the identity claim
     * @param secret a nonce
     * @return the encrypted blob forming the identity claim challenge
     */
    protected ByteString tpm20MakeCredential(final RSAPublicKey ek, final RSAPublicKey ak,
                                             final byte[] secret) {
        // check size of the secret
        if (secret.length > MAX_SECRET_LENGTH) {
            throw new IllegalArgumentException("Secret must be " + MAX_SECRET_LENGTH
                    + " bytes or smaller.");
        }

        // generate a random 32 byte seed
        byte[] seed = generateRandomBytes(SEED_LENGTH);

        try {
            // encrypt seed with pubEk
            Cipher asymCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepSpec = new OAEPParameterSpec("SHA-256", "MGF1",
                    MGF1ParameterSpec.SHA256, new PSource.PSpecified("IDENTITY\0".getBytes()));
            asymCipher.init(Cipher.PUBLIC_KEY, ek, oaepSpec);
            asymCipher.update(seed);
            byte[] encSeed = asymCipher.doFinal();

            // generate ak name from akMod
            byte[] akModTemp = ak.getModulus().toByteArray();
            byte[] akMod = new byte[RSA_MODULUS_LENGTH];
            int startpos = 0;
            // BigIntegers are signed, so a modulus that has a first bit of 1
            // will be padded with a zero byte that must be removed
            if (akModTemp[0] == 0x00) {
                startpos = 1;
            }
            System.arraycopy(akModTemp, startpos, akMod, 0, RSA_MODULUS_LENGTH);
            byte[] akName = generateAkName(akMod);

            // generate AES and HMAC keys from seed
            byte[] aesKey = cryptKDFa(seed, "STORAGE", akName, AES_KEY_LENGTH_BYTES);
            byte[] hmacKey = cryptKDFa(seed, "INTEGRITY", null, HMAC_KEY_LENGTH_BYTES);

            // use two bytes to add a size prefix on secret
            ByteBuffer b;
            b = ByteBuffer.allocate(2);
            b.putShort((short) (secret.length));
            byte[] secretLength = b.array();
            byte[] secretBytes = new byte[secret.length + 2];
            System.arraycopy(secretLength, 0, secretBytes, 0, 2);
            System.arraycopy(secret, 0, secretBytes, 2, secret.length);

            // encrypt size prefix + secret with AES key
            Cipher symCipher = Cipher.getInstance("AES/CFB/NoPadding");
            byte[] defaultIv = HexUtils.hexStringToByteArray("00000000000000000000000000000000");
            IvParameterSpec ivSpec = new IvParameterSpec(defaultIv);
            symCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), ivSpec);
            byte[] encSecret = symCipher.doFinal(secretBytes);

            // generate HMAC covering encrypted secret and ak name
            Mac integrityHmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec integrityKey = new SecretKeySpec(hmacKey, integrityHmac.getAlgorithm());
            integrityHmac.init(integrityKey);
            byte[] message = new byte[encSecret.length + akName.length];
            System.arraycopy(encSecret, 0, message, 0, encSecret.length);
            System.arraycopy(akName, 0, message, encSecret.length, akName.length);
            integrityHmac.update(message);
            byte[] integrity = integrityHmac.doFinal();
            b = ByteBuffer.allocate(2);
            b.putShort((short) (HMAC_SIZE_LENGTH_BYTES + HMAC_KEY_LENGTH_BYTES + encSecret.length));
            byte[] topSize = b.array();

            // return ordered blob of assembled credentials
            byte[] bytesToReturn = assembleCredential(topSize, integrity, encSecret, encSeed);
            return ByteString.copyFrom(bytesToReturn);

        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
                | InvalidKeyException | InvalidAlgorithmParameterException
                | NoSuchPaddingException e) {
            throw new IdentityProcessingException(
                    "Encountered error while making the identity claim challenge: "
                            + e.getMessage(), e);
        }
    }

    @SuppressWarnings("magicnumber")
    private byte[] assembleCredential(final byte[] topSize, final byte[] integrityHmac,
                                      final byte[] encryptedSecret,
                                      final byte[] encryptedSeed) {
        /*
         * Credential structure breakdown with endianness:
         * 0-1 topSize (2), LE
         * 2-3 hashsize (2), BE always 0x0020
         * 4-35 integrity HMac (32)
         * 36-133 (98 = 32*3 +2) of zeros, copy over from encSecret starting at [36]
         * 134-135 (2) LE size, always 0x0001
         * 136-391 (256) copy over with encSeed
         * */
        byte[] credentialBlob = new byte[TPM2_CREDENTIAL_BLOB_SIZE];
        credentialBlob[0] = topSize[1];
        credentialBlob[1] = topSize[0];
        credentialBlob[2] = 0x00;
        credentialBlob[3] = 0x20;
        System.arraycopy(integrityHmac, 0, credentialBlob, 4, 32);
        for (int i = 0; i < 98; i++) {
            credentialBlob[36 + i] = 0x00;
        }
        System.arraycopy(encryptedSecret, 0, credentialBlob, 36, encryptedSecret.length);
        credentialBlob[134] = 0x00;
        credentialBlob[135] = 0x01;
        System.arraycopy(encryptedSeed, 0, credentialBlob, 136, 256);
        // return the result
        return credentialBlob;
    }

    /**
     * Determines the AK name from the AK Modulus.
     * @param akModulus modulus of an attestation key
     * @return the ak name byte array
     * @throws NoSuchAlgorithmException Underlying SHA256 method used a bad algorithm
     */
    byte[] generateAkName(final byte[] akModulus) throws NoSuchAlgorithmException {
        byte[] namePrefix = HexUtils.hexStringToByteArray(AK_NAME_PREFIX);
        byte[] hashPrefix = HexUtils.hexStringToByteArray(AK_NAME_HASH_PREFIX);
        byte[] toHash = new byte[hashPrefix.length + akModulus.length];
        System.arraycopy(hashPrefix, 0, toHash, 0, hashPrefix.length);
        System.arraycopy(akModulus, 0, toHash, hashPrefix.length, akModulus.length);
        byte[] nameHash = sha256hash(toHash);
        byte[] toReturn = new byte[namePrefix.length + nameHash.length];
        System.arraycopy(namePrefix, 0, toReturn, 0, namePrefix.length);
        System.arraycopy(nameHash, 0, toReturn, namePrefix.length, nameHash.length);
        return toReturn;
    }

    /**
     * This replicates the TPM 2.0 CryptKDFa function to an extent. It will only work for generation
     * that uses SHA-256, and will only generate values of 32 B or less. Counters above zero and
     * multiple contexts are not supported in this implementation. This should work for all uses of
     * the KDF for TPM2_MakeCredential.
     *
     * @param seed random value used to generate the key
     * @param label first portion of message used to generate key
     * @param context second portion of message used to generate key
     * @param sizeInBytes size of key to generate in bytes
     * @return the derived key
     * @throws NoSuchAlgorithmException Wrong crypto algorithm selected
     * @throws InvalidKeyException Invalid key used
     */
    @SuppressWarnings("magicnumber")
    private byte[] cryptKDFa(final byte[] seed, final String label, final byte[] context,
                             final int sizeInBytes)
            throws NoSuchAlgorithmException, InvalidKeyException {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(1);
        byte[] counter = b.array();
        // get the label
        String labelWithEnding = label;
        if (label.charAt(label.length() - 1) != "\0".charAt(0)) {
            labelWithEnding = label + "\0";
        }
        byte[] labelBytes = labelWithEnding.getBytes();
        b = ByteBuffer.allocate(4);
        b.putInt(sizeInBytes * 8);
        byte[] desiredSizeInBits = b.array();
        int sizeOfMessage = 8 + labelBytes.length;
        if (context != null) {
            sizeOfMessage += context.length;
        }
        byte[] message = new byte[sizeOfMessage];
        int marker = 0;
        System.arraycopy(counter, 0, message, marker, 4);
        marker += 4;
        System.arraycopy(labelBytes, 0, message, marker, labelBytes.length);
        marker += labelBytes.length;
        if (context != null) {
            System.arraycopy(context, 0, message, marker, context.length);
            marker += context.length;
        }
        System.arraycopy(desiredSizeInBits, 0, message, marker, 4);
        Mac hmac;
        byte[] toReturn = new byte[sizeInBytes];

        hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec hmacKey = new SecretKeySpec(seed, hmac.getAlgorithm());
        hmac.init(hmacKey);
        hmac.update(message);
        byte[] hmacResult = hmac.doFinal();
        System.arraycopy(hmacResult, 0, toReturn, 0, sizeInBytes);
        return toReturn;
    }

    /**
     * Computes the sha256 hash of the given blob.
     * @param blob byte array to take the hash of
     * @return sha256 hash of blob
     * @throws NoSuchAlgorithmException improper algorithm selected
     */
    private byte[] sha256hash(final byte[] blob) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(blob);
        return md.digest();
    }
}
