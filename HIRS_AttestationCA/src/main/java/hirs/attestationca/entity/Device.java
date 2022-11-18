package hirs.attestationca.entity;

import hirs.data.persist.AbstractEntity;
import hirs.data.persist.AppraisalStatus;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.enums.HealthStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.foss.XMLCleaner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Date;

/**
 * This class represents a device that is attesting to the appraiser. A
 * <code>Device</code> can be any type of machine. It could be a laptop,
 * desktop, tablet, mobile, tv, etc.
 * <p>
 * A <code>Device</code> contains a few properties. It contains a name and a
 * <code>DeviceInfoReport</code>. The name must uniquely identify the device. No
 * other device may have that name. The naming strategy is up to the deployer.
 * The names could be CPU serial numbers, fully-qualified domain names, UUIDs,
 * or manually assigned names. This is not the complete set but examples.
 * <p>
 * The <code>DeviceInfoReport</code> contains ancillary information on the
 * <code>Device</code> that is reporting. This would include information such as
 * the operating system and networking addresses. This information is useful for
 * determining policy and successfully appraising devices.
 */
@Entity
@Table(name = "Device")
@XmlRootElement(name = "device")
@XmlAccessorType(XmlAccessType.FIELD)
public class Device extends AbstractEntity {

    private static final Logger LOGGER = LogManager.getLogger();

