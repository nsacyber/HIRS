package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.spdm.SpdmHa;
import hirs.utils.tpm.eventlog.spdm.SpdmMeasurementBlock;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_HEADER.
 * DEVICE_SECURITY_EVENT_DATA_HEADER contains the measurement(s) and hash algorithm identifier
 * returned by the SPDM "GET_MEASUREMENTS" function.
 * <p>
 * HEADERS defined by PFP v1.06 Rev 52:
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER {
 * .    UINT8                           Signature[16];
 * .    UINT16                          Version;
 * .    UINT16                          Length;
 * .    UINT32                          SpdmHashAlg;
 * .    UINT32                          DeviceType;
 * .    SPDM_MEASUREMENT_BLOCK          SpdmMeasurementBlock;
 * .    UINT64                          DevicePathLength;
 * .    UNIT8                           DevicePath[DevicePathLength]
 * } DEVICE_SECURITY_EVENT_DATA_HEADER;
 * <p>
 * Assumption: there is only 1 SpdmMeasurementBlock per event. Need more test patterns to verify.
 */
public class DeviceSecurityEventDataHeader extends DeviceSecurityEventHeader {

    /**
     * Event data length.
     */
    @Getter
    private int length = 0;
    /**
     * SPDM hash algorithm.
     */
    @Getter
    private int spdmHashAlgo = -1;

    /**
     * SPDM Measurement Block.
     */
    private SpdmMeasurementBlock spdmMeasurementBlock = null;

    /**
     * Human-readable description of the data within the
     * SpdmMeasurementBlock.
     */
    private String spdmMeasurementBlockInfo = "";

    /**
     * DeviceSecurityEventDataHeader Constructor.
     *
     * @param dsedBytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventDataHeader(final byte[] dsedBytes) {

        super(dsedBytes);

        final int dsedBytesSrcIndex1 = 18;
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(dsedBytes, dsedBytesSrcIndex1, lengthBytes, 0,
                UefiConstants.SIZE_2);
        length = HexUtils.leReverseInt(lengthBytes);

        byte[] spdmHashAlgoBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(dsedBytes, UefiConstants.OFFSET_20, spdmHashAlgoBytes, 0,
                UefiConstants.SIZE_4);
        spdmHashAlgo = HexUtils.leReverseInt(spdmHashAlgoBytes);

        final int dsedBytesStartByte = 24;
        extractDeviceType(dsedBytes, dsedBytesStartByte);

        // get the size of the SPDM Measurement Block
        final int dsedBytesSrcIndex2 = 30;
        byte[] sizeOfSpdmMeasBlockBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(dsedBytes, dsedBytesSrcIndex2, sizeOfSpdmMeasBlockBytes, 0,
                UefiConstants.SIZE_2);
        final int sizeOfSpdmMeas = HexUtils.leReverseInt(sizeOfSpdmMeasBlockBytes);
        final int offSetBytesForSpdm = 4;
        final int sizeOfSpdmMeasBlock = sizeOfSpdmMeas + offSetBytesForSpdm;   // header is 4 bytes

        // extract the bytes that comprise the SPDM Measurement Block
        final int dsedBytesSrcIndex3 = 28;
        byte[] spdmMeasBlockBytes = new byte[sizeOfSpdmMeasBlock];
        System.arraycopy(dsedBytes, dsedBytesSrcIndex3, spdmMeasBlockBytes, 0,
                sizeOfSpdmMeasBlock);

        ByteArrayInputStream spdmMeasurementBlockData =
                new ByteArrayInputStream(spdmMeasBlockBytes);

        try {
            spdmMeasurementBlock = new SpdmMeasurementBlock(spdmMeasurementBlockData);
            spdmMeasurementBlockInfo = spdmMeasurementBlock.toString();
        } catch (IOException e) {
            spdmMeasurementBlockInfo = "      Error reading SPDM Measurement Block";
        }

        final int offSetBytesForDevPath = 28;
        final int devPathLenStartByte = offSetBytesForDevPath + sizeOfSpdmMeasBlock;
        extractDevicePathAndFinalSize(dsedBytes, devPathLenStartByte);
    }

    /**
     * Returns a human-readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {
        String dsedHeaderInfo = super.toString();
        String spdmHashAlgoStr = SpdmHa.tcgAlgIdToString(spdmHashAlgo);
        dsedHeaderInfo += "   SPDM Hash Algorithm = " + spdmHashAlgoStr + "\n";
        dsedHeaderInfo += "   SPDM Measurement Block:\n";
        dsedHeaderInfo += spdmMeasurementBlockInfo;

        return dsedHeaderInfo;
    }
}
