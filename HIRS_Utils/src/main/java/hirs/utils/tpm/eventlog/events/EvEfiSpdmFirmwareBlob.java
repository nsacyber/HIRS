package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.TcgTpmtHa;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to process the EV_EFI_SPDM_FIRMWARE_BLOB event. The event field MUST be a
 *      1) DEVICE_SECURITY_EVENT_DATA or
 *      1) DEVICE_SECURITY_EVENT_DATA2
 * DEVICE_SECURITY_EVENT_DATA has 2 structures:
 *    1) DEVICE_SECURITY_EVENT_DATA_HEADER
 *    2) DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT, which has 2 structures
 *       a) DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT
 *       b) DEVICE_SECURITY_EVENT_DATA_USB_CONTEXT
 * DEVICE_SECURITY_EVENT_DATA2 has 3 structures:
 *    1) DEVICE_SECURITY_EVENT_DATA_HEADER2
 *    2) DEVICE_SECURITY_EVENT_DATA_SUB_HEADER
 *    3) DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT, which has 2 structures (see above)
 * The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 * NUL-terminated, per PFP. The only currently defined Signature is "SPDM Device Sec",
 * which implies the data is a DEVICE_SECURITY_EVENT_DATA.
 * This event is used to record an extended digest for the firmware of an embedded component
 * or an add-in device that supports SPDM “GET_MEASUREMENTS” functionality. This event records
 * extended digests of SPDM GET_MEASUREMENT responses that correspond to firmware, such as
 * immutable ROM, mutable firmware, firmware version, firmware secure version number, etc.
 */
public class EvEfiSpdmFirmwareBlob {

    /**
     * Signature (text) data.
     */
    private String signature = "";
    /**
     * True if the event is a DEVICE_SECURITY_EVENT_DATA.
     */
    private boolean bDeviceSecurityEventData = false;
    /**
     * DeviceSecurityEventData Object.
     */
    @Getter
    private DeviceSecurityEventData deviceSecurityEventData = null;

    /**
     * EvEfiSpdmFirmwareBlob constructor.
     *
     * @param eventData byte array holding the event to process.
     * @throws java.io.UnsupportedEncodingException if input fails to parse.
     */
    public EvEfiSpdmFirmwareBlob(final byte[] eventData) throws UnsupportedEncodingException {
        byte[] signatureBytes = new byte[UefiConstants.SIZE_15];
        System.arraycopy(eventData, 0, signatureBytes, 0, UefiConstants.SIZE_15);
        signature = new String(signatureBytes, StandardCharsets.UTF_8);
        signature = signature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters
        if (signature.contains("SPDM Device Sec")) {      // implies Device Security event
            deviceSecurityEventData = new DeviceSecurityEventData(eventData);
            bDeviceSecurityEventData = true;
        }
    }

    /**
     * Determines if this event is a DeviceSecurityEventData.
     *
     * @return true of the event is a DeviceSecurityEventData.
     */
    public boolean isDeviceSecurityEventData() {
        return bDeviceSecurityEventData;
    }

    /**
     * Returns a description of this event.
     *
     * @return Human readable description of this event.
     */
    public String toString() {
        String specInfo = "";
        if (bDeviceSecurityEventData) {
            specInfo += "   Signature =  SPDM Device Sec : ";
//            if (specIDEvent.isCryptoAgile()) {
//                specInfo += "Log format is Crypto Agile\n";
//            } else {
//                specInfo += "Log format is SHA 1 (NOT Crypto Agile)\n";
//            }
//            specInfo += "   Platform Profile Specification version = "
//                    + specIDEvent.getVersionMajor() + "." + specIDEvent.getVersionMinor()
//                    + " using errata version " + specIDEvent.getErrata();
        } else {
            specInfo = "EV_EFI_SPDM_FIRMWARE_BLOB event named " + signature
                    + " encountered but support for processing it has not been added to this application.\n";
        }
        return specInfo;
    }
}
