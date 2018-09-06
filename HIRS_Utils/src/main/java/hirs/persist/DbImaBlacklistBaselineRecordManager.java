package hirs.persist;

import hirs.data.persist.Digest;
import hirs.data.persist.ImaBlacklistRecord;
import hirs.data.persist.OptionalDigest;
import hirs.data.persist.QueryableRecordImaBaseline;
import hirs.data.persist.ImaBlacklistBaseline;

import hirs.utils.Callback;
import hirs.utils.Job;
import hirs.utils.JobExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class DbImaBlacklistBaselineRecordManager extends DBManager<ImaBlacklistRecord>
    implements ImaBlacklistBaselineRecordManager {

    private static final Logger LOGGER =
            LogManager.getLogger(DbImaBlacklistBaselineRecordManager.class);
    private static final int LOGGING_INTERVAL = 500;

    /**
     * Creates a new <code>DBImaBaselineRecordManager</code>. The optional
     * SessionFactory parameter is used to manage sessions with a
     * hibernate db.
     *
     * @param factory session factory used to access database connections
     */
    public DbImaBlacklistBaselineRecordManager(final SessionFactory factory) {
        super(ImaBlacklistRecord.class, factory);
    }

    /**
     * Stores a new <code>ImaBlacklistRecord</code>. This stores a new
     * <code>ImaBlacklistRecord</code> to be managed by the <code>IMABaselineRecordManager</code>.
     * If the <code>ImaBlacklistRecord</code> is successfully saved then a reference to it is
     * returned.
     * <p>
     * Should only save the record to a <code>ImaBlacklistBaseline</code>  Other baselines should
     * not be controlled by the user.
     * <p> Records added in this manner MUST have a baseline set to them.
     *
     * @param record
     *            Record to save
     * @return reference to saved ImaBlacklistRecord
     * @throws ImaBaselineRecordManagerException
     *             if the record has previously been saved or unexpected error
     *             occurs
     */
    @Override
    public ImaBlacklistRecord saveRecord(final ImaBlacklistRecord record)
            throws ImaBaselineRecordManagerException {
        LOGGER.debug("saving ima baseline record: {}", record);

        //When adding a record through the manager, the record should be associated with a baseline
        if (record == null || record.getBaseline() == null) {
            throw new ImaBaselineRecordManagerException("Record cannot be saved without setting"
                    + " baseline.");
        }

        try {
            //Checks to make sure a duplicate record does not already exist in the baseline.  This
            //is done in this fashion due to the inability to put constraints on the table.
            if (getRecord(record.getPath(), record.getHash(),
                    (ImaBlacklistBaseline) record.getBaseline()) != null) {
                throw new ImaBaselineRecordManagerException("Record already exists.");
            } else {
                return super.save(record);
            }
        } catch (DBManagerException e) {
            throw new ImaBaselineRecordManagerException(e);
        }
    }

    /**
     * Retrieves an <code>ImaBlacklistRecord</code> based on the id of the record.
     *
     * @param id
     *      id of the <code>ImaBlacklistRecord</code>
     * @return
     *      instance of the requested <code>ImaBlacklistRecord</code>
     * @throws ImaBaselineRecordManagerException
     *      if the record has previously been saved or unexpected error occurs
     */
    @Override
    public ImaBlacklistRecord getRecord(final long id) throws ImaBaselineRecordManagerException {
        LOGGER.debug("getting baseline: {}", id);
        try {
            return super.get(id);
        } catch (DBManagerException e) {
            throw new ImaBaselineRecordManagerException(e);
        }
    }

    /**
     * Retrieves <code>ImaBlacklistRecord</code> with the given path, hash, and assigned IMA
     * baseline id.
     *
     * @param path
     *      path of the record to be retrieved
     * @param hash
     *      hash of the record to be retrieved
     * @param baseline
     *      baseline that is associated with the desired record.
     * @return
     *      ImaBlacklistRecord that matches the provided path, hash, and baseline ID
     */
    private ImaBlacklistRecord getRecord(final String path, final OptionalDigest hash,
                                       final ImaBlacklistBaseline baseline) {
        LOGGER.debug("Getting object list");

        if (path == null || hash == null || baseline == null) {
            LOGGER.error("path, hash, or baseline was null");
            return null;
        }

        ImaBlacklistRecord record = null;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            record = (ImaBlacklistRecord) session.createCriteria(ImaBlacklistRecord.class)
                    .add(Restrictions.eq("path", path))
                    .add(Restrictions.eq("hash", hash))
                    .add(Restrictions.eq("baseline", baseline))
                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).uniqueResult();
            tx.commit();
            LOGGER.debug("Retrieved record object");
        } catch (HibernateException e) {
            LOGGER.error("Unable to retrieve record", e);
            if (tx != null) {
                LOGGER.debug("Rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
        return record;
    }

    /**
     * Retrieves <code>ImaBlacklistRecord</code> with the given path, hash, and assigned IMA
     * baseline id.
     *
     * @param path
     *      path of the record to be retrieved
     * @param hash
     *      hash of the record to be retrieved
     * @param baseline
     *      baseline that is associated with the desired record.
     * @return
     *      ImaBlacklistRecord that matches the provided path, hash, and baseline ID
     */
    @Override
    public ImaBlacklistRecord getRecord(final String path, final Digest hash,
            final ImaBlacklistBaseline baseline) {
        OptionalDigest optionalDigest = null;
        if (hash != null) {
            optionalDigest = hash.asOptionalDigest();
        }
        return getRecord(path, optionalDigest, baseline);
    }

    /**
     * Deletes the <code>ImaBaselineRecord</code> identified by <code>id</code>. If the
     * <code>ImaBaselineRecord</code> is found and deleted then true is returned,
     * otherwise false.
     * <p>
     * Will likely be used to delete records of a <code>ImaBlacklistBaseline</code>  Other IMA
     * baselines would not be controlled by the user at this time.
     *
     * @param id
     *            id of the <code>ImaBaselineRecord</code> to delete
     * @return true if successfully found and deleted from database, otherwise false
     * @throws ImaBaselineRecordManagerException
     *             if unable to delete the ImaBaselineRecord for any reason other than
     *             not found
     */
    @Override
    public boolean deleteRecord(final Long id) throws ImaBaselineRecordManagerException {
        LOGGER.debug("deleting ima baseline record: {}", id);
        try {
            return super.delete(id);
        } catch (DBManagerException e) {
            throw new ImaBaselineRecordManagerException(e);
        }
    }

    /**
     * Deletes the <code>ImaBaselineRecord</code> provided. If the
     * <code>ImaBaselineRecord</code> is found and deleted then true is returned,
     * otherwise false.
     * @param record
     *          record object to be deleted.
     * @return true if successfully found and deleted from database, otherwise false
     * @throws ImaBaselineRecordManagerException
     *             if unable to delete the ImaBaselineRecord for any reason other than
     *             not found
     */
    @Override
    public boolean deleteRecord(final ImaBlacklistRecord record)
            throws ImaBaselineRecordManagerException {
        if (record == null || record.getBaseline() == null) {
            throw new ImaBaselineRecordManagerException("record or baseline of record not set");
        }
        LOGGER.debug("deleting ima baseline record: {}", record.getId());
        try {
            return super.delete(record);
        } catch (DBManagerException e) {
            throw new ImaBaselineRecordManagerException(e);
        }
    }

    /**
     * Iterates over the {@link ImaBlacklistRecord}s in the given baseline, and calls the given
     * Callback on each record.  If the callback returns a non-null value, the returned value will
     * be added to a collection, which is returned when iteration is finished.
     *
     * @param baseline    the baseline whose {@link ImaBlacklistRecord}s we should iterate over
     * @param callback    the callback to run on each record
     * @param <T>         the return type of the callback
     * @return the total collection of objects returned as results from the given Callback
     */
    public final <T> Collection<T> iterateOverBaselineRecords(
            final QueryableRecordImaBaseline baseline,
            final Callback<ImaBlacklistRecord, T> callback) {
        final Collection<T> allResults = new ConcurrentLinkedQueue<>();
        Collection<Callable<Void>> tasks = new ArrayList<>();

        int fetchSize;
        switch (getConfiguredImplementation()) {
            case MYSQL:
                fetchSize = Integer.MIN_VALUE;
                break;
            case HSQL:
            default:
                fetchSize = 1;
        }

        DBImpl impl = getConfiguredImplementation();
        if (impl == DBImpl.MYSQL) {
            // provides a hint to the JDBC connector that records should be streamed
            fetchSize = Integer.MIN_VALUE;
        } else if (impl == DBImpl.HSQL) {
            fetchSize = 1;
        }

        final AtomicInteger recCounter = new AtomicInteger();
        for (int i = 0; i < ImaBlacklistRecord.FILENAME_HASH_BUCKET_COUNT; i++) {
            final int bucket = i;
            final int finalFetchSize = fetchSize;
            tasks.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    LOGGER.debug(String.format(
                            "IMA record iteration: starting examiner thread %d",
                            bucket
                    ));
                    List<T> results = new LinkedList<>();

                    StatelessSession statelessSession = getStatelessSession();
                    try {
                        Transaction tx = statelessSession.beginTransaction();
                        Criteria criteria = statelessSession.createCriteria(baseline.getClass());
                        baseline.configureCriteriaForBaselineRecords(criteria, bucket);
                        criteria.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
                        criteria.setReadOnly(true);
                        criteria.setFetchSize(finalFetchSize);
                        ScrollableResults records = criteria.scroll(ScrollMode.FORWARD_ONLY);

                        Map entry;
                        ImaBlacklistRecord baselineRecord;
                        while (records.next()) {
                            // get(0) guarantees other rows won't be initialized
                            entry = (Map) records.get(0);
                            String path = (String) entry.get(ImaBlacklistRecord.PATH_FIELD);
                            OptionalDigest digest =
                                    (OptionalDigest) entry.get(ImaBlacklistRecord.HASH_FIELD);
                            baselineRecord = new ImaBlacklistRecord(path, digest.asDigest());
                            T result = callback.call(baselineRecord);
                            if (result != null) {
                                results.add(result);
                            }
                            int count = recCounter.incrementAndGet();
                            if (count % LOGGING_INTERVAL == 0) {
                                LOGGER.debug(String.format(
                                        "IMA record iteration: examined %d records", count
                                ));
                            }
                        }
                        tx.commit();
                    } catch (Exception e) {
                        throw e;
                    } finally {
                        statelessSession.close();
                    }

                    allResults.addAll(results);
                    return null;
                }
            });
        }

        JobExecutor executor = new JobExecutor();
        Job<Void> job = new Job<>(tasks);
        executor.scheduleJob(job);

        try {
            executor.shutdown();
        } catch (InterruptedException e) {
            throw new DBManagerException(e);
        }

        if (job.getState() != Job.State.COMPLETED) {
            LOGGER.error("Appraisal job finished in state {}", job.getState().toString());
            for (String s : job.getAllFailures()) {
                throw new DBManagerException(s);
            }
        }

        return allResults;
    }
}
