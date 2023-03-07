package hirs.attestationca.persist.entity.userdefined.info;

import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.utils.StringValidator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
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
                .notBlank().maxLength(DeviceInfoReport.MED_STRING_LENGTH).getValue();
        this.model = StringValidator.check(model, "model")
                .notBlank().maxLength(DeviceInfoReport.MED_STRING_LENGTH).getValue();
        this.fileHash = StringValidator.check(fileHash, "fileHash")
                .notBlank().maxLength(DeviceInfoReport.MED_STRING_LENGTH).getValue();
        this.pcrHash = StringValidator.check(pcrHash, "pcrHash")
                .notBlank().maxLength(DeviceInfoReport.MED_STRING_LENGTH).getValue();
    }

    /**
     * Default no parameter constructor.
     */
    public RIMInfo() {
        this(DeviceInfoReport.NOT_SPECIFIED, DeviceInfoReport.NOT_SPECIFIED,
                DeviceInfoReport.NOT_SPECIFIED, DeviceInfoReport.NOT_SPECIFIED);
    }

    @Override
    public String toString() {
        return String.format("%s, %s, %s, %s", rimManufacturer, model,
                fileHash, pcrHash);
    }
}
