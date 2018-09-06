package hirs.data.persist.alert;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The AlertServiceConfig holds persistent configuration data for the
 * ManagedAlertService.
 */
@Entity
@Table(name = "AlertServiceConfig")
public class AlertServiceConfig {

    @Id
    @Column(name = "id", unique = true)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "enabled")
    private boolean enabled = false;

    @Column
    private String serviceIdentifier;

    @Column(unique = true, nullable = false)
    private String type;

    /**
     * Default constructor.
     * Needed for Hibernate unit tests.
     */
    protected AlertServiceConfig() {
        // Do Nothing
    }

    /**
     * Creates a new <code>AlertServiceConfig</code> that uses the default
     * database. The default database is used to store all of the objects.
     *
     * @param serviceType type assigned to this alert service.
     */
    public AlertServiceConfig(final String serviceType) {
        if (serviceType == null) {
            throw new NullPointerException("AlertServiceConfig name not set");
        }
        type = serviceType;
    }

    /**
     * Enables the alert service.
     */
    public final void enable() {
        enabled = true;
    }

    /**
     * Disables the alert service.
     */
    public final void disable() {
        enabled = false;
    }

    /**
     * Gets enabled status (Alert Service state).
     *
     * @return True if the service is enabled, false if disabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the type of the AlertService.
     *
     * @return Type of the service
     */
    public final String getType() {
        return type;
    }

    /**
     * Gets the ID of the Alert Service.
     *
     * @return ID of the service
     */
    public final String getServiceIdentifier() {
        return serviceIdentifier;
    }

    /**
     * Set the Identifier of the service.
     *
     * @param id
     *            Identifier to assign
     */
    public final void setServiceIdentifier(final String id) {
        serviceIdentifier = id;
    }

    /**
     * Returns a hash code for this <code>Device</code>. The hash code is
     * determined from the name of the <code>Device</code>.
     *
     * @return hash code
     */
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + type.hashCode();
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
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AlertServiceConfig)) {
            return false;
        }
        AlertServiceConfig other = (AlertServiceConfig) obj;
        return this.type.equals(other.type);
    }

    /**
     * Returns a <code>String</code> representation of this class. This returns
     * the <code>AlertMonitor</code> name.
     *
     * @return <code>Policy</code> name
     */
    @Override
    public final String toString() {
        return type.toString();
    }
}
