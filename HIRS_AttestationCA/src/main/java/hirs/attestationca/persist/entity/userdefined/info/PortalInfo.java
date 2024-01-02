package hirs.attestationca.persist.entity.userdefined.info;

import hirs.utils.enums.PortalScheme;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Store information about the Portal into the database.
 */
@NoArgsConstructor
@Entity
@Table(name = "PortalInfo")
@Access(AccessType.FIELD)
public class PortalInfo {

    @Id
    @Getter
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Getter
    @Column(unique = true, nullable = false)
    private String name;
    @Column
    private InetAddress ipAddress;
    @Getter
    @Column
    private int port = 0;
    @Getter
    @Column
    private String context;

    /**
     * Sets the scheme name of the portal.
     *
     * @param scheme Name of the portal.
     */
    public void setSchemeName(final PortalScheme scheme) {
        if (scheme == null) {
            throw new NullPointerException("Scheme cannot be null");
        }
        this.name = scheme.name();
    }

    /**
     * Stores the address of the portal.
     *
     * @param newip address used by the portal.
     */
    public void setIpAddress(final InetAddress newip) {
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
    public void setIpAddress(final String host) throws UnknownHostException {
        ipAddress = InetAddress.getByName(host);
    }

    /**
     * Store the port of the portal.
     *
     * @param newport port of the portal
     */
    public void setPort(final int newport) {
        final int upperBound = 65535;
        if (newport > 0 && newport <= upperBound) {
            port = newport;
        } else {
            throw new IllegalArgumentException("Failed to store portal port.  Provided number was"
                    + " outside of valid range (1 - " + upperBound + ")");
        }
    }

    /**
     * Sets the context name of the portal.
     *
     * @param context Context name of portal.
     */
    public void setContextName(final String context) {
        if (context == null) {
            throw new NullPointerException("Context cannot be null");
        }
        this.context = context;
    }
}
