package hirs.attestationca.persist.entity.userdefined.info;

import hirs.utils.StringValidator;
import hirs.utils.enums.DeviceInfoEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Store information about the RIM into the database.
 */
@Getter
@EqualsAndHashCode
@ToString
@Embeddable
public class RIMInfo implements Serializable {

    @XmlElement
    @Column(length = DeviceInfoEnums.MED_STRING_LENGTH, nullable = false)
    private final String rimManufacturer;

    @XmlElement
    @Column(length = DeviceInfoEnums.MED_STRING_LENGTH, nullable = false)
    private final String model;

    @XmlElement
    @Column(length = DeviceInfoEnums.MED_STRING_LENGTH, nullable = false)
    private final String fileHash;

    @XmlElement
    @Column(length = DeviceInfoEnums.MED_STRING_LENGTH, nullable = false)
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
                .notBlank().maxLength(DeviceInfoEnums.MED_STRING_LENGTH).getValue();
        this.model = StringValidator.check(model, "model")
                .notBlank().maxLength(DeviceInfoEnums.MED_STRING_LENGTH).getValue();
        this.fileHash = StringValidator.check(fileHash, "fileHash")
                .notBlank().maxLength(DeviceInfoEnums.MED_STRING_LENGTH).getValue();
        this.pcrHash = StringValidator.check(pcrHash, "pcrHash")
                .notBlank().maxLength(DeviceInfoEnums.MED_STRING_LENGTH).getValue();
    }

    /**
     * Default no parameter constructor.
     */
    public RIMInfo() {
        this(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED);
    }
}
