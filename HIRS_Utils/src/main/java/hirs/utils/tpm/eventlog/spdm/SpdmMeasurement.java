package hirs.utils.tpm.eventlog.spdm;

import hirs.utils.HexUtils;
import lombok.Getter;

/**
 * Class to process the SpdmMeasurement.
 * <p>
 * Measurement, defined by SPDM v1.03, Sect 10.11.1, Table 54:
 * DMTF measurement spec format {
 * DMTFSpecMeasurementValueType    1 byte;
 * DMTFSpecMeasurementValueSize    2 bytes;
 * DMTFSpecMeasurementValue        <DMTFSpecMeasurementValueSize> bytes;
 * }
 * <p>
 * DMTFSpecMeasurementValueType[7]
 * Indicates how bits [0:6] are represented
 * Bit = 0: Digest
 * Bit = 1: Raw bit stream
 * DMTFSpecMeasurementValueType[6:0]            (see SPDM Spec, Table 55 "DMTFSpecMeasurementValueType[6:0]")
 * Immutable ROM                   0x0
 * Mutable firmware                0x1
 * Hardware configuration          0x2
 * Firmware configuration          0x3
 * etc.
 * <p>
 */
public class SpdmMeasurement {

    /**
     * Measurement value (digest).
     */
    private final byte[] dmtfSpecMeasurementValue;
    /**
     * Measurement value type (such as mutable firmware, etc).
     */
    @Getter
    private int dmtfSpecMeasurementValueType = 0;

    /**
     * SpdmMeasurement Constructor.
     *
     * @param spdmMeasBytes byte array holding the SPDM Measurement bytes.
     */
    public SpdmMeasurement(final byte[] spdmMeasBytes) {

        byte[] dmtfSpecMeasurementValueTypeBytes = new byte[1];
        System.arraycopy(spdmMeasBytes, 0, dmtfSpecMeasurementValueTypeBytes, 0,
                1);
        dmtfSpecMeasurementValueType = HexUtils.leReverseInt(dmtfSpecMeasurementValueTypeBytes);

        // in the future, can crosscheck this value size + 3 with the spdm block MeasurementSize size
        byte[] dmtfSpecMeasurementValueSizeBytes = new byte[2];
        System.arraycopy(spdmMeasBytes, 1, dmtfSpecMeasurementValueSizeBytes, 0,
                2);
        int dmtfSpecMeasurementValueSize = HexUtils.leReverseInt(dmtfSpecMeasurementValueSizeBytes);

        dmtfSpecMeasurementValue = new byte[dmtfSpecMeasurementValueSize];

        final int sourceIndex = 3;
        System.arraycopy(spdmMeasBytes, sourceIndex, dmtfSpecMeasurementValue, 0,
                dmtfSpecMeasurementValueSize);
    }

    /**
     * Lookup for SPDM measurement value type.
     *
     * @param measValType the numerical representation of the measurement value type.
     * @return a description of the measurement value type.
     */
    public String dmtfSpecMeasurementValueTypeToString(final int measValType) {

        String measValTypeStr = switch (measValType) {
            case 0 -> "Immutable ROM";
            case 1 -> "Mutable firmware";
            case 2 -> "Hardware configuration";
            case 3 -> "Firmware configuration";
            case 4 -> "Freeform measurement manifest";
            case 5 -> "Structured representation of debug and device mode";
            case 6 -> "Mutable firmware's version number";
            case 7 -> "Mutable firmware's security version number";
            case 8 -> "Hash-extended measurement";
            case 9 -> "Informational";
            case 10 -> "Structured measurement manifest";
            default -> "Unknown or invalid DMTF Spec Measurement Value Type";
        };
        return measValTypeStr;
    }

    /**
     * Returns a human-readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {
        String spdmMeasInfo = "";

        spdmMeasInfo += "      SPDM Measurement Value Type = "
                + dmtfSpecMeasurementValueTypeToString(dmtfSpecMeasurementValueType);
        spdmMeasInfo += "\n      SPDM Measurement Value = "
                + HexUtils.byteArrayToHexString(dmtfSpecMeasurementValue);
        spdmMeasInfo += "\n";

        return spdmMeasInfo;
    }
}
