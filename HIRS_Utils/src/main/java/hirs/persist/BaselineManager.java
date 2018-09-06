package hirs.persist;

import hirs.FilteredRecordsList;
import hirs.data.bean.SimpleBaselineBean;
import hirs.data.persist.Baseline;
import hirs.data.persist.IMABaselineRecord;
import hirs.data.persist.ImaBlacklistRecord;
import hirs.repository.RepoPackage;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A <code>BaselineManager</code> manages <code>Baseline</code>s. A <code>BaselineManager</code> can
 * read, update, and archive <code>Baseline</code>s.
 */
public interface BaselineManager {

    /**
     * Stores a new <code>Baseline</code>. This stores a new
     * <code>Baseline</code> to be managed by the <code>BaselineManager</code>.
     * If the <code>Baseline</code> is successfully saved then a reference to it
     * is returned.
     *
     * @param baseline baseline to save
     * @return reference to saved baseline
     * @throws BaselineManagerException if baseline is already saved or other error occurs
     */
    Baseline saveBaseline(Baseline baseline) throws BaselineManagerException;

    /**
     * Updates a <code>Baseline</code>. This updates the <code>Baseline</code>
     * that is managed so subsequent calls to get this <code>Baseline</code>
     * will return the values set by the incoming <code>Baseline</code>.
     *
     * @param baseline baseline
     * @throws BaselineManagerException if unable to update the baseline
     */
    void updateBaseline(Baseline baseline) throws BaselineManagerException;

    /**
     * Returns a list of all baseline names managed by this manager. Every
     * <code>Baseline</code> must have a name that users can use to reference
     * the <code>Baseline</code>. This returns a listing of all the
     * <code>Baseline</code>s.
     * <p>
     * A <code>Class</code> argument may be specified to limit which types of
     * <code>Baseline</code>s to return. This argument may be null to return all
     * <code>Baseline</code>s.
     *
     * @param clazz
     *            class type of <code>Baseline</code>s to return (may be null)
     * @return list of <code>Baseline</code> names
     * @throws BaselineManagerException if unable to create the list
     */
    List<Baseline> getBaselineList(Class<? extends Baseline> clazz)
            throws BaselineManagerException;

    /**
     * Returns a list of all <code>Baseline</code>s that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables. The returned list does not
     * contain <code>Baseline</code>s that have been soft-deleted.
     *
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @param searchableColumns Map of String and boolean values with column
     *      headers and whether they are to.  Boolean is true if field provides
     *      a typical String that can be searched by Hibernate without
     *      transformation.
     * @return FilteredRecordsList object with fields for DataTables
     * @throws BaselineManagerException if unable to create the list
     */
    FilteredRecordsList<Baseline> getOrderedBaselineList(
            String columnToOrder, boolean ascending,
            int firstResult, int maxResults, String search,
            Map<String, Boolean> searchableColumns)
            throws BaselineManagerException;

     /**
     * Returns a list of all ImaBaseline Records that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables.
     *
     * @param baselineId id of the baseline
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @return FilteredRecordsList object with fields for DataTables
     * @throws BaselineManagerException if unable to create the list
     */
    FilteredRecordsList<IMABaselineRecord> getOrderedRecordList(
            UUID baselineId, String columnToOrder, boolean ascending,
            int firstResult, int maxResults, String search)
            throws BaselineManagerException;

    /**
    * Returns a list of all ImaBlacklistBaseline Records that are ordered by a column
    * and direction (ASC, DESC) that is provided by the user.  This method
    * helps support the server-side processing in the JQuery DataTables.
    *
    * @param baselineId id of the baseline
    * @param columnToOrder Column to be ordered
    * @param ascending direction of sort
    * @param firstResult starting point of first result in set
    * @param maxResults total number we want returned for display in table
    * @param search string of criteria to be matched to visible columns
    * @return FilteredRecordsList object with fields for DataTables
    * @throws BaselineManagerException if unable to create the list
    */
   FilteredRecordsList<ImaBlacklistRecord> getOrderedBlacklistRecordList(
           UUID baselineId, String columnToOrder, boolean ascending,
           int firstResult, int maxResults, String search)
           throws BaselineManagerException;

