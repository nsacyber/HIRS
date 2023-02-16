package hirs.attestationca.portal.utils.tpm.eventlog.uefi;

import hirs.attestationca.portal.utils.HexUtils;
import lombok.Getter;

import java.math.BigInteger;

/**
 * Class to process the PFP defined UEFI_PLATFORM_FIRMWARE_BLOB structure.
 * <p>
 * typedef struct tdUEFI_PLATFORM_FIRMWARE_BLOB {
 * UEFI_PHYSICAL_ADDRESS   BlobBase;
 * UINT64 BlobLength;
 * } UEFI_PLATFORM_FIRMWARE_BLOB;
 */
public class UefiFirmware {
    private boolean bError = false;
    /**
     * byte array holding the firmwares physical address.
     */
    private byte[] physicalAddress = null;
    /**
     * byte array holding the uefi address length.
     */
    private byte[] addressLength = null;
    /**
     * uefi physical address.
     */
    @Getter
    private int physicalBlobAddress = 0;
    /**
     * uefi address length.
     */
    @Getter
    private int blobLength = 0;

    /**
     * UefiFirmware constructor.
     *
     * @param blob byte array holding a Firmware Blob.
     */
    public UefiFirmware(final byte[] blob) {
        if (blob.length != UefiConstants.SIZE_16) {
            bError = true;
        } else {
            physicalAddress = new byte[UefiConstants.SIZE_8];
            addressLength = new byte[UefiConstants.SIZE_8];
            System.arraycopy(blob, 0, physicalAddress, 0, UefiConstants.SIZE_8);
            System.arraycopy(blob, UefiConstants.SIZE_8, addressLength, 0, UefiConstants.SIZE_8);
            byte[] lelength = HexUtils.leReverseByte(addressLength);
            BigInteger bigIntLength = new BigInteger(lelength);
            blobLength = bigIntLength.intValue();
            byte[] leAddress = HexUtils.leReverseByte(physicalAddress);
            BigInteger bigIntAddress = new BigInteger(leAddress);
            physicalBlobAddress = bigIntAddress.intValue();
        }
    }

    /**
     * Returns a description of the firmware blobs location.
     *
     * @return a description of the the firmware blobs location.
     */
    public String toString() {
        StringBuilder blobInfo = new StringBuilder();
        if (!bError) {
            blobInfo.append(String.format("   Platform Firmware Blob Address = %s",
                    Integer.toHexString(physicalBlobAddress)));
            blobInfo.append(String.format(" length = %d", blobLength));
        } else {
            blobInfo.append(" Invalid Firmware Blob event encountered");
        }
        return blobInfo.toString();
    }
}
