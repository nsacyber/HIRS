package hirs.data.persist;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * DTO representing a Report. Reports are the basic DTOs exchanged between the
 * HIRS client machines and the HIRS server machines.
 *
 * Reports are partially persisted in HIRS server database as rows in the
 * 'Report' table. Only the values necessary for locating particular reports for
 * processing or presentation are saved in the database; the remainder of the
 * Report's contents are saved on disk as a file. The expected use case of the
 * Report records in the database is:
 *   - Select one or more Report objects from
 * the database based on some search criteria.
 *   - From the returned Report
 * objects, obtain the IDs of the reports in question. - Use these IDs to access
 * the complete report from disk.
 *
 * Reports have an XML representation, generated via JAXB
 *
 *
 */
@Entity
@Table(name = "ReportMapper")
@XmlRootElement(name = "report")
public class ReportMapper {

    /*------------------- HIBERNATE-MAPPED PROPERTIES -----------------------*/
    /**
     * The unique ID of the Report, if one exists. Reports are assigned their ID
     * when they are first inserted into the HIRS database. Newly-created
     * Reports have a 'null' ID, indicating that they have not yet been
     * persisted. An important use case for this is when a Report DTO is
     * deserialized as it is submitted by a HIRS client: in this case, no ID
     * will have been reported by the client (since the client has no idea of
     * the existence of the server database)
     *
     * In XML representation, a Report's id is represented by its 'id'
     * attribute.
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @XmlAttribute(name = "id", required = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * The processing state of this report. Reports have a very simple
     * lifecycle: - NEW: The report is newly generated - INIT: The report has
     * had its nonce value assigned and returned to the client; the appraiser is
     * waiting for the client to return the completed report. - DONE: The report
     * has been received, saved in the database, and processed.
     *
     * Additional states may be added as the system evolves.
     */
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private State state;

    @XmlType(name = "ReportState")
    public enum State {

        NEW, INIT, DONE
    };

    @XmlAttribute(name = "state")
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /**
     * The Client DTO associated with this Report. Each Report is (optionally)
     * associated with a Client object, which encapsulates business information
     * about the client (IP number, OS version, etc.) The Client objects are
     * persisted in their own database table and have a many-to-one relationship
     * with their Reports: many Reports can be associated with a single Client
     * in the database.
     *
     * In XML representation, a Report's associated Client is represented by a
     * child <client> element.
     */
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Device client;

    @XmlElement(name = "client", required = false)
    public Device getClient() {
        return client;
    }

    public void setClient(Device client) {
        this.client = client;
    }

    @Column(name = "timestamp")
    private Timestamp timestamp;

    @XmlElement(name = "timestamp")
    public Timestamp getTimestamp() {
        return new Timestamp(timestamp.getTime());
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = new Timestamp(timestamp.getTime());
    }

    /**
     * The nonce value of the Report, used to prevent replays. The intended use
     * case is: - A client contacts the appraiser to begin the report submission
     * process by calling the 'initReport()' method. - The appraiser creates a
     * new Report object, assigns it a nonce value, and saves the Report in the
     * database - The appraiser responds back to the client, including the nonce
     * value.
     *
     * Later, when the Client has finished generating the report, it submits it
     * back to the appraiser including the nonce value: - The client sends the
     * report to the appraiser. - The appraiser extracts out the nonce value of
     * the report, and uses it to locate the corresponding Report in the
     * database. - Once the Report has been located, the appraiser fills out its
     * details from the report that the client has submitted.
     */
    @Column(name = "nonce")
    private byte[] nonce;

    @XmlElement(name = "nonce")
    public byte[] getNonce() {
        return nonce.clone();
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce.clone();
    }

}
