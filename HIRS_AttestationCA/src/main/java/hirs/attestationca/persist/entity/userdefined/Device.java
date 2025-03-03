package hirs.attestationca.persist.entity.userdefined;

import hirs.attestationca.persist.entity.AbstractEntity;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.enums.HealthStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "Device")
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Device extends AbstractEntity {

    @Getter
    @Column(name = "name", unique = true)
    private String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
            orphanRemoval = true)
    private DeviceInfoReport deviceInfo;

    @Getter
    @Column
    @Enumerated(EnumType.ORDINAL)
    private HealthStatus healthStatus;

    @Getter
    @Column
    @Enumerated(EnumType.ORDINAL)
    private AppraisalStatus.Status supplyChainValidationStatus;

    /**
     * Time stamp for the report.
     */
    @Column(name = "last_report_timestamp")
    private Timestamp lastReportTimestamp;

    @Getter
    @Column(name = "is_state_overridden")
    private boolean isStateOverridden;

    @Getter
    @Column(name = "state_override_reason")
    private String overrideReason;

    @Getter
    @Column(name = "summary_id")
    private String summaryId;

    /**
     * Constructor creates a Device object using the provided device info report.
     *
     * @param deviceInfoReport device information report
     */
    public Device(final DeviceInfoReport deviceInfoReport) {
        super();
        if (deviceInfoReport != null) {
            this.name = deviceInfoReport.getNetworkInfo().getHostname();
            this.deviceInfo = deviceInfoReport;
        } else {
            name = "";
        }
    }

    /**
     * Returns a report with information about this device. This may return null
     * if this property has not been set.
     *
     * @return device info report
     */
    public final DeviceInfoReport getDeviceInfo() {
        if (deviceInfo != null) {
            return new DeviceInfoReport(deviceInfo.getNetworkInfo(),
                    deviceInfo.getOSInfo(), deviceInfo.getFirmwareInfo(),
                    deviceInfo.getHardwareInfo(), deviceInfo.getTpmInfo(),
                    deviceInfo.getClientApplicationVersion());
        } else {
            return null;
        }
    }

    /**
     * Getter for the report time stamp.
     *
     * @return a cloned version
     */
    public Timestamp getLastReportTimestamp() {
        if (lastReportTimestamp != null) {
            return (Timestamp) lastReportTimestamp.clone();
        } else {
            return Timestamp.valueOf(LocalDateTime.MAX);
        }
    }

    /**
     * Setter for the report time stamp.
     *
     * @param lastReportTimestamp last reported time
     */
    public void setLastReportTimestamp(final Timestamp lastReportTimestamp) {
        this.lastReportTimestamp = (Timestamp) lastReportTimestamp.clone();
    }

    /**
     * Creates a string representation of the Device object.
     *
     * @return a string representation of the Device object.
     */
    @Override
    public String toString() {
        return String.format("Device Name: %s%nStatus: %s%nSummary: %s%n",
                name, (healthStatus == null ? "N/A" : healthStatus.getHealthStatus()),
                (supplyChainValidationStatus == null ? "N/A" : supplyChainValidationStatus.toString()));
    }
}
