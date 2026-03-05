package hirs.attestationca.persist.provision.service;

import com.google.protobuf.ByteString;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.ComponentInfoRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
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
import hirs.attestationca.persist.exceptions.IdentityProcessingException;
import hirs.attestationca.persist.validation.SupplyChainCredentialValidator;
import hirs.utils.HexUtils;
import hirs.utils.SwidResource;
import hirs.utils.enums.DeviceInfoEnums;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import io.micrometer.common.util.StringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service class that parses and processes Device information from the Identity Claim and uses it to update the Device
 * and RIMs.
 */
@Service
@Log4j2
public class DeviceInfoProcessorService {
    private static final int MAC_BYTES = 6;

    private final DeviceRepository deviceRepository;
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final ComponentInfoRepository componentInfoRepository;
    private final MessageDigest messageDigest;

    /**
     * Constructor.
     *
     * @param deviceRepository               device repository
     * @param referenceManifestRepository    reference manifest repository
     * @param referenceDigestValueRepository reference digest value repository
     * @param componentInfoRepository        component info repository
     * @throws NoSuchAlgorithmException if any issues arise from creating a {@link MessageDigest}
     */
    @Autowired
    public DeviceInfoProcessorService(final DeviceRepository deviceRepository,
                                      final ReferenceManifestRepository referenceManifestRepository,
                                      final ReferenceDigestValueRepository referenceDigestValueRepository,
                                      final ComponentInfoRepository componentInfoRepository)
            throws NoSuchAlgorithmException {
        this.deviceRepository = deviceRepository;
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.componentInfoRepository = componentInfoRepository;
        this.messageDigest = MessageDigest.getInstance("SHA-256");
    }

    /**
     * Creates a {@link DeviceInfoReport} from the identity claim's Device Info and updates the corresponding
     * {@link Device}.
     *
     * @param identityClaim Identity Claim
     * @return device
     */
    public Device processDeviceInfo(final ProvisionerTpm2.IdentityClaim identityClaim) {
        DeviceInfoReport deviceInfoReport = null;

        log.info("Parsing Device Info from the Identity Claim");

        try {
            deviceInfoReport = parseDeviceInfo(identityClaim);
        } catch (NoSuchAlgorithmException noSaEx) {
            log.error(noSaEx);
        }

        if (deviceInfoReport == null) {
            final String errorMsg = "Failed to parse device info from Protobuf Identity Claim.";
            log.error(errorMsg);
            throw new IdentityProcessingException(errorMsg);
        }

        log.info("Processing Device Info Report");

        // store device and device info report.
        Device device = null;

        if (deviceInfoReport.getNetworkInfo() != null &&
                !StringUtils.isBlank(deviceInfoReport.getNetworkInfo().getHostname())) {
            device = this.deviceRepository.findByName(deviceInfoReport.getNetworkInfo().getHostname());
        }

        if (device == null) {
            device = new Device(deviceInfoReport);
        }
        device.setDeviceInfo(deviceInfoReport);

        processDeviceComponents(deviceInfoReport.getNetworkInfo().getHostname(),
                deviceInfoReport.getPaccorOutputString());

        return this.deviceRepository.save(device);
    }

    /**
     * Helper method that creates a Device Info Report using the provided Identity Claim's Device Info.
     *
     * @param identityClaim the protobuf serialized Identity Claim containing the device info
     * @return {@link DeviceInfoReport}
     */
    private DeviceInfoReport parseDeviceInfo(final ProvisionerTpm2.IdentityClaim identityClaim)
            throws NoSuchAlgorithmException {
        ProvisionerTpm2.DeviceInfo tpmDeviceInfo = identityClaim.getDv();

        // Get Hardware info
        HardwareInfo hardwareInfo = getHardwareInfo(tpmDeviceInfo.getHw());

        // Get TPM info ( todo Currently unimplemented)
        TPMInfo tpmInfo = getTPMInfo(tpmDeviceInfo);

        // Get Network info
        NetworkInfo networkInfo = getNetworkInfo(tpmDeviceInfo.getNw());

        // Get Firmware info
        FirmwareInfo firmwareInfo = getFirmwareInfo(tpmDeviceInfo.getFw());

        // Get OS info
        OSInfo osInfo = getOSInfo(tpmDeviceInfo.getOs());

        getAndUpdateRIMSUsingTPMDeviceInfo(tpmDeviceInfo);

        // Create final device info report
        DeviceInfoReport dvReport = new DeviceInfoReport(networkInfo, osInfo, firmwareInfo, hardwareInfo, tpmInfo,
                identityClaim.getClientVersion());
        dvReport.setPaccorOutputString(identityClaim.getPaccorOutput());

        return dvReport;
    }

