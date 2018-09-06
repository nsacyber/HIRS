package hirs.data.persist;

import static org.apache.logging.log4j.LogManager.getLogger;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import hirs.utils.VersionHelper;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A <code>DeficeInfoReport</code> is a <code>Report</code> used to transfer the
 * information about the device. This <code>Report</code> includes the network,
 * OS, and TPM information.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(namespace = "hirs.report")
@XmlSeeAlso({NetworkInfo.class, OSInfo.class, TPMInfo.class})
@Entity
public class DeviceInfoReport extends Report implements Serializable {

    private static final Logger LOGGER = getLogger(DeviceInfoReport.class);

    /**
     * A variable used to describe unavailable hardware, firmware, or OS info.
     */
    public static final String NOT_SPECIFIED = "Not Specified";

    @XmlElement
    @Embedded
    private NetworkInfo networkInfo;

    @XmlElement
    @Embedded
    private OSInfo osInfo;

    @XmlElement
    @Embedded
    private FirmwareInfo firmwareInfo;

    @XmlElement
    @Embedded
    private HardwareInfo hardwareInfo;

    @XmlElement
    @Embedded
    private TPMInfo tpmInfo;

    @XmlElement
    @Column(nullable = false)
    private String clientApplicationVersion;

    @XmlElement
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "deviceInfoReport_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<ChassisComponentInfo> chassisInfo = new ArrayList<>();

    @XmlElement
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "deviceInfoReport_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<BaseboardComponentInfo> baseboardInfo = new ArrayList<>();

    @XmlElement
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "deviceInfoReport_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<ProcessorComponentInfo> processorInfo = new ArrayList<>();

    @XmlElement
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "deviceInfoReport_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<BIOSComponentInfo> biosInfo = new ArrayList<>();

    @XmlElement
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "deviceInfoReport_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<NICComponentInfo> nicInfo = new ArrayList<>();

    @XmlElement
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "deviceInfoReport_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<HardDriveComponentInfo> hardDriveInfo = new ArrayList<>();

    @XmlElement
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "deviceInfoReport_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<MemoryComponentInfo> memoryInfo = new ArrayList<>();



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
        setNetworkInfo(networkInfo);
        setOSInfo(osInfo);
        setFirmwareInfo(firmwareInfo);
        setHardwareInfo(hardwareInfo);
        setTPMInfo(tpmInfo);
        clientApplicationVersion = VersionHelper.getVersion();
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

    /**
     * Get list of ChassisComponentInfo obects, each representing a chassis.
     * @return list of ChassisComponentInfo obects, each representing a chassis
     */
    public List<ChassisComponentInfo> getChassisInfo() {
        return chassisInfo;
    }

    /**
     * Get list of BaseboardComponentInfo obects, each representing a baseboard.
     * @return list of BaseboardComponentInfo obects, each representing a baseboard
     */
    public List<BaseboardComponentInfo> getBaseboardInfo() {
        return baseboardInfo;
    }

    /**
     * Get list of ProcessorComponentInfo obects, each representing a processor.
     * @return list of ProcessorComponentInfo obects, each representing a processor
     */
    public List<ProcessorComponentInfo> getProcessorInfo() {
        return processorInfo;
    }

    /**
     * Get list of BIOSComponentInfo obects, each representing a BIOS.
     * @return list of BIOSComponentInfo obects, each representing a BIOS
     */
    public List<BIOSComponentInfo> getBiosInfo() {
        return biosInfo;
    }

    /**
     * Get list of NICComponentInfo obects, each representing a NIC.
     * @return list of NICComponentInfo obects, each representing a NIC
     */
    public List<NICComponentInfo> getNicInfo() {
        return nicInfo;
    }

    /**
     * Get list of HardDriveComponentInfo obects, each representing a hard drive.
     * @return list of HardDriveComponentInfo obects, each representing a hard drive
     */
    public List<HardDriveComponentInfo> getHardDriveInfo() {
        return hardDriveInfo;
    }

    /**
     * Get list of MemoryComponentInfo obects, each representing a memory DIMM.
     * @return list of MemoryComponentInfo obects, each representing a memory DIMM
     */
    public List<MemoryComponentInfo> getMemoryInfo() {
        return memoryInfo;
    }

    /**
     * Gets the client application version.
     * @return the client application version
     */
    public String getClientApplicationVersion() {
        return clientApplicationVersion;
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

    public void setChassisInfo(List<ChassisComponentInfo> chassisInfo) {
        this.chassisInfo = Collections.unmodifiableList(chassisInfo);
    }

    public void setBaseboardInfo(List<BaseboardComponentInfo> baseboardInfo) {
        this.baseboardInfo = Collections.unmodifiableList(baseboardInfo);
    }

    public void setProcessorInfo(List<ProcessorComponentInfo> processorInfo) {
        this.processorInfo = Collections.unmodifiableList(processorInfo);
    }

    public void setBiosInfo(List<BIOSComponentInfo> biosInfo) {
        this.biosInfo = Collections.unmodifiableList(biosInfo);
    }

    public void setNicInfo(List<NICComponentInfo> nicInfo) {
        this.nicInfo = Collections.unmodifiableList(nicInfo);
    }

    public void setHardDriveInfo(List<HardDriveComponentInfo> hardDriveInfo) {
        this.hardDriveInfo = Collections.unmodifiableList(hardDriveInfo);
    }

    public void setMemoryInfo(List<MemoryComponentInfo> memoryInfo) {
        this.memoryInfo = Collections.unmodifiableList(memoryInfo);
    }
}
