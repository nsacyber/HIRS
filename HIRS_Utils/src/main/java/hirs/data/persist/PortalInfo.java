package hirs.data.persist;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Store information about the Portal into the database.
 */
@Entity
@Table(name = "PortalInfo")
@Access(AccessType.FIELD)
public class PortalInfo {
    /**
     * Schemes used by the HIRS Portal.
     */
    public enum Scheme {
        /**
         * HTTP.
         */
        HTTP,
        /**
         * HTTPS.
         */
        HTTPS;
    }

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

    @Column
    private String context;

    /**
     * Constructor.
     */
    public PortalInfo() {
    }

    /**
     * Sets the scheme name of the portal.
     *
     * @param scheme Name of the portal.
     */
    public final void setSchemeName(final PortalInfo.Scheme scheme) {
        if (scheme == null) {
            throw new NullPointerException("Scheme cannot be null");
        }
        this.name = scheme.name();
    }

    /**
     * Gets the scheme name of the portal.
     *
     * @return Scheme name of the portal.
     */
    public final String getSchemeName() {
        return name;
    }

    /**
     * Stores the address of the portal.
     *
     * @param newip address used by the portal.
     */
    public final void setIpAddress(final InetAddress newip) {
        if (newip == null) {
            throw new IllegalArgumentException("setIpAddress input was null.");
        }

        ipAddress = newip;
    }

    /**
     * Resolves, then stores the address of the portal.
     *
     * @param host host name or address of the portal
     * @throws UnknownHostException For problems resolving or storing the host.
     */
    public final void setIpAddress(final String host) throws UnknownHostException {
        ipAddress = InetAddress.getByName(host);
    }

    /**
     * Gets the IP of the portal.
     *
     * @return InetAddress of the portal.
     */
    public final InetAddress getIpAddress() {
        return ipAddress;
    }

    /**
     * Store the port of the portal.
     *
     * @param newport port of the portal
     */
    public final void setPort(final int newport) {
        final int upperBound = 65535;
        if (0 < newport && newport <= upperBound) {
            port = newport;
        } else {
            throw new IllegalArgumentException("Failed to store portal port.  Provided number was"
                    + " outside of valid range (1 - " + upperBound + ")");
        }
    }

    /**
     * Gets port assigned to the portal.
     *
     * @return the port used by the portal
     */
    public final int getPort() {
        return port;
    }

    /**
     * Sets the context name of the portal.
     *
     * @param context Context name of portal.
     */
    public final void setContextName(final String context) {
        if (context == null) {
            throw new NullPointerException("Context cannot be null");
        }
        this.context = context;
    }

    /**
     * Gets the context assigned to the portal.
     *
     * @return Context name of the portal
     */
    public final String getContextName() {
        return context;
    }
}