    @Column(name = "name", unique = true)
    @XmlElement(name = "name", required = true)
    private String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
            optional = true, orphanRemoval = true)
    @JsonIgnore
    @XmlElement
    private DeviceInfoReport deviceInfo;

    @XmlTransient
    @Column
    @Enumerated(EnumType.ORDINAL)
    private HealthStatus healthStatus;

    @Column
    @Enumerated(EnumType.ORDINAL)
    private AppraisalStatus.Status supplyChainValidationStatus;

    /**
     * Time stamp for the report.
     */
    @XmlTransient
    @Column(name = "last_report_timestamp")
    private Timestamp lastReportTimestamp;

    @Column(name = "is_state_overridden")
    private boolean isStateOverridden;

    @Column(name = "state_override_reason")
    private String overrideReason;

    @Column(name = "summary_id")
    private String summaryId;

    /**
     * Default constructor required by Hibernate.
     */
    protected Device() {
        healthStatus = HealthStatus.UNKNOWN;
    }

    /**
     * Creates a new <code>Device</code>. The <code>Device</code> is named
     * <code>name</code> and has no other device information associated with it.
     *
     * @param name unique name for this device
     */
    public Device(final String name) {
        this(name, null);
    }

    /**
     * Returns a new <code>Device</code> instance from the XML string. This
     * unmarshals the XML string and generates a <code>Device</code> object.
     * This is a utility method for creating <code>Device</code> objects.
     *
     * @param xml
     *            XML representation of device
     * @return device
     * @throws JAXBException
     *             if unable to unmarshal the string
     */
    public static Device getInstance(final String xml) throws JAXBException {
        final JAXBContext context = JAXBContext.newInstance(Device.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        final StringReader reader = new StringReader(xml);
        return (Device) unmarshaller.unmarshal(reader);
    }

    /**
     * Creates a new <code>Device</code>. The <code>Device</code> is named
     * <code>name</code> and has the properties specified by the device info
     * report.
     *
     * @param name unique name for this device
     * @param deviceInfo information about this device (may be null)
     */
    public Device(final String name, final DeviceInfoReport deviceInfo) {
        setName(name);
        setDeviceInfo(deviceInfo);
        isStateOverridden = false;
        healthStatus = HealthStatus.UNKNOWN;
        supplyChainValidationStatus = AppraisalStatus.Status.UNKNOWN;
    }

    /**
     * Returns the unique name for this device.
     *
     * @return unique device name
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the unique name for this device.
     *
     * @param name unique device name
     */
    public final void setName(final String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
    }

    /**
     * Returns a report with information about this device. This may return null
     * if this property has not been set.
     *
     * @return device info report
     */
    public final DeviceInfoReport getDeviceInfo() {
        return deviceInfo;
    }

    /**
     * Sets the information about this device from the report.
     *
     * @param deviceInfo device info report (may be null)
     */
    public final void setDeviceInfo(final DeviceInfoReport deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    /**
     * Returns an XML string representation of this <code>Device</code>.
     *
     * @return XML representation of this <code>Device</code>
     */
    public final String toXML() {
        try {
            final JAXBContext context = JAXBContext.newInstance(Device.class);
            final Marshaller marshaller = context.createMarshaller();
            final StringWriter writer = new StringWriter();
            marshaller.marshal(this, writer);
            final String xml = writer.toString();
            return XMLCleaner.stripNonValidXMLCharacters(xml);
        } catch (JAXBException e) {
            final String msg = "error while marshalling device object";
            LOGGER.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }


    /**
     * Gets the timestamp of the Report in the summary.
     *
     * @return TimeStamp
     */
    public final Timestamp getLastReportTimestamp() {
        if (lastReportTimestamp != null) {
            return new Timestamp(lastReportTimestamp.getTime());
        }

        return null;
    }

    /**
     * Sets the time stamp for the summary.
     *
     * @param timestamp
     *            timestamp for the summary
     */
    public void setLastReportTimestamp(final Timestamp timestamp) {
        this.lastReportTimestamp = new Timestamp(timestamp.getTime());
    }

    /**
     * Clear the override by setting isStateOverridden flag back to false and overwriting the reason
     * given by the user.
     */
    public final void clearOverride() {
        this.isStateOverridden = false;
        this.overrideReason = String.format("Device state override cleared at %s.",
                new Date().toString());
    }

    /**
     * Override the device state of this device and give a reason for doing so.
     *
     * @param newOverrideReason the reason stated by the user for the override
     */
    public final void overrideState(final String newOverrideReason) {
        isStateOverridden = true;
        overrideReason = newOverrideReason;
    }

    /**
     * Return the user-given reason why the state is overridden.
     *
     * @return the reason as a String
     */
    public final String getOverrideReason() {
        return overrideReason;
    }

    /**
     * Determine whether or not this <code>Device</code> is currently set to have its device state
     * overridden.
     *
     * @return true if the state is currently overridden, false if not
     */
    public boolean isStateOverridden() {
        return this.isStateOverridden;
    }

    /**
     * Returns the HealthStatus for the device.
     *
     * @return HealthStatus
     */
    public HealthStatus getHealthStatus() {
        return this.healthStatus;
    }

    /**
     * Sets the health status for the device. Cannot be null.
     *
     * @param healthStatus
     *             health status of the device
     */
    public void setHealthStatus(final HealthStatus healthStatus) {
        if (healthStatus == null) {
            throw new NullPointerException("health status");
        }
        this.healthStatus = healthStatus;
    }

    /**
     * Returns the supply chain appraisal status for the device.
     * @return the supply chain appraisal status for the device
     */
    public AppraisalStatus.Status getSupplyChainStatus() {
        return supplyChainValidationStatus;
    }

    /**
     * Sets the supply chain appraisal status for the device.
     * @param supplyChainValidationStatus the supply chain appraisal status for the device
     */
    public void setSupplyChainStatus(final AppraisalStatus.Status supplyChainValidationStatus) {
        if (supplyChainValidationStatus == null) {
            throw new NullPointerException(" supply chain validation status");
        }
        this.supplyChainValidationStatus = supplyChainValidationStatus;
    }

    /**
     * Getter for the last summary id.
     * @return UUID for the summary
     */
    public String getSummaryId() {
        return summaryId;
    }

    /**
     * Setter for the last summary id.
     * @param summaryId UUID
     */
    public void setSummaryId(final String summaryId) {
        this.summaryId = summaryId;
    }

    /**
     * Returns a hash code for this <code>Device</code>. The hash code is
     * determined from the name of the <code>Device</code>.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        return result;
    }

    /**
     * Tests whether <code>this</code> is equal to <code>obj</code>. This
     * returns true if and only if the class of obj is equal to this class and
     * the names are equal.
     *
     * @param obj other object to compare against
     * @return equality
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Device other = (Device) obj;
        return this.name.equals(other.name);
    }

    @Override
    public String toString() {
        return String.format("Device{name=%s, status=%s}",
                name, supplyChainValidationStatus);
    }
}
