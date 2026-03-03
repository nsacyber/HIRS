package hirs.attestationca.persist.provision.service;

import com.google.protobuf.ByteString;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
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
import hirs.utils.HexUtils;
import hirs.utils.SwidResource;
import hirs.utils.enums.DeviceInfoEnums;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import jakarta.xml.bind.UnmarshalException;
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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Log4j2
public class DeviceManagementService {
    private static final int NUM_OF_VARIABLES = 5;
    private static final int MAC_BYTES = 6;

    private final DeviceRepository deviceRepository;
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;

    @Autowired
    public DeviceManagementService(final DeviceRepository deviceRepository,
                                   final ReferenceManifestRepository referenceManifestRepository,
                                   final ReferenceDigestValueRepository referenceDigestValueRepository) {
        this.deviceRepository = deviceRepository;
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
    }

    /**
     * Utilizes the identity claim to produce a device info report.
     *
     * @param claim identity claim
     * @return device info
     */
    public Device processDeviceInfo(final ProvisionerTpm2.IdentityClaim claim) {
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
     * Helper method that creates a Device Info Report objec using the provided protobuf identity claim's device info.
     *
     * @param protoIdentityClaim the protobuf serialized identity claim containing the device info
     * @return {@link DeviceInfoReport}
     */
    private DeviceInfoReport parseDeviceInfo(final ProvisionerTpm2.IdentityClaim protoIdentityClaim)
            throws NoSuchAlgorithmException {
        ProvisionerTpm2.DeviceInfo deviceInfoProto = protoIdentityClaim.getDv();

        String pcrValues = "";

        if (deviceInfoProto.hasPcrslist()) {
            pcrValues = deviceInfoProto.getPcrslist().toStringUtf8();
        }

        // Get Hardware info
        HardwareInfo hardwareInfo = getHardwareInfo(deviceInfoProto.getHw());

        retrieveDeviceInfoFromRIMs(deviceInfoProto, hardwareInfo);

        // Get TPM info, currently unimplemented
        TPMInfo tpmInfo = new TPMInfo(DeviceInfoEnums.NOT_SPECIFIED,
                (short) 0,
                (short) 0,
                (short) 0,
                (short) 0,
                pcrValues.getBytes(StandardCharsets.UTF_8),
                null, null);

        // Get Network info
        NetworkInfo networkInfo = getNetworkInfo(deviceInfoProto.getNw());

        // Get Firmware info
        FirmwareInfo firmwareInfo = getFirmwareInfo(deviceInfoProto.getFw());

        // Get OS info
        OSInfo osInfo = getOSInfo(deviceInfoProto.getOs());

        // Create final report
        DeviceInfoReport dvReport = new DeviceInfoReport(networkInfo, osInfo, firmwareInfo, hardwareInfo, tpmInfo,
                protoIdentityClaim.getClientVersion());
        dvReport.setPaccorOutputString(protoIdentityClaim.getPaccorOutput());

        return dvReport;
    }

    /**
     * Helper method that creates a hardware info object using the provided protobuf's version of hardware info.
     *
     * @param hardwareInfoProto Protobuf's version of Hardware Info
     * @return {@link HardwareInfo}
     */
    private HardwareInfo getHardwareInfo(ProvisionerTpm2.HardwareInfo hardwareInfoProto) {

        // Make sure chassis info has at least one chassis
        String firstChassisSerialNumber = DeviceInfoEnums.NOT_SPECIFIED;
        if (hardwareInfoProto.getChassisInfoCount() > 0) {
            firstChassisSerialNumber = hardwareInfoProto.getChassisInfo(0).getSerialNumber();
        }

        // Make sure baseboard info has at least one baseboard
        String firstBaseboardSerialNumber = DeviceInfoEnums.NOT_SPECIFIED;
        if (hardwareInfoProto.getBaseboardInfoCount() > 0) {
            firstBaseboardSerialNumber = hardwareInfoProto.getBaseboardInfo(0).getSerialNumber();
        }

        return new HardwareInfo(hardwareInfoProto.getManufacturer(), hardwareInfoProto.getProductName(),
                hardwareInfoProto.getProductVersion(), hardwareInfoProto.getSystemSerialNumber(),
                firstChassisSerialNumber, firstBaseboardSerialNumber);
    }

    /**
     * @param networkInfoProto Protobuf's version of Network Info
     * @return {@link NetworkInfo}
     */
    private NetworkInfo getNetworkInfo(ProvisionerTpm2.NetworkInfo networkInfoProto) {
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(networkInfoProto.getIpAddress());
        } catch (UnknownHostException uhEx) {
            log.error("Unable to parse IP address: ", uhEx);
        }
        String[] macAddressParts = networkInfoProto.getMacAddress().split(":");

        // convert mac hex string to byte values
        byte[] macAddressBytes = new byte[MAC_BYTES];
        Integer hex;
        if (macAddressParts.length == MAC_BYTES) {
            for (int i = 0; i < MAC_BYTES; i++) {
                hex = HexUtils.hexToInt(macAddressParts[i]);
                macAddressBytes[i] = hex.byteValue();
            }
        }

        return new NetworkInfo(networkInfoProto.getHostname(), ip, macAddressBytes);
    }

