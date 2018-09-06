package hirs.persist;

import hirs.FilteredRecordsList;
import hirs.data.bean.SimpleBaselineBean;
import hirs.data.persist.Baseline;
import hirs.data.persist.BroadRepoImaBaseline;
import hirs.data.persist.IMABaselineRecord;
import hirs.data.persist.ImaBlacklistRecord;
import hirs.repository.RepoPackage;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import static org.hibernate.criterion.Restrictions.ilike;
import static org.hibernate.criterion.Restrictions.sqlRestriction;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Subqueries;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.transform.Transformers;

/**
 * This class defines a <code>BaselineManager</code> that stores the baselines
 * in a database.
 */
public class DBBaselineManager extends DBManager<Baseline> implements BaselineManager {

    private static final Logger LOGGER = LogManager.getLogger(DBBaselineManager.class);

    /**
     * Creates a new <code>DBBaselineManager</code> that uses the default
     * database. The default database is used to store all of the
     * <code>Baseline</code>s.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBBaselineManager(final SessionFactory sessionFactory) {
        super(Baseline.class, sessionFactory);
    }

    /**
     * Saves the <code>Baseline</code> in the database. This creates a new
     * database session and saves the baseline. If the <code>Baseline</code> had
     * previously been saved then a <code>BaselineManagerException</code> is
     * thrown.
     *
     * @param baseline
     *            baseline to save
     * @return reference to saved baseline
     * @throws BaselineManagerException
     *             if baseline has previously been saved or an error occurs
     *             while trying to save it to the database
     */
    @Override
    public final Baseline saveBaseline(final Baseline baseline)
            throws BaselineManagerException {
        LOGGER.debug("saving baseline: {}", baseline);
        try {
            return super.save(baseline);
        } catch (DBManagerException e) {
            throw new BaselineManagerException(e);
        }
    }

    /**
     * Updates a <code>Baseline</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param baseline
     *            baseline
     * @throws BaselineManagerException
     *             if baseline has not previously been saved or an error occurs
     *             while trying to save it to the database
     */
    @Override
    public final void updateBaseline(final Baseline baseline) throws BaselineManagerException {
        LOGGER.debug("updating baseline: {}", baseline);
        try {
            super.update(baseline);
        } catch (DBManagerException e) {
            throw new BaselineManagerException(e);
        }
    }

    /**
     * Queries the database for a list of all the <code>Baseline</code>s of type <code>clazz</code>
     * that have not been soft-deleted.
     *
     * @param clazz
     *            class type of <code>Baseline</code>s to return (may be null)
     * @return list of <code>Baseline</code> names
     * @throws BaselineManagerException
     *             if unable to search the database
     */
    @Override
    public final List<Baseline> getBaselineList(final Class<? extends Baseline> clazz)
            throws BaselineManagerException {
        LOGGER.debug("Getting baseline list");
        final List<Baseline> baselines = new ArrayList<>();

        Class<? extends Baseline> searchClass = clazz;
        if (clazz == null) {
            LOGGER.debug("null clazz, using Baseline as the default search class");
            searchClass = Baseline.class;
        }

        Session session = getFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            Criteria criteria = session.createCriteria(searchClass)
                    .add(Restrictions.isNull("archivedTime"))
                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            List list = criteria.list();
            for (Object o : list) {
                if (o instanceof Baseline) {
                    baselines.add((Baseline) o);
                }
            }
            tx.commit();
            LOGGER.debug("Got {} baselines", baselines.size());
        } catch (HibernateException e) {
            LOGGER.error("Unable to retrieve baselines", e);
            LOGGER.debug("Rolling back transaction");
            tx.rollback();
            throw e;
        }

