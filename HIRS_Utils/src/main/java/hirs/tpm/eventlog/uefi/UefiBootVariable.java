package hirs.tpm.eventlog.uefi;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import hirs.utils.HexUtils;
/**
 * Class to process a UEFI Boot#### variable.
 * Data is defined using the EFI_LOAD_OptionStructure:
 * typedef struct _EFI_LOAD_OPTION {
 *   UINT32  Attributes;
 *   UINT16   FilePathListLength;
 *   // CHAR16    Description[];
 *   // EFI_DEVICE_PATH_PROTOCOL  FilePathList[];
 *   // UINT8  OptionalData[];
 * } EFI_LOAD_OPTION;
 *
 * No length field for the Description is given
 * so we need to calculate it by search for a null termination on the Description field
 * Data following the Description should  be an EFI Device Path
 */
public class UefiBootVariable {
  /** Human readable description of the variable. */
  private String description = "";
  /** Variable attributes. */
  private byte[] attributes = null;
  /** Firmware memory blob.*/
  private byte[] blob = null;
  /** UEFI Device Path.*/
  private UefiDevicePath efiDevPath = null;

 /**
   * UefiBootVariable Constructor.
   * @param bootVar byte array holding the boot variable.
   * @throws UnsupportedEncodingException if the data fails to parse.
   */
public UefiBootVariable(final byte[] bootVar) throws UnsupportedEncodingException {
  attributes = new byte[UefiConstants.SIZE_4];
  System.arraycopy(bootVar, 0, attributes, 0, UefiConstants.SIZE_4);
  byte[] blobLen = new byte[UefiConstants.SIZE_2];
  System.arraycopy(bootVar, UefiConstants.OFFSET_4, blobLen, 0, UefiConstants.SIZE_2);
  int blobLength = HexUtils.leReverseInt(blobLen);
  if (blobLength % UefiConstants.SIZE_2 == 0) {
    blob = new byte[blobLength];
  } else {
    blob = new byte[blobLength + 1];
  }
  System.arraycopy(bootVar, UefiConstants.OFFSET_6, blob, 0, blobLength);
  int descLength = getChar16ArrayLength(blob);
  byte[] desc = new byte[descLength * UefiConstants.SIZE_2];
  System.arraycopy(bootVar, UefiConstants.OFFSET_6, desc, 0, descLength * UefiConstants.SIZE_2);
  description = new String(UefiDevicePath.convertChar16tobyteArray(desc), "UTF-8");
  // Data following the Description should be EFI Partition Data (EFI_DEVICE_PATH_PROTOCOL)
  int devPathLength = blobLength;
  int devPathOffset = UefiConstants.OFFSET_6 + descLength;   //attributes+bloblength+desc+length+2
  byte[] devPath = new byte[devPathLength];
  System.arraycopy(bootVar, devPathOffset, devPath, 0, devPathLength);
  efiDevPath = new UefiDevicePath(devPath);
}

/**
 * Returns a string that represents a UEFI boot variable.
 * Some devices have not properly terminated the Description filed with null characters
 * so garbage bytes are appended to the string that we  must strip off.
 * All non-alpha numeric is stripped from the string.
 * @return string that represents a UEFI boot variable.
 */
public String toString() {
  String bootInfo = "";
  String bootvar = description.replaceAll("[^a-zA-Z_0-0\\s]", "");  // remove all non ascii chars
  bootInfo += "Description = " + bootvar + "\n";
  bootInfo += efiDevPath.toString();
  return bootInfo;
}

/**
 * Searches for the first char16 based null character (2 bytes of zeros).
 * Searches in a given byte array and returns the length of data up to that point in bytes.
 * @param data  a byte array to search for the data.
 * @return the length of the data in bytes at the beginning of the byte array.
 * which was terminated by a null character.
 */
public int getChar16ArrayLength(final byte[] data) {
  int count = 0;
  byte[] nullTerminitor = new byte[UefiConstants.SIZE_2];
  byte[] char16 = new byte[UefiConstants.SIZE_2];
  nullTerminitor[0] = 0;
  nullTerminitor[1] = 0;
  for (int i = 0; i < data.length; i = i + UefiConstants.SIZE_2) {
    char16[0] = data[i];
    char16[1] = data[i + 1];
    count++;
    if (Arrays.equals(nullTerminitor, char16)) {
        return count * UefiConstants.SIZE_2;
    }
  }
  return count * UefiConstants.SIZE_2 + 1;
 }
}
