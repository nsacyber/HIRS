package hirs.data.persist;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.Arrays;
import java.util.Date;

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
 * An <code>IMADeviceState</code> manages the IMA state for appraisal. This
 * information is useful so that the IMA appraiser does not have to request or
 * appraise the full report for each appraisal. The appraiser can fetch the
 * information from the last appraisal and begin appraising from the saved
 * state.
 * <p>
 * For instance consider a client that first appraises at t0. At t0 the client
 * send the full report and IMA appraiser appraises the X entries in the report.
 * Then at time t1 the client has another appraisal. If the machine has not been
 * rebooted then the appraiser can validate the integrity of the full report
 * using the saved PCR value from t0. The server can also validate only the IMA
 * entries after t0.
 *
 */
@Entity
@Access(AccessType.FIELD)
public class IMADeviceState extends DeviceState {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "device", nullable = false, unique = true)
    private final Device device;

    @Column(name = "bootcycleId")
    private String bootcycleId;

    @Column(name = "appraiseIndex")
    private int index;

    @Column(nullable = true, name = "pcrState",
            length = Digest.SHA512_DIGEST_LENGTH)
    private byte[] pcrState;

    @Column(name = "mostRecentFullReportDate")
    private Date mostRecentFullReportDate;

    /**
     * Creates a new <code>IMADeviceState</code> to manage the state for the
     * <code>Device</code>. The boot-cycle ID will be null to indicate that this
     * has not saved state for previous report. The index will be 0 to indicate
     * that no entries have been appraised, and the PCR state will be null as
     * well.
     *
     * @param device
     *            device
     */
    public IMADeviceState(final Device device) {
        if (device == null) {
            throw new NullPointerException("device");
        }
        this.device = device;
        resetState();
    }

    /**
     * Default constructor that has no state saved.
     */
    protected IMADeviceState() {
        this.device = null;
        resetState();
    }

    /**
     * Returns the database ID associated with this entity. After this object is
     * stored in a database then this ID will be set. This is necessary only for
     * Hibernate.
     *
     * @return the id
     */
    public final Long getId() {
        return id;
    }

    /**
     * Resets the state. This sets boot-cycle ID to null, index to 0, and PCR
     * state to null.
     */
    public final void resetState() {
        this.bootcycleId = null;
        this.index = 0;
        this.pcrState = null;
    }

    /**
     * Returns the <code>Device</code> associated with this state.
     *
     * @return the device
     */
    public final Device getDevice() {
        return device;
    }

    /**
     * Returns the boot-cycle ID associated with the last appraisal. This may be
     * null to indicate a reset in state. If the <code>IMAAppraiser</code> sees
     * this value as null then it will know to request a full report and
     * appraise the full report.
     *
     * @return the bootcycleId (may be null if no appraisals yet or state is
     *         reset)
     */
    public final String getBootcycleId() {
        return bootcycleId;
    }

    /**
     * Sets the boot-cycle ID associated with the last appraisal.
     *
     * @param bootcycleId
     *            bootcycleId
     */
    public final void setBootcycleId(final String bootcycleId) {
        this.bootcycleId = bootcycleId;
    }

    /**
     * Returns the index of the next IMA record to be appraised. The first
     * record has index 0. If zero is returned then this indicates the first
     * entry in the report is to be appraised.
     *
     * @return index of last successfully appraised IMA record
     */
    public final int getIndex() {
        return index;
    }

    /**
     * Sets the index of the next IMA record to be appraised.
     *
     * @param index
     *            index of last IMA record that was successfully appraised
     * @throws IllegalArgumentException
     *             if index &lt; 0
     */
    public final void setIndex(final int index)
            throws IllegalArgumentException {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }
        this.index = index;
    }

    /**
     * Sets the date of the most recent full report. This is useful for determining the start time
     * of the most recent delta report series, as the first delta report is indistinguishable from
     * a full report.
     *
     * @return date of most recent full report or null if there have not been any reports yet
     */
    public final Date getMostRecentFullReportDate() {
        if (mostRecentFullReportDate == null) {
            return null;
        } else {
            return (Date) mostRecentFullReportDate.clone();
        }
    }

    /**
     * Sets the date of the most recent full report. This is useful for determining the start time
     * of the most recent delta report series, as the first delta report is indistinguishable from
     * a full report.
     *
     * @param date date of the most recent full report or null to unset the date
     */
    public final void setMostRecentFullReportDate(final Date date) {
        if (date == null) {
            this.mostRecentFullReportDate = null;
        } else {
            this.mostRecentFullReportDate = (Date) date.clone();
        }
    }

    @Override
    public Criterion getDeviceTrustAlertCriterion() {
        Criterion createTimeRestriction =  Restrictions.ge("createTime", mostRecentFullReportDate);
        Criterion sourceRestriction = Restrictions.eq("source", Alert.Source.IMA_APPRAISER);
        return Restrictions.and(createTimeRestriction, sourceRestriction);
    }

    /**
     * Returns the PCR hash that verified the last IMA report. An IMA report can
     * be verified by recalculating the PCR hash in the TPM. This value
     * indicates the last verified PCR value for a valid report in the
     * boot-cycle ID.
     * <p>
     * This may return null if the state has been reset, no entries have been
     * appraised, or the <code>Device</code> does not have a TPM.
     *
     * @return PCR state
     */
    public final byte[] getPcrState() {
        if (pcrState == null) {
            return null;
        } else {
            return Arrays.copyOf(pcrState, pcrState.length);
        }
    }

    /**
     * Sets the PCR state. See {@link #getPcrState()} for more details.
     *
     * @param pcrState PCR state
     */
    public final void setPcrState(final byte[] pcrState) {
        if (pcrState == null) {
            this.pcrState = null;
        } else {
            this.pcrState = Arrays.copyOf(pcrState, pcrState.length);
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
     * <code>IMADeviceState</code> objects are considered equal if they have
     * equal <code>Device</code>s.
     *
     * @param obj
     *            other object
     * @return true if both are instances of <code>IMADeviceState</code> and
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
        if (!(obj instanceof IMADeviceState)) {
            return false;
        }
        final IMADeviceState other = (IMADeviceState) obj;
        return device.equals(other.device);
    }

    @Override
    public final String toString() {
        return String.format("(%s %s %d)", device, bootcycleId, index);
    }
}
