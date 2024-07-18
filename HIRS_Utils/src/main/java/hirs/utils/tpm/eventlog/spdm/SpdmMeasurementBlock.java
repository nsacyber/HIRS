package hirs.utils.tpm.eventlog.spdm;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    /**
     * Error reading SPDM Measurement Block.
     */
    private boolean spdmMeasurementBlockReadError = false;

    /**
     * SpdmMeasurementBlock Constructor.
     *
     * @param spdmMeasBlocks byte array holding the SPDM Measurement Block bytes.
     */
    public SpdmMeasurementBlock(final ByteArrayInputStream spdmMeasBlocks) {

        try {
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
        } catch (IOException ioEx) {
            spdmMeasurementBlockReadError = true;
        }
    }

    /**
     * Returns a human-readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {

        String spdmMeasBlockInfo = "";

        if(spdmMeasurementBlockReadError) {
            spdmMeasBlockInfo += "\n      Error reading SPDM Measurement Block";
        }
        else {
            spdmMeasBlockInfo += "\n      Index = " + index;
            spdmMeasBlockInfo += "\n      MeasurementSpec = " +  measurementSpec;
            spdmMeasBlockInfo += spdmMeasurement.toString();
        }

        return spdmMeasBlockInfo;
    }
}
