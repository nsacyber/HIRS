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
 * Class to process the EV_EFI_SPDM_FIRMWARE_BLOB event using structure DEVICE_SECURITY_EVENT_DATA
 * DEVICE_SECURITY_EVENT_DATA has 2 structures:
 *    1) DEVICE_SECURITY_EVENT_DATA_HEADER
 *    2) DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT
 * The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 *    NUL-terminated. The only currently defined Signature is "SPDM Device Sec"
 *    which implies the event data is a DEVICE_SECURITY_EVENT_DATA.
 */
public class EvEfiSpdmFirmwareBlob {

    /**
     * Signature (text) data.
     */
    private String signature = "";
    /**
     * True if the event is a DEVICE_SECURITY_EVENT_DATA.
     */
    private boolean bDeviceSecurityEventDataHeader = false;
    /**
     * DeviceSecurityEventDataHeader Object.
     */
    @Getter
    private DeviceSecurityEventDataHeader deviceSecurityEventDataHeader = null;

    /**
     * EvEfiSpdmFirmwareBlob constructor.
     *
     * @param eventData byte array holding the event to process.
     * @throws java.io.UnsupportedEncodingException if input fails to parse.
     */
    public EvEfiSpdmFirmwareBlob(final byte[] eventData) throws UnsupportedEncodingException {
        byte[] signatureBytes = new byte[UefiConstants.SIZE_15];
//        System.arraycopy(eventData, 0, signatureBytes, 0, UefiConstants.SIZE_15);
//        signature = new String(signatureBytes, StandardCharsets.UTF_8);
//        signature = signature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters
//        if (signature.contains("Spec ID Event03")) {      // implies CryptAgileFormat
//            specIDEvent = new EvEfiSpecIdEvent(eventData);
//            bSpecIDEvent = true;
//        }
    }

    /**
     * Determines if this event is a SpecIDEvent.
     *
     * @return true of the event is a SpecIDEvent.
     */
    public boolean isDeviceSecurityEventDataHeader() {
        return bDeviceSecurityEventDataHeader;
    }

    /**
     * Returns a description of this event.
     *
     * @return Human readable description of this event.
     */
//    public String toString() {
//        String specInfo = "";
//        if (bSpecIDEvent) {
//            specInfo += "   Signature = Spec ID Event03 : ";
//            if (specIDEvent.isCryptoAgile()) {
//                specInfo += "Log format is Crypto Agile\n";
//            } else {
//                specInfo += "Log format is SHA 1 (NOT Crypto Agile)\n";
//            }
//            specInfo += "   Platform Profile Specification version = "
//                    + specIDEvent.getVersionMajor() + "." + specIDEvent.getVersionMinor()
//                    + " using errata version " + specIDEvent.getErrata();
//        } else {
//            specInfo = "EV_NO_ACTION event named " + signature
//                    + " encountered but support for processing it has not been added to this application.\n";
//        }
//        return specInfo;
//    }
}
