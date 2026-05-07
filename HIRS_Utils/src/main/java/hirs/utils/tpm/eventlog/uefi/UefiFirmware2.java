package hirs.utils.tpm.eventlog.uefi;

import hirs.utils.HexUtils;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Class to process the PFP defined UEFI_PLATFORM_FIRMWARE_BLOB2 structure.
 * <p>
 * typedef struct tdUEFI_PLATFORM_FIRMWARE_BLOB2 {
 * UINT8 BlobDescriptionSize;
 * BYTE[BlobDescriptionSize] BlobDescription;
 * EFI_PHYSICAL_ADDRESS BlobBase;
 * UINT64 BlobLength;
 * } UEFI_PLATFORM_FIRMWARE_BLOB2;
 */
public class UefiFirmware2 {

    /**
     * true if any errors occur.
     */
    private boolean bError = false;
    /**
     * string holding the description.
     */
    private String blobDescription = "";
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
     * UefiFirmware2 constructor.
     *
     * @param blob byte array holding a Firmware Blob.
     */
    public UefiFirmware2(final byte[] blob) {

        int blobDescriptionSize = 0;                                    // size of description bytes
        byte[] physicalAddressBytes = new byte[UefiConstants.SIZE_8];   // firmware's physical address
        byte[] addressLenBytes = new byte[UefiConstants.SIZE_8];        // uefi address length
        int offset = 0;

        // Get description size
        if (blob.length >= (1 + UefiConstants.SIZE_16)) {
            byte[] blobDescriptionSizeBytes = new byte[1];
            System.arraycopy(blob, offset, blobDescriptionSizeBytes, 0, 1);
            blobDescriptionSize = HexUtils.leReverseInt(blobDescriptionSizeBytes);
            offset += 1;
        }

        if (blob.length != (1 + blobDescriptionSize + UefiConstants.SIZE_16)) {
            bError = true;
        } else {
            // Get description
            byte[] blobDescriptionBytes = new byte[blobDescriptionSize];
            System.arraycopy(blob, offset, blobDescriptionBytes, 0, blobDescriptionSize);
            blobDescription = new String(blobDescriptionBytes, StandardCharsets.UTF_8);
            offset += blobDescriptionSize;

            // Get physical address
            System.arraycopy(blob, offset, physicalAddressBytes, 0, UefiConstants.SIZE_8);
            physicalBlobAddress = HexUtils.leReverseInt(physicalAddressBytes);
            offset += UefiConstants.SIZE_8;

            // Get address length
            System.arraycopy(blob, offset, addressLenBytes, 0, UefiConstants.SIZE_8);
            blobLength = HexUtils.leReverseInt(addressLenBytes);
        }
    }

    /**
     * Returns a description of the firmware blob's location.
     *
     * @return a description of the firmware blob's location.
     */
    public String toString() {
        StringBuilder blobInfo = new StringBuilder();
        if (!bError) {
            blobInfo.append(String.format("   Event: %s%n", UefiConstants.UEFI_FIRMWARE_BLOB2_LABEL));
            blobInfo.append(String.format("   Description: %s%n", blobDescription));
            blobInfo.append(String.format("   Address = 0x%s, Length = %d",
                    Integer.toHexString(physicalBlobAddress), blobLength));
        } else {
            blobInfo.append("    Invalid Firmware Blob 2 event encountered");
        }
        return blobInfo.toString();
    }
}
