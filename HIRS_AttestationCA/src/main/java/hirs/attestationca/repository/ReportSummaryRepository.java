package hirs.attestationca.repository;

import hirs.attestationca.entity.ReportSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Setting up for new creation for CRUD operations.
 * @param <T> super type for ReportSummary child type
 */
public interface ReportSummaryRepository<T extends ReportSummary>
        extends JpaRepository<ReportSummary, UUID> {

//    /**
//     * Saves the <code>ReportSummary</code> in the database. This creates a new
//     * database session and saves the report summary.
//     *
//     * @param reportSummary ReportSummary to save
//     * @return reference to saved reportSummary
//     */
//    T save(T reportSummary);

    /**
     * Update the <code>ReportSummary</code> in the database. This creates a new
     * database session and updates the report summary.
     *
     * @param reportSummary ReportSummary to save
     * @return reference to saved reportSummary
     */
    T update(T reportSummary);

    /**
     * Retrieve the <code>ReportSummary</code> in the database. This returns a new
     * database session.
     *
     * @param hostName hostName to look up
     * @return reference to saved reportSummary
     */
    List<T> getByClientHostname(String hostName);
}
