package hirs.data.persist;

import hirs.data.persist.type.ReportRequestType;
import org.apache.commons.lang3.ArrayUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import java.util.Date;

/**
 * Whenever the server issues a {@link hirs.ReportRequest}  for a {@link Device}, the server
 * will create a <code>ReportRequestState</code>. This state will be used track if the given
 * <code>Device</code> has fulfilled reporting requirements in a timely manner. A client is late in
 * reporting if a report has not been received within the {@link DeviceGroup} threshold. This state
 * includes the necessary information for the server to generate alerts when a client fails to
 * report.
 */
@Entity
public class ReportRequestState extends State {

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    private Device device;

    @Enumerated(value = EnumType.STRING)
    private ReportRequestType reportRequestType;

    @Column
    private byte[] nonce;

    @Column(nullable = false)
    private Date dueDate;

    /**
     * Access the Device that this ReportRequestState belongs to.
     *
     * @return the associated Device
     */
    public final Device getDevice() {
        return device;
    }

    /**
     * Sets the Device that this ReportRequestState belongs to.
     *
     * @param device cannot be null
     */
    public void setDevice(final Device device) {
        this.device = device;
    }

    /**
     * The {@link ReportRequestType} of this state.
     *
     * @return of type of request
     */
    public ReportRequestType getReportRequestType() {
        return reportRequestType;
    }

    /**
     * Sets the type of this report request.
     *
     * @param reportRequestType cannot be null.
     */
    public void setReportRequestType(final ReportRequestType reportRequestType) {
        this.reportRequestType = reportRequestType;
    }

    /**
     * When a {@link Report} is due for to fulfill this request.
     *
     * @return when a report is due for this request
     */
    public Date getDueDate() {
        return new Date(dueDate.getTime());
    }

    /**
     * Sets when a {@link Report} is due to fulfill this request.
     *
     * @param dueDate cannot be null
     */
    public void setDueDate(final Date dueDate) {
        this.dueDate = new Date(dueDate.getTime());
    }

    /**
     * Sets when a {@link Report} is due to fulfill this request by computing the current
     * time and adding the delayThreshold to the current time.
     *
     * @param delayThreshold length of time to wait before report is due. Cannot be null
     */
    public void setDueDate(final long delayThreshold) {
        long currentTimeSeconds = new Date().getTime();
        long dueDateSeconds = currentTimeSeconds + delayThreshold;
        this.dueDate = new Date(dueDateSeconds);
    }

    /**
     * The nonce, if one was specified, in a report request issued to a client. This field shall be
     * used by a {@link hirs.appraiser.TPMAppraiser} to ensure that a {@link TPMReport} nonce
     * matches the originally requested nonce.
     *
     * @return of the report request
     */
    public byte[] getNonce() {
        return ArrayUtils.clone(nonce);
    }

    /**
     * Sets the nonce that was issued in a report request to the client.
     *
     * @param nonce can be null.
     */
    public void setNonce(final byte[] nonce) {
        this.nonce = ArrayUtils.clone(nonce);
    }

    @Override
    public String toString() {
        return "ReportRequestState{"
                + "device=" + device
                + ", reportRequestType=" + reportRequestType
                + ", dueDate=" + dueDate
                + '}';
    }
}
