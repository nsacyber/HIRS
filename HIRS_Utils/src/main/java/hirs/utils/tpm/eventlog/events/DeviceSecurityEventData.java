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
    /**
     * Contains the human-readable info inside the Device Security Event.
     */
    @Getter
    private String dSEDinfo = "";
    /**
     * DeviceSecurityEventDataHeader Object.
     */
    @Getter
    private DeviceSecurityEventDataHeader dSEDheader = null;
    /**
     * DeviceSecurityEventDataSubHeader Object.
     */
    @Getter
    private DeviceSecurityEventDataHeader dSEDsubHeader = null;
    /**
     * DeviceSecurityEventDataDeviceContext Object.
     */
    @Getter
    private DeviceSecurityEventDataDeviceContext dSEDdeviceContext = null;

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
                .substring(0, UefiConstants.SIZE_15);

        byte[] versionBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(dSEDbytes, UefiConstants.OFFSET_16, versionBytes, 0,
                UefiConstants.SIZE_4);
        version = HexUtils.byteArrayToHexString(versionBytes);

        // If version is 0x01, the event is a DEVICE_SECURITY_EVENT_DATA
        // If version is 0x02, the event is a DEVICE_SECURITY_EVENT_DATA2
        int byteOffset = 0;
        dSEDheader = new DeviceSecurityEventDataHeader(dSEDbytes);
        byteOffset = dSEDheader.getDSEDheaderByteSize();
        if (version == "2") {
//            dSEDsubHeader = new DeviceSecurityEventDataSubHeader(dSEDbytes,byteOffset);
//            byteOffset = dSEDheader.getDSEDsubHeaderByteSize();
        }
        dSEDdeviceContext = new DeviceSecurityEventDataDeviceContext(dSEDbytes, byteOffset);

//        if (version == "1") {
//            dSEDinfo =+
//                    dSEDataHeader.getDSEDheaderInfo();
//            dSEDinfo =+
//                    dSEDdeviceContext.getdSEDdeviceContextInfo();
//        } else if (version == "2") {
//            dSEDinfo =+
//                    dSEDheader.getDSEDheaderInfo();
//            dSEDinfo =+
//                    dSEDsubHeader.getDSEDsubHeaderInfo();
//            dSEDinfo =+
//                    dSEDdeviceContext.getDSEDdeviceContextInfo();
//        }
    }

    public String toString() {
        String specInfo = "";

        specInfo += "   Signature =  SPDM Device Sec : ";
//            if (specIDEvent.isCryptoAgile()) {
//                specInfo += "Log format is Crypto Agile\n";
//            } else {
//                specInfo += "Log format is SHA 1 (NOT Crypto Agile)\n";
//            }
//            specInfo += "   Platform Profile Specification version = "
//                    + specIDEvent.getVersionMajor() + "." + specIDEvent.getVersionMinor()
//                    + " using errata version " + specIDEvent.getErrata();
//            specInfo += DeviceSecurityEventData.toString();
//        } else {
//            specInfo = "EV_EFI_SPDM_FIRMWARE_BLOB event named " + signature
//                    + " encountered but support for processing it has not been added to this application.\n";
//        }
        return specInfo;
    }
}
