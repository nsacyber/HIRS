package hirs.data.persist;

import hirs.utils.StringValidator;
import java.io.Serializable;
import javax.persistence.Column;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 */
public class RimInfo implements Serializable {

    @XmlElement
    @Column(length = DeviceInfoReport.MED_STRING_LENGTH, nullable = false)
    private final String manufacturer;

    @XmlElement
    @Column(length = DeviceInfoReport.MED_STRING_LENGTH, nullable = false)
    private final String model;

    @XmlElement
    @Column(length = DeviceInfoReport.MED_STRING_LENGTH, nullable = false)
    private final String fileHash;

    @XmlElement
    @Column(length = DeviceInfoReport.MED_STRING_LENGTH, nullable = false)
    private final String pcrHash;

    /**
     * Constructor for the initial values of the class.
     * @param manufacturer string of the manufacturer
     * @param model string of the model
     * @param fileHash string of the file hash
     * @param pcrHash string of the pcr hash
     */
    public RimInfo(final String manufacturer, final String model,
            final String fileHash, final String pcrHash) {
        this.manufacturer = StringValidator.check(manufacturer, "manufacturer")
                .notBlank().maxLength(DeviceInfoReport.MED_STRING_LENGTH).get();
        this.model = StringValidator.check(model, "model")
                .notBlank().maxLength(DeviceInfoReport.MED_STRING_LENGTH).get();
        this.fileHash = StringValidator.check(fileHash, "fileHash")
                .notBlank().maxLength(DeviceInfoReport.MED_STRING_LENGTH).get();
        this.pcrHash = StringValidator.check(pcrHash, "pcrHash")
                .notBlank().maxLength(DeviceInfoReport.MED_STRING_LENGTH).get();
    }

    /**
     * Default no parameter constructor.
     */
    public RimInfo() {
        this(DeviceInfoReport.NOT_SPECIFIED, DeviceInfoReport.NOT_SPECIFIED,
                DeviceInfoReport.NOT_SPECIFIED, DeviceInfoReport.NOT_SPECIFIED);
    }

    /**
     * Getter for the manufacturer string.
     * @return string of the manufacturer.
     */
    public final String getManufacturer() {
        return this.manufacturer;
    }

    /**
     * Getter for the model string.
     * @return of the model string
     */
    public final String getModel() {
        return this.model;
    }

    @Override
    public String toString() {
        return String.format("%s, %s", manufacturer, model);
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RimInfo)) {
            return false;
        }
        RimInfo other = (RimInfo) obj;

        if (manufacturer != null && !manufacturer.equals(other.manufacturer)) {
            return false;
        }
        if (model != null && !model.equals(other.model)) {
            return false;
        }

        return true;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + manufacturer.hashCode();
        result = prime * result + model.hashCode();
        result = prime * result + fileHash.hashCode();
        result = prime * result + pcrHash.hashCode();

        return result;
    }
}
