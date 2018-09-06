package hirs.data.persist.alert;

/**
 * The Alert Monitor holds configuration information about an individual remote monitor (a remote
 * "Alert Manager") that is subscribing (monitoring) the HIRS alert service.
 */
import javax.persistence.Access;
import javax.persistence.AccessType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * Database Object representing the configuration of a remote Alert Monitor. The Alert Monitor
 * configuration holds information about an individual remote monitor (a remote "Alert Manager")
 * that is subscribing (monitoring) the HIRS alert service. The configuration allows for one alert
 * to be sent for every event or one alert per report processed that encountered at least one event.
 */
@Entity
@Table(name = "AlertMonitor")
@Inheritance(strategy = InheritanceType.JOINED)
@Access(AccessType.FIELD)
public abstract class AlertMonitor {

    /**
     * ID assigned to the configuration.
     */
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column
    private InetAddress ipAddress;

    @Column
    private int port = 0;

    @Column(name = "alertOnSummary")
    private boolean alertOnSummaryEnabled = false;

    @Column(name = "alertOnEvent")
    private boolean individualAlertEnabled = false;

    @Column
    private String alertServiceType;

    @Column
    private boolean monitorEnabled;

    /**
     * Default constructor. Needed for Hibernate unit tests.
     */
    protected AlertMonitor() {
        // Do nothing
    }

    /**
     * Creates an Alert Monitor.
     *
     * @param monitorName name assigned to this monitor.
     */
    public AlertMonitor(final String monitorName) {

        if (monitorName == null) {
            throw new NullPointerException("AlertMonitor name not set");
        }
        name = monitorName;
    }

    /**
     * Returns the persisted ID of the monitor.
     *
     * @return persistence ID of the monitor
     */
    public final Long getId() {
        return id;
    }

    /**
     * Sets the FQDN of of the Alert Monitor.
     *
     * @param type Type of the AlertService
     */
    public void setAlertServiceType(final String type) {
        if (type == null) {
            throw new NullPointerException("AlertMonitor type not set");
        }
        alertServiceType = type;
    }

    /**
     * Returns the Type of the AlertService associated with the monitor.
     *
     * @return Type of the AlertService this monitor is associated with
     */
    public final String getAlertServiceType() {
        return alertServiceType;
    }

    /**
     * Sets the name of the remote Manager.
     *
     * @param newName Name of the remote manager
     */
    public final void setName(final String newName) {
        if (newName == null) {
            throw new NullPointerException("AlertMonitor name cannot be null");
        }
        name = newName;
    }

    /**
     * Gets the name assigned to the remote alert manager.
     *
     * @return Name of the remote alert manager
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the address of the remote alert manager.
     *
     * @param newip address assigned to the remote manager
     */
    public final void setIpAddress(final InetAddress newip) {
        ipAddress = newip;
    }

    /**
     * Resolves, then sets the address of the remote alert manager.
     *
     * @param host address assigned to the remote manager
     * @throws UnknownHostException For problems resolving or storing the host.
     */
    public final void setIpAddress(final String host) throws UnknownHostException {
        ipAddress = InetAddress.getByName(host);
    }

    /**
     * Gets the IP assigned to the remote manager.
     *
     * @return IP of the remote manager
     */
    public final InetAddress getIpAddress() {
        return ipAddress;
    }

    /**
     * Assign port of the remote alert manager.
     *
     * @param newport port assigned to the remote manager
     */
    public final void setPort(final int newport) {
        final int upperBound = 65535;
        if (0 < newport && newport <= upperBound) {
            port = newport;
        } else {
            throw new IllegalArgumentException("Failed to set monitor port.  Provided number was"
                    + " outside of valid range (1 - 65535)");
        }
    }

    /**
     * Gets port assigned to the remote manager.
     *
     * @return the port used by the remote manager
     */
    public final int getPort() {
        return port;
    }

    /**
     * Returns the summary Alert enable setting.
     * If true the Alert Manager will send one summary message per message that incurred an alert.
     *
     * @return true if Alert Monitor will send an alert upon an Alert Summary
     */
    public final boolean isAlertOnSummaryEnabled() {
        return this.alertOnSummaryEnabled;
    }

    /**
     * Enables the Remote Alert Manager to send a summary of alerts per report.
     * This does not effect the enableIndividualAlert method.
     */
    public final void enableAlertOnSummary() {
        alertOnSummaryEnabled = true;
    }

    /**
     * Disables the Remote Alert Manager to send a summary of alerts per report.
     * This does not effect the disableIndividualAlert method.
     */
    public final void disableAlertOnSummary() {
        alertOnSummaryEnabled = false;
    }

    /**
     * Returns the individual Alert enable setting.
     * If true the Alert Manager will send the remote alert manager one message per alert.
     *
     * @return Enable setting for the Remote Alert monitor
     */
    public final boolean isIndividualAlertEnabled() {
        return individualAlertEnabled;
    }

    /**
     * Enables the Remote Alert Manager to send individual alerts.
     * This could lead to hundreds of alerts per issued per report.
     */
    public final void enableIndividualAlert() {
        individualAlertEnabled = true;
    }

    /**
     * Disables the Remote Alert Manager to send individual alerts.
     */
    public final void disableIndividualAlert() {
        individualAlertEnabled = false;
    }

    /**
     * Enables AlertMonitor.  This needs to be called in order for an Alert to be sent to the
     * provided monitor.
     */
    public final void enable() {
        monitorEnabled = true;
    }

    /**
     * Disables AlertMonitor.  This will prevent Alerts from being forwarded to the supplied
     * monitor.
     */
    public final void disable() {
        monitorEnabled = false;
    }

    /**
     * Returns the enabled/disabled status of the monitor.
     *
     * @return the status of the monitor.  True if enabled, false if disabled.
     */
    public final boolean isMonitorEnabled() {
        return monitorEnabled;
    }

    /**
     * Returns a boolean if other is equal to this. <code>AlertMonitor</code>s are identified
     * by their name, so this returns true if <code>other</code> is an instance of
     * <code>AlertMonitor</code> and its name is the same as this <code>AlertMonitor</code>.
     * Otherwise this returns false.
     *
     * @param other other object to test for equals
     * @return true if other is <code>AlertMonitor</code> and has same name
     */
    @Override
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AlertMonitor)) {
            return false;
        }

        final AlertMonitor config = (AlertMonitor) other;
        return this.getName().equals(config.getName());
    }

    /**
     * Returns a hash code for this <code>AlertMonitor</code>. <code>AlertMonitor</code>
     * are identified by their name, so the returned hash is the hash of the name.
     *
     * @return hash
     */
    @Override
    public final int hashCode() {
        return name.hashCode();
    }

    /**
     * Returns a <code>String</code> representation of this class. This returns the
     * <code>AlertMonitor</code> name.
     *
     * @return <code>AlertMonitor</code> name
     */
    @Override
    public final String toString() {
        return this.name;
    }

}
