package hirs.utils.tpm.eventlog.uefi;

import hirs.utils.HexUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Class to process a UEFI Boot#### variable.
 * Data is defined using the EFI_LOAD_OptionStructure:
 * typedef struct _EFI_LOAD_OPTION {
 * UINT32  Attributes;
 * UINT16   FilePathListLength;                 // this is the length in bytes of the FilePathList
 * // CHAR16    Description[];                  // A user-readable, null-terminated Unicode string (UTF-16)
 *                                              // that describes the option (e.g., "Windows Boot Manager")
 * // EFI_DEVICE_PATH_PROTOCOL  FilePathList[];
 * // UINT8  OptionalData[];
 * } EFI_LOAD_OPTION;
 * <p>
 * No length field for the Description is given
 * so we need to calculate it by search for a null termination on the Description field
 * Data following the Description should  be an EFI Device Path
 */
public class UefiBootVariable {
    /**
     * Human-readable description of the variable.
     */
    private String description = "";
    /**
     * Variable attributes.
     */
    private byte[] attributesBytes = null;
    /**
     * UEFI Device Path.
     */
    private UefiDevicePath efiDevPath = null;

    /**
     * UefiBootVariable Constructor.
     *
     * @param bootVar byte array holding the boot variable.
     * @throws java.io.UnsupportedEncodingException if the data fails to parse.
     */
    public UefiBootVariable(final byte[] bootVar) throws UnsupportedEncodingException {

        // attributes
        attributesBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(bootVar, 0, attributesBytes, 0, UefiConstants.SIZE_4);

        // file path list length
        byte[] filePathListLenBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(bootVar, UefiConstants.OFFSET_4, filePathListLenBytes, 0, UefiConstants.SIZE_2);
        int filePathListLen = HexUtils.leReverseInt(filePathListLenBytes);

        // description
        int restOfBytesLen = bootVar.length - UefiConstants.OFFSET_6;
        byte[] restOfBytes = new byte[restOfBytesLen];
        System.arraycopy(bootVar, UefiConstants.OFFSET_6, restOfBytes, 0, restOfBytesLen);
        int descriptionLen = getChar16ArrayLength(restOfBytes);
        byte[] descriptionBytes = new byte[descriptionLen];
        System.arraycopy(bootVar, UefiConstants.OFFSET_6, descriptionBytes, 0, descriptionLen);
        description = new String(UefiDevicePath.convertChar16tobyteArray(descriptionBytes),
                StandardCharsets.UTF_8);

        // filePathList: a packed array of UEFI device paths
        // The first element (FilePathList[0]) defines the location of the image (e.g., .efi file)
        int filePathListOffset = UefiConstants.OFFSET_6 + descriptionLen;
        byte[] filePathListBytes = new byte[filePathListLen];
        System.arraycopy(bootVar, filePathListOffset, filePathListBytes, 0, filePathListLen);
        efiDevPath = new UefiDevicePath(filePathListBytes);

        // OptionalData is any remaining bytes, not processed for now
    }

    /**
     * Returns a string that represents a UEFI boot variable.
     * Some devices have not properly terminated the Description filed with null characters
     * so garbage bytes are appended to the string that we  must strip off.
     * All non-alpha numeric is stripped from the string.
     *
     * @return string that represents a UEFI boot variable.
     */
    public String toString() {
        StringBuilder bootInfo = new StringBuilder("      EFI Load Option = ");
        // remove all non ascii chars
        String bootVar = description.replaceAll("[^a-zA-Z_0-0\\s]", "");
        bootInfo.append(bootVar + "\n" + efiDevPath.toString());
        return bootInfo.toString();
    }

    /**
     * Searches for the first char16 based null character (2 bytes of zeros).
     * Searches in a given byte array and returns the length of data up to that point in bytes.
     *
     * @param data a byte array to search for the data.
     * @return the length of the data in bytes at the beginning of the byte array.
     * which was terminated by a null character.
     */
    public int getChar16ArrayLength(final byte[] data) {
        int count = 0;
        byte[] nullTerminator = new byte[UefiConstants.SIZE_2];
        byte[] char16 = new byte[UefiConstants.SIZE_2];
        nullTerminator[0] = 0;
        nullTerminator[1] = 0;
        for (int i = 0; i < data.length; i += UefiConstants.SIZE_2) {
            char16[0] = data[i];
            char16[1] = data[i + 1];
            count++;
            if (Arrays.equals(nullTerminator, char16)) {
                return count * UefiConstants.SIZE_2;
            }
        }
        return count * UefiConstants.SIZE_2 + 1;
    }
}