    /**
     * @param osInfoProto Protobuf's version of OS Info
     * @return {@link OSInfo}
     */
    private OSInfo getOSInfo(ProvisionerTpm2.OsInfo osInfoProto) {
        return new OSInfo(osInfoProto.getOsName(), osInfoProto.getOsVersion(), osInfoProto.getOsArch(),
                osInfoProto.getDistribution(), osInfoProto.getDistributionRelease());
    }

    /**
     * @param firmwareInfoProto Protobuf's version of Firmware Info
     * @return {@link FirmwareInfo}
     */
    private FirmwareInfo getFirmwareInfo(ProvisionerTpm2.FirmwareInfo firmwareInfoProto) {
        return new FirmwareInfo(firmwareInfoProto.getBiosVendor(), firmwareInfoProto.getBiosVersion(),
                firmwareInfoProto.getBiosReleaseDate());
    }

    /**
     * @param deviceInfoProto
     * @param hw
     * @throws NoSuchAlgorithmException
     */
    private void retrieveDeviceInfoFromRIMs(ProvisionerTpm2.DeviceInfo deviceInfoProto, HardwareInfo hw)
            throws NoSuchAlgorithmException {

        // check for RIM Base and Support files, if they don't exist in the database, load them
        final String defaultClientName = String.format("%s_%s", deviceInfoProto.getHw().getManufacturer(),
                deviceInfoProto.getHw().getProductName());

        BaseReferenceManifest baseRim = null;
        SupportReferenceManifest supportRim = null;
        EventLogMeasurements integrityMeasurements;
        boolean isReplacement = false;
        String replacementRimId = "";
        String tagId = "";
        String fileName = "";
        Pattern pattern = Pattern.compile("(\\S+(\\.(?i)(rimpcr|rimel|bin|log))$)");
        Matcher matcher;
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        if (deviceInfoProto.getSwidfileCount() > 0) {
            for (ByteString swidFile : deviceInfoProto.getSwidfileList()) {
                try {
                    baseRim = (BaseReferenceManifest) referenceManifestRepository.findByBase64Hash(Base64.getEncoder()
                            .encodeToString(messageDigest.digest(swidFile.toByteArray())));

                    if (baseRim == null) {
                        /*
                        Either the swidFile does not have a corresponding base RIM in the backend
                        or it was deleted. Check if there is a replacement by comparing tagId against
                        all other base RIMs, and then set the corresponding support rim's deviceName.
                         */
                        baseRim = new BaseReferenceManifest(String.format("%s.swidtag", defaultClientName),
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
                        baseRim.setDeviceName(deviceInfoProto.getNw().getHostname());
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
                                baseRim.setDeviceName(deviceInfoProto.getNw().getHostname());
                            }
                        }
                        if (baseRim.isArchived()) {
                            throw new Exception("Unable to locate an unarchived base RIM.");
                        } else {
                            this.referenceManifestRepository.save(baseRim);
                        }
                    } else {
                        baseRim.setDeviceName(deviceInfoProto.getNw().getHostname());
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
            log.warn("{} did not send swid tag file...", deviceInfoProto.getNw().getHostname());
        }

        if (deviceInfoProto.getLogfileCount() > 0) {
            for (ByteString logFile : deviceInfoProto.getLogfileList()) {
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
                                supportRim.setDeviceName(deviceInfoProto.getNw().getHostname());
                            } else {
                                throw new Exception("Unable to locate support RIM " + replacementRimId);
                            }
                        } else {
                            supportRim = new SupportReferenceManifest(String.format("%s.rimel", defaultClientName),
                                    logFile.toByteArray());
                            // this is a validity check
                            new TCGEventLog(supportRim.getRimBytes());
                            // no issues, continue
                            supportRim.setPlatformManufacturer(deviceInfoProto.getHw().getManufacturer());
                            supportRim.setPlatformModel(deviceInfoProto.getHw().getProductName());
                            supportRim.setFileName(String.format("%s_[%s].rimel", defaultClientName,
                                    supportRim.getHexDecHash().substring(
                                            supportRim.getHexDecHash().length() - NUM_OF_VARIABLES)));
                        }
                        supportRim.setDeviceName(deviceInfoProto.getNw().getHostname());
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
                                supportRim.setDeviceName(deviceInfoProto.getNw().getHostname());
                            }
                        }
                        if (supportRim.isArchived()) {
                            throw new Exception("Unable to locate an unarchived support RIM.");
                        } else {
                            this.referenceManifestRepository.save(supportRim);
                        }
                    } else {
                        supportRim.setDeviceName(deviceInfoProto.getNw().getHostname());
                        this.referenceManifestRepository.save(supportRim);
                    }
                } catch (IOException ioEx) {
                    log.error(ioEx);
                } catch (Exception ex) {
                    log.error("Failed to load support rim: {}", ex.getMessage());
                }
            }
        } else {
            log.warn("{} did not send support RIM file...", deviceInfoProto.getNw().getHostname());
        }

        //update Support RIMs and Base RIMs.
        for (ByteString swidFile : deviceInfoProto.getSwidfileList()) {
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

        if (deviceInfoProto.hasLivelog()) {
            log.info("Device sent bios measurement log...");
            fileName = String.format("%s.measurement", deviceInfoProto.getNw().getHostname());
            try {
                EventLogMeasurements deviceLiveLog = new EventLogMeasurements(fileName,
                        deviceInfoProto.getLivelog().toByteArray());
                // find previous version.
                integrityMeasurements = referenceManifestRepository.byMeasurementDeviceNameUnarchived(
                        deviceInfoProto.getNw().getHostname());

                if (integrityMeasurements != null) {
                    // Find previous log and archive it
                    integrityMeasurements.archive();
                    this.referenceManifestRepository.save(integrityMeasurements);
                }

                List<BaseReferenceManifest> baseRims = referenceManifestRepository.getBaseByManufacturerModel(
                        deviceInfoProto.getHw().getManufacturer(),
                        deviceInfoProto.getHw().getProductName());

                integrityMeasurements = deviceLiveLog;
                integrityMeasurements.setPlatformManufacturer(deviceInfoProto.getHw().getManufacturer());
                integrityMeasurements.setPlatformModel(deviceInfoProto.getHw().getProductName());

                if (tagId != null && !tagId.trim().isEmpty()) {
                    integrityMeasurements.setTagId(tagId);
                }

                integrityMeasurements.setDeviceName(deviceInfoProto.getNw().getHostname());

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
                            log.warn("Could not locate support RIM associated with base RIM {}", bRim.getId());
                        }
                    }
                }
            } catch (IOException ioEx) {
                log.error(ioEx);
            }
        } else {
            log.warn("{} did not send bios measurement log...", deviceInfoProto.getNw().getHostname());
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
}
