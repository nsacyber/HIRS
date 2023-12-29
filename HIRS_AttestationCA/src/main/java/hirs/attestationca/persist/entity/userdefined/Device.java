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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "Device")
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Device extends AbstractEntity {

    @Getter
    @Column(name = "name", unique = true)
    private String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
            optional = true, orphanRemoval = true)
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
        return new DeviceInfoReport(deviceInfo.getNetworkInfo(),
                deviceInfo.getOSInfo(), deviceInfo.getFirmwareInfo(),
                deviceInfo.getHardwareInfo(), deviceInfo.getTpmInfo(),
                deviceInfo.getClientApplicationVersion());
    }

    /**
     * Getter for the report time stamp.
     * @return a cloned version
     */
    public Timestamp getLastReportTimestamp() {
        return (Timestamp) lastReportTimestamp.clone();
    }

    public String toString() {
        return String.format("Device Name: %s%nStatus: %s%nSummary: %s",
                name, healthStatus.getStatus(),
                supplyChainValidationStatus.toString(),
                summaryId);
    }
}