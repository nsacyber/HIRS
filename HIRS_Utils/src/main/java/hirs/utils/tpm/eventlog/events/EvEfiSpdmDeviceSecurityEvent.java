package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Abstract class to process any SPDM event that is solely a DEVICE_SECURITY_EVENT_DATA or
 * DEVICE_SECURITY_EVENT_DATA2. The event field MUST be a
 *    1) DEVICE_SECURITY_EVENT_DATA or
 *    2) DEVICE_SECURITY_EVENT_DATA2
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
 * which implies the data is a DEVICE_SECURITY_EVENT_DATA or ..DATA2.
 * The EV_EFI_SPDM_FIRMWARE_BLOB event is used to record an extended digest for the firmware of
 * an embedded component or an add-in device that supports SPDM "GET_MEASUREMENTS" functionality.
 * This event records extended digests of SPDM GET_MEASUREMENT responses that correspond to
 * firmware, such as immutable ROM, mutable firmware, firmware version, firmware secure version
 * number, etc.
 */
public class EvEfiSpdmDeviceSecurityEvent {

    /**
     * DeviceSecurityEvent Object.
     */
    @Getter
    private DeviceSecurityEvent dSED = null;

    /**
     * Signature (text) data.
     */
    private String signature = "";

    /**
     * Human readable description of the data within this DEVICE_SECURITY_EVENT_DATA/..DATA2 event.
     */
    String spdmInfo = "";

    /**
     * EvEfiSpdmFirmwareBlob constructor.
     *
     * @param eventData byte array holding the event to process.
     * @throws java.io.UnsupportedEncodingException if input fails to parse.
     */
    public EvEfiSpdmDeviceSecurityEvent(final byte[] eventData) throws IOException {

        byte[] signatureBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(eventData, 0, signatureBytes, 0, UefiConstants.SIZE_16);
        signature = new String(signatureBytes, StandardCharsets.UTF_8);
        signature = signature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters

        byte[] versionBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(eventData, UefiConstants.OFFSET_16, versionBytes, 0,
                UefiConstants.SIZE_2);
        String version = HexUtils.byteArrayToHexString(versionBytes);
        if (version == "") {
            version = "version not readable";
        }

        if (signature.contains("SPDM Device Sec2")) {

            spdmInfo = "   Signature = SPDM Device Sec2";

            if (version.equals("0200")) {
                dSED = new DeviceSecurityEventData2(eventData);
                spdmInfo += dSED.toString();
            }
            else {
                spdmInfo += "    Incompatible version for DeviceSecurityEventData2: " + version;
            }
        }
        else if (signature.contains("SPDM Device Sec")) {      // implies Device Security event

            spdmInfo = "   Signature = SPDM Device Sec";

            if (version.equals("0100")) {
                dSED = new DeviceSecurityEventData(eventData);
                spdmInfo += dSED.toString();
            }
            else {
                spdmInfo += "    Incompatible version for DeviceSecurityEventData: " + version;
            }
        }
        else {
            spdmInfo = "   Signature = Undetermined value: " + signature;
        }
    }

    /**
     * Returns a description of this event.
     *
     * @return Human readable description of this event.
     */
    public String toString() {
        return spdmInfo;
    }
}
