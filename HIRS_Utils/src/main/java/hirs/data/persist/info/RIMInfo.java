package hirs.data.persist.info;

import hirs.data.persist.DeviceInfoReport;
import hirs.utils.StringValidator;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 */
@Embeddable
public class RIMInfo implements Serializable {

    @XmlElement
    @Column(length = DeviceInfoReport.MED_STRING_LENGTH, nullable = false)
    private final String rimManufacturer;

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
     * @param rimManufacturer string of the rimManufacturer
     * @param model string of the model
     * @param fileHash string of the file hash
     * @param pcrHash string of the pcr hash
     */
    public RIMInfo(final String rimManufacturer, final String model,
            final String fileHash, final String pcrHash) {
        this.rimManufacturer = StringValidator.check(rimManufacturer, "rimManufacturer")
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
    public RIMInfo() {
        this(DeviceInfoReport.NOT_SPECIFIED, DeviceInfoReport.NOT_SPECIFIED,
                DeviceInfoReport.NOT_SPECIFIED, DeviceInfoReport.NOT_SPECIFIED);
    }

    /**
     * Getter for the rimManufacturer string.
     * @return string of the rimManufacturer.
     */
    public final String getRimManufacturer() {
        return this.rimManufacturer;
    }

    /**
     * Getter for the model string.
     * @return of the model string
     */
    public final String getModel() {
        return this.model;
    }

    /**
     * Getter for the file hash string.
     * @return fileHash string
     */
    public String getFileHash() {
        return fileHash;
    }

    /**
     * Getter for the pcr hash.
     * @return pcrhash string
     */
    public String getPcrHash() {
        return pcrHash;
    }

    @Override
    public String toString() {
        return String.format("%s, %s, %s, %s", rimManufacturer, model,
                fileHash, pcrHash);
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RIMInfo)) {
            return false;
        }
        RIMInfo other = (RIMInfo) obj;

        if (rimManufacturer != null && !rimManufacturer.equals(other.rimManufacturer)) {
            return false;
        }
        if (model != null && !model.equals(other.model)) {
            return false;
        }
        if (fileHash != null && !fileHash.equals(other.fileHash)) {
            return false;
        }
        if (pcrHash != null && !pcrHash.equals(other.pcrHash)) {
            return false;
        }

        return true;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + rimManufacturer.hashCode();
        result = prime * result + model.hashCode();
        result = prime * result + fileHash.hashCode();
        result = prime * result + pcrHash.hashCode();

        return result;
    }
}
