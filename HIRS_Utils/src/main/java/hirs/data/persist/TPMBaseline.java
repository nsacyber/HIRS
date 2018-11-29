package hirs.data.persist;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class represents a Trusted Platform Module (TPM) measurement baseline. A TPM baseline consists of
 * a set of <code>TPMMeasurementRecord</code>s. The set of
 * <code>TPMMeasurementRecord</code>s may have multiple entries for the same PCR ID. This is useful
 * for scenarios where the PCR may have a few possible valid entries.
 */
@Entity
public abstract class TPMBaseline extends Baseline {

    private static final Logger LOGGER = LogManager.getLogger(TPMBaseline.class);
    private static final String NOT_SPECIFIED = "Not Specified";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "TPMBaselineRecords",
        joinColumns = { @JoinColumn(name = "BaselineID", nullable = false) })
    private final Set<TPMMeasurementRecord> pcrRecords = new LinkedHashSet<>();


    @Embedded
    private FirmwareInfo firmwareInfo;

    @Embedded
    private HardwareInfo hardwareInfo;

    @Embedded
    private OSInfo osInfo;

    @Embedded
    private TPMInfo tpmInfo;


    /**
     * Creates a new <code>TPMBaseline</code> with no valid PCR entries and no device-specific PCRs.
     *
     * @param name
     *            a name used to uniquely identify and reference the PCR baseline
     */
    public TPMBaseline(final String name) {
        super(name);
        initDeviceInfo();
    }

    /**
     * Default constructor necessary for Hibernate and BaselineAlertResolver.
     */
    public TPMBaseline() {
        super();
        initDeviceInfo();
    }



    private void initDeviceInfo() {
        initFirmwareInfo();
        initHardwareInfo();
        initOSInfo();
        initTPMInfo();
    }

    /**
     * Creates default FirmwareInfo object.
     */
    private void initFirmwareInfo() {
        firmwareInfo = new FirmwareInfo();
    }


    /**
     * Creates default HardwareInfo object.
     */
    private void initHardwareInfo() {
        hardwareInfo =
                new HardwareInfo();
    }

    /**
     * Creates default OSInfo object.
     */
    private void initOSInfo() {
        osInfo =
                new OSInfo();
    }

    /**
     * Creates default TPMInfo object.
     */
    private void initTPMInfo() {
        tpmInfo =
                new TPMInfo();
    }


    /**
     * Retrieves the FirmwareInfo for this <code>TPMBaseline</code>.
     * @return FirmwareInfo
     */
    public final FirmwareInfo getFirmwareInfo() {
        return firmwareInfo;
    }

    /**
     * Retrieves the HardwareInfo for this <code>TPMBaseline</code>.
     * @return FirmwareInfo
     */
    public final HardwareInfo getHardwareInfo() {
        return hardwareInfo;
    }

    /**
     * Retrieves the OSInfo for this <code>TPMBaseline</code>.
     * @return FirmwareInfo
     */
    public final OSInfo getOSInfo() {
        return osInfo;
    }

    /**
     * Retrieves the TPMInfo for this <code>TPMBaseline</code>.
     * @return FirmwareInfo
     */
    public final TPMInfo getTPMInfo() {
        return tpmInfo;
    }

    /**
     * Copy the Firmware data from another object.  If null, the default
     * FirmwareInfo data will be used.
     * @param firmwareInfo FirmwareInfo object or null.
     */
    public final void setFirmwareInfo(final FirmwareInfo firmwareInfo) {
        if (firmwareInfo == null) {
            initFirmwareInfo();
        } else {
            this.firmwareInfo = firmwareInfo;
        }
    }

    /**
     * Copy the Hardware data from another object.  If null, the default
     * HardwareInfo data will be used.
     * @param hardwareInfo HardwareInfo object or null.
     */
    public final void setHardwareInfo(final HardwareInfo hardwareInfo) {
        if (hardwareInfo == null) {
            initHardwareInfo();
        } else {
            this.hardwareInfo = hardwareInfo;
        }
    }

    /**
     * Copy the OSInfo data from another object.  If null, the default
     * OSInfo data will be used.
     * @param osInfo OSInfo object or null.
     */
    public final void setOSInfo(final OSInfo osInfo) {
        if (osInfo == null) {
            initOSInfo();
        } else {
            this.osInfo = osInfo;
        }
    }

    /**
     * Copy the TPMInfo data from another object.  If null, the default
     * TPMInfo data will be used.
     * @param tpmInfo TPMInfo object or null.
     */
    public final void setTPMInfo(final TPMInfo tpmInfo) {
        if (tpmInfo == null) {
            initTPMInfo();
        } else {
            this.tpmInfo = tpmInfo;
        }
    }

    /**
     * Returns acceptable hash values for a PCR ID. This returns a set of all the acceptable PCR
     * hash values. The set may be empty if none are found.
     *
     * @param pcrId
     *            PCR index
     * @return list of acceptable hash values
     */
    public final Set<Digest> getPCRHashes(final int pcrId) {
        TPMMeasurementRecord.checkForValidPcrId(pcrId);
        final Set<Digest> ret = new LinkedHashSet<>();
        for (TPMMeasurementRecord record : pcrRecords) {
            if (record.getPcrId() == pcrId) {
                ret.add(record.getHash());
            }
        }
        return ret;
    }

    /**
     * Returns a set of all PCR records associated with baseline.
     *
     * @return set of PCR records found in TPM baseline
     */
    public final Set<TPMMeasurementRecord> getPcrRecords() {
        return Collections.unmodifiableSet(pcrRecords);
    }

    /**
     * Searches this baseline for the supplied <code>TPMMeasurementRecord</code> . This returns true
     * if a measurement record was found with a matching PCR ID and hash value. Otherwise this
     * returns false.
     *
     * @param record
     *            record to find
     * @return true if measurement record is found in list, otherwise false
     */
    public final boolean isInBaseline(final TPMMeasurementRecord record) {
        if (record == null) {
            return false;
        }
        return pcrRecords.contains(record);
    }

    /**
     * Adds a PCR measurement record to baseline. If there is already a measurement with the same
     * PCR ID and hash value then an <code>IllegalArgumentException</code> is thrown.
     *
     * @param record
     *            record to add to TPM baseline
     */
    public final void addToBaseline(final TPMMeasurementRecord record) {
        LOGGER.debug("adding record {} to baseline {}", record, getName());
        if (record == null) {
            LOGGER.error("null record");
            throw new NullPointerException("record");
        }

        if (pcrRecords.contains(record)) {
            final String msg = String.format("record already exist: %s", record);
            LOGGER.info(msg);
            throw new IllegalArgumentException(msg);
        }

        pcrRecords.add(record);
        LOGGER.debug("record added");
    }

    /**
     * Removes the <code>TPMMeasurementRecord</code> from this baseline. If the record is found and
     * successfully removed then true is returned. Otherwise false is returned.
     *
     * @param record
     *            record to remove from baseline
     * @return true if found and removed, otherwise false
     */
    public final boolean removeFromBaseline(final TPMMeasurementRecord record) {
        LOGGER.debug("removing record {} from baseline {}", record, getName());
        if (record == null) {
            LOGGER.error("null record");
            return false;
        }

        return pcrRecords.remove(record);
    }

    /**
     * Checks the properties of FirmwareInfo, HardwareInfo, OSInfo, and TPMInfo and the contents of
     * pcrRecords to determine if this instance of TPMBaseline is empty or not.
     *
     * @return true if baseline has no data
     */
    public boolean isEmpty() {
        LOGGER.debug("Check for empty baseline");
        return (firmwareInfo.getBiosReleaseDate().equals(NOT_SPECIFIED)
                && firmwareInfo.getBiosVendor().equals(NOT_SPECIFIED)
                && firmwareInfo.getBiosVersion().equals(NOT_SPECIFIED)
                && hardwareInfo.getBaseboardSerialNumber().equals(NOT_SPECIFIED)
                && hardwareInfo.getChassisSerialNumber().equals(NOT_SPECIFIED)
                && hardwareInfo.getManufacturer().equals(NOT_SPECIFIED)
                && hardwareInfo.getProductName().equals(NOT_SPECIFIED)
                && hardwareInfo.getSystemSerialNumber().equals(NOT_SPECIFIED)
                && hardwareInfo.getVersion().equals(NOT_SPECIFIED)
                && osInfo.getDistribution().equals(NOT_SPECIFIED)
                && osInfo.getDistributionRelease().equals(NOT_SPECIFIED)
                && osInfo.getOSArch().equals(NOT_SPECIFIED)
                && osInfo.getOSName().equals(NOT_SPECIFIED)
                && osInfo.getOSVersion().equals(NOT_SPECIFIED)
                && tpmInfo.getTPMMake().equals(NOT_SPECIFIED)
                && tpmInfo.getTPMVersionMajor() == 0
                && tpmInfo.getTPMVersionMinor() == 0
                && tpmInfo.getTPMVersionRevMajor() == 0
                && tpmInfo.getTPMVersionRevMinor() == 0
                && pcrRecords.isEmpty());
    }
}
