package hirs.persist.imarecord;

import hirs.FilteredRecordsList;
import hirs.data.persist.IMAMeasurementRecord;
import org.hibernate.Query;
import org.hibernate.Session;

import java.util.List;

/**
 * Abstract base class for executing IMAMeasurementRecord queries. Provides standardized support for
 * querying the various counts required by DataTables for server-side paging and filtering.
 */
public abstract class DbImaRecordQuery {

    /**
     * Session to use when executing the query.
     */
    private final Session session;

    /**
     * Object containing the parameters required to execute the query.
     */
    private final DbImaRecordQueryParameters params;

    /**
     * Constructor setting the parameters required to execute the query.
     *
     * @param session the Session to use to execute the query
     * @param params the parameters specifying how to execute the query.
     */
    public DbImaRecordQuery(final Session session, final DbImaRecordQueryParameters params) {
        this.session = session;
        this.params = params;
    }

    /**
     * Returns the IMA record query parameters object.
     *
     * @return the IMA record query parameters object.
     */
    protected final DbImaRecordQueryParameters getParams() {
        return params;
    }

    /**
     * Executes the query using the specified Session and DbImaRecordQueryParameters.
     *
     * @return FilteredRecordsList containing the results of the query.
     */
    public abstract FilteredRecordsList query();

    private String addSearchToHQL(final String hql) {
        return hql + " and lower(rec.path) like lower(:search)";
    }

    private String addCountToHQL(final String hql) {
        return hql.replace("select rec", "select count(rec)");
    }

    @SuppressWarnings("checkstyle:avoidinlineconditionals")
    private String addOrderToHQL(final String hql) {
        if (params.getColumnToOrder() == null) {
            return hql;
        } else {
            return hql
                    + " order by rec." + params.getColumnToOrder().getHQL()
                    + (params.isAscending() ? " asc" : " desc");
        }
    }

    /**
     * Executes a query using the specified id.
     *
     * @param hql the HQL query to execute
     * @param idParam the id for the query as the correct class
     * @return the results of the query as a FilteredRecordsList.
     */
    protected FilteredRecordsList<IMAMeasurementRecord> query(
            final String hql, final Object idParam) {

        FilteredRecordsList<IMAMeasurementRecord> imaMeasurementRecords
                = new FilteredRecordsList<>();

        // count total records
        final String totalHQL = addCountToHQL(hql);
        final Query totalQuery = session.createQuery(totalHQL);
        if (idParam != null) {
            totalQuery.setParameter("id", idParam);
        }
        imaMeasurementRecords.setRecordsTotal((Long) totalQuery.uniqueResult());

        // count filtered records
        final String filteredHQL = addCountToHQL(addSearchToHQL(hql));
        final Query filteredQuery = session.createQuery(filteredHQL);
        if (idParam != null) {
            filteredQuery.setParameter("id", idParam);
        }
        filteredQuery.setParameter("search", "%" + params.getSearch() + "%");
        imaMeasurementRecords.setRecordsFiltered((Long) filteredQuery.uniqueResult());

        // get IMAMeasurementRecords
        final String dataHQL = addOrderToHQL(addSearchToHQL(hql));
        final Query dataQuery = session.createQuery(dataHQL);
        if (idParam != null) {
            dataQuery.setParameter("id", idParam);
        }
        dataQuery.setParameter("search", "%" + params.getSearch() + "%");
        dataQuery.setFirstResult(params.getFirstResult());
        dataQuery.setMaxResults(params.getMaxResults());
        List list = dataQuery.list();
        for (Object o : list) {
            if (o instanceof IMAMeasurementRecord) {
                imaMeasurementRecords.add((IMAMeasurementRecord) o);
            }
        }

        return imaMeasurementRecords;

    }

}