    /**
     * Helper method that creates a {@link TPMInfo} using the provided TPM Device Info ( todo Currently unimplemented)
     *
     * @param tpmDeviceInfo TPM Device Info
     * @return {@link TPMInfo}
     */
    private TPMInfo getTPMInfo(ProvisionerTpm2.DeviceInfo tpmDeviceInfo) {
        String pcrValues = "";

        if (tpmDeviceInfo.hasPcrslist()) {
            pcrValues = tpmDeviceInfo.getPcrslist().toStringUtf8();
        }

        return new TPMInfo(DeviceInfoEnums.NOT_SPECIFIED,
                (short) 0,
                (short) 0,
                (short) 0,
                (short) 0,
                pcrValues.getBytes(StandardCharsets.UTF_8),
                null, null);
    }

    /**
     * Helper method that creates a {@link HardwareInfo} using the provided TPM Device Info's Hardware Info.
     *
     * @param tpmHardwareInfo TPM Device Info's Hardware Info
     * @return {@link HardwareInfo}
     */
    private HardwareInfo getHardwareInfo(final ProvisionerTpm2.HardwareInfo tpmHardwareInfo) {

        // Make sure chassis info has at least one chassis
        String firstChassisSerialNumber = DeviceInfoEnums.NOT_SPECIFIED;
        if (tpmHardwareInfo.getChassisInfoCount() > 0) {
            firstChassisSerialNumber = tpmHardwareInfo.getChassisInfo(0).getSerialNumber();
        }

        // Make sure baseboard info has at least one baseboard
        String firstBaseboardSerialNumber = DeviceInfoEnums.NOT_SPECIFIED;
        if (tpmHardwareInfo.getBaseboardInfoCount() > 0) {
            firstBaseboardSerialNumber = tpmHardwareInfo.getBaseboardInfo(0).getSerialNumber();
        }

        return new HardwareInfo(tpmHardwareInfo.getManufacturer(), tpmHardwareInfo.getProductName(),
                tpmHardwareInfo.getProductVersion(), tpmHardwareInfo.getSystemSerialNumber(),
                firstChassisSerialNumber, firstBaseboardSerialNumber);
    }

    /**
     * Helper method that creates a {@link NetworkInfo} using the provided TPM Device Info's Network Info.
     *
     * @param tpmNetworkInfo TPM Device Info's Network Info
     * @return {@link NetworkInfo}
     */
    private NetworkInfo getNetworkInfo(final ProvisionerTpm2.NetworkInfo tpmNetworkInfo) {
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(tpmNetworkInfo.getIpAddress());
        } catch (UnknownHostException uhEx) {
            log.error("Unable to parse IP address from the TPM Device Info: ", uhEx);
        }
        String[] macAddressParts = tpmNetworkInfo.getMacAddress().split(":");

        // convert mac hex string to byte values
        byte[] macAddressBytes = new byte[MAC_BYTES];

        if (macAddressParts.length == MAC_BYTES) {
            for (int i = 0; i < MAC_BYTES; i++) {
                Integer hex = HexUtils.hexToInt(macAddressParts[i]);
                macAddressBytes[i] = hex.byteValue();
            }
        }

