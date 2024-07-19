package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.spdm.SpdmMeasurementBlock;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_CERT_CHAIN event per PFP.
 *
 * <p>
 * typedef union tdDEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_CERT_CHAIN {
 *      UINT16                  SpdmVersion;
 *      UINT8                   SpdmSlotId;
 *      UINT8                   Reserved;
 *      UINT32                  SpdmBaseHashAlgo;
 *      SPDM_CERT_CHAIN         SpdmCertChain;
 * } DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_CERT_CHAIN;
 * <p>
 */
public class DeviceSecurityEventDataSubHeaderCertChain extends DeviceSecurityEventDataSubHeader{

    /**
     * SPDM version.
     */
    @Getter
    private int spdmVersion = 0;
    /**
     * SPDM slot ID.
     */
    @Getter
    private int spdmLotId = 0;
    /**
     * SPDM base hash algorithm.
     */
    @Getter
    private int spdmBaseHashAlgo = -1;

    /**
     * DeviceSecurityEventDataSubHeaderCertChain Constructor.
     *
     * @param dsedSubHBytes byte array holding the DeviceSecurityEventDataSubHeaderSpdmMeasurementBlock.
     */
    public DeviceSecurityEventDataSubHeaderCertChain(final byte[] dsedSubHBytes) {

        byte[] spdmVersionBytes = new byte[2];
        System.arraycopy(dsedSubHBytes, 0, spdmVersionBytes, 0, 2);
        spdmVersion = HexUtils.leReverseInt(spdmVersionBytes);

        byte[] spdmLotIdBytes = new byte[1];
        System.arraycopy(dsedSubHBytes, 2, spdmLotIdBytes, 0, 1);
        spdmLotId = HexUtils.leReverseInt(spdmLotIdBytes);

        // byte[] reserved[Bytes]: 1 byte

        byte[] spdmBaseHashAlgoBytes = new byte[4];
        System.arraycopy(dsedSubHBytes, 4, spdmBaseHashAlgoBytes, 0, 4);
        spdmBaseHashAlgo = HexUtils.leReverseInt(spdmBaseHashAlgoBytes);

        // get the size of the SPDM Cert Chain
        int spdmCertChainSize = dsedSubHBytes.length - 8;

        // extract the bytes that comprise the SPDM Cert Chain
        byte[] spdmCertChainBytes = new byte[spdmCertChainSize];
        System.arraycopy(dsedSubHBytes, 8, spdmCertChainBytes, 0,
                spdmCertChainSize);

//        ByteArrayInputStream spdmMeasurementBlockListData =
//                new ByteArrayInputStream(spdmMeasurementBlockListBytes);
//        while (spdmMeasurementBlockListData.available() > 0) {
//            SpdmMeasurementBlock spdmMeasurementBlock;
//            spdmMeasurementBlock = new SpdmMeasurementBlock(spdmMeasurementBlockListData);
//            spdmMeasurementBlockList.add(spdmMeasurementBlock);
//        }
    }
}
