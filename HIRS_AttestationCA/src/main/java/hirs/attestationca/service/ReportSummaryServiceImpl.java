package hirs.attestationca.service;

import hirs.FilteredRecordsList;
import hirs.attestationca.entity.ReportSummary;
import hirs.attestationca.repository.ReportSummaryRepository;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.ReportSummaryManagerException;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.LogManager.getLogger;

public class ReportSummaryServiceImpl extends DbServiceImpl<ReportSummary>
        implements DefaultService<ReportSummary>, ReportSummaryService {

    private static final Logger LOGGER = getLogger(ReportSummaryServiceImpl.class);
    @Autowired
    private ReportSummaryRepository<ReportSummary> reportSummaryRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean archive(UUID uuid) {
        return false;
    }

    @Override
    public List<ReportSummary> getList() {
        LOGGER.debug("Getting all Report Summary...");

        return getRetryTemplate().execute(new RetryCallback<List<ReportSummary>,
                DBManagerException>() {
            @Override
            public List<ReportSummary> doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return reportSummaryRepository.findAll();
            }
        });
    }

    @Override
    public void updateElements(final List<ReportSummary> elements) {
        LOGGER.debug("updating Report Summary list");
        for (ReportSummary summary : elements) {
            try {
                reportSummaryRepository.update(summary);
            } catch (DBManagerException e) {
                throw new ReportSummaryManagerException(e);
            }
        }
    }

    @Override
    public void deleteObjectById(UUID uuid) {
        if (uuid != null) {
            this.reportSummaryRepository.delete(
                    this.reportSummaryRepository.getReferenceById(uuid));
        }
    }

    @Override
    public ReportSummary saveReportSummary(final ReportSummary report)
            throws ReportSummaryManagerException {
        LOGGER.debug("Saving ReportSummary: {}", report);
        try {
            return reportSummaryRepository.save(report);
        } catch (DBManagerException e) {
            throw new ReportSummaryManagerException(e);
        }
    }

    @Override
    public void updateReportSummary(final ReportSummary report) {
        LOGGER.debug("updating ReportSummary: {}", report);
        try {
            reportSummaryRepository.update(report);
        } catch (DBManagerException e) {
            throw new ReportSummaryManagerException(e);
        }
    }

    @Override
    public List<ReportSummary> getReportSummaryList(final ReportSummary clazz) {
        LOGGER.debug("getting Report Summary list");
        try {
            // this should work but there is an issue with the ReportSummary class
            return this.getList();
        } catch (DBManagerException e) {
            throw new ReportSummaryManagerException(e);
        }
    }

    @Override
    public List<ReportSummary> getReportSummaryListByHostname(
            final String hostname) {
        return reportSummaryRepository.getByClientHostname(hostname);
    }

    @Override
    public ReportSummary getReportSummaryByReportID(final UUID id) {
        LOGGER.debug("Getting all Report Summary...");

        if (id != null) {
            for (ReportSummary summary : this.getList()) {
                if (id == summary.getReport().getId()) {
                    return summary;
                }
            }
        }

        return null;
    }

    @Override
    public FilteredRecordsList getOrderedList(Class<ReportSummary> clazz,
                                              String columnToOrder,
                                              boolean ascending,
                                              int firstResult,
                                              int maxResults,
                                              String search,
                                              Map<String, Boolean> searchableColumns)
            throws DBManagerException {
        return null;
    }

    @Override
    public FilteredRecordsList<ReportSummary> getOrderedList(
            Class<ReportSummary> clazz, String columnToOrder,
            boolean ascending, int firstResult, int maxResults,
            String search, Map<String, Boolean> searchableColumns,
            CriteriaModifier criteriaModifier) throws DBManagerException {
        return null;
    }
}
