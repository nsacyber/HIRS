package hirs.utils.tpm.eventlog.spdm;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

/**
 * Class to process the SpdmMeasurementBlock.
 * <p>
 * Measurement block format, defined by SPDM v1.03, Sect 10.11.1, Table 53:
 * Measurement block format {
 *      Index                           1 byte;
 *      MeasurementSpec                 1 byte;
 *      MeasurementSize                 2 bytes;
 *      Measurement                     <MeasurementSize> bytes;
 * }
 * <p>
 * Index: index of the measurement block, as there can be more than one
 * MeasurementSpec: bit mask; the measurement specification that the requested Measurement follows
 *     See "MeasurementSpecificationSel" in Table 21. See Tables 29, 53, 54
 *     Bit 0: DMTFmeasSpec, per Table 54
 *     Bit 1-7: Reserved
 * Measurement: the digest
 */
public class SpdmMeasurementBlock {

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
     * SPDM Measurement.
     */
    private SpdmMeasurement spdmMeasurement;

    public SpdmMeasurementBlock(final byte[] spdmMeasBlockBytes) {

        byte[] indexBytes = new byte[1];
        System.arraycopy(spdmMeasBlockBytes, 0, indexBytes, 0,
                1);
        index = HexUtils.leReverseInt(indexBytes);

        byte[] measurementSpecBytes = new byte[1];
        System.arraycopy(spdmMeasBlockBytes, 1, measurementSpecBytes, 0,
                1);
        measurementSpec = HexUtils.leReverseInt(measurementSpecBytes);

        // in future, can crosscheck this measurement size with the MeasurementSpec hash alg size
        byte[] measurementSizeBytes = new byte[2];
        System.arraycopy(spdmMeasBlockBytes, 2, measurementSizeBytes, 0,
                2);
        int measurementSize = HexUtils.leReverseInt(measurementSizeBytes);

        byte[] measurementBytes = new byte[measurementSize];
        System.arraycopy(spdmMeasBlockBytes, 4, measurementBytes, 0,
                measurementSize);
        spdmMeasurement = new SpdmMeasurement(measurementBytes);
    }

    public String toString() {
        String spdmMeasBlockInfo = "";

        spdmMeasBlockInfo += "\n      Index = " + index;
        spdmMeasBlockInfo += "\n      MeasurementSpec = " +  measurementSpec;
        spdmMeasBlockInfo += spdmMeasurement.toString();

        return spdmMeasBlockInfo;
    }

}