        LOGGER.debug("Got {} baselines", baselines.size());
        return baselines;
    }

    /**
     * Returns a list of all <code>Baseline</code>s that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables. Soft-deleted
     * <code>Baseline</code>s are not included in the returned list.
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
     * @throws BaselineManagerException
     *          if unable to create the list
     */
    @Override
    public final FilteredRecordsList<Baseline> getOrderedBaselineList(
            final String columnToOrder, final boolean ascending,
            final int firstResult, final int maxResults, final String search,
            final Map<String, Boolean> searchableColumns)
            throws BaselineManagerException {

        if (columnToOrder == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        // check that the baseline is not archived
        CriteriaModifier modifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                criteria.add(Restrictions.isNull("archivedTime"));
            }
        };

        final FilteredRecordsList<Baseline> baselines;
        try {
            LOGGER.debug("querying db for baselines");
            baselines = super.getOrderedList(Baseline.class, columnToOrder, ascending, firstResult,
                    maxResults, search, searchableColumns, modifier);
        } catch (DBManagerException e) {
            throw new BaselineManagerException(e);
        }
        return baselines;
    }

    /**
     * Returns a list of all IMABaselineRecord that are ordered by a column
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
     * @throws BaselineManagerException
     *          if unable to create the list
     */
    @Override
    public final FilteredRecordsList<IMABaselineRecord> getOrderedRecordList(
            final UUID baselineId, final String columnToOrder, final boolean ascending,
            final int firstResult, final int maxResults, final String search)
            throws BaselineManagerException {
        if (columnToOrder == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        LOGGER.debug("Getting baseline list");
        //Object that will store query values
        FilteredRecordsList<IMABaselineRecord> queryResults = new FilteredRecordsList<>();

        Transaction tx = null;

        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("updating object in db");
            tx = session.beginTransaction();

            Criteria criteria = session
                    .createCriteria(IMABaselineRecord.class, "record")
                    .add(sqlRestriction("ima_baseline_id='" + baselineId + "'"))
                    .setProjection(Projections.count("id"));
            Long totalResultCount = (Long) criteria.uniqueResult();

            // Search for all words in all searchable columns
            Conjunction and = buildBaselineRecordSearchFilter(search);
            criteria.add(and);

            Long recordsFiltered = (Long) criteria.uniqueResult();

            criteria.setProjection(null)
                    .setFirstResult(firstResult)
                    .setMaxResults(maxResults);

            if (ascending) {
                criteria.addOrder(Order.asc(columnToOrder));
            } else {
                criteria.addOrder(Order.desc(columnToOrder));
            }

            //Stores results of all the queries for the JQuery Datatable
            queryResults.setRecordsTotal(totalResultCount);
            queryResults.setRecordsFiltered(recordsFiltered);

            List list = criteria.list();
            for (Object o : list) {
                if (o instanceof IMABaselineRecord) {
                    queryResults.add((IMABaselineRecord) o);
                }
            }
            tx.commit();
        } catch (HibernateException e) {
            final String msg = "unable to update object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
        return queryResults;
    }

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
     * @throws BaselineManagerException
     *          if unable to create the list
     */
    @Override
    public final FilteredRecordsList<ImaBlacklistRecord> getOrderedBlacklistRecordList(
            final UUID baselineId, final String columnToOrder, final boolean ascending,
            final int firstResult, final int maxResults, final String search)
            throws BaselineManagerException {
        if (columnToOrder == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        LOGGER.debug("Getting baseline list");
        //Object that will store query values
        FilteredRecordsList<ImaBlacklistRecord> queryResults = new FilteredRecordsList<>();

        Transaction tx = null;

        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("updating object in db");
            tx = session.beginTransaction();

            Criteria criteria = session
                    .createCriteria(ImaBlacklistRecord.class, "record")
                    .add(sqlRestriction("ima_baseline_id='" + baselineId + "'"))
                    .setProjection(Projections.count("id"));
            Long totalResultCount = (Long) criteria.uniqueResult();

            // Search for all words in all searchable columns
            Conjunction and = buildBaselineRecordSearchFilter(search);
            criteria.add(and);

            Long recordsFiltered = (Long) criteria.uniqueResult();

            criteria.setProjection(null)
                    .setFirstResult(firstResult)
                    .setMaxResults(maxResults);

            if (ascending) {
                criteria.addOrder(Order.asc(columnToOrder));
            } else {
                criteria.addOrder(Order.desc(columnToOrder));
            }

            //Stores results of all the queries for the JQuery Datatable
            queryResults.setRecordsTotal(totalResultCount);
            queryResults.setRecordsFiltered(recordsFiltered);
            List list = criteria.list();
            for (Object o : list) {
                if (o instanceof ImaBlacklistRecord) {
                    queryResults.add((ImaBlacklistRecord) o);
                }
            }
            tx.commit();
        } catch (HibernateException e) {
            final String msg = "unable to update object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
        return queryResults;
    }

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
     * @throws BaselineManagerException
     *          if unable to create the list
     */
    @Override
    public final FilteredRecordsList<RepoPackage> getOrderedPackageList(
            final String name, final String columnToOrder, final boolean ascending,
            final int firstResult, final int maxResults, final String search)
            throws BaselineManagerException {
        if (columnToOrder == null) {
            LOGGER.debug("null columnToOrder argument");
            throw new NullPointerException("columnToOrder");
        }

        LOGGER.debug("Getting package list");
        //Object that will store query values
        FilteredRecordsList<RepoPackage> queryResults = new FilteredRecordsList<>();

        Transaction tx = null;

        Session session = getFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();

            // first use a subquery to list the ids for the packages in the baseline
            Criteria criteria = session.createCriteria(BroadRepoImaBaseline.class)
                    .createAlias("repoPackages", "pkg")
                    .add(Restrictions.eq("name", name));

            // Get the total result count
            long totalResultCount = (long) criteria
                    .setProjection(Projections.countDistinct("pkg.id"))
                    .uniqueResult();

            queryResults.setRecordsTotal(totalResultCount);

            if (totalResultCount > 0) {
                // Get the package ids related to the baseline
                List firstList = criteria.setProjection(Property.forName("pkg.id"))
                        .list();
                final List<UUID> packageIds = new ArrayList<>();
                for (Object o : firstList) {
                    if (o instanceof UUID) {
                        packageIds.add((UUID) o);
                    }
                }

                // Get the IDs for the packages that match the search filter
                criteria = session.createCriteria(RepoPackage.class)
                        .createAlias("sourceRepository", "repo")
                        .createAlias("packageRecords", "rec")
                        .add(Restrictions.in("id", packageIds));

                final long recordsFiltered;

                // Add the search filters
                if (StringUtils.isNotBlank(search)) {
                    Conjunction and = buildPackageSearchFilter(search, session);
                    criteria.add(and);

                    recordsFiltered = (long) criteria
                        .setProjection(Projections.countDistinct("id"))
                        .uniqueResult();
                } else {
                    // If there s no search, there are no filtered records
                    recordsFiltered = totalResultCount;
                }

                queryResults.setRecordsFiltered(recordsFiltered);

                if (recordsFiltered > 0) {
                    // Get the filtered ids
                    List secondList = criteria.setProjection(
                            Projections.distinct(Property.forName("id"))).list();

                    final List<UUID> morePackageIds = new ArrayList<>();
                    for (Object o : secondList) {
                        if (o instanceof UUID) {
                            morePackageIds.add((UUID) o);
                        }
                    }

                    // Remove the count projection, and sort and page to get results
                    criteria = session.createCriteria(RepoPackage.class)
                            .createAlias("sourceRepository", "repo")
                            .add(Restrictions.in("id", morePackageIds))
                            .setFirstResult(firstResult)
                            .setMaxResults(maxResults);

                    if (ascending) {
                        criteria.addOrder(Order.asc(columnToOrder));
                    } else {
                        criteria.addOrder(Order.desc(columnToOrder));
                    }

                    List list = criteria.list();
                    for (Object o : list) {
                        if (o instanceof RepoPackage) {
                            queryResults.add((RepoPackage) o);
                        }
                    }
                }
            }

            tx.commit();
        } catch (SQLGrammarException ex) {
            // This sometimes happens when the result set is empty,
            // due to the ugly auto-built SQL
            LOGGER.error("Error getting (probably empty) package list", ex);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
        } catch (HibernateException e) {
            final String msg = "unable to get packages";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
        return queryResults;
    }

    private Conjunction buildPackageSearchFilter(final String search, final Session session) {
        final List<String> searchableColumns = Arrays.asList(
                "repo.name", "name", "version", "release", "rec.path");
        // Search for all words in all searchable columns
        Conjunction and = Restrictions.conjunction();
        String[] searchWords = search.split(" ");
        for (String word : searchWords) {
            // Every word must be in at least one column
            Disjunction or = Restrictions.disjunction();
            for (String column : searchableColumns) {
                or.add(ilike(column, word, MatchMode.ANYWHERE));
            }

            // Add additional search filter for records, since aliasing wasn't
            // working properly for searching the hash/digest field
            or.add(Restrictions.in("rec.id", getMatchingRecordIds(word, session)));

            and.add(or);
        }
        return and;
    }

    private List<Long> getMatchingRecordIds(final String search, final Session session) {
        // Get the IDs for records that match the search filter.
        List<Long> matchingRecordIds = new ArrayList<>();
        if (StringUtils.isNotBlank(search)) {
            // Search for all words in all searchable columns
            Conjunction and = buildBaselineRecordSearchFilter(search);
            List list = session.createCriteria(IMABaselineRecord.class)
                    .add(and)
                    .setProjection(Property.forName("id"))
                    .list();

            for (Object o : list) {
                matchingRecordIds.add(Long.parseLong(o.toString()));
            }
        }
        return matchingRecordIds;
    }

    /**
     * Returns a list of all IMABaselineRecords in the specified package.
     *
     * @param id id of the package
     * @param search string of criteria to be matched to visible columns
     * @return List the records
     * @throws BaselineManagerException if unable to create the list
     */
    @Override
    public List<IMABaselineRecord> getPackageRecords(final UUID id, final String search)
            throws BaselineManagerException {
        LOGGER.debug("Getting package records");

        //Object that will store query values

        Transaction tx = null;

        Session session = getFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();

            // first use a subquery to list the ids for the records in the package
            DetachedCriteria ids = DetachedCriteria.forClass(RepoPackage.class)
                    .createAlias("packageRecords", "rec")
                    .add(Restrictions.eq("id", id))
                    .setProjection(Property.forName("rec.id"));

            Conjunction and = buildBaselineRecordSearchFilter(search);

            // Get the records
            List list = session.createCriteria(IMABaselineRecord.class)
                    .add(Subqueries.propertyIn("id", ids))
                    .add(and)
                    .addOrder(Order.asc("path"))
                    .list();

            List<IMABaselineRecord> records = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof IMABaselineRecord) {
                    records.add((IMABaselineRecord) o);
                }
            }

            //Stores results of all the queries for the JQuery Datatable
            tx.commit();
            return records;
        } catch (HibernateException e) {
            final String msg = "unable to get packages";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
    }

    private Conjunction buildBaselineRecordSearchFilter(final String search) {
        // Search for all words in all searchable columns
        Conjunction and = Restrictions.conjunction();
        String[] searchWords = StringUtils.split(search);
        for (String word : searchWords) {
            // Every word must be in at least one column
            Disjunction or = Restrictions.disjunction();
            or.add(ilike("path", word, MatchMode.ANYWHERE));
            or.add(ilikeHex("digest", word));
            and.add(or);
        }
        return and;
    }

    /**
     * Retrieves the <code>Baseline</code> from the database. This searches the
     * database for an entry whose name matches <code>name</code>. It then
     * reconstructs a <code>Baseline</code> object from the database entry
     *
     * @param name
     *            name of the baseline
     * @return baseline if found, otherwise null.
     * @throws BaselineManagerException
     *             if unable to search the database or recreate the
     *             <code>Baseline</code>
     */
    @Override
    public final Baseline getBaseline(final String name)
            throws BaselineManagerException {
        LOGGER.debug("getting baseline: {}", name);
        try {
            return super.get(name);
        } catch (DBManagerException e) {
            throw new BaselineManagerException(e);
        }
    }

    @Override
    public final Baseline getBaseline(final Serializable id) {
        LOGGER.debug("getting baseline with id: {}", id);
        try {
            return super.get(id);
        } catch (DBManagerException e) {
            throw new BaselineManagerException(e);
        }
    }

    @Override
    public final Baseline getCompleteBaseline(final String name) throws BaselineManagerException {
        LOGGER.debug("getting full baseline: {}", name);
        try {
            return super.getAndLoadLazyFields(name, true);
        } catch (DBManagerException e) {
            throw new BaselineManagerException(e);
        }
    }

    @Override
    public final FilteredRecordsList<SimpleBaselineBean> getOrderedBaselineListWithoutRecords(
            final String columnToOrder, final boolean ascending,
            final int firstResult, final int maxResults, final String search,
            final Map<String, Boolean> searchableColumns)
            throws BaselineManagerException {

        if (columnToOrder == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        final FilteredRecordsList<SimpleBaselineBean> baselines =
                new FilteredRecordsList<>();
        final SessionFactory factory = getFactory();
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        Long totalResultCount;
        Long filteredResultCount = 0L;
        try {
            tx = session.beginTransaction();
            LOGGER.debug("retrieving baseline list without records");
            // The first query gets the total number of non-archived baselines
            Criteria cr = session.createCriteria(Baseline.class)
                    .add(Restrictions.isNull("archivedTime"))
                    .setProjection(Projections.countDistinct("id"));
            totalResultCount = (Long) cr.uniqueResult();

            // This second query finds the number of non-archived baselines matching the filter
            cr = session.createCriteria(Baseline.class)
                .add(Restrictions.isNull("archivedTime"))
                .setProjection(Projections.countDistinct("id"));

            // Filter using the search terms provided by the user
            Conjunction and = Restrictions.conjunction();
            if (totalResultCount != 0) {
                //Builds the search criteria from all of the searchable columns
                if (searchableColumns != null) {
                    // Search for all words in all searchable columns
                    String[] searchWords = search.split(" ");
                    for (String word : searchWords) {
                        // Every word must be in at least one column
                        Disjunction or = Restrictions.disjunction();
                        for (Entry<String, Boolean> entry : searchableColumns.entrySet()) {
                            if (entry.getValue()) {
                                or.add(ilike(entry.getKey(), word, MatchMode.ANYWHERE));
                            } else {
                                or.add(ilikeCast(entry.getKey(), word));
                            }
                        }
                        and.add(or);
                    }
                }
                cr.add(and);
                filteredResultCount = (Long) cr.uniqueResult();
            }

            if (filteredResultCount != 0) {
                // The third query builds a list from the filters, limits, and sorting options
                cr = session.createCriteria(Baseline.class)
                        .add(Restrictions.isNull("archivedTime"))
                        .add(and)
                        .setProjection(Projections.projectionList()
                            .add(Projections.property("id"), "id")
                            .add(Projections.property("createTime"), "createTime")
                            .add(Projections.property("name"), "name")
                            .add(Projections.property("severity"), "severity")
                            .add(Projections.property("type"), "type"))
                        .setResultTransformer(Transformers.aliasToBean(SimpleBaselineBean.class))
                        .setFirstResult(firstResult)
                        .setMaxResults(maxResults);

                if (ascending) {
                    cr.addOrder(Order.asc(columnToOrder));
                } else {
                    cr.addOrder(Order.desc(columnToOrder));
                }

                // Perform the query and add all baselines to the list
                List list = cr.list();
                for (Object o : list) {
                    if (o instanceof SimpleBaselineBean) {
                        baselines.add((SimpleBaselineBean) o);
                    }

                }
            }

            // Update meta data for the Data Table.
            baselines.setRecordsTotal(totalResultCount);
            baselines.setRecordsFiltered(filteredResultCount);
            tx.commit();
        } catch (HibernateException e) {
            final String msg = "Error getting the SimpleBaselineBean list";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        } finally {
            if (session != null && session.isConnected()) {
                session.close();
            }
        }

        return baselines;
    }
}
