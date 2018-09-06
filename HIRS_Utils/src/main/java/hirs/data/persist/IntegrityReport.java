package hirs.data.persist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * The IntegrityReport represents the DTO used to transfer the results of
 * appraisals back to the server. The IntegrityReport holds a list of different
 * reports, such as a DeviceInfoReport or IMAReport, that are added after
 * initialization.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
@Entity
@Access(AccessType.FIELD)
public class IntegrityReport extends Report {

    private static Logger LOGGER = LogManager.getLogger(IntegrityReport.class);

    @XmlElement
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinTable(
            name = "IntegrityReports_Reports_Join",
            joinColumns = @JoinColumn(name = "integrity_report_id"),
            inverseJoinColumns = @JoinColumn(name = "report_id")
    )
    private Set<Report> reports;

    public IntegrityReport() {
        reports = new HashSet<>();
    }

    /**
     * Add a report (e.g., DeviceInfoReport, IMAReport) to this IntegrityReport.
     *
     * @param report
     *            Report object to be added to the IntegrityReport
     */
    public final void addReport(final Report report) {
        if (report == null) {
            LOGGER.error("report cannot be null");
            throw new NullPointerException("report");
        }
        if (reports.add(report)) {
            LOGGER.debug("Report {} successfully added", report);
        } else {
            LOGGER.debug("Report {} already exists in the IntegrityReport",
                    report);
        }
    }

    /**
     * Checks to see if this integrity report contains a report of the specified type.
     *
     * @param reportType to check
     * @return a boolean indicating if this report contains the specified report.
     */
    public final boolean contains(Class<? extends Report> reportType) {
        for (Report report : reports) {
            if (reportType.isAssignableFrom(report.getClass())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves the set of Reports included in this IntegrityReport.
     *
     * @return a Set of Report objects
     */
    public final Set<Report> getReports() {
        return Collections.unmodifiableSet(reports);
    }

    /**
     * Removes a report from the IntegrityReport.
     *
     * @param report
     *            Report to be removed
     */
    public final void removeReport(final Report report) {
        if (report == null) {
            LOGGER.error("report cannot be null");
            throw new NullPointerException("report");
        }

        if (reports.contains(report)) {
            if (reports.remove(report)) {
                LOGGER.info("report {} successfully removed", report);
            } else {
                LOGGER.info("unable to remove report {} from IntegrityReport",
                        report);
            }
        } else {
            LOGGER.info("report {} not found in IntegrityReport", report);
        }
    }

    /**
     * Extract a report of the given type from the IntegrityReport.
     *
     * @param <T> The class of the report to return
     * @param reportType Report type to extract
     * @return the requested report or null if it is not found
     */
    public final <T extends Report> T extractReport(final Class<T> reportType) {
        final Set<Report> reports = this.getReports();
        for (Report report : reports) {
            if (reportType.isInstance(report)) {
                return reportType.cast(report);
            }
        }
        throw new IllegalArgumentException(
                String.format("This %s does not contain a %s. Before calling extractReport(), " +
                                "call contains() to make sure the reportType exists."
                        , getClass().getName(), reportType.getName()));
    }    

    /**
     * This method is used to extract the name of the device from the report.
     * Currently, the name is the hostname, but this method could be changed
     * to use another value, such as a serial number. This method returns null
     * if the report does not contain a <code>DeviceInfoReport</code>.
     *
     * @return the identifier of the report, null if hostname not found
     */
    public String getDeviceName() {

        if (!contains(DeviceInfoReport.class)) {
            LOGGER.debug("integrity report does not contain a device info "
                    + "report, cannot derive device name");
            return null;
        }

        DeviceInfoReport deviceInfoReport = extractReport(DeviceInfoReport.class);
        String name = deviceInfoReport.getNetworkInfo().getHostname();
        if (name == null) {
            LOGGER.debug("hostname was not included in the device info report");
        }
        return name;
    }

    @Override
    public final String getReportType() {
        return this.getClass().getName();
    }
}
