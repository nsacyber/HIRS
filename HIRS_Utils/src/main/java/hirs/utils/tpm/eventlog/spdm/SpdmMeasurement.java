package hirs.utils.tpm.eventlog.spdm;

import hirs.utils.HexUtils;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Class to process the SpdmMeasurement.
 * <p>
 * Measurement, defined by SPDM v1.03, Sect 10.11.1, Table 54:
 * DMTF measurement spec format {
 *      DMTFSpecMeasurementValueType    1 byte;
 *      DMTFSpecMeasurementValueSize    2 bytes;
 *      DMTFSpecMeasurementValue        <DMTFSpecMeasurementValueSize> bytes;
 * }
 * <p>
 * DMTFSpecMeasurementValueType[7]
 *      Indicates how bits [0:6] are represented
 *      Bit = 0: Digest
 *      Bit = 1: Raw bit stream
 * DMTFSpecMeasurementValueType[6:0]            (see SPDM Spec, Table 55 "DMTFSpecMeasurementValueType[6:0]")
 *      Immutable ROM                   0x0
 *      Mutable firmware                0x1
 *      Hardware configuration          0x2
 *      Firmware configuration          0x3
 *      etc.
 * <p>
 */
public class SpdmMeasurement {

    /**
     * Measurement value type (such as mutable firmware, etc).
     */
    @Getter
    private int dmtfSpecMeasurementValueType = 0;
    /**
     * Measurement value (digest).
     */
    @Getter
    private byte[] dmtfSpecMeasurementValue = null;

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

        // in future, can crosscheck this value size + 3 with the spdm block MeasurementSize size
        byte[] dmtfSpecMeasurementValueSizeBytes = new byte[2];
        System.arraycopy(spdmMeasBytes, 1, dmtfSpecMeasurementValueSizeBytes, 0,
                2);
        int dmtfSpecMeasurementValueSize = HexUtils.leReverseInt(dmtfSpecMeasurementValueSizeBytes);

        dmtfSpecMeasurementValue = new byte[dmtfSpecMeasurementValueSize];
        System.arraycopy(spdmMeasBytes, 3, dmtfSpecMeasurementValue, 0,
                dmtfSpecMeasurementValueSize);
    }

    /**
     * Returns a human readable description of the data within this structure.
     *
     * @return a description of this structure..
     */
    public String dmtfSpecMeasurementValueTypeToString(final int measValType) {

        String measValTypeStr;
        switch (measValType) {
            case 0:
                measValTypeStr = "Immutable ROM";
                break;
            case 1:
                measValTypeStr = "Mutable firmware";
                break;
            case 2:
                measValTypeStr = "Hardware configuration";
                break;
            case 3:
                measValTypeStr = "Firmware configuration";
                break;
            case 4:
                measValTypeStr = "Freeform measurement manifest";
                break;
            case 5:
                measValTypeStr = "Structured representation of debug and device mode";
                break;
            case 6:
                measValTypeStr = "Mutable firmware's version number";
                break;
            case 7:
                measValTypeStr = "Mutable firmware's security verison number";
                break;
            case 8:
                measValTypeStr = "Hash-extended measurement";
                break;
            case 9:
                measValTypeStr = "Informational";
                break;
            case 10:
                measValTypeStr = "Structured measurement manifest";
                break;
            default:
                measValTypeStr = "Unknown or invalid DMTF Spec Measurement Value Type";
        }
        return measValTypeStr;
    }

    public String toString() {
        String spdmMeasInfo = "";

        spdmMeasInfo += "\n      SPDM Measurement Value Type = " +
                dmtfSpecMeasurementValueTypeToString(dmtfSpecMeasurementValueType);
        spdmMeasInfo += "\n      SPDM Measurement Value = " +
                HexUtils.byteArrayToHexString(dmtfSpecMeasurementValue);

        return spdmMeasInfo;
    }
}
