package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.TcgTpmtHa;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA or ..DATA2 event per PFP.
 * The event data comes in 2 forms:
 *    1) DEVICE_SECURITY_EVENT_DATA or
 *    2) DEVICE_SECURITY_EVENT_DATA2
 * The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 * NUL-terminated, per PFP. The only currently defined Signature is "SPDM Device Sec", which
 * implies the data is a DEVICE_SECURITY_EVENT_DATA or ..DATA2. The Version field in the HEADER
 * or HEADER2 indicates whether the Device Security Event is ..DATA or ..DATA2.
 *
 * DEVICE SECURITY EVENT structures defined by PFP v1.06 Rev 52:
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA {
 * DEVICE_SECURITY_EVENT_DATA_HEADER            EventDataHeader;
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT    DeviceContext;
 * } DEVICE_SECURITY_EVENT_DATA;
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA2 {
 * DEVICE_SECURITY_EVENT_DATA_HEADER2           EventDataHeader;
 * DEVICE_SECURITY_EVENT_DATA_SUB_HEADER        EventDataSubHeader;
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT    DeviceContext;
 * } DEVICE_SECURITY_EVENT_DATA;
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER or HEADER2 {
 *      UINT8                           Signature[16];
 *      UINT16                          Version;
 *      ...                             ...
 * }
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT {
 * DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT        PciContext;
 * DEVICE_SECURITY_EVENT_DATA_USB_CONTEXT        UsbContext;
 * } DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT;
 * <p>
 * Notes: Parses event data for an DEVICE_SECURITY_EVENT_DATA per PFP v1.06 Rev52 Table 20.
 * 1. Has an EventType of EV_EFI_SPDM_FIRMWARE_BLOB (0x800000E1)
 * 2. Digest of 48 bytes
 * 3. Event content defined as DEVICE_SECURITY_EVENT_DATA Struct.
 * 4. First 16 bytes of the structure header is an ASCII "SPDM Device Sec"
 */
public class DeviceSecurityEventData {

    /**
     * Signature (text) data.
     */
    @Getter
    private String signature = "";
    /**
     * Version determines data structure used (..DATA or ..DATA2).
     */
    @Getter
    private String version = "";
//    /**
//     * Contains the human-readable info inside the Device Security Event.
//     */
//    @Getter
//    private String dsedInfo = "";
    /**
     * DeviceSecurityEventDataHeader Object.
     */
    @Getter
    private DeviceSecurityEventDataHeader dsedHeader = null;
    /**
     * DeviceSecurityEventDataSubHeader Object.
     */
//    @Getter
//    private DeviceSecurityEventDataSubHeader dsedSubHeader = null;
    /**
     * DeviceSecurityEventDataDeviceContext Object.
     */
    @Getter
    private DeviceSecurityEventDataDeviceContext dsedDeviceContext = null;

    /**
     * DeviceSecurityEventData Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventData(final byte[] dSEDbytes) {

        byte[] signatureBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(dSEDbytes, 0, signatureBytes, 0, UefiConstants.SIZE_16);
        //signature = HexUtils.byteArrayToHexString(signatureBytes);
        signature = new String(signatureBytes, StandardCharsets.UTF_8)
                .substring(0, UefiConstants.SIZE_15);       // size 15 bc last letter is a 00 (null)

        byte[] versionBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(dSEDbytes, UefiConstants.OFFSET_16, versionBytes, 0,
                UefiConstants.SIZE_2);
        version = HexUtils.byteArrayToHexString(versionBytes);

//        int byteOffset = 0;
//        byteOffset = dsedHeader.getDsedHeaderByteSize();

        // If version is 0x01, the event is a DEVICE_SECURITY_EVENT_DATA
        // If version is 0x02, the event is a DEVICE_SECURITY_EVENT_DATA2
        switch (version) {
            case "0100":
                dsedHeader = new DeviceSecurityEventDataHeader(dSEDbytes);
//                dsedDeviceContext = new DeviceSecurityEventDataDeviceContext(dSEDbytes,
//                        dsedHeader.getDSEDheaderByteSize());
                break;
            case "0200":
                dsedHeader = new DeviceSecurityEventDataHeader(dSEDbytes);
//            dsedSubHeader = new DeviceSecurityEventDataSubHeader(dSEDbytes,byteOffset);
//            byteOffset = dsedHeader.getDSEDsubHeaderByteSize();
//                dsedDeviceContext = new DeviceSecurityEventDataDeviceContext(dSEDbytes, byteOffset);
                break;
            default:
                break;


//        if (version == "1") {
//            dSEDinfo =+
//                    dSEDataHeader.getDSEDheaderInfo();
//            dSEDinfo =+
//                    dsedDeviceContext.getdSEDdeviceContextInfo();
//        } else if (version == "2") {
//            dSEDinfo =+
//                    dSEDheader.getDSEDheaderInfo();
//            dSEDinfo =+
//                    dsedSubHeader.getDSEDsubHeaderInfo();
//            dSEDinfo =+
//                    dsedDeviceContext.getDSEDdeviceContextInfo();
//        }
        }
    }

    public String toString() {
        String dsedInfo = "";
        switch (version) {
            case "0100":
                dsedInfo += dsedHeader.toString();
//                dsedInfo += dsedDeviceContext.toString();
                break;
            case "0200":
//                dsedInfo += dsedHeader.toString();
//                dsedInfo += dsedSubHeader.toString();
//                dsedInfo += dsedDeviceContext.toString();
                break;
            default:
                dsedInfo += " Unknown SPDM Device Security Event Data version " + version + " found" + "\n";
        }
        return dsedInfo;
    }
}
