package hirs.attestationca.portal.utils.tpm.eventlog.uefi;

import hirs.attestationca.portal.utils.HexUtils;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Class to process EFI Partitions for EFI Partition tables defined in UEFI section 5.3.3
 * typedef struct {
 * EFI_GUID PartitionTypeGUID;
 * EFI_GUID UniquePartitionGUID;
 * EFI_LBA StartingLBA;      // Same as UINT64.
 * EFI_LBA EndingLBA;
 * UINT64 Attributes;
 * CHAR16 PartitionName[36]; // 36 CHAR16 = 72 Bytes
 * } EFI_PARTITION_ENTRY;
 * <p>
 * UEFI Table 23. Defined GPT Partition Entry - Partition Type GUIDs (implemented in EFIGui.java)
 * Examples:
 * Unused Entry                             00000000-0000-0000-0000-000000000000
 * EFI System Partition                     C12A7328-F81F-11D2-BA4B-00A0C93EC93B
 * Partition containing a legacy MBR        024DEE41-33E7-11D3-9D69-0008C781F39F
 * Linux filesystem data                    0FC63DAF-8483-4772-8E79-3D69D8477DE4
 * Logical Volume Manager (LVM) partition   E6D6D379-F507-44C2-A23C-238F2A3DF928
 * Plain dm-crypt partition                 7FFEC5C9-2D00-49B7-8941-3EA10A5586B7
 * Root partition (x86-64)                  4F68BCE3-E8CD-4DB1-96E7-FBCAF984B709
 * RAID partition                           A19D880F-05FC-4D3B-A006-743F0F84911E
 * LUKS partition                           CA7D7CCB-63ED-4C53-861C-1742536059CC
 * <p>
 * linux commands to check uuids:
 * blkid list //unique parition guids
 * ls /dev/disk/by-partuuid
 */
@Getter
public class UefiPartition {
    private UefiGuid partitionTypeGUID = null;
    private UefiGuid uniquePartitionGUID = null;
    private String partitionName = "";
    private String attributes = "";

    /**
     * Processes a UEFI defined partition entry.
     *
     * @param table byte array holding the partition table.
     */
    public UefiPartition(final byte[] table) {
        byte[] partitionGuidBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(table, 0, partitionGuidBytes, 0, UefiConstants.SIZE_16);
        partitionTypeGUID = new UefiGuid(partitionGuidBytes);
        byte[] uniquePartGuidBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(table, UefiConstants.SIZE_16, uniquePartGuidBytes, 0, UefiConstants.SIZE_16);
        uniquePartitionGUID = new UefiGuid(uniquePartGuidBytes);
        byte[] attributeBytes = new byte[UefiConstants.SIZE_8];
        System.arraycopy(table, UefiConstants.ATTRIBUTE_LENGTH, attributeBytes,
                0, UefiConstants.SIZE_8);
        attributes = HexUtils.byteArrayToHexString(attributeBytes);
        byte[] partitionNameBytes = new byte[UefiConstants.UEFI_PT_LENGTH];
        System.arraycopy(table, UefiConstants.PART_NAME_LENGTH, partitionNameBytes,
                0, UefiConstants.UEFI_PT_LENGTH);
        byte[] pName = convertChar16tobyteArray(partitionNameBytes);
        partitionName = new String(pName, StandardCharsets.UTF_8).trim();
    }

    /**
     * Returns a description of the partition.
     *
     * @return partition description.
     */
    public String toString() {
        String partitionInfo = "";
        partitionInfo += "     Partition Name        : " + partitionName + "\n";
        partitionInfo += "     Partition Type GUID   : " + partitionTypeGUID.toString() + "\n";
        partitionInfo += "     Unique Partition GUID : " + uniquePartitionGUID.toStringNoLookup() + "\n";
        partitionInfo += "     Attributes            : " + attributes;
        return partitionInfo;
    }

    /**
     * Copies character array to a byte by removing upper byte of character array.
     *
     * @param data input char array
     * @return byte array
     */
    private byte[] convertChar16tobyteArray(final byte[] data) {
        byte[] hexdata = new byte[data.length];
        int j = 0;
        for (int i = 0; i < data.length; i += 2) {
            hexdata[j++] = data[i];
        }
        return hexdata;
    }
}
