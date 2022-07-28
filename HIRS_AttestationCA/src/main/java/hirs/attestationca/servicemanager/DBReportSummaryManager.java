package hirs.attestationca.servicemanager;

import hirs.data.persist.ReportSummary;
import hirs.persist.DBManagerException;
import hirs.persist.ReportSummaryManager;
import hirs.persist.ReportSummaryManagerException;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.query.Query;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * This class defines a <code>DBReportSummaryManager</code> that stores the
 * report
 * summary in a database.
 */
@Service
public class DBReportSummaryManager extends DBManager<ReportSummary>
        implements ReportSummaryManager {

    private static final Logger LOGGER = getLogger(DBReportSummaryManager.class);

    /**
     * Creates a new <code>DBReportSummaryManager</code> that uses the default
     * database.
     * The default database is used to store all of the
     * <code>ReportSummary</code> objects.
     *
     * @param em entity manager used to access database connections
     */
    public DBReportSummaryManager(final EntityManager em) {
        super(ReportSummary.class, em);
    }

    /**
     * Saves the <code>ReportSummary</code> in the database and returns it.
     *
     * @param report
     *            report summary to save
     * @return <code>ReportSummary</code> that was saved
     * @throws hirs.persist.ReportSummaryManagerException
     *             if ReportSummary has previously been saved or an error
     *             occurs while trying to save it to the database
     */
    @Override
    public final ReportSummary saveReportSummary(final ReportSummary report)
            throws ReportSummaryManagerException {
        LOGGER.debug("Saving ReportSummary: {}", report);
        try {
            return super.save(report);
        } catch (DBManagerException e) {
            throw new ReportSummaryManagerException(e);
        }
    }

    /**
     * Updates a <code>ReportSummary</code>. This updates the database entries
     * to reflect the new values that should be set.
     *
     * @param report
     *            report
     * @throws ReportSummaryManagerException
     *             if Report has not previously been saved or an error occurs
     *             while trying to save it to the database
     */
    @Override
    public final void updateReportSummary(final ReportSummary report)
            throws ReportSummaryManagerException {
        LOGGER.debug("updating ReportSummary: {}", report);
        try {
            super.update(report);
        } catch (DBManagerException e) {
            throw new ReportSummaryManagerException(e);
        }
    }

    /**
     * Returns a list of all <code>ReportSummary</code>s of type
     * <code>clazz</code>. This searches through the database for this
     * information.
     *
     * @param clazz
     *            class type of <code>ReportSummary</code>s to return (may be
     *            null)
     * @return list of <code>ReportSummary</code>s
     * @throws ReportSummaryManagerException
     *             if unable to search the database
     */
    @Override
    public final List<ReportSummary> getReportSummaryList(
            final ReportSummary clazz)
            throws ReportSummaryManagerException {
        LOGGER.debug("getting ReportSummary list");
        try {
            // this should work but there is an issue with the ReportSummary class
            return null; //super.getList(clazz);
        } catch (DBManagerException e) {
            throw new ReportSummaryManagerException(e);
        }
    }

    /**
     * Returns a list of <code>ReportSummary</code>s of type
     * <code>clazz</code> that share the provided <code>hostname</code>. This
     * searches through the database for this information.
     *
     * @param hostname
     *            hostname for the machine in which the
     *            <code>ReportSummary</code> was generated for.
     * @return list of <code>ReportSummary</code>s
     * @throws ReportSummaryManagerException
     *             if unable to search the database
     */
    @Override
    public final List<ReportSummary> getReportSummaryListByHostname(
            final String hostname) throws ReportSummaryManagerException {
        LOGGER.debug("getting ReportSummary hostname list");

        if (hostname == null) {
            LOGGER.debug("hostname was null");
            throw new ReportSummaryManagerException(
                    "Hostname provided was null");
        }

        List<ReportSummary> reportSummaryList = new ArrayList<>();
        Transaction tx = null;
        Session session = getSession();
        try {
            LOGGER.debug("retrieving objects from db");
            tx = session.beginTransaction();
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<ReportSummary> criteriaQuery = criteriaBuilder
                    .createQuery(ReportSummary.class);
            Root<ReportSummary> root = criteriaQuery.from(ReportSummary.class);
            Predicate recordPredicate = criteriaBuilder
                    .and(criteriaBuilder.equal(root.get("clientHostname"), hostname));
            criteriaQuery.select(root).where(recordPredicate).distinct(true);
            Query<ReportSummary> query = session.createQuery(criteriaQuery);
            List<ReportSummary> results = query.getResultList();
            if (results != null) {
                reportSummaryList.addAll(results);
            }

            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve query list";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new ReportSummaryManagerException(e);
        }
        return reportSummaryList;
    }

    /**
     * Retrieves the <code>ReportSummary</code> from the database. This
     * searches the database for an entry whose id matches <code>id</code>.
     * It then reconstructs a <code>ReportSummary</code> object from the
     * database entry
     *
     * @param id
     *            id of the ReportSummary
     * @return ReportSummary if found, otherwise null.
     * @throws ReportSummaryManagerException
     *             if unable to search the database or recreate the
     *             <code>ReportSummary</code>
     */
    @Override
    public final ReportSummary getReportSummary(final long id)
            throws ReportSummaryManagerException {
        LOGGER.debug("getting ReportSummary: {}", id);
        try {
            return super.get(id);
        } catch (DBManagerException e) {
            throw new ReportSummaryManagerException(e);
        }
    }

    /**
     * Retrieves the <code>ReportSummary</code> identified by the
     * <code>Report</code>'s <code>id</code>. If the <code>ReportSummary</code>
     * cannot be found then null is returned.
     *
     * @param id
     *            <code>UUID</code> of the <code>Report</code>
     * @return <code>ReportSummary</code> whose <code>Report</code>'s
     *         <code>UUID</code> is <code>id</code> or null if not found
     */
    @Override
    public final ReportSummary getReportSummaryByReportID(final UUID id) {
        LOGGER.debug("getting ReportSummary that matches uuid");

        if (id == null) {
            LOGGER.debug("id provided was null");
            throw new ReportSummaryManagerException(
                    "id provided was null");
        }

        ReportSummary object;
        Transaction tx = null;
        Session session = getSession();
        try {
            LOGGER.debug("retrieving objects from db");
            tx = session.beginTransaction();
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<ReportSummary> criteriaQuery = criteriaBuilder
                    .createQuery(ReportSummary.class);
            Root<ReportSummary> root = criteriaQuery.from(ReportSummary.class);
            Predicate recordPredicate = criteriaBuilder
                    .and(criteriaBuilder.equal(root.get("report.id"), id));
            criteriaQuery.select(root).where(recordPredicate);
            Query<ReportSummary> query = session.createQuery(criteriaQuery);
            object = query.uniqueResult();
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve query list";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new ReportSummaryManagerException(e);
        }
        return object;
    }

    /**
     * Returns a list of <code>ReportSummary</code>s that contains the latest
     * report from each client. This searches through the database for this
     * information.
     *
     * @return list of <code>ReportSummary</code>s
     * @throws ReportSummaryManagerException
     *             if unable to search the database
     */
    @Override
    public final List<ReportSummary> getUniqueClientLatestReportList()
            throws ReportSummaryManagerException {
        LOGGER.debug("getting latest reports for all clients");

        List<ReportSummary> reportSummaryList = new ArrayList<>();
        Transaction tx = null;
        Session session = getSession();
        try {
            LOGGER.debug("retrieving objects from db");
            tx = session.beginTransaction();
//            DetachedCriteria uniqueHosts = DetachedCriteria.forClass(
//                    ReportSummary.class);

//            ProjectionList properties = Projections.projectionList();
//            properties.add(Projections.groupProperty("clientHostname"));
//            properties.add(Projections.max("timestamp"), "timestamp");
//
//            uniqueHosts.setProjection(properties);
//
//            List list = session.createCriteria(ReportSummary.class)
//                    .add(Subqueries.propertiesIn(
//                            new String[]{"clientHostname", "timestamp"},
//                            uniqueHosts))
//                    .list();
//            for (Object o : list) {
//                if (o instanceof ReportSummary) {
//                    reportSummaryList.add((ReportSummary) o);
//                }
//            }

            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<ReportSummary> criteriaQuery = criteriaBuilder
                    .createQuery(ReportSummary.class);
            Root<ReportSummary> root = criteriaQuery.from(ReportSummary.class);
            criteriaQuery.multiselect(root.get("clientHostname"), root.get("timestamp"));
            Query<ReportSummary> query = session.createQuery(criteriaQuery);
            List<ReportSummary> results = query.getResultList();
            if (results != null) {
                reportSummaryList.addAll(results);
            }
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve query list";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new ReportSummaryManagerException(e);
        }
        return reportSummaryList;
    }

    /**
     * Retrieves the newest report timestamp for a given <code>hostname</code>.
     * @param hostname
     *            hostname of <code>client</code> to be checked for
     * @return newest timestamp from matching <code>ReportSummaries</code>
     * @throws ReportSummaryManagerException
     *             if unable to return a timestamp
     */
    @Override
    public final ReportSummary getNewestReport(final String hostname)
            throws ReportSummaryManagerException {
        LOGGER.debug("getting newest report timestamp for: " + hostname);

        if (hostname == null) {
            LOGGER.debug("hostname was null, trying to return generic list");
            throw new ReportSummaryManagerException(
                    "Hostname provided was null");
        }

        //Sets the order so the first result is the most recent Timestamp
        Order order = Order.desc("timestamp");

        return getReportTimestamp(hostname, order);
    }

    /**
     * Retrieves the newest report timestamp for a given <code>hostname</code>.
     * @param hostname
     *            hostname of <code>client</code> to be checked for
     * @return newest timestamp from matching <code>ReportSummaries</code>
     * @throws ReportSummaryManagerException
     *             if unable to return a timestamp
     */
    @Override
    public final ReportSummary getFirstReport(final String hostname)
            throws ReportSummaryManagerException {
        LOGGER.debug("getting newest report timestamp for: " + hostname);

        if (hostname == null) {
            LOGGER.debug("hostname was null, trying to return generic list");
            throw new ReportSummaryManagerException(
                    "Hostname provided was null");
        }

        //Sets the order so the first result is oldest timestamp
        Order order = Order.asc("timestamp");

        return getReportTimestamp(hostname, order);
    }

    private ReportSummary getReportTimestamp(final String hostname,
            final Order order) throws ReportSummaryManagerException {
        final int firstResult = 0;
        final int uniqueResult = 1;

        ReportSummary object;
        Transaction tx = null;
        Session session = getSession();
        try {
            LOGGER.debug("retrieving objects from db");
            tx = session.beginTransaction();
            //Returns a ReportSummary based on the timestamp and hostname
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<ReportSummary> criteriaQuery = criteriaBuilder
                    .createQuery(ReportSummary.class);
            Root<ReportSummary> root = criteriaQuery.from(ReportSummary.class);
            Predicate recordPredicate = criteriaBuilder
                    .and(criteriaBuilder.equal(root.get("clientHostname"), hostname));
            criteriaQuery.select(root).where(recordPredicate);
            Query<ReportSummary> query = session.createQuery(criteriaQuery);
            query.setFirstResult(firstResult);
            query.setMaxResults(uniqueResult);
            object = query.getSingleResult();

//            object = (ReportSummary) session.createCriteria(ReportSummary.class)
//                    .addOrder(order)
//                    .add(Restrictions.eq("clientHostname", hostname))
//                    .setFirstResult(firstResult)
//                    .setMaxResults(uniqueResult)
//                    .uniqueResult();
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve query list";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new ReportSummaryManagerException(e);
        }
        return object;
    }
}
