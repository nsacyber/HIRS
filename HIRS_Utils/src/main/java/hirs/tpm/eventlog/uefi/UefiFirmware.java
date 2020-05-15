package hirs.tpm.eventlog.uefi;

import java.math.BigInteger;
import hirs.utils.HexUtils;

/**
 * Class to process the PFP defined UEFI_PLATFORM_FIRMWARE_BLOB structure.
 *
 * typedef struct tdUEFI_PLATFORM_FIRMWARE_BLOB {
 *  UEFI_PHYSICAL_ADDRESS   BlobBase;
 *  UINT64 BlobLength;
 * } UEFI_PLATFORM_FIRMWARE_BLOB;
 */
public class UefiFirmware {
  private boolean berror = false;
  /** byte array holding the firmwares physical address. */
  private byte[] physicalAddress = null;
  /** byte array holding the uefi address length. */
  private byte[] addressLength = null;
  /** uefi physical address. */
  private int blobAddress = 0;
  /** uefi address length. */
  private int blobLength = 0;

  /**
   * UefiFirmware constructor.
   * @param blob byte array holding a Firmware Blob.
   */
  public UefiFirmware(final byte[] blob) {
    if (blob.length != UefiConstants.SIZE_16) {
        berror = true;
     } else {
        physicalAddress = new byte[UefiConstants.SIZE_8];
        addressLength = new byte[UefiConstants.SIZE_8];
        System.arraycopy(blob, 0, physicalAddress, 0, UefiConstants.SIZE_8);
        System.arraycopy(blob, UefiConstants.SIZE_8, addressLength, 0, UefiConstants.SIZE_8);
        byte[] lelength = HexUtils.leReverseByte(addressLength);
        BigInteger bigIntLength = new BigInteger(lelength);
        blobLength = bigIntLength.intValue();
        byte[]leAddress = HexUtils.leReverseByte(physicalAddress);
        BigInteger bigIntAddress = new BigInteger(leAddress);
        blobAddress = bigIntAddress.intValue();
      }
}
/**
 * Returns the uefi firmware blobs physical address.
 * @return uefi firmware address.
 */
public int getPhysicalAddress() {
   return blobAddress;
 }
/**
 * Returns the length of the blobs physical address.
 * @return length of the address.
 */
public int getBlobLength() {
    return blobLength;
}
/**
 *  Returns a description of the firmware blobs location.
 *  @return a description of the the firmware blobs location.
 */
public String toString() {
     String blobInfo = "";
     if (!berror) {
         blobInfo += "   Platform Firwmare Blob Address = " + blobAddress;
         blobInfo += " length = " + blobLength;
      } else {
         blobInfo += " Invalid Firmware Blob event encountered";
      }
    return blobInfo;
 }
}