    /**
     * Returns a list of all RepoPackages that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables.
     *
     * @param name name of the baseline
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @return FilteredRecordsList object with fields for DataTables
     * @throws BaselineManagerException if unable to create the list
     */
    FilteredRecordsList<RepoPackage> getOrderedPackageList(
            String name, String columnToOrder, boolean ascending,
            int firstResult, int maxResults, String search)
            throws BaselineManagerException;

    /**
     * Returns a list of all IMABaselineRecords in the specified package.
     *
     * @param id of the package
     * @param search string of criteria to be matched to visible columns
     * @return List the records
     * @throws BaselineManagerException if unable to create the list
     */
    List<IMABaselineRecord> getPackageRecords(
            UUID id, String search) throws BaselineManagerException;

    /**
     * Retrieves the <code>Baseline</code> identified by <code>name</code>. If
     * the <code>Baseline</code> cannot be found then null is returned.
     *
     * @param name  name of the <code>Baseline</code>
     * @return <code>Baseline</code> whose name is <code>name</code> or null if not found
     * @throws BaselineManagerException if unable to retrieve the baseline
     */
    Baseline getBaseline(String name) throws BaselineManagerException;

    /**
     * Retrieves the <code>Baseline</code> identified by the given <code>id</code>. If
     * the <code>Baseline</code> cannot be found then null is returned.
     *
     * @param id the id of the desired <code>Baseline</code>
     * @return <code>Baseline</code> whose id is <code>id</code> or null if not found
     * @throws BaselineManagerException if unable to retrieve the baseline
     */
    Baseline getBaseline(Serializable id) throws BaselineManagerException;

    /**
     * Retrieves the <code>Baseline</code> identified by <code>name</code>.  This method
     * fully loads a Baseline object; any lazy fields will be recursively loaded.  This is
     * necessary when conducting an appraisal. If the <code>Baseline</code> cannot be found
     * then null is returned.
     *
     * @param name name of the <code>Baseline</code>
     * @return <code>Baseline</code> whose name is <code>name</code> or null if not found
     * @throws BaselineManagerException if unable to retrieve the baseline
     */
    Baseline getCompleteBaseline(String name) throws BaselineManagerException;

    /**
     * Archives the named {@link Baseline} and updates it in the database.
     *
     * @param name name of the {@link Baseline} to <code>archive</code>
     * @return true if the {@link Baseline} was successfully found and archived, false if the
     * {@link Baseline} was not found
     * @throws DBManagerException
     *      if the {@link Baseline} is not an instance of ArchivableEntity.
     */
    boolean archive(String name) throws DBManagerException;

    /**
     * Deletes the named {@link Baseline} from the database.
     *
     * @param baseline      {@link Baseline} to be deleted
     * @return              <code>true</code> if the {@link Baseline} was successfully found and
     *                      deleted, <code>false</code> if the {@link Baseline} was not found
     * @throws DBManagerException
     *      if the {@link Baseline} is not an instance of ArchivableEntity.
     */
    boolean delete(Baseline baseline) throws DBManagerException;

    /**
     * Returns a list of all <code>Baseline</code>s that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables. The returned list does not
     * contain <code>Baseline</code>s that have been soft-deleted.  The records stored within each
     * baseline are not loaded.
     *
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @param searchableColumns Map of String and boolean values with column
     *      headers and whether they are to.  Boolean is true if field provides
     *      a typical String that can be searched by Hibernate without
     *      transformation.
     * @return FilteredRecordsList object with fields for DataTables
     * @throws BaselineManagerException if unable to create the list
     */
    FilteredRecordsList<SimpleBaselineBean> getOrderedBaselineListWithoutRecords(
            String columnToOrder, boolean ascending,
            int firstResult, int maxResults, String search,
            Map<String, Boolean> searchableColumns)
            throws BaselineManagerException;
}
