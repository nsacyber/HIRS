package hirs.attestationca.entity;

import hirs.data.persist.ArchivableEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * This class will represent an entry a table that'll be associated
 * with the manufacturer and model with all digest values,
 * Event Type, index, RIM TagId.
 */
@Entity
@Table(name = "ReferenceDigestRecord")
public class ReferenceDigestRecord extends ArchivableEntity {

    private static final Logger LOGGER = LogManager.getLogger(ReferenceDigestRecord.class);

    @Type(type = "uuid-char")
    @Column
    private UUID supportRim;
    @Column(nullable = false)
    private String manufacturer;
    @Column(nullable = false)
    private String model;
    @Column(nullable = false)
    private String deviceName;
    @Column(columnDefinition = "mediumblob", nullable = true)
    private byte[] valueBlob;

    /**
     * Default Constructor.
     */
    protected ReferenceDigestRecord() {
        super();
        this.supportRim = UUID.randomUUID();
        this.manufacturer = "";
        this.model = "";
        this.valueBlob = null;
    }

    /**
     * Default constructor with parameters.
     * @param supportRim link to the source of data
     * @param manufacturer device manufacturer
     * @param model device model
     * @param valueBlob the data values of the event.
     */
    public ReferenceDigestRecord(final UUID supportRim,
                                 final String manufacturer,
                                 final String model,
                                 final byte[] valueBlob) {
        super();
        // need to put up nullable entries
        this.supportRim = supportRim;
        this.manufacturer = manufacturer;
        this.model = model;
        this.valueBlob = Arrays.clone(valueBlob);
    }

    /**
     * Default constructor with parameters specific to a RIM object.
     * @param referenceManifest rim object to use.
     * @param manufacturer device manufacturer
     * @param model device model
     */
    public ReferenceDigestRecord(final ReferenceManifest referenceManifest,
                                 final String manufacturer,
                                 final String model) {
        super();
        if (referenceManifest instanceof SupportReferenceManifest) {
            this.supportRim = referenceManifest.getId();
            SupportReferenceManifest srm = (SupportReferenceManifest) referenceManifest;
            this.valueBlob = Arrays.clone(srm.getRimBytes());
        } else if (referenceManifest != null) {
            // the assumption is that there is a support RIM.
            this.supportRim = referenceManifest.getAssociatedRim();
            // I will just test for loaded and if true but blob is empty, pull
            // that information later and update the object
        }

        this.manufacturer = manufacturer;
        this.model = model;
    }

    /**
     * Getter for the linked source for the data.
     * @return UUID of the source
     */
    public UUID getSupportRim() {
        return supportRim;
    }

    /**
     * Setter for the linked source for the data.
     * @param supportRim UUID of the source
     */
    public void setSupportRim(final UUID supportRim) {
        this.supportRim = supportRim;
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
     * Getter for the deviceName associated.
     * @return the string of the deviceName
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Setter for the deviceName associated.
     * @param deviceName the string of the model
     */
    public void setDeviceName(final String deviceName) {
        this.deviceName = deviceName;
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

    /**
     * The string value representative of this class.
     * @return  manufacturer and model for this record
     */
    @Override
    public String toString() {
        return String.format("ReferenceDigestRecord: %s%n%s::%s::%s",
                super.toString(), this.manufacturer, this.model, this.deviceName);
    }
}
