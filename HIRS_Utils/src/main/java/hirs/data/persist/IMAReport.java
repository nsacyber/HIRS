package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.apache.logging.log4j.Logger;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * IMAReport is a listing of <code>IMAMeasurementRecord</code>s. The list of
 * <code>IMAMeasurementRecord</code>s is an ordered list. The list is ordered
 * based upon the order in which the files were measured. The ordering is
 * important because the TPM hash value can only be verified if the order is
 * correct.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
@XmlSeeAlso(IMAMeasurementRecord.class)
@Entity
public class IMAReport extends Report {

    private static final Logger LOGGER = getLogger(IMAReport.class);
    private static final int MAX_BOOTCYCLE_LENGTH = 128;

    @Column(nullable = true, length = MAX_BOOTCYCLE_LENGTH)
    @XmlElement(name = "bootcycleID", required = false)
    private String bootcycleId;

    @XmlElement(name = "startIndex", required = true)
    @Column(nullable = false, name = "startingIndex")
    private int index;

    @XmlElement
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
            mappedBy = "report")
    @Access(AccessType.FIELD)
    @JsonIgnore
    private final Set<IMAMeasurementRecord> imaRecords;

    /**
     * Constructor used to initialize an IMA report. This creates an empty set
     * of IMA records and sets the default index to zero.
     */
    public IMAReport() {
        imaRecords = new LinkedHashSet<>();
        index = 0;
    }

    @Override
    public final String getReportType() {
        return this.getClass().getName();
    }

    /**
     * Returns the boot cycle ID. A boot cycle ID is a unique identifier that
     * indicates a machine reboot. If the machine is rebooted then it will have
     * a different boot cycle ID.
     * <p>
     * The boot cycle ID is useful for IMA delta measurements. With this ID the
     * appraiser can know if a machine has rebooted. If it has not rebooted then
     * efficiencies can be applied, such as not re-appraising a part of a report
     * that has previously been appraised.
     * <p>
     * This is an optional component of an IMA report. The return value may be
     * null.
     *
     * @return boot cycle ID
     */
    public final String getBootcycleId() {
        return this.bootcycleId;
    }

    /**
     * Sets the boot cycle ID for this report. See {@link #getBootcycleId()} for
     * more details.
     *
     * @param bootcycleId boot cycle ID (may be null to omit from report)
     */
    public final void setBootcycleId(final String bootcycleId) {
        this.bootcycleId = bootcycleId;
    }

    /**
     * Returns the index of the first record. An IMA report contains an array of
     * measurement records. This value indicates the index of the first record
     * in this report.
     * <p>
     * This can be non-zero if this report represents a delta report. A report
     * can be sent at time t0. This report contains y records. At time t1, if
     * the machine has not rebooted, then the full IMA report will have x+y
     * records where y&ge;0. A delta report can be sent with just the records in
     * the set {y-x}. In that case the index would be x.
     * <p>
     * The first entry in the IMA report has index 0.
     *
     * @return index of the first measurement record in this report
     */
    public final int getIndex() {
        return index;
    }

    /**
     * Sets the index of the first measurement record.
     *
     * @param index
     *            index of first measurement record
     * @throws IllegalArgumentException
     *             if index&lt;0
     */
    public final void setIndex(final int index) {
        if (index < 0) {
            final String msg = "index cannot be less than zero";
            LOGGER.warn(msg);
            throw new IllegalArgumentException(msg);
        }
        this.index = index;
    }

    /**
     * Gets the list of IMA records.  The <code>IMAMeasurementRecords</code> are
     * lazily loaded and this method will have to be called within a transaction
     * in order to properly load and return all of the records related to the
     * report.
     *
     * @return list of IMA measurement records
     */
    @JsonIgnore
    public Set<IMAMeasurementRecord> getRecords() {
        return Collections.unmodifiableSet(imaRecords);
    }

    /**
     * Adds a record to the list of IMA measurement records by appending it to
     * the ordered list.
     *
     * @param record
     *            IMA record to be added
     */
    public final void addRecord(final IMAMeasurementRecord record) {
        if (record == null) {
            LOGGER.error("null record");
            throw new NullPointerException("record");
        }

        imaRecords.add(record);
        LOGGER.debug("record added: {}", record);
    }

    /**
     * Removes a record from the list.
     *
     * @param record
     *            record to be removed
     * @return a boolean indicating if the removal was successful
     */
    public final boolean removeRecord(final IMAMeasurementRecord record) {
        return imaRecords.remove(record);
    }

    /**
     * Returns a boolean indicating if this report is a full report. The first measurement in the
     * IMA log has an index of 0. If delta reports are enabled, the first delta report should be a
     * full report (index starts at 0), and subsequent delta reports will have index values greater
     * than 0.
     *
     * @return true if a full report or first delta report, false if not the first delta report
     */
    public final boolean isFullReport() {
        return index == 0;
    }

    /**
     * Method returns the number of records in the IMA report.
     * @return the number of records found in the IMA Report
     */
    public int getRecordCount() {
        return imaRecords.size();
    }
}