        return new NetworkInfo(tpmNetworkInfo.getHostname(), ip, macAddressBytes);
    }

    /**
     * Helper method that creates an {@link OSInfo} using the provided TPM Device Info's OS info.
     *
     * @param tpmOsInfo TPM Device Info's OS Info
     * @return {@link OSInfo}
     */
    private OSInfo getOSInfo(final ProvisionerTpm2.OsInfo tpmOsInfo) {
        return new OSInfo(tpmOsInfo.getOsName(), tpmOsInfo.getOsVersion(), tpmOsInfo.getOsArch(),
                tpmOsInfo.getDistribution(), tpmOsInfo.getDistributionRelease());
    }

    /**
     * Helper method that creates a {@link FirmwareInfo}  using the provided TPM Device Info's Firmware Info.
     *
     * @param tpmFirmwareInfo TPM Device Info's Firmware Info
     * @return {@link FirmwareInfo}
     */
    private FirmwareInfo getFirmwareInfo(final ProvisionerTpm2.FirmwareInfo tpmFirmwareInfo) {
        return new FirmwareInfo(tpmFirmwareInfo.getBiosVendor(), tpmFirmwareInfo.getBiosVersion(),
                tpmFirmwareInfo.getBiosReleaseDate());
    }

    /**
     * Helper method that updates all Reference Integrity Manifests (RIMs) using the provided TPM's Device Info.
     *
     * @param tpmDeviceInfo TPM Device Info
     */
    private void getAndUpdateRIMSUsingTPMDeviceInfo(ProvisionerTpm2.DeviceInfo tpmDeviceInfo) {
        // check for RIM Base and Support files, if they don't exist in the database, load them
        final String defaultClientName = String.format("%s_%s", tpmDeviceInfo.getHw().getManufacturer(),
                tpmDeviceInfo.getHw().getProductName());

        final String deviceInfoHostName = tpmDeviceInfo.getNw().getHostname();

        // update base RIMs using the identity claim's device information
        if (tpmDeviceInfo.getSwidfileCount() > 0) {
            updateBaseRIMSUsingTPMDeviceInfo(defaultClientName, tpmDeviceInfo);
        } else {
            log.warn("Device {} did not send SWID tag files...", deviceInfoHostName);
        }

        // update support RIMs using the identity claim's device information
        if (tpmDeviceInfo.getLogfileCount() > 0) {
            updateSupportRIMSUsingTPMDeviceInfo(defaultClientName, tpmDeviceInfo);
        } else {
            log.warn("Device {} did not send Support RIM files...", deviceInfoHostName);
        }

        // update both base and support RIMs to ensure updates are consistent
        updateBaseSupportRIMSUsingTpmDeviceInfo(tpmDeviceInfo);

        generateDigestRecords(tpmDeviceInfo.getHw().getManufacturer(), tpmDeviceInfo.getHw().getProductName());

        // update event log information
        if (tpmDeviceInfo.hasLivelog()) {
            updateEventLogInfoUsingTPMDeviceInfo(tpmDeviceInfo);
        } else {
            log.warn("Device {} did not send BIOS measurement log...", deviceInfoHostName);
        }
    }

    /**
     * Helper method that updates the Base RIMs in the database using the information provided by the TPM's Device Info.
     *
     * @param defaultClientName default client name
     * @param tpmDeviceInfo     TPM Device Info
     */
    private void updateBaseRIMSUsingTPMDeviceInfo(final String defaultClientName,
                                                  final ProvisionerTpm2.DeviceInfo tpmDeviceInfo) {
        final List<BaseReferenceManifest> baseRims = referenceManifestRepository.findAllBaseRims();
        final List<ReferenceManifest> unarchivedRims = referenceManifestRepository.findByArchiveFlag(false);
        final String deviceHostName = tpmDeviceInfo.getNw().getHostname();

        log.info("Device {} sent SWID tag files", deviceHostName);

        for (ByteString swidFile : tpmDeviceInfo.getSwidfileList()) {
            try {
                final String swidFileHash =
                        Base64.getEncoder().encodeToString(messageDigest.digest(swidFile.toByteArray()));

                final BaseReferenceManifest baseRim =
                        (BaseReferenceManifest) referenceManifestRepository.findByBase64Hash(swidFileHash);
                /*
                Either the swidFile does not have a corresponding base RIM in the backend,
                or it was deleted. Check if there is a replacement by comparing tagId against
                all other base RIMs, and then set the corresponding support rim's deviceName. */
                if (baseRim == null) {
                    final BaseReferenceManifest replacementBaseRIM =
                            new BaseReferenceManifest(String.format("%s.swidtag", defaultClientName),
                                    swidFile.toByteArray());
                    replacementBaseRIM.setDeviceName(deviceHostName);

                    Optional<BaseReferenceManifest> matchedReplacementBaseRIMOptional = baseRims.stream()
                            .filter(bRim ->
                                    bRim.getTagId().equals(replacementBaseRIM.getTagId()))
                            .findFirst();

                    // if there is a match, save the matched base RIM
                    if (matchedReplacementBaseRIMOptional.isPresent()) {
                        final BaseReferenceManifest matchedReplacementBaseRIM =
                                matchedReplacementBaseRIMOptional.get();
                        matchedReplacementBaseRIM.setDeviceName(replacementBaseRIM.getDeviceName());
                        this.referenceManifestRepository.save(matchedReplacementBaseRIM);
                        continue;
                    }

                    // otherwise save the replacement base RIM we created
                    this.referenceManifestRepository.save(replacementBaseRIM);
                } else if (baseRim.isArchived()) {
                        /*  This block accounts for RIMs that may have been soft-deleted (archived)
                        in an older version of the ACA. */
                    // Filter out unarchived base RIMs that match the tagId and are newer than the baseRim
                    Optional<BaseReferenceManifest> matchedUnarchivedBaseRIMOptional = unarchivedRims.stream()
                            .filter(rim -> rim.isBase()
                                    && rim.getTagId().equals(baseRim.getTagId())
                                    && rim.getCreateTime().after(baseRim.getCreateTime()))
                            .map(rim -> (BaseReferenceManifest) rim)
                            .findFirst();

                    if (matchedUnarchivedBaseRIMOptional.isEmpty()) {
                        throw new Exception("Unable to locate an unarchived base RIM.");
                    }

                    final BaseReferenceManifest matchedUnarchivedBaseRIM = matchedUnarchivedBaseRIMOptional.get();
                    matchedUnarchivedBaseRIM.setDeviceName(deviceHostName);
                    this.referenceManifestRepository.save(matchedUnarchivedBaseRIM);
                } else {
                    baseRim.setDeviceName(deviceHostName);
                    this.referenceManifestRepository.save(baseRim);
                }
            } catch (Exception exception) {
                log.error("Failed to process Base RIM file for device {}: {}", deviceHostName,
                        exception.getMessage(), exception);
            }
        }
    }

    /**
     * Helper method that updates the Support RIMs in the database using the information provided by the TPM's Device
     * Info.
     *
     * @param defaultClientName default client name
     * @param tpmDeviceInfo     TPM Device Info
     */
    private void updateSupportRIMSUsingTPMDeviceInfo(final String defaultClientName,
                                                     final ProvisionerTpm2.DeviceInfo tpmDeviceInfo) {
        final String deviceHostName = tpmDeviceInfo.getNw().getHostname();
        final int NUM_OF_VARIABLES = 5;

        log.info("Device {} sent Support RIM files", deviceHostName);

        final List<ReferenceManifest> unarchivedRims = referenceManifestRepository.findByArchiveFlag(false);

        for (ByteString logFile : tpmDeviceInfo.getLogfileList()) {
            try {
                final String logFileHash = Hex.encodeHexString(messageDigest.digest(logFile.toByteArray()));

                final SupportReferenceManifest supportRim =
                        (SupportReferenceManifest) referenceManifestRepository.findByHexDecHashAndRimType(
                                logFileHash, ReferenceManifest.SUPPORT_RIM);

                if (supportRim == null) {
                    /*  Either the logFile does not have a corresponding support RIM in the backend,
                      or it was deleted. The support RIM for a replacement base RIM is handled
                      in the previous loop block. */
                    final SupportReferenceManifest replacementSupportRIM =
                            new SupportReferenceManifest(String.format("%s.rimel", defaultClientName),
                                    logFile.toByteArray());

                    // this is a validity check
                    new TCGEventLog(replacementSupportRIM.getRimBytes());

                    // no issues, continue
                    replacementSupportRIM.setPlatformManufacturer(tpmDeviceInfo.getHw().getManufacturer());
                    replacementSupportRIM.setPlatformModel(tpmDeviceInfo.getHw().getProductName());
                    replacementSupportRIM.setFileName(String.format("%s_[%s].rimel", defaultClientName,
                            replacementSupportRIM.getHexDecHash().substring(
                                    replacementSupportRIM.getHexDecHash().length() - NUM_OF_VARIABLES)));
                    replacementSupportRIM.setDeviceName(deviceHostName);
                    this.referenceManifestRepository.save(replacementSupportRIM);
                } else if (supportRim.isArchived()) {
                    /*
                     This block accounts for RIMs that may have been soft-deleted (archived)
                     in an older version of the ACA.
                     */
                    // Filter out unarchived support RIMs that match the tagId and are newer than the support RIM
                    Optional<SupportReferenceManifest> matchedUnarchivedSupportRIMOptional = unarchivedRims.stream()
                            .filter(rim -> rim.isSupport()
                                    && rim.getTagId().equals(supportRim.getTagId())
                                    && rim.getCreateTime().after(supportRim.getCreateTime()))
                            .map(rim -> (SupportReferenceManifest) rim)
                            .findFirst();

                    if (matchedUnarchivedSupportRIMOptional.isEmpty()) {
                        throw new Exception("Unable to locate an unarchived support RIM.");
                    }

                    final SupportReferenceManifest matchedUnarchivedSupportRIM =
                            matchedUnarchivedSupportRIMOptional.get();
                    matchedUnarchivedSupportRIM.setDeviceName(deviceHostName);
                    this.referenceManifestRepository.save(matchedUnarchivedSupportRIM);
                } else {
                    supportRim.setDeviceName(deviceHostName);
                    this.referenceManifestRepository.save(supportRim);
                }
            } catch (Exception exception) {
                log.error("Failed to process Support RIM file for device {}: {}", deviceHostName,
                        exception.getMessage(), exception);

            }
        }
    }

    /**
     * Helper method that updates both base and support RIMs after modifying each type of RIM, ensuring that the
     * updates are consistent and aligned.
     *
     * @param tpmDeviceInfo TPM Device Info
     */
    private void updateBaseSupportRIMSUsingTpmDeviceInfo(ProvisionerTpm2.DeviceInfo tpmDeviceInfo) {
        final String SUPPORT_RIM_FILE_PATTERN = "(\\S+(\\.(?i)(rimpcr|rimel|bin|log))$)";
        final Pattern supportRimPattern = Pattern.compile(SUPPORT_RIM_FILE_PATTERN);

        //update Support RIMs and Base RIMs.
        for (ByteString swidFile : tpmDeviceInfo.getSwidfileList()) {
            final String swidFileHash =
                    Base64.getEncoder().encodeToString(messageDigest.digest(swidFile.toByteArray()));

            final BaseReferenceManifest baseRim =
                    (BaseReferenceManifest) referenceManifestRepository.findByBase64Hash(swidFileHash);

            if (baseRim != null) {
                for (SwidResource swid : baseRim.getFileResources()) {
                    if (supportRimPattern.matcher(swid.getName()).matches()) {
                        final int dotIndex = swid.getName().lastIndexOf(".");
                        final String fileName = swid.getName().substring(0, dotIndex);
                        baseRim.setFileName(String.format("%s.swidtag", fileName));
                    }

                    // now update support rim
                    SupportReferenceManifest dbSupportRIM = (SupportReferenceManifest) referenceManifestRepository
                            .findByHexDecHashAndRimType(swid.getHashValue(), ReferenceManifest.SUPPORT_RIM);

                    if (dbSupportRIM == null) {
                        log.warn("Could not locate support RIM with hash {}}", swid.getHashValue());
                        continue;
                    }

                    dbSupportRIM.setFileName(swid.getName());
                    dbSupportRIM.setSwidTagVersion(baseRim.getSwidTagVersion());
                    dbSupportRIM.setTagId(baseRim.getTagId());
                    dbSupportRIM.setSwidTagVersion(baseRim.getSwidTagVersion());
                    dbSupportRIM.setSwidVersion(baseRim.getSwidVersion());
                    dbSupportRIM.setSwidPatch(baseRim.isSwidPatch());
                    dbSupportRIM.setSwidSupplemental(baseRim.isSwidSupplemental());
                    dbSupportRIM.setUpdated(true);
                    dbSupportRIM.setAssociatedRim(baseRim.getId());
                    baseRim.setAssociatedRim(dbSupportRIM.getId());
                    this.referenceManifestRepository.save(dbSupportRIM);
                }

                this.referenceManifestRepository.save(baseRim);
            }
        }
    }

    /**
     * Helper method that generates digest records using the provided device's manufacturer and model
     * information.
     *
     * @param manufacturer device manufacturer
     * @param model        device model
     */
    private void generateDigestRecords(final String manufacturer, final String model) {
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

        if (baseSupportRim != null &&
                referenceDigestValueRepository.findBySupportRimHash(baseSupportRim.getHexDecHash()).isEmpty()) {
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
    }

    /**
     * @param tpmDeviceInfo TPM Device Info
     */
    private void updateEventLogInfoUsingTPMDeviceInfo(ProvisionerTpm2.DeviceInfo tpmDeviceInfo) {
        final String deviceHostName = tpmDeviceInfo.getNw().getHostname();
        final String fileName = String.format("%s.measurement", deviceHostName);

        log.info("Device {} sent bios measurement log...", deviceHostName);

        try {
            // grab the event log from TPM's device info
            EventLogMeasurements tpmEventLog = new EventLogMeasurements(fileName,
                    tpmDeviceInfo.getLivelog().toByteArray());

            // find the previous event log that's stored in the database.
            EventLogMeasurements integrityMeasurements =
                    referenceManifestRepository.byMeasurementDeviceNameUnarchived(deviceHostName);

            // if the event log does exist in the database
            if (integrityMeasurements != null) {
                // archive it and update the entity in the database
                integrityMeasurements.archive();
                this.referenceManifestRepository.save(integrityMeasurements);
            }

            List<BaseReferenceManifest> baseRims = referenceManifestRepository.getBaseByManufacturerModel(
                    tpmDeviceInfo.getHw().getManufacturer(),
                    tpmDeviceInfo.getHw().getProductName());

            tpmEventLog.setDeviceName(deviceHostName);
            tpmEventLog.setPlatformManufacturer(tpmDeviceInfo.getHw().getManufacturer());
            tpmEventLog.setPlatformModel(tpmDeviceInfo.getHw().getProductName());
            this.referenceManifestRepository.save(tpmEventLog);

            for (BaseReferenceManifest bRim : baseRims) {
                if (bRim != null) {
                    // pull the base versions of the swidtag and rimel and set the
                    // event log hash for use during provision
                    SupportReferenceManifest sBaseRim = referenceManifestRepository
                            .getSupportRimEntityById(bRim.getAssociatedRim());
                    if (sBaseRim != null) {
                        bRim.setEventLogHash(tpmEventLog.getHexDecHash());
                        sBaseRim.setEventLogHash(tpmEventLog.getHexDecHash());
                        referenceManifestRepository.save(bRim);
                        referenceManifestRepository.save(sBaseRim);
                    } else {
                        log.warn("Could not locate support RIM associated with base RIM {}", bRim.getId());
                    }
                }
            }
        } catch (Exception exception) {
            log.error(exception);
        }
    }

    /**
     * Helper method that attempts to find all the provided device's components and stores them in the database.
     *
     * @param hostName     device's host name
     * @param paccorString string representation of the PACCOR tool output
     */
    private void processDeviceComponents(final String hostName, final String paccorString) {
        Map<Integer, ComponentInfo> componentInfoMap = new HashMap<>();

        try {
            List<ComponentInfo> componentInfos =
                    SupplyChainCredentialValidator.getComponentInfoFromPaccorOutput(hostName, paccorString);

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
