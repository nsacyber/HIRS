package hirs.attestationca.persist.provision;

import com.google.protobuf.ByteString;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class IdentityClaimProcessor extends AbstractProcessor {
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
    private ComponentResultRepository componentResultRepository;
    private ComponentInfoRepository componentInfoRepository;
    private ReferenceManifestRepository referenceManifestRepository;
    private ReferenceDigestValueRepository referenceDigestValueRepository;
    private DeviceRepository deviceRepository;
    private TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository;

    /**
     * Constructor.
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
     *            attCert.setPcrValues(pcrValues);

     * @param identityClaim the request to process, cannot be null
     * @return an identity claim response for the specified request containing a wrapped blob
     */
    public byte[] processIdentityClaimTpm2(final byte[] identityClaim) {
        log.info("Identity Claim received...");

        if (ArrayUtils.isEmpty(identityClaim)) {
            log.error("Identity claim empty throwing exception.");
            throw new IllegalArgumentException("The IdentityClaim sent by the client"
                    + " cannot be null or empty.");
        }

        // attempt to deserialize Protobuf IdentityClaim
        ProvisionerTpm2.IdentityClaim claim = ProvisionUtils.parseIdentityClaim(identityClaim);

        // parse the EK Public key from the IdentityClaim once for use in supply chain validation
        // and later tpm20MakeCredential function
        RSAPublicKey ekPub = ProvisionUtils.parsePublicKey(claim.getEkPublicArea().toByteArray());
        AppraisalStatus.Status validationResult = AppraisalStatus.Status.FAIL;

        try {
            validationResult = doSupplyChainValidation(claim, ekPub);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            for (StackTraceElement ste : ex.getStackTrace()) {
                log.error(ste.toString());
            }
        }

        ByteString blobStr = ByteString.copyFrom(new byte[]{});
        if (validationResult == AppraisalStatus.Status.PASS) {
            RSAPublicKey akPub = ProvisionUtils.parsePublicKey(claim.getAkPublicArea().toByteArray());
            byte[] nonce = ProvisionUtils.generateRandomBytes(NONCE_LENGTH);
            blobStr = ProvisionUtils.tpm20MakeCredential(ekPub, akPub, nonce);
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
                handlePlatformComponents(platformCredential);
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
        BaseReferenceManifest dbBaseRim = null;
        SupportReferenceManifest support;
        EventLogMeasurements measurements;
        String tagId = "";
        String fileName = "";
        Pattern pattern = Pattern.compile("([^\\s]+(\\.(?i)(rimpcr|rimel|bin|log))$)");
        Matcher matcher;
        MessageDigest messageDigest =  MessageDigest.getInstance("SHA-256");

        if (dv.getLogfileCount() > 0) {
            for (ByteString logFile : dv.getLogfileList()) {
                try {
                    support = (SupportReferenceManifest) referenceManifestRepository.findByHexDecHashAndRimType(
                                    Hex.encodeHexString(messageDigest.digest(logFile.toByteArray())),
                            ReferenceManifest.SUPPORT_RIM);
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
                    log.error(String.format("Failed to load support rim: %s", ex.getMessage()));
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
                    tagId = dbBaseRim.getTagId();
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
                            .findByHexDecHashAndRimType(swid.getHashValue(), ReferenceManifest.SUPPORT_RIM);
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
                    }
                }
                this.referenceManifestRepository.save(dbBaseRim);
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

                List<BaseReferenceManifest> baseRims = referenceManifestRepository
                        .getBaseByManufacturerModel(dv.getHw().getManufacturer(),
                                dv.getHw().getProductName());
                measurements = temp;
                measurements.setPlatformManufacturer(dv.getHw().getManufacturer());
                measurements.setPlatformModel(dv.getHw().getProductName());
                measurements.setTagId(tagId);
                measurements.setDeviceName(dv.getNw().getHostname());
                measurements.archive();

                this.referenceManifestRepository.save(measurements);

                for (BaseReferenceManifest baseRim : baseRims) {
                    if (baseRim != null) {
                        // pull the base versions of the swidtag and rimel and set the
                        // event log hash for use during provision
                        SupportReferenceManifest sBaseRim = referenceManifestRepository
                                .getSupportRimEntityById(baseRim.getAssociatedRim());
                        baseRim.setEventLogHash(temp.getHexDecHash());
                        sBaseRim.setEventLogHash(temp.getHexDecHash());
                        referenceManifestRepository.save(baseRim);
                        referenceManifestRepository.save(sBaseRim);
                    }
                }
            } catch (IOException ioEx) {
                log.error(ioEx);
            }
        } else {
            log.warn(String.format("%s did not send bios measurement log...",
                    dv.getNw().getHostname()));
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

    private int handlePlatformComponents(final Certificate certificate) {
        PlatformCredential platformCredential;
        int componentResults = 0;
        if (certificate instanceof PlatformCredential) {
            platformCredential = (PlatformCredential) certificate;
            ComponentResult componentResult;
            for (ComponentIdentifier componentIdentifier : platformCredential
                    .getComponentIdentifiers()) {

                componentResult = new ComponentResult(platformCredential.getPlatformSerial(),
                        platformCredential.getSerialNumber().toString(),
                        platformCredential.getPlatformChainType(),
                        componentIdentifier);
                componentResult.setMismatched(false);
                componentResultRepository.save(componentResult);
                componentResults++;
            }
        }
        return componentResults;
    }

    private int handleDeviceComponents(final String hostName, final String paccorString) {
        int deviceComponents = 0 ;
        Map<Integer, ComponentInfo> componentInfoMap = new HashMap<>();
        try {
            List<ComponentInfo> componentInfos = SupplyChainCredentialValidator
                    .getComponentInfoFromPaccorOutput(hostName, paccorString);

            // check the DB for like component infos
            List<ComponentInfo> dbComponentInfos = this.componentInfoRepository.findByDeviceName(hostName);
            dbComponentInfos.stream().forEach((infos) -> {
                componentInfoMap.put(infos.hashCode(), infos);
            });

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

        return deviceComponents;
    }
}
