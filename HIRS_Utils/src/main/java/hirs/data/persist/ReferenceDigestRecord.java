package hirs.data.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * This class will represent an entry a table that'll be associated
 * with the manufacturer and model with all digest values,
 * Event Type, index, RIM Tagid.
 */
@Entity
@Table(name = "ReferenceDigestRecord")
public class ReferenceDigestRecord extends ArchivableEntity {

    @Column(nullable = false)
    private String manufacturer;
    @Column(nullable = false)
    private String model;
    @Column(columnDefinition = "blob", nullable = false)
    private byte[] valueBlob;

    // NOTE: when this works, and do a show tables to give to Lawrence
    private List<ReferenceDigestValue> associatedDigests = new ArrayList<>();

    /**
     * Default Constructor.
     */
    protected ReferenceDigestRecord() {
        super();
        this.manufacturer = null;
        this.model = null;
        this.valueBlob = null;
    }

    /**
     * Default constructor with parameters.
     * @param manufacturer device manufacturer
     * @param model device model
     * @param valueBlob the data values of the event.
     */
    public ReferenceDigestRecord(final String manufacturer,
                                 final String model,
                                 final byte[] valueBlob) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.valueBlob = valueBlob.clone();
    }

    /**
     * Getter for the manufacturer associated.
     * @return the string of the manufacturer
     */
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * Setter for the manufacturer associated.
     * @param manufacturer the string of the manufacturer
     */
    public void setManufacturer(final String manufacturer) {
        this.manufacturer = manufacturer;
    }

    /**
     * Getter for the model associated.
     * @return the string of the model
     */
    public String getModel() {
        return model;
    }

    /**
     * Setter for the model associated.
     * @param model the string of the model
     */
    public void setModel(final String model) {
        this.model = model;
    }

    /**
     * Getter for the byte array of event values.
     * @return a clone of the byte array
     */
    public byte[] getValueBlob() {
        return valueBlob.clone();
    }

    /**
     * Setter for the byte array of values.
     * @param valueBlob non-null array.
     */
    public void setValueBlob(final byte[] valueBlob) {
        if (valueBlob != null) {
            this.valueBlob = valueBlob.clone();
        }
    }
}
