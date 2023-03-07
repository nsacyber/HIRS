package hirs.utils.tpm.eventlog.events;

import hirs.utils.tpm.eventlog.uefi.UefiFirmware;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Class for processing EV_POST_CODE event types
 *
 * typedef struct tdUEFI_PLATFORM_FIRMWARE_BLOB {
 *         UEFI_PHYSICAL_ADDRESS   BlobBase;    // Same as UINT64 for most systems
 *         UINT64                  BlobLength;
 *        } UEFI_PLATFORM_FIRMWARE_BLOB;
 *
 * However Table 9 of the PC Client Platform firmware profile states that even content is a string
 *   For POST code, the event data SHOULD be POST CODE.
 *   For embedded SMM code, the event data SHOULD be SMM CODE.
 *   For ACPI flash data, the event data SHOULD be ACPI DATA.
 *   For BIS code, the event data SHOULD be BIS CODE.
 *   For embedded option ROMs, the event data SHOULD be Embedded UEFI Driver.
 */
public class EvPostCode {
    /** Event Description. */
    private String codeInfo = "";
    /** String type flag. */
    private boolean bisString = false;
    /** Firmware object. */
    @Getter
    private UefiFirmware firmwareBlob = null;

    /**
     * EcPostCode constructor.
     * @param postCode byte array holding the post code content.
     */
    public EvPostCode(final byte[] postCode) {
        // 2 ways post code has been implemented, check for the ascii string first
        if (isAscii(postCode)) {
            codeInfo = new String(postCode, StandardCharsets.UTF_8);
            bisString = true;
        } else {
            firmwareBlob = new UefiFirmware(postCode);
        }
    }

    /**
     * Flag set to true if Post Code is a string.
     * @return true if Post Code is a string.
     */
    public boolean isString() {
        return bisString;
    }

    /**
     * Returns a human readable string of the Post Code information.
     * @return  human readable string.
     */
    public String toString() {
        if (bisString) {
            return codeInfo;
        }
       return firmwareBlob.toString();
     }

    /**
     * Determines if the byte array is a string.
     * @param postCode byte array input.
     * @return true if byte array is a string.
     */
    public static boolean isAscii(final byte[] postCode) {
        for (byte b : postCode) {
            if (!Character.isDefined(b)) {
                return false;
            }
        }
       return true;
    }
}
