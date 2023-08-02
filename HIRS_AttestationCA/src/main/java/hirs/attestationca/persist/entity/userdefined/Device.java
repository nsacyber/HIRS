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
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Device extends AbstractEntity {

    @Column(name = "name", unique = true)
    private String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
            optional = true, orphanRemoval = true)
    private DeviceInfoReport deviceInfo;

    @Column
    @Enumerated(EnumType.ORDINAL)
    private HealthStatus healthStatus;

    @Column
    @Enumerated(EnumType.ORDINAL)
    private AppraisalStatus.Status supplyChainValidationStatus;

    /**
     * Time stamp for the report.
     */
    @Column(name = "last_report_timestamp")
    private Timestamp lastReportTimestamp;

    @Column(name = "is_state_overridden")
    private boolean isStateOverridden;

    @Column(name = "state_override_reason")
    private String overrideReason;

    @Column(name = "summary_id")
    private String summaryId;

    public String toString() {
        return String.format("Device Name: %s%nStatus: %s%nSummary: %s",
                name, healthStatus.getStatus(),
                supplyChainValidationStatus.toString(),
                summaryId);
    }
}