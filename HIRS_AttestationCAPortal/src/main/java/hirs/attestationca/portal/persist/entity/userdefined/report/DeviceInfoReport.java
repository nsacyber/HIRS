package hirs.attestationca.portal.persist.entity.userdefined.report;

import hirs.attestationca.persist.entity.userdefined.Report;
import hirs.attestationca.persist.entity.userdefined.info.FirmwareInfo;
import hirs.attestationca.persist.entity.userdefined.info.HardwareInfo;
import hirs.attestationca.persist.entity.userdefined.info.NetworkInfo;
import hirs.attestationca.persist.entity.userdefined.info.OSInfo;
import hirs.attestationca.persist.entity.userdefined.info.TPMInfo;
import hirs.attestationca.utils.VersionHelper;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.logging.Logger;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * A <code>DeviceInfoReport</code> is a <code>Report</code> used to transfer the
 * information about the device. This <code>Report</code> includes the network,
 * OS, and TPM information.
 */
@Entity
public class DeviceInfoReport extends Report implements Serializable {

    private static final Logger LOGGER = getLogger(DeviceInfoReport.class);

    /**
     * A variable used to describe unavailable hardware, firmware, or OS info.
     */
    public static final String NOT_SPECIFIED = "Not Specified";
    /**
     * Constant variable representing the various Short sized strings.
     */
    public static final int SHORT_STRING_LENGTH = 32;
    /**
     * Constant variable representing the various Medium sized strings.
     */
    public static final int MED_STRING_LENGTH = 64;
    /**
     * Constant variable representing the various Long sized strings.
     */
    public static final int LONG_STRING_LENGTH = 255;

    @Embedded
    private NetworkInfo networkInfo;

    @Embedded
    private OSInfo osInfo;

    @Embedded
    private FirmwareInfo firmwareInfo;

    @Embedded
    private HardwareInfo hardwareInfo;

    @Embedded
    private TPMInfo tpmInfo;

    @Getter
    @Column(nullable = false)
    private String clientApplicationVersion;

    @Getter
    @Setter
    @Transient
    private String paccorOutputString;

    /**
     * Default constructor necessary for marshalling/unmarshalling.
     */
    public DeviceInfoReport() {
        /* do nothing */
    }

    /**
     * Constructor used to create a <code>DeviceInfoReport</code>. The
     * information cannot be changed after the <code>DeviceInfoReport</code> is
     * created.
     *
     * @param networkInfo
     *            NetworkInfo object, cannot be null
     * @param osInfo
     *            OSInfo object, cannot be null
     * @param firmwareInfo
     *            FirmwareInfo object, cannot be null
     * @param hardwareInfo
     *            HardwareInfo object, cannot be null
     * @param tpmInfo
     *            TPMInfo object, may be null if a TPM is not available on the
     *            device
     */
    public DeviceInfoReport(final NetworkInfo networkInfo, final OSInfo osInfo,
                            final FirmwareInfo firmwareInfo, final HardwareInfo hardwareInfo,
                            final TPMInfo tpmInfo) {
        this(networkInfo, osInfo, firmwareInfo, hardwareInfo, tpmInfo, VersionHelper.getVersion());
    }

    /**
     * Constructor used to create a <code>DeviceInfoReport</code>. The
     * information cannot be changed after the <code>DeviceInfoReport</code> is
     * created.
     *
     * @param networkInfo
     *            NetworkInfo object, cannot be null
     * @param osInfo
     *            OSInfo object, cannot be null
     * @param firmwareInfo
     *            FirmwareInfo object, cannot be null
     * @param hardwareInfo
     *            HardwareInfo object, cannot be null
     * @param tpmInfo
     *            TPMInfo object, may be null if a TPM is not available on the
     *            device
     * @param clientApplicationVersion
     *            string representing the version of the client that submitted this report,
     *            cannot be null
     */
    public DeviceInfoReport(final NetworkInfo networkInfo, final OSInfo osInfo,
                            final FirmwareInfo firmwareInfo, final HardwareInfo hardwareInfo,
                            final TPMInfo tpmInfo, final String clientApplicationVersion) {
        setNetworkInfo(networkInfo);
        setOSInfo(osInfo);
        setFirmwareInfo(firmwareInfo);
        setHardwareInfo(hardwareInfo);
        setTPMInfo(tpmInfo);
        this.clientApplicationVersion = clientApplicationVersion;
    }

    /**
     * Retrieves the NetworkInfo for this <code>DeviceInfoReport</code>.
     *
     * @return networkInfo
     */
    public final NetworkInfo getNetworkInfo() {
        /*
         * Hibernate bug requires this
         * https://hibernate.atlassian.net/browse/HHH-7610
         * without null may be returned, which this interface does not support
         */
        if (networkInfo == null) {
            networkInfo = new NetworkInfo(null, null, null);
        }
        return networkInfo;
    }

