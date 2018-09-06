package hirs.persist;

import hirs.data.persist.Digest;
import hirs.data.persist.IMABaselineRecord;
import hirs.data.persist.QueryableRecordImaBaseline;
import hirs.data.persist.SimpleImaBaseline;
import hirs.utils.Callback;

import java.util.Collection;

/**
 * A <code>ImaBaselineRecordManager</code> manages <code>IMABaselineRecord</code>s. It has support
 * for the basic create, read, update, and delete methods.
 */
public interface ImaBaselineRecordManager {

    /**
     * Stores a new <code>IMABaselineRecord</code>. This stores a new
     * <code>IMABaselineRecord</code> to be managed by the <code>IMABaselineRecordManager</code>.
     * If the <code>IMABaselineRecord</code> is successfully saved then a reference to it is
     * returned.
     *
     * @param record Alert to save
     * @return reference to saved IMABaselineRecord
     * @throws ImaBaselineRecordManagerException if the Alert has previously been saved or
     *               unexpected error occurs
     */
    IMABaselineRecord saveRecord(IMABaselineRecord record)
            throws ImaBaselineRecordManagerException;

    /**
     * Retrieves the <code>IMABaselineRecord</code> identified by <code>id</code>. If
     * the <code>IMABaselineRecord</code> cannot be found then null is returned.
     *
     * @param id id of the <code>IMABaselineRecord</code>
     * @return <code>IMABaselineRecord</code> whose name is <code>id</code> or null if
     * not found
     * @throws ImaBaselineRecordManagerException if unable to retrieve the IMABaselineRecord
     */
    IMABaselineRecord getRecord(long id) throws ImaBaselineRecordManagerException;

    /**
     * Retrieves <code>IMABaselineRecord</code> with the given path, hash, and assigned IMA
     * baseline id.
     *
     * @param path     path of the record to be retrieved
     * @param hash     hash of the record to be retrieved
     * @param baseline baseline that is associated with the desired record.
     * @return IMABaselineRecord that matches the provided path, hash, and baseline ID
     */
    IMABaselineRecord getRecord(String path, Digest hash,
                                SimpleImaBaseline baseline);

    /**
     * Deletes the <code>ImaBaselineRecord</code> identified by <code>id</code>. If the
     * <code>ImaBaselineRecord</code> is found and deleted then true is returned,
     * otherwise false.
     *
     * @param id id of the <code>ImaBaselineRecord</code> to delete
     * @return true if successfully found and deleted from database, otherwise false
     * @throws ImaBaselineRecordManagerException if unable to delete the ImaBaselineRecord for any
     *                  reason other than not found
     */
    boolean deleteRecord(Long id) throws ImaBaselineRecordManagerException;

    /**
     * Deletes the <code>ImaBaselineRecord</code> provided. If the
     * <code>ImaBaselineRecord</code> is found and deleted then true is returned,
     * otherwise false.
     *
     * @param record record object to be deleted.
     * @return true if successfully found and deleted from database, otherwise false
     * @throws ImaBaselineRecordManagerException if unable to delete the ImaBaselineRecord for any
     *                   reason other than not found
     */
    boolean deleteRecord(IMABaselineRecord record) throws ImaBaselineRecordManagerException;

    /**
     * Iterates over the {@link IMABaselineRecord}s in the given baseline, and calls the given
     * Callback on each record.  If the callback returns a non-null value, the returned value will
     * be added to a collection, which is returned when iteration is finished.
     *
     * @param baseline    the baseline whose {@link IMABaselineRecord}s we should iterate over
     * @param callback    the callback to run on each record
     * @param <T>         the return type of the callback
     * @return the total collection of objects returned as results from the given Callback
     */
    <T> Collection<T> iterateOverBaselineRecords(QueryableRecordImaBaseline baseline,
                                                 Callback<IMABaselineRecord, T> callback);
}
