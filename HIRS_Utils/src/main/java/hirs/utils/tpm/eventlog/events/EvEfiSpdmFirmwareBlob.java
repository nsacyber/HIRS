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
 * Class to process the EV_EFI_SPDM_FIRMWARE_BLOB event using structures:
 *    1) DEVICE_SECURITY_EVENT_DATA_HEADER    [ delete: TCG_EfiSpecIDEvent]
 *    2) DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT
 * DEVICE_SECURITY_EVENT_DATA_HEADER
 *    The first 16 bytes of the event data MUST be a String based identifier (Signature), NUL-terminated.
 *    The only currently defined Signature is "SPDM Device Sec"
 *       which implies the data is a DEVICE_SECURITY_EVENT_DATA_HEADER.
 *    DEVICE_SECURITY_EVENT_DATA_HEADER  contains the measurement(s) and hash algorithm
 *       (SpdmHashAlg) identifier returned by the SPDM "GET_MEASUREMENTS" function
 * DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT
 *    DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT is a common SPDM structure which includes the
 *       identification of the device, device vendor, subsystem, etc for PCI connection devices
 */
public class EvEfiSpdmFirmwareBlob {

    /**
     * Signature (text) data.
     */
    private String signature = "";
    /**
     * True if the event is a DEVICE_SECURITY_EVENT_DATA_HEADER.
     */
    private boolean bDeviceSecurityEventDataHeader = false;
    /**
     * evDeviceSecurityEventDataHeader Object.
     */
    @Getter
    private evDeviceSecurityEventDataHeader deviceSecurityEventDataHeader = null;

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