    /**
     * Retrieves the OSInfo for this <code>DeviceInfoReport</code>.
     *
     * @return osInfo
     */
    public final OSInfo getOSInfo() {
        /*
         * Hibernate bug requires this
         * https://hibernate.atlassian.net/browse/HHH-7610
         * without null may be returned, which this interface does not support
         */
        if (osInfo == null) {
            osInfo = new OSInfo(NOT_SPECIFIED, NOT_SPECIFIED,
                    NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED);
        }
        return osInfo;
    }

    /**
     * Retrieves the FirmwareInfo for this <code>DeviceInfoReport</code>.
     *
     * @return osInfo
     */
    public final FirmwareInfo getFirmwareInfo() {
        /*
         * Hibernate bug requires this
         * https://hibernate.atlassian.net/browse/HHH-7610
         * without null may be returned, which this interface does not support
         */
        if (firmwareInfo == null) {
            firmwareInfo = new FirmwareInfo(NOT_SPECIFIED,
                    NOT_SPECIFIED, NOT_SPECIFIED);
        }
        return firmwareInfo;
    }

    /**
     * Retrieves the OSInfo for this <code>DeviceInfoReport</code>.
     *
     * @return osInfo
     */
    public HardwareInfo getHardwareInfo() {
        /*
         * Hibernate bug requires this
         * https://hibernate.atlassian.net/browse/HHH-7610
         * without null may be returned, which this interface does not support
         */
        if (hardwareInfo == null) {
            hardwareInfo = new HardwareInfo(
                    NOT_SPECIFIED,
                    NOT_SPECIFIED,
                    NOT_SPECIFIED,
                    NOT_SPECIFIED,
                    NOT_SPECIFIED,
                    NOT_SPECIFIED
            );
        }
        return hardwareInfo;
    }

    /**
     * Retrieves the TPMInfo for this <code>DeviceInfoReport</code>. TPMInfo may
     * be null if a TPM is not available on the device.
     *
     * @return tpmInfo, may be null if a TPM is not available on the device
     */
    public final TPMInfo getTPMInfo() {
        return tpmInfo;
    }

    @Override
    public String getReportType() {
        return this.getClass().getName();
    }

    /**
     * Searches the given set of TPMBaselines for matching device info fields that
     * are determined critical to detecting a kernel update.
     * @param tpmBaselines Iterable&lt;TPMBaseline&gt; set of TPMBaseline objects.
     * @return True, if one of the TPM baselines in the set has the same kernel-specific
     * info as this DeviceinfoReport.
     */
    public final boolean matchesKernelInfo(final Iterable<TpmWhiteListBaseline> tpmBaselines) {
        boolean match = false;

        if (tpmBaselines != null) {
            // Retrieve the fields which indicate a kernel update
            final OSInfo kernelOSInfo = getOSInfo();

            // perform the search
            for (final TpmWhiteListBaseline baseline : tpmBaselines) {
                final OSInfo baselineOSInfo = baseline.getOSInfo();
                if(baselineOSInfo.getOSName().equalsIgnoreCase(kernelOSInfo.getOSName())
                        && baselineOSInfo.getOSVersion().equalsIgnoreCase(kernelOSInfo.getOSVersion())) {
                    match = true;
                    break;
                }
            }
        }

        return match;
    }

    private void setNetworkInfo(NetworkInfo networkInfo) {
        if (networkInfo == null) {
            LOGGER.error("NetworkInfo cannot be null");
            throw new NullPointerException("network info");
        }
        this.networkInfo = networkInfo;
    }

    private void setOSInfo(OSInfo osInfo) {
        if (osInfo == null) {
            LOGGER.error("OSInfo cannot be null");
            throw new NullPointerException("os info");
        }
        this.osInfo = osInfo;
    }

    private void setFirmwareInfo(FirmwareInfo firmwareInfo) {
        if (firmwareInfo == null) {
            LOGGER.error("FirmwareInfo cannot be null");
            throw new NullPointerException("firmware info");
        }
        this.firmwareInfo = firmwareInfo;
    }

    private void setHardwareInfo(HardwareInfo hardwareInfo) {
        if (hardwareInfo == null) {
            LOGGER.error("HardwareInfo cannot be null");
            throw new NullPointerException("hardware info");
        }
        this.hardwareInfo = hardwareInfo;
    }

    private void setTPMInfo(TPMInfo tpmInfo) {
        this.tpmInfo = tpmInfo;
    }
}
