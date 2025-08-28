package hirs.utils.rim.unsignedRim.common.measurement;

import hirs.utils.rim.unsignedRim.common.IanaHashAlg;
import lombok.Getter;
import lombok.Setter;

import java.util.HexFormat;
import java.util.UUID;

/**
 * Class that stores the attributes of a Measurement Hash from a RIM file.
 */
@Getter
@Setter
public class Measurement {
    /** name of file that the measurement is for. */
    private MeasurementType measurementType = MeasurementType.UNKNOWN;

    /** a reference to the specific RIM that holds this measurement. */
    private UUID rimID = null;

    /** manufacturer. */
    private String manufacturer = null;

    /** model. */
    private String model = null;

    /** serial number. */
    private String serialNumber = null;

    /** index can be used differently depending on measurement type.
     * (ie. PC Client uses it as PCR index, DICE uses it as layer type, etc) */
    private int index = -1;

    /** software version. */
    private String revision = null;

    /** additional metadata such as path, filename, etc for informational or printing purposes. */
    private String additionalMetadata = null;

    /** hash algorithm used for measurement.
     * hash-entry array: https://www.iana.org/assignments/named-information/named-information.xhtml */
    private IanaHashAlg alg = null;

    /** the measurement bytes. */
    private byte[] measurementBytes = null;

    /**
     * Default toString that contains the attributes of a Measurement Hash from a RIM file.
     * @return measurementData the human-readable measurement data
     */
    public String toString() {
        String measurementData = "";
        HexFormat hexTool =  HexFormat.of();

        measurementData += "  Measurement Type: " + measurementType.getType() + "\n";
        measurementData += "  RIM ID: " + rimID + "\n";
        measurementData += "  Manufacturer: " + manufacturer + "\n";
        measurementData += "  Model: " + model + "\n";
        measurementData += "  Serial Number: " + serialNumber + "\n";
        measurementData += "  Index: " + index + "\n";
        measurementData += "  Software Revision: " + revision + "\n";
        measurementData += "  Additional Metadata: " + additionalMetadata + "\n";
        measurementData += "  Hash Alg: " + alg.getAlgName() + "\n";
        measurementData += "  Measurement: " + hexTool.formatHex(measurementBytes) + "\n";

        return measurementData;
    }
}
