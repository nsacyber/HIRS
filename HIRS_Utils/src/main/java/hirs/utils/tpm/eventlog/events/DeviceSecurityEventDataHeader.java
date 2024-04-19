package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.spdm.SpdmHa;
import hirs.utils.tpm.eventlog.spdm.SpdmMeasurementBlock;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

public class DeviceSecurityEventDataHeader extends DeviceSecurityEventDataHeaderBase {

    /** ----------- Variables specific to Header Type 1 -----------
     //    /**
     //     * Type Header 1 event data length.
     //     */
//    @Getter
//    private String h1Length = "";
    /**
     * Type Header 1 SPDM hash algorithm.
     */
    @Getter
    private String h1SpdmHashAlgo = "";
//    /**
//     * Type Header 1 SPDM Measurement Block list.
//     */
//    private List<SpdmMeasurementBlock> h1SpdmMeasurementBlockList;
    /**
     * Type Header 1 SPDM Measurement Block.
     */
    private SpdmMeasurementBlock h1SpdmMeasurementBlock;

    public DeviceSecurityEventDataHeader(final byte[] dSEDbytes) {

        super(dSEDbytes);

        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(dSEDbytes, 18, lengthBytes, 0,
                UefiConstants.SIZE_2);
        int h1Length = HexUtils.leReverseInt(lengthBytes);

        byte[] spdmHashAlgoBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(dSEDbytes, UefiConstants.OFFSET_20, spdmHashAlgoBytes, 0,
                UefiConstants.SIZE_4);
        int h1SpdmHashAlgoInt = HexUtils.leReverseInt(spdmHashAlgoBytes);
        h1SpdmHashAlgo = SpdmHa.tcgAlgIdToString(h1SpdmHashAlgoInt);

//        byte[] deviceTypeBytes = new byte[UefiConstants.SIZE_4];
//        System.arraycopy(dSEDbytes, UefiConstants.OFFSET_24, deviceTypeBytes, 0,
//                UefiConstants.SIZE_4);
//        int deviceTypeInt = HexUtils.leReverseInt(deviceTypeBytes);
//        deviceType = deviceTypeToString(deviceTypeInt);

        // For each measurement block, create a SpdmMeasurementBlock object (can there be many blocks ?)

        // get the size of the SPDM Measurement Block
        byte[] sizeOfSpdmMeasBlockBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(dSEDbytes, 30, sizeOfSpdmMeasBlockBytes, 0,
                UefiConstants.SIZE_2);
        int sizeOfSpdmMeas = HexUtils.leReverseInt(sizeOfSpdmMeasBlockBytes);
        int sizeOfSpdmMeasBlock = sizeOfSpdmMeas + 4;

        // extract the bytes from the SPDM Measurement Block
        byte[] spdmMeasBlockBytes = new byte[sizeOfSpdmMeasBlock];
        System.arraycopy(dSEDbytes, 28, spdmMeasBlockBytes, 0,
                sizeOfSpdmMeasBlock);
        h1SpdmMeasurementBlock = new SpdmMeasurementBlock(spdmMeasBlockBytes);

//        byte[] algorithmIDBytes = new byte[UefiConstants.SIZE_2];
//        int algLocation = UefiConstants.SIZE_28;
//        for (int i = 0; i < numberOfAlg; i++) {
//            System.arraycopy(efiSpecId, algLocation + UefiConstants.OFFSET_4 * i, algorithmIDBytes,
//                    0, UefiConstants.SIZE_2);
//            String alg = TcgTpmtHa.tcgAlgIdToString(HexUtils.leReverseInt(algorithmIDBytes));
//            algList.add(alg);
//        }
//        if ((algList.size() == 1) && (algList.get(0).compareTo("SHA1") == 0)) {
//            cryptoAgile = false;
//        } else {
//            cryptoAgile = true;
//        }

    }


    /**
     * Returns a human readable description of the data within this event.
     *
     * @return a description of this event..
     */
    public String toString() {
        String dsedHeaderInfo = "";
        dsedHeaderInfo += "\n   SPDM hash algorithm = " + h1SpdmHashAlgo;
        dsedHeaderInfo += "\n   SPDM Measurement Block " + h1SpdmMeasurementBlock.toString();

        return dsedHeaderInfo;
    }
}
