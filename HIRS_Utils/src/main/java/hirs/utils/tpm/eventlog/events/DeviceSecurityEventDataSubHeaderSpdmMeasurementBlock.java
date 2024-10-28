package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.spdm.SpdmHa;
import hirs.utils.tpm.eventlog.spdm.SpdmMeasurementBlock;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_MEASUREMENT_BLOCK event per PFP.
 *
 * <p>
 * typedef union tdDEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_MEASUREMENT_BLOCK {
 * .     UINT16                  SpdmVersion;
 * .     UINT8                   SpdmMeasurementBlockCount;
 * .     UINT8                   Reserved;
 * .     UINT32                  SpdmMeasurementHashAlgo;
 * .     SPDM_MEASUREMENT_BLOCK  SpdmMeasurementBlock[SpdmMeasurementBlockCount];
 * } DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_MEASUREMENT_BLOCK;
 * <p>
 * <p>
 * SpdmMeasurementBlock is an array of SPDM_MEASUREMENT_BLOCKs
 * .  The size of each block is the same and can be found by either:
 * .      1) 4 + SpdmMeasurementBlock MeasurementSize
 * .      OR
 * .      2) 4 + hash length of the hash algorithm found in
 * .             DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_MEASUREMENT_BLOCK SpdmMeasurementHashAlgo
 * .      where 4 is the size of the SpdmMeasurementBlock header
 */
public class DeviceSecurityEventDataSubHeaderSpdmMeasurementBlock extends DeviceSecurityEventDataSubHeader {

    /**
     * List of SPDM Measurement Blocks.
     */
    private final List<SpdmMeasurementBlock> spdmMeasurementBlockList;
    /**
     * SPDM version.
     */
    @Getter
    private int spdmVersion = 0;
    /**
     * SPDM measurement block count.
     */
    @Getter
    private int spdmMeasurementBlockCount = 0;
    /**
     * SPDM measurement hash algorithm.
     */
    @Getter
    private int spdmMeasurementHashAlgo = -1;
    /**
     * Error reading SPDM Measurement Block.
     */
    private boolean spdmMeasurementBlockReadError = false;

    /**
     * DeviceSecurityEventDataSubHeaderSpdmMeasurementBlock Constructor.
     *
     * @param dsedSubHBytes byte array holding the DeviceSecurityEventDataSubHeaderSpdmMeasurementBlock.
     */
    public DeviceSecurityEventDataSubHeaderSpdmMeasurementBlock(final byte[] dsedSubHBytes) {

        spdmMeasurementBlockList = new ArrayList<>();

        byte[] spdmVersionBytes = new byte[2];
        System.arraycopy(dsedSubHBytes, 0, spdmVersionBytes, 0, 2);
        spdmVersion = HexUtils.leReverseInt(spdmVersionBytes);

        byte[] spdmMeasurementBlockCountBytes = new byte[1];
        System.arraycopy(dsedSubHBytes, 2, spdmMeasurementBlockCountBytes, 0, 1);
        spdmMeasurementBlockCount = HexUtils.leReverseInt(spdmMeasurementBlockCountBytes);

        // byte[] reserved[Bytes]: 1 byte

        final int spdmMeasurementHashAlgoBytesSize = 4;
        final int dsedSubHBytesSrcIndex1 = 4;
        byte[] spdmMeasurementHashAlgoBytes = new byte[spdmMeasurementHashAlgoBytesSize];
        System.arraycopy(dsedSubHBytes, dsedSubHBytesSrcIndex1, spdmMeasurementHashAlgoBytes, 0,
                spdmMeasurementHashAlgoBytesSize);
        spdmMeasurementHashAlgo = HexUtils.leReverseInt(spdmMeasurementHashAlgoBytes);

        // get the total size of the SPDM Measurement Block List
        final int offsetForspdmMeasurementBlockList = 8;
        final int spdmMeasurementBlockListSize = dsedSubHBytes.length - offsetForspdmMeasurementBlockList;

        // extract the bytes that comprise the SPDM Measurement Block List
        final int dsedSubHBytesSrcIndex2 = 8;
        byte[] spdmMeasurementBlockListBytes = new byte[spdmMeasurementBlockListSize];
        System.arraycopy(dsedSubHBytes, dsedSubHBytesSrcIndex2, spdmMeasurementBlockListBytes, 0,
                spdmMeasurementBlockListSize);

        ByteArrayInputStream spdmMeasurementBlockListData =
                new ByteArrayInputStream(spdmMeasurementBlockListBytes);
        while (spdmMeasurementBlockListData.available() > 0) {
            try {
                SpdmMeasurementBlock spdmMeasurementBlock =
                        new SpdmMeasurementBlock(spdmMeasurementBlockListData);
                spdmMeasurementBlockList.add(spdmMeasurementBlock);
            } catch (IOException e) {
                spdmMeasurementBlockReadError = true;
                break;
            }
        }
    }

    /**
     * Returns a human-readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {
        String dsedSubHeaderInfo = "";
        dsedSubHeaderInfo += "   SPDM Version = " + spdmVersion + "\n";
        String spdmHashAlgoStr = SpdmHa.tcgAlgIdToString(spdmMeasurementHashAlgo);
        dsedSubHeaderInfo += "   SPDM Hash Algorithm = " + spdmHashAlgoStr + "\n";

        // SPDM Measurement Block List output
        dsedSubHeaderInfo += "   Number of SPDM Measurement Blocks = "
                + spdmMeasurementBlockList.size() + "\n";
        int spdmMeasBlockCnt = 1;
        for (SpdmMeasurementBlock spdmMeasBlock : spdmMeasurementBlockList) {
            dsedSubHeaderInfo += "   SPDM Measurement Block # " + spdmMeasBlockCnt++ + " of "
                    + spdmMeasurementBlockList.size() + "\n";
            dsedSubHeaderInfo += spdmMeasBlock.toString();
        }
        if (spdmMeasurementBlockReadError) {
            dsedSubHeaderInfo += "      Error reading SPDM Measurement Block #"
                    + spdmMeasBlockCnt + ", halting processing\n";
        }

        return dsedSubHeaderInfo;
    }
}
