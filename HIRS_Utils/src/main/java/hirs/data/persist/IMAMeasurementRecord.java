package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

/**
 * This class represents an IMA measurement record. A measurement record
 * contains a file path and a hash. The file path represents the file name and
 * the hash is the hash of the file.
 * <p>
 * In IMA the file path is not guaranteed to be unique. For instance initrd has
 * files measured at /. The root file space that is mounted later is also
 * mounted at /.
 */
@XmlSeeAlso(Digest.class)
@Entity
@XmlAccessorType(XmlAccessType.FIELD)
public class IMAMeasurementRecord extends ExaminableRecord {

    private static final Logger LOGGER = LogManager.getLogger(IMAMeasurementRecord.class);

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @XmlElement
    @Column(nullable = false)
    private final String path;

    @XmlElement
    @Embedded
    private final Digest hash;

    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "ima_report_id")
    @JsonIgnore
    private IMAReport report;

    /**
     * Creates a new <code>IMAMeasurementRecord</code>. The record contains the
     * file path and its hash.
     *
     * @param path
     *            the file path that identifies the file location
     * @param hash
     *            SHA-1 hash of the file
     * @throws IllegalArgumentException
     *             if digest algorithm is not SHA-1
     */
    public IMAMeasurementRecord(final String path, final Digest hash)
            throws IllegalArgumentException {
        super();
        checkNullArg(path, "path");
        checkNullArg(hash, "hash");
        if (hash.getAlgorithm() != DigestAlgorithm.SHA1) {
            throw new IllegalArgumentException("digest algorithm is not SHA-1");
        }
        this.path = path;
        this.hash = hash;
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected IMAMeasurementRecord() {
        super();
        this.path = null;
        this.hash = null;
    }

    /**
     * Returns the ID of the IMAMeasurementRecord.
     *
     * @return id of IMAMeasurementRecord
     */
    public final Long getId() {
        return id;
    }
    /**
     * Returns the path (including file name) of the IMA baseline record.
     *
     * @return file path of baseline record
     */
    public final String getPath() {
        return this.path;
    }

    /**
     * Returns the SHA1 hash of the file associated with IMA baseline record.
     *
     * @return hash of file associated with baseline record
     */
    public final Digest getHash() {
        return this.hash;
    }

    /**
     * This gets the report.
     *
     * @return Report
     */
    public final IMAReport getReport() {
        return report;
    }

    /**
     * Sets the given report.
     *
     * @param report report that matches the given record
     */
    public final void setReport(final IMAReport report) {
        this.report = report;
    }

    /**
     * Overrides hashCode() method in order to generate a new hashCode based on
     * hashCode of path and hash. This is required because of override of
     * equals() method.
     *
     * @return generated hash code
     */
    @Override
    public final int hashCode() {
        if (id == null) {
            return super.hashCode();
        }

        return id.hashCode();
    }

    /**
     * Returns a boolean if other is equal to this.
     * <code>IMAMeasurementRecord</code>s are identified by their name and hash,
     * so this returns true if <code>other</code> is an instance of
     * <code>IMAMeasurementRecord</code> and its name and hash are the same as
     * this <code>IMAMeasurementRecord</code>. Otherwise this returns false.
     *
     * @param obj
     *            other object to test for equals
     * @return true if other is <code>IMAMeasurementRecord</code> and has same
     *         name and same hash
     */
    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof IMAMeasurementRecord)) {
            return false;
        }
        IMAMeasurementRecord other = (IMAMeasurementRecord) obj;

        if (other.id == null || id == null) {
            return super.equals(other);
        }
        return other.id.equals(id);
    }

    @Override
    public final String toString() {
        return String.format("(%s, %s)", path, hash);
    }

    private void checkNullArg(final Object arg, final String argName) {
        if (arg == null) {
            final String msg = String.format("null argument: %s", argName);
            LOGGER.error(msg);
            throw new NullPointerException(msg);
        }
    }
}
