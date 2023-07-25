package hirs.data.persist.baseline;

import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAPolicy;
import hirs.ima.matching.BatchImaMatchStatus;
import hirs.persist.ImaBaselineRecordManager;
import org.hibernate.annotations.Type;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Collection;
import java.util.Date;

/**
 * Class represents the basic functionality of an IMA baseline: determining whether a baseline
 * 'contains' {@link IMAMeasurementRecord}s that were collected from a machine's IMA log.
 * Extending classes represent different ways to assemble and manipulate these baselines.  See
 * {@link SimpleImaBaseline} for an example of a baseline with records that can be manually
 * added and removed.
 *
 * @param <T> the type of record that this baseline holds
 */
@Entity
@Access(AccessType.FIELD)
public abstract class ImaBaseline<T extends AbstractImaBaselineRecord> extends Baseline {
    @Column
    @Type(type = "timestamp")
    private Date date;

    /**
     * Creates a new ImaBaseline with the given name.
     *
     * @param name a name used to uniquely identify and reference the IMA baseline
     */
    public ImaBaseline(final String name) {
        super(name);
        date = new Date();
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected ImaBaseline() {
        super();
        date = new Date();
    }

    /**
     * Tests whether the record is found in the baseline. This returns a
     * <code>ReportMatchStatus</code> representing the result of the search for
     * the record. The returns conditions are as follows:
     * <ul>
     * <li>MATCH - if an <code>IMABaselineRecord</code> is found with a matching
     * path and hash</li>
     * <li>MISMATCH - if at least one <code>IMABaselineRecord</code> is found
     * with a matching path but none with a matching path and hash</li>
     * <li>UNKNOWN - if no <code>IMABaselineRecord</code>s found with a matching
     * path</li>
     * </ul>
     * <p>
     * If partial paths are enabled, records not starting with '/' are compared
     * against all baseline records not starting with '/' and the last segment
     * (the filename after the last '/') of each full path baseline record.
     * Records starting with '/' are compared against all full path baseline
     * records and the last segment of the record is compared against all
     * partial path baseline records.
     * <p>
     * If partial paths are disabled, records are only compared using the full
     * path of the baseline record and the report record.
     *
     * @param records
     *            measurement records to find in this baseline
     * @param recordManager
     *            an ImaBaselineRecordManager that can be used to retrieve persisted records
     * @param imaPolicy
     *            the IMA policy to use while determining if a baseline contains the given records
     *
     * @return batch match status for the measurement records
     */
    public abstract BatchImaMatchStatus<T> contains(
            Collection<IMAMeasurementRecord> records,
            ImaBaselineRecordManager recordManager,
            IMAPolicy imaPolicy
    );

    /**
     * Set this <code>IMABaselines</code>'s <code>date</code>, which can either
     * be the date that it was added, or some other date such as the patch date
     * this baseline is associated with.
     *
     * @param newDate
     *      the new date to set, can be null
     */
    public final void setDate(final Date newDate) {
        if (newDate == null) {
            this.date = null;
        } else {
            this.date = new Date(newDate.getTime());
        }
    }

    /**
     * Get this <code>IMABaselines</code>'s <code>date</code>, which can either
     * be the date that it was added, or some other date such as the patch date
     * this baseline is associated with.
     *
     * @return this baseline's date, can be null
     */
    public final Date getDate() {
        return new Date(date.getTime());
    }
}
