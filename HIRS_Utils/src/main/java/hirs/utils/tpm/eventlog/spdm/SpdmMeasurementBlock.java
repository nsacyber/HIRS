package hirs.utils.tpm.eventlog.spdm;

import hirs.utils.HexUtils;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Class to process the SpdmMeasurementBlock.
 *
 * <pre>
 * Measurement block format, defined by SPDM v1.03, Sect 10.11.1, Table 53:
 * Measurement block format {
 * Index                           1 byte;
 * MeasurementSpec                 1 byte;
 * MeasurementSize                 2 bytes;
 * Measurement                     (MeasurementSize) bytes;
 * }
 * </pre>
 * <p>
 * Index: index of the measurement block, as there can be more than one
 * MeasurementSpec: bit mask; the measurement specification that the requested Measurement follows
 * See "MeasurementSpecificationSel" in Table 21. See Tables 29, 53, 54
 * Bit 0: DMTFmeasSpec, per Table 54
 * Bit 1-7: Reserved
 * Measurement: the digest
 */
public class SpdmMeasurementBlock {

    /**
     * SPDM Measurement.
     */
    private final SpdmMeasurement spdmMeasurement;

    /**
     * Measurement block index, as an SPDM measurement exchange can contain several measurements.
     */
    @Getter
    private int index = 0;

    /**
     * Measurement Spec.
     */
    @Getter
    private int measurementSpec = 0;

    /**
     * SpdmMeasurementBlock Constructor.
     *
     * @param spdmMeasBlocks byte array holding the SPDM Measurement Block bytes.
     * @throws IOException if any issues arise creating the SPDM Measurement Block object.
     */
    public SpdmMeasurementBlock(final ByteArrayInputStream spdmMeasBlocks) throws IOException {

        byte[] indexBytes = new byte[1];
        spdmMeasBlocks.read(indexBytes);
        index = HexUtils.leReverseInt(indexBytes);

        byte[] measurementSpecBytes = new byte[1];
        spdmMeasBlocks.read(measurementSpecBytes);
        measurementSpec = HexUtils.leReverseInt(measurementSpecBytes);

        // in future, can crosscheck this measurement size with the MeasurementSpec hash alg size
        byte[] measurementSizeBytes = new byte[2];
        spdmMeasBlocks.read(measurementSizeBytes);
        int measurementSize = HexUtils.leReverseInt(measurementSizeBytes);

        byte[] measurementBytes = new byte[measurementSize];
        spdmMeasBlocks.read(measurementBytes);
        spdmMeasurement = new SpdmMeasurement(measurementBytes);
    }

    /**
     * Returns a human-readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {

        String spdmMeasBlockInfo = "";

        spdmMeasBlockInfo += "      Index = " + index + "\n";
        spdmMeasBlockInfo += "      MeasurementSpec = " + measurementSpec + "\n";
        spdmMeasBlockInfo += spdmMeasurement.toString();

        return spdmMeasBlockInfo;
    }
}
