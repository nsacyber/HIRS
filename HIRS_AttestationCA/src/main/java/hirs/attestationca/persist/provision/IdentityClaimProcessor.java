package hirs.attestationca.persist.provision;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentInfoRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.manager.TPM2ProvisionerStateRepository;
import hirs.attestationca.persist.entity.tpm.TPM2ProvisionerState;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
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
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import hirs.attestationca.persist.service.SupplyChainValidationService;
import hirs.attestationca.persist.validation.SupplyChainCredentialValidator;
import hirs.utils.HexUtils;
import hirs.utils.SwidResource;
import hirs.utils.enums.DeviceInfoEnums;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import jakarta.xml.bind.UnmarshalException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class IdentityClaimProcessor extends AbstractProcessor {
    /**
     * Number of bytes to include in the TPM2.0 nonce.
     */
    public static final int NONCE_LENGTH = 20;
    private static final String PCR_QUOTE_MASK = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,"
            + "14,15,16,17,18,19,20,21,22,23";
    private static final int NUM_OF_VARIABLES = 5;
    private static final int MAC_BYTES = 6;

    private final SupplyChainValidationService supplyChainValidationService;
    private final CertificateRepository certificateRepository;
    private final ComponentResultRepository componentResultRepository;
    private final ComponentInfoRepository componentInfoRepository;
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final DeviceRepository deviceRepository;
    private final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository;

    /**
     * Constructor.
     *
     * @param supplyChainValidationService   supply chain validation service
     * @param certificateRepository          certificate repository
     * @param componentResultRepository      component result repository
     * @param componentInfoRepository        component info repository
     * @param referenceManifestRepository    reference manifest repository
     * @param referenceDigestValueRepository reference digest value repository
     * @param deviceRepository               device repository
     * @param tpm2ProvisionerStateRepository tpm2 provisioner state repository
     * @param policyRepository               policy repository
     */
    public IdentityClaimProcessor(
            final SupplyChainValidationService supplyChainValidationService,
            final CertificateRepository certificateRepository,
            final ComponentResultRepository componentResultRepository,
            final ComponentInfoRepository componentInfoRepository,
            final ReferenceManifestRepository referenceManifestRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository,
            final DeviceRepository deviceRepository,
            final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository,
            final PolicyRepository policyRepository) {
        this.supplyChainValidationService = supplyChainValidationService;
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
        this.componentInfoRepository = componentInfoRepository;
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
     * attCert.setPcrValues(pcrValues);
     *
     * @param identityClaim the request to process, cannot be null
     * @return an identity claim response for the specified request containing a wrapped blob
     */
    public byte[] processIdentityClaimTpm2(final byte[] identityClaim) {
        log.info("Identity Claim has been received and is ready to be processed");

        if (ArrayUtils.isEmpty(identityClaim)) {
            log.error("Identity claim empty throwing exception.");
            throw new IllegalArgumentException("The IdentityClaim sent by the client"
                    + " cannot be null or empty.");
        }

        final PolicyRepository policyRepository = this.getPolicyRepository();
        final PolicySettings policySettings = policyRepository.findByName("Default");

        // attempt to deserialize Protobuf IdentityClaim
        ProvisionerTpm2.IdentityClaim claim = ProvisionUtils.parseIdentityClaim(identityClaim);

        String identityClaimJsonString = "";
        try {
            identityClaimJsonString = JsonFormat.printer().print(claim);
        } catch (InvalidProtocolBufferException exception) {
            log.error("Identity claim could not be parsed properly into a json string");
        }

        // parse the EK Public key from the IdentityClaim once for use in supply chain validation
        // and later tpm20MakeCredential function
        RSAPublicKey ekPub = ProvisionUtils.parsePublicKey(claim.getEkPublicArea().toByteArray());
        AppraisalStatus.Status validationResult = AppraisalStatus.Status.FAIL;

        try {
            validationResult = doSupplyChainValidation(claim, ekPub);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        ByteString blobStr = ByteString.copyFrom(new byte[]{});

        if (validationResult == AppraisalStatus.Status.PASS) {
            RSAPublicKey akPub = ProvisionUtils.parsePublicKey(claim.getAkPublicArea().toByteArray());
            byte[] nonce = ProvisionUtils.generateRandomBytes(NONCE_LENGTH);
            blobStr = ProvisionUtils.tpm20MakeCredential(ekPub, akPub, nonce);

            String pcrQuoteMask = PCR_QUOTE_MASK;

            String strNonce = HexUtils.byteArrayToHexString(nonce);
            log.info("Sending nonce: {}", strNonce);
            log.info("Persisting identity claim of length: {}", identityClaim.length);

            tpm2ProvisionerStateRepository.save(new TPM2ProvisionerState(nonce, identityClaim));

            if (policySettings.isIgnoreImaEnabled()) {
                pcrQuoteMask = PCR_QUOTE_MASK.replace("10,", "");
            }

            // Package response
            ProvisionerTpm2.IdentityClaimResponse identityClaimResponse
                    = ProvisionerTpm2.IdentityClaimResponse.newBuilder()
                    .setCredentialBlob(blobStr).setPcrMask(pcrQuoteMask)
                    .setStatus(ProvisionerTpm2.ResponseStatus.PASS)
                    .build();

            String identityClaimResponseJsonStringAfterSuccess = "";
            try {
                identityClaimResponseJsonStringAfterSuccess =
                        JsonFormat.printer().print(identityClaimResponse);
            } catch (InvalidProtocolBufferException exception) {
                log.error("Identity claim response after a successful validation "
                        + "could not be parsed properly into a json string");
            }

            if (!policySettings.isSaveProtobufToLogNeverEnabled()
                    && policySettings.isSaveProtobufToLogAlwaysEnabled()) {

                log.info("----------------- Start Of Protobuf Logging Of Identity Claim/Response "
                        + " After Successful Validation -----------------");

                log.info("Identity Claim object received after a "
                        + "successful validation: {}", identityClaimJsonString.isEmpty()
                        ? claim : identityClaimJsonString);

                log.info("Identity Claim Response object after a "
                        + "successful validation: {}", identityClaimResponseJsonStringAfterSuccess.isEmpty()
                        ? identityClaimResponse : identityClaimResponseJsonStringAfterSuccess);

                log.info("----------------- End Of Protobuf Logging Of Identity Claim/Response "
                        + " After Successful Validation -----------------");
            }

            return identityClaimResponse.toByteArray();
        } else {
            log.error("Supply chain validation did not succeed. Result is: {}", validationResult);
            // empty response
            ProvisionerTpm2.IdentityClaimResponse identityClaimResponse
                    = ProvisionerTpm2.IdentityClaimResponse.newBuilder()
                    .setCredentialBlob(blobStr)
                    .setStatus(ProvisionerTpm2.ResponseStatus.FAIL)
                    .build();

            String identityClaimResponseJsonStringAfterFailure = "";
            try {
                identityClaimResponseJsonStringAfterFailure =
                        JsonFormat.printer().print(identityClaimResponse);
            } catch (InvalidProtocolBufferException exception) {
                log.error("Identity claim response after a failed validation "
                        + "could not be parsed properly into a json string");
            }

            if (!policySettings.isSaveProtobufToLogNeverEnabled()
                    && (policySettings.isSaveProtobufToLogAlwaysEnabled()
                    || policySettings.isSaveProtobufToLogOnFailedValEnabled())) {
                log.info("----------------- Start Of Protobuf Logging Of Identity Claim/Response "
                        + " After Failed Validation -----------------");

                log.info("Identity Claim object received after a "
                        + "failed validation: {}", identityClaimJsonString.isEmpty()
                        ? claim : identityClaimJsonString);

                log.info("Identity Claim Response object after a "
                        + "failed validation: {}", identityClaimResponseJsonStringAfterFailure.isEmpty()
                        ? identityClaimResponse : identityClaimResponseJsonStringAfterFailure);

                log.info("----------------- End Of Protobuf Logging Of Identity Claim/Response "
                        + " After Failed Validation -----------------");
            }

            return identityClaimResponse.toByteArray();
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
            final ProvisionerTpm2.IdentityClaim claim, final PublicKey ekPub) throws IOException {

        // attempt to find an endorsement credential to validate
        EndorsementCredential endorsementCredential =
                parseEcFromIdentityClaim(claim, ekPub, certificateRepository);

        // attempt to find platform credentials to validate
        List<PlatformCredential> platformCredentials = parsePcsFromIdentityClaim(claim,
                endorsementCredential, certificateRepository);

        // Parse and save device info
        Device device = processDeviceInfo(claim);

//        device.getDeviceInfo().setPaccorOutputString(claim.getPaccorOutput());
        handleDeviceComponents(device.getDeviceInfo().getNetworkInfo().getHostname(),
                claim.getPaccorOutput());

        // There are situations in which the claim is sent with no PCs
        // or a PC from the tpm which will be deprecated
        // this is to check what is in the platform object and pull
        // additional information from the DB if information exists
        if (platformCredentials.size() == 1) {
            List<PlatformCredential> tempList = new LinkedList<>();
            for (PlatformCredential pc : platformCredentials) {
                if (pc != null && pc.getPlatformSerial() != null) {
                    tempList.addAll(certificateRepository
                            .byBoardSerialNumber(pc.getPlatformSerial()));
                }
            }

            platformCredentials.addAll(tempList);
        }

        // store component results objects
        for (PlatformCredential platformCredential : platformCredentials) {
            List<ComponentResult> componentResults = componentResultRepository
                    .findByCertificateSerialNumberAndBoardSerialNumber(
                            platformCredential.getSerialNumber().toString(),
                            platformCredential.getPlatformSerial());

            if (componentResults.isEmpty()) {
                savePlatformComponents(platformCredential);
            } else {
                componentResults.forEach((componentResult) -> {
                    componentResult.restore();
                    componentResult.resetCreateTime();
                    componentResultRepository.save(componentResult);
                });
            }
        }

        // perform supply chain validation
        SupplyChainValidationSummary summary = supplyChainValidationService.validateSupplyChain(
                endorsementCredential, platformCredentials, device,
                componentInfoRepository.findByDeviceName(device.getName()));
        device.setSummaryId(summary.getId().toString());

        // update the validation result in the device
        AppraisalStatus.Status validationResult = summary.getOverallValidationResult();
        device.setSupplyChainValidationStatus(validationResult);
        this.deviceRepository.save(device);

        return validationResult;
    }

    /**
     * Helper method that utilizes the identity claim to produce a device info report.
     *
     * @param claim identity claim
     * @return device info
     */
    private Device processDeviceInfo(final ProvisionerTpm2.IdentityClaim claim) {
        DeviceInfoReport deviceInfoReport = null;

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
        Device device = null;

        if (deviceInfoReport.getNetworkInfo() != null
                && deviceInfoReport.getNetworkInfo().getHostname() != null
                && !deviceInfoReport.getNetworkInfo().getHostname().isEmpty()) {
            device = this.deviceRepository.findByName(deviceInfoReport.getNetworkInfo().getHostname());
        }

        if (device == null) {
            device = new Device(deviceInfoReport);
        }
        device.setDeviceInfo(deviceInfoReport);
        return this.deviceRepository.save(device);
    }

    /**
     * Converts a protobuf DeviceInfo object to a HIRS Utils DeviceInfoReport object.
     *
     * @param claim the protobuf serialized identity claim containing the device info
     * @return a HIRS Utils DeviceInfoReport representation of device info
     */
    private DeviceInfoReport parseDeviceInfo(final ProvisionerTpm2.IdentityClaim claim)
            throws NoSuchAlgorithmException {
        ProvisionerTpm2.DeviceInfo dv = claim.getDv();
        String pcrValues = "";

        // Get network info
        ProvisionerTpm2.NetworkInfo nwProto = dv.getNw();

        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(nwProto.getIpAddress());
        } catch (UnknownHostException uhEx) {
            log.error("Unable to parse IP address: ", uhEx);
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

        // check for RIM Base and Support files, if they don't exist in the database, load them
        String defaultClientName = String.format("%s_%s",
                dv.getHw().getManufacturer(),
                dv.getHw().getProductName());
        BaseReferenceManifest baseRim = null;
        SupportReferenceManifest supportRim = null;
        EventLogMeasurements integrityMeasurements;
        boolean isReplacement = false;
        String replacementRimId = "";
        String tagId = "";
        String fileName = "";
        Pattern pattern = Pattern.compile("([^\\s]+(\\.(?i)(rimpcr|rimel|bin|log))$)");
        Matcher matcher;
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        if (dv.getSwidfileCount() > 0) {
            for (ByteString swidFile : dv.getSwidfileList()) {
                try {
                    baseRim = (BaseReferenceManifest) referenceManifestRepository
                            .findByBase64Hash(Base64.getEncoder()
                                    .encodeToString(messageDigest
                                            .digest(swidFile.toByteArray())));
                    if (baseRim == null) {
                        /*
                        Either the swidFile does not have a corresponding base RIM in the backend
                        or it was deleted. Check if there is a replacement by comparing tagId against
                        all other base RIMs, and then set the corresponding support rim's deviceName.
                         */
                        baseRim = new BaseReferenceManifest(
                                String.format("%s.swidtag",
                                        defaultClientName),
                                swidFile.toByteArray());
                        List<BaseReferenceManifest> baseRims = referenceManifestRepository.findAllBaseRims();
                        for (BaseReferenceManifest bRim : baseRims) {
                            if (bRim.getTagId().equals(baseRim.getTagId())) {
                                baseRim = bRim;
                                replacementRimId = baseRim.getAssociatedRim().toString();
                                isReplacement = true;
                                break;
                            }
                        }
                        baseRim.setDeviceName(dv.getNw().getHostname());
                        this.referenceManifestRepository.save(baseRim);
                    } else if (baseRim.isArchived()) {
                        /*
                        This block accounts for RIMs that may have been soft-deleted (archived)
                        in an older version of the ACA.
                         */
                        List<ReferenceManifest> rims = referenceManifestRepository.findByArchiveFlag(false);
                        for (ReferenceManifest rim : rims) {
                            if (rim.isBase() && rim.getTagId().equals(baseRim.getTagId())
                                    && rim.getCreateTime().after(baseRim.getCreateTime())) {
                                baseRim.setDeviceName(null);
                                baseRim = (BaseReferenceManifest) rim;
                                baseRim.setDeviceName(dv.getNw().getHostname());
                            }
                        }
                        if (baseRim.isArchived()) {
                            throw new Exception("Unable to locate an unarchived base RIM.");
                        } else {
                            this.referenceManifestRepository.save(baseRim);
                        }
                    } else {
                        baseRim.setDeviceName(dv.getNw().getHostname());
                        this.referenceManifestRepository.save(baseRim);
                    }
                    tagId = baseRim.getTagId();
                } catch (UnmarshalException e) {
                    log.error(e);
                } catch (Exception ex) {
                    log.error("Failed to load base rim: {}", ex.getMessage());
                }
            }
        } else {
            log.warn("{} did not send swid tag file...", dv.getNw().getHostname());
        }

        if (dv.getLogfileCount() > 0) {
            for (ByteString logFile : dv.getLogfileList()) {
                try {
                    supportRim =
                            (SupportReferenceManifest) referenceManifestRepository.findByHexDecHashAndRimType(
                                    Hex.encodeHexString(messageDigest.digest(logFile.toByteArray())),
                                    ReferenceManifest.SUPPORT_RIM);
                    if (supportRim == null) {
                        /*
                        Either the logFile does not have a corresponding support RIM in the backend
                        or it was deleted. The support RIM for a replacement base RIM is handled
                        in the previous loop block.
                         */
                        if (isReplacement) {
                            Optional<ReferenceManifest> replacementRim =
                                    referenceManifestRepository.findById(UUID.fromString(replacementRimId));
                            if (replacementRim.isPresent()) {
                                supportRim = (SupportReferenceManifest) replacementRim.get();
                                supportRim.setDeviceName(dv.getNw().getHostname());
                            } else {
                                throw new Exception("Unable to locate support RIM " + replacementRimId);
                            }
                        } else {
                            supportRim = new SupportReferenceManifest(
                                    String.format("%s.rimel",
                                            defaultClientName),
                                    logFile.toByteArray());
                            // this is a validity check
                            new TCGEventLog(supportRim.getRimBytes());
                            // no issues, continue
                            supportRim.setPlatformManufacturer(dv.getHw().getManufacturer());
                            supportRim.setPlatformModel(dv.getHw().getProductName());
                            supportRim.setFileName(String.format("%s_[%s].rimel", defaultClientName,
                                    supportRim.getHexDecHash().substring(
                                            supportRim.getHexDecHash().length() - NUM_OF_VARIABLES)));
                        }
                        supportRim.setDeviceName(dv.getNw().getHostname());
                        this.referenceManifestRepository.save(supportRim);
                    } else if (supportRim.isArchived()) {
                        /*
                        This block accounts for RIMs that may have been soft-deleted (archived)
                        in an older version of the ACA.
                         */
                        List<ReferenceManifest> rims = referenceManifestRepository.findByArchiveFlag(false);
                        for (ReferenceManifest rim : rims) {
                            if (rim.isSupport()
                                    && rim.getTagId().equals(supportRim.getTagId())
                                    && rim.getCreateTime().after(supportRim.getCreateTime())) {
                                supportRim.setDeviceName(null);
                                supportRim = (SupportReferenceManifest) rim;
                                supportRim.setDeviceName(dv.getNw().getHostname());
                            }
                        }
                        if (supportRim.isArchived()) {
                            throw new Exception("Unable to locate an unarchived support RIM.");
                        } else {
                            this.referenceManifestRepository.save(supportRim);
                        }
                    } else {
                        supportRim.setDeviceName(dv.getNw().getHostname());
                        this.referenceManifestRepository.save(supportRim);
                    }
                } catch (IOException ioEx) {
                    log.error(ioEx);
                } catch (Exception ex) {
                    log.error("Failed to load support rim: {}", ex.getMessage());
                }
            }
        } else {
            log.warn("{} did not send support RIM file...", dv.getNw().getHostname());
        }

        //update Support RIMs and Base RIMs.
        for (ByteString swidFile : dv.getSwidfileList()) {
            baseRim = (BaseReferenceManifest) referenceManifestRepository
                    .findByBase64Hash(Base64.getEncoder().encodeToString(messageDigest.digest(
                            swidFile.toByteArray())));
            if (baseRim != null) {
                // get file name to use
                for (SwidResource swid : baseRim.getFileResources()) {
                    matcher = pattern.matcher(swid.getName());
                    if (matcher.matches()) {
                        //found the file name
                        int dotIndex = swid.getName().lastIndexOf(".");
                        fileName = swid.getName().substring(0, dotIndex);
                        baseRim.setFileName(String.format("%s.swidtag",
                                fileName));
                    }

                    // now update support rim
                    SupportReferenceManifest dbSupport =
                            (SupportReferenceManifest) referenceManifestRepository
                                    .findByHexDecHashAndRimType(swid.getHashValue(),
                                            ReferenceManifest.SUPPORT_RIM);
                    if (dbSupport != null) {
                        dbSupport.setFileName(swid.getName());
                        dbSupport.setSwidTagVersion(baseRim.getSwidTagVersion());
                        dbSupport.setTagId(baseRim.getTagId());
                        dbSupport.setSwidTagVersion(baseRim.getSwidTagVersion());
                        dbSupport.setSwidVersion(baseRim.getSwidVersion());
                        dbSupport.setSwidPatch(baseRim.isSwidPatch());
                        dbSupport.setSwidSupplemental(baseRim.isSwidSupplemental());
                        baseRim.setAssociatedRim(dbSupport.getId());
                        dbSupport.setUpdated(true);
                        dbSupport.setAssociatedRim(baseRim.getId());
                        this.referenceManifestRepository.save(dbSupport);
                    } else {
                        log.warn("Could not locate support RIM with hash {}}", swid.getHashValue());
                    }
                }
                this.referenceManifestRepository.save(baseRim);
            }
        }

        generateDigestRecords(hw.getManufacturer(), hw.getProductName());

        if (dv.hasLivelog()) {
            log.info("Device sent bios measurement log...");
            fileName = String.format("%s.measurement",
                    dv.getNw().getHostname());
            try {
                EventLogMeasurements deviceLiveLog = new EventLogMeasurements(fileName,
                        dv.getLivelog().toByteArray());
                // find previous version.
                integrityMeasurements = referenceManifestRepository
                        .byMeasurementDeviceNameUnarchived(dv.getNw().getHostname());

                if (integrityMeasurements != null) {
                    // Find previous log and archive it
                    integrityMeasurements.archive();
                    this.referenceManifestRepository.save(integrityMeasurements);
                }

                List<BaseReferenceManifest> baseRims = referenceManifestRepository
                        .getBaseByManufacturerModel(dv.getHw().getManufacturer(),
                                dv.getHw().getProductName());
                integrityMeasurements = deviceLiveLog;
                integrityMeasurements.setPlatformManufacturer(dv.getHw().getManufacturer());
                integrityMeasurements.setPlatformModel(dv.getHw().getProductName());
                if (tagId != null && !tagId.trim().isEmpty()) {
                    integrityMeasurements.setTagId(tagId);
                }
                integrityMeasurements.setDeviceName(dv.getNw().getHostname());

                this.referenceManifestRepository.save(integrityMeasurements);

                for (BaseReferenceManifest bRim : baseRims) {
                    if (bRim != null) {
                        // pull the base versions of the swidtag and rimel and set the
                        // event log hash for use during provision
                        SupportReferenceManifest sBaseRim = referenceManifestRepository
                                .getSupportRimEntityById(bRim.getAssociatedRim());
                        if (sBaseRim != null) {
                            bRim.setEventLogHash(deviceLiveLog.getHexDecHash());
                            sBaseRim.setEventLogHash(deviceLiveLog.getHexDecHash());
                            referenceManifestRepository.save(bRim);
                            referenceManifestRepository.save(sBaseRim);
                        } else {
                            log.warn("Could not locate support RIM associated with " +
                                    "base RIM " + bRim.getId());
                        }
                    }
                }
            } catch (IOException ioEx) {
                log.error(ioEx);
            }
        } else {
            log.warn("{} did not send bios measurement log...", dv.getNw().getHostname());
        }

        // Get TPM info, currently unimplemented
        TPMInfo tpmInfo = new TPMInfo(DeviceInfoEnums.NOT_SPECIFIED,
                (short) 0,
                (short) 0,
                (short) 0,
                (short) 0,
                pcrValues.getBytes(StandardCharsets.UTF_8),
                null, null);

        // Create final report
        DeviceInfoReport dvReport = new DeviceInfoReport(nw, os, fw, hw, tpmInfo,
                claim.getClientVersion());
        dvReport.setPaccorOutputString(claim.getPaccorOutput());

        return dvReport;
    }

    /**
     * Helper method that generates digest records using the provided device's manufacturer and model
     * information.
     *
     * @param manufacturer device manufacturer
     * @param model        device model
     * @return boolean that represents that status of the digest records generation
     */
    private boolean generateDigestRecords(final String manufacturer, final String model) {
        List<ReferenceDigestValue> rdValues = new LinkedList<>();
        SupportReferenceManifest baseSupportRim = null;
        List<SupportReferenceManifest> supplementalRims = new ArrayList<>();
        List<SupportReferenceManifest> patchRims = new ArrayList<>();
        List<SupportReferenceManifest> dbSupportRims = this.referenceManifestRepository
                .getSupportByManufacturerModel(manufacturer, model);
        List<ReferenceDigestValue> expectedValues = referenceDigestValueRepository
                .findByManufacturerAndModel(manufacturer, model);

        Map<String, ReferenceDigestValue> digestValueMap = new HashMap<>();
        expectedValues.forEach((rdv) -> digestValueMap.put(rdv.getDigestValue(), rdv));

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
                && referenceDigestValueRepository.findBySupportRimHash(baseSupportRim.getHexDecHash())
                .isEmpty()) {
            try {
                TCGEventLog eventLog = new TCGEventLog(baseSupportRim.getRimBytes());
                ReferenceDigestValue rdv;
                for (TpmPcrEvent tpe : eventLog.getEventList()) {
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
                    eventLog = new TCGEventLog(supplemental.getRimBytes());
                    for (TpmPcrEvent tpe : eventLog.getEventList()) {
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
                    eventLog = new TCGEventLog(patch.getRimBytes());
                    for (TpmPcrEvent tpe : eventLog.getEventList()) {
                        patchedValue = tpe.getEventDigestStr();
                        dbRdv = digestValueMap.get(patchedValue);

                        if (dbRdv == null) {
                            log.error("Patching value does not exist ({})", patchedValue);
                        } else {
                            // WIP - Until we get patch examples
                            dbRdv.setPatched(true);
                        }
                    }
                }
            } catch (CertificateException | NoSuchAlgorithmException | IOException ex) {
                log.error(ex);
            }
        }

        return true;
    }

    /**
     * Helper method that saves the provided platform certificate's components in the database.
     *
     * @param certificate certificate
     */
    private void savePlatformComponents(final Certificate certificate) throws IOException {
        PlatformCredential platformCredential;

        if (certificate instanceof PlatformCredential) {
            platformCredential = (PlatformCredential) certificate;
            ComponentResult componentResult;

            if (platformCredential.getPlatformConfigurationV1() != null) {
                List<ComponentIdentifier> componentIdentifiers = platformCredential
                        .getComponentIdentifiers();

                for (ComponentIdentifier componentIdentifier : componentIdentifiers) {
                    componentResult = new ComponentResult(platformCredential.getPlatformSerial(),
                            platformCredential.getSerialNumber().toString(),
                            platformCredential.getPlatformChainType(),
                            componentIdentifier);
                    componentResult.setFailedValidation(false);
                    componentResult.setDelta(!platformCredential.isPlatformBase());
                    componentResultRepository.save(componentResult);
                }
            } else if (platformCredential.getPlatformConfigurationV2() != null) {
                List<ComponentIdentifierV2> componentIdentifiersV2 = platformCredential
                        .getComponentIdentifiersV2();

                for (ComponentIdentifierV2 componentIdentifierV2 : componentIdentifiersV2) {
                    componentResult = new ComponentResult(platformCredential.getPlatformSerial(),
                            platformCredential.getSerialNumber().toString(),
                            platformCredential.getPlatformChainType(),
                            componentIdentifierV2);
                    componentResult.setFailedValidation(false);
                    componentResult.setDelta(!platformCredential.isPlatformBase());
                    componentResultRepository.save(componentResult);
                }
            }
        }
    }

    /**
     * Helper method that attempts to find all the provided device's components.
     *
     * @param hostName     device's host name
     * @param paccorString string representation of the paccor tool output
     */
    private void handleDeviceComponents(final String hostName, final String paccorString) {
        Map<Integer, ComponentInfo> componentInfoMap = new HashMap<>();

        try {
            List<ComponentInfo> componentInfos = SupplyChainCredentialValidator
                    .getComponentInfoFromPaccorOutput(hostName, paccorString);

            // check the DB for like component infos
            List<ComponentInfo> dbComponentInfos = this.componentInfoRepository.findByDeviceName(hostName);
            dbComponentInfos.forEach((infos) -> componentInfoMap.put(infos.hashCode(), infos));

            for (ComponentInfo componentInfo : dbComponentInfos) {
                if (componentInfoMap.containsKey(componentInfo.hashCode())) {
                    componentInfos.remove(componentInfo);
                }
            }

            for (ComponentInfo componentInfo : componentInfos) {
                this.componentInfoRepository.save(componentInfo);
            }
        } catch (IOException ioEx) {
            log.warn("Error parsing paccor string");
        }
    }
}
