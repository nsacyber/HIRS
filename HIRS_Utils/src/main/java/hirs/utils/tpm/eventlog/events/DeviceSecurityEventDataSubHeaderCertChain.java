package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.spdm.SpdmCertificateChain;
import hirs.utils.tpm.eventlog.spdm.SpdmHa;
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
 * SpdmVersion: SpdmBaseHashAlgo
 * SpdmSlotId: SlotId associated with this SPDM Certificate Chain
 * SpdmBaseHashAlgo: SPDM Base Hash Algorithm for the root certificate in the SPDM Certificate chain
 * SpdmCertChain: SPDM Certificate Chain
 */
public class DeviceSecurityEventDataSubHeaderCertChain extends DeviceSecurityEventDataSubHeader{

    /**
     * SPDM version.
     */
    private int spdmVersion = 0;
    /**
     * SPDM slot ID.
     */
    private int spdmSlotId = 0;
    /**
     * SPDM base hash algorithm.
     */
    private int spdmBaseHashAlgo = -1;
    /**
     * SPDM cert chain.
     */
    private SpdmCertificateChain spdmCertChain = null;
    /**
     * Human-readable description of any error associated with SPDM base hash alg.
     */
    String spdmBaseHashAlgoError = "";

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
        spdmSlotId = HexUtils.leReverseInt(spdmLotIdBytes);

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

        int spdmBaseHashAlgoSize = SpdmHa.tcgAlgIdToByteSize(spdmBaseHashAlgo);

        if(spdmBaseHashAlgoSize > 0) {
            spdmCertChain = new SpdmCertificateChain(spdmCertChainBytes, spdmBaseHashAlgoSize);
        }
        else {
            spdmBaseHashAlgoError += "SPDM base hash algorithm size is not >0";
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
        dsedSubHeaderInfo += "   SPDM Slot ID = " + spdmSlotId + "\n";
        String spdmBaseHashAlgoStr = SpdmHa.tcgAlgIdToString(spdmBaseHashAlgo);
        dsedSubHeaderInfo += "   SPDM Base Hash Algorithm = " + spdmBaseHashAlgoStr + "\n";

        // SPDM Certificate Chain output
        dsedSubHeaderInfo += spdmCertChain.toString();

        return dsedSubHeaderInfo;
    }
}
