package hirs.data.persist.alert;

import hirs.alert.JsonAlertService;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * This configuration extends the generic AlertMonitor for JSON.
 */
@Entity
@Table(name = "JsonAlertMonitor")
@Access(AccessType.FIELD)
public class JsonAlertMonitor extends AlertMonitor {
    /**
     * The protocol used to send the JSON Alerts.
     */
    public enum JsonAlertMode {
        /**
         * via TCP.
         */
        TCP,
        /**
         * via UDP.
         */
        UDP;
    }

    @Column
    private JsonAlertMode mode;

    /**
     * Constructor.
     */
    public JsonAlertMonitor() {
        super();
    }

    /**
     * Constructor.
     *
     * @param name - name of the monitor
     */
    public JsonAlertMonitor(final String name) {
        super(name);
        setAlertServiceType(JsonAlertService.NAME);
        setTCP();
        disable();
    }

    /**
     * Returns the <code>JsonAlertMode</code> of this <code>JsonAlertMonitor</code>.
     * @return JsonAlertMode
     */
    @Enumerated(EnumType.ORDINAL)
    public JsonAlertMode getJsonAlertMode() {
        return this.mode;
    }

    /**
     * Returns true if this <code>JsonAlertMonitor</code> is configured to forward alerts via TCP.
     * @return boolean
     */
    public boolean isTCP() {
        return this.mode == JsonAlertMode.TCP;
    }

    /**
     * Set the mode to TCP.
     */
    public void setTCP() {
        this.mode = JsonAlertMode.TCP;
    }

    /**
     * Returns true if this <code>JsonAlertMonitor</code> is configured to forward alerts via UDP.
     * @return boolean
     */
    public boolean isUDP() {
        return this.mode == JsonAlertMode.UDP;
    }

    /**
     * Set the mode to UDP.
     */
    public void setUDP() {
        this.mode = JsonAlertMode.UDP;
    }
}
