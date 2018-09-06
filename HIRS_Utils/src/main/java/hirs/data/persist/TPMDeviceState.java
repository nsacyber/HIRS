package hirs.data.persist;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * <code>TPMDeviceState</code> is used to maintain a reference to a particular TPM report. The
 * TPM device state is used during TPM appraisal to compare the PCR values against an old report,
 * the report referenced in this state object.
 */
@Entity
@Access(AccessType.FIELD)
public class TPMDeviceState extends DeviceState {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "device", nullable = false, unique = true)
    private final Device device;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tpmReport", nullable = true)
    private TPMReport report;

    /**
     * Public constructor used to create a <code>TPMDeviceState</code>. Sets the device but has no
     * associated report. Set the report using #setTPMMeasurementRecords(TPMReport).
     *
     * @param device
     *            Device object associated with this TPMDeviceState
     */
    public TPMDeviceState(final Device device) {
        if (device == null) {
            throw new NullPointerException("device");
        }
        this.device = device;
    }

    /**
     * Used by Hibernate, should never be called.
     */
    protected TPMDeviceState() {
        this.device = null;
    }

    /**
     * Retrieves the ID of this TPMDeviceState.
     *
     * @return ID of the state object
     */
    public final Long getId() {
        return id;
    }

    /**
     * Retrieves the device associated with this TPMDeviceState.
     *
     * @return Device associated with the state object
     */
    public final Device getDevice() {
        return device;
    }

    /**
     * Retrieves the list of TPM measurement records associated with the current report. If no
     * report is currently associated, returns an empty list.
     *
     * @return unmodifiable list of TPMMeasurementRecords, empty list if no report is associated
     */
    public final List<TPMMeasurementRecord> getTPMMeasurementRecords() {
        if (this.report == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.report.getTPMMeasurementRecords());
    }

    /**
     * Retrieves the <code>TPMReport</code> currently associated with the device state. May be null
     * if it has not been set yet.
     *
     * @return TPMReport
     */
    public final TPMReport getReport() {
        return this.report;
    }

    /**
     * Sets the <code>TPMReport</code> associated with this state.
     *
     * @param report
     *            TPMReport, may be null
     */
    public final void setTPMReport(final TPMReport report) {
        this.report = report;
    }


    /**
     * Checks if the given TPMMeasurementRecord is contained in the TPM Report.
     *
     * @param record
     *            TPMMeasurementRecord
     * @return boolean indicating if the saved report contains the record.
     */
    public final boolean contains(final TPMMeasurementRecord record) {
        if (this.report == null) {
            return false;
        } else {
            return this.report.getTPMMeasurementRecords().contains(record);
        }
    }

    /**
     * Returns the hash code representing this object. The hash code is derived
     * from the <code>Device</code> this state represents.
     *
     * @return hash code
     */
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + device.hashCode();
        return result;
    }

    /**
     * Compares this object for equality with <code>obj</code>.
     * <code>TPMDeviceState</code> objects are considered equal if they have
     * equal <code>Device</code>s.
     *
     * @param obj
     *            other object
     * @return true if both are instances of <code>TPMDeviceState</code> and
     *         both have the same <code>Device</code>
     */
    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TPMDeviceState)) {
            return false;
        }
        final TPMDeviceState other = (TPMDeviceState) obj;
        return device.equals(other.device);
    }

    @Override
    public final String toString() {
        UUID reportId = null;
        if (this.report != null) {
            reportId = this.report.getId();
        }
        return String.format("(%s %s)", this.device, reportId);
    }

}
