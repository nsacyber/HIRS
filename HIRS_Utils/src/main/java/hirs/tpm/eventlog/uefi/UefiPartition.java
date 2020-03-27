package hirs.tpm.eventlog.uefi;

import java.io.UnsupportedEncodingException;

import hirs.utils.HexUtils;
/**
 * Class to process EFI Partitions for EFI Partition tables defined in UEFI section 5.3.3
 * typedef struct {
 *     EFI_GUID PartitionTypeGUID;
 *     EFI_GUID UniquePartitionGUID;
 *     EFI_LBA StartingLBA;      // Same as UINT64.
 *     EFI_LBA EndingLBA;
 *     UINT64 Attributes;
 *     CHAR16 PartitionName[36]; // 36 CHAR16 = 72 Bytes
 *  } EFI_PARTITION_ENTRY;
 *
 *   UEFI Table 23. Defined GPT Partition Entry - Partition Type GUIDs (implemented in EFIGui.java)
 *   Examples:
 *   Unused Entry                             00000000-0000-0000-0000-000000000000
 *   EFI System Partition                     C12A7328-F81F-11D2-BA4B-00A0C93EC93B
 *   Partition containing a legacy MBR        024DEE41-33E7-11D3-9D69-0008C781F39F
 *   Linux filesystem data                    0FC63DAF-8483-4772-8E79-3D69D8477DE4
 *   Logical Volume Manager (LVM) partition   E6D6D379-F507-44C2-A23C-238F2A3DF928
 *   Plain dm-crypt partition                 7FFEC5C9-2D00-49B7-8941-3EA10A5586B7
 *   Root partition (x86-64)                  4F68BCE3-E8CD-4DB1-96E7-FBCAF984B709
 *   RAID partition                           A19D880F-05FC-4D3B-A006-743F0F84911E
 *   LUKS partition                           CA7D7CCB-63ED-4C53-861C-1742536059CC
 *
 *   linux commands to check uuids:
 *       blkid list //unique parition guids
 *       ls /dev/disk/by-partuuid
 */
public class UefiPartition {
  private UefiGuid partitionTypeGUID = null;
  private UefiGuid uniquePartitionGUID = null;
  private String partitionName = "";
  private String attributes = "";

/**
 * Processes a UEFI defined partition entry.
 * @param table byte array holding the partition table.
 * @throws UnsupportedEncodingException if parsing of the data fails.
 */
  public UefiPartition(final byte[] table) throws UnsupportedEncodingException {
    byte[] partitionGUID = new byte[UefiGuid.getGuidLength()];
    System.arraycopy(table, 0, partitionGUID, 0, UefiGuid.getGuidLength());
    partitionTypeGUID = new UefiGuid(partitionGUID);
    byte[] uniquePartGUID = new byte[UefiGuid.getGuidLength()];
    System.arraycopy(table, UefiGuid.getGuidLength(), uniquePartGUID, 0, UefiGuid.getGuidLength());
    uniquePartitionGUID = new UefiGuid(uniquePartGUID);
    byte[] attribute = new byte[UefiConstants.SIZE_8];
    System.arraycopy(table, UefiConstants.ATTRIBUTE_LENGTH, attribute, 0, UefiConstants.SIZE_8);
    attributes = HexUtils.byteArrayToHexString(attribute);
    byte[] partitionname = new byte[UefiConstants.UEFI_PT_LENGTH];
    System.arraycopy(table, UefiConstants.PART_NAME_LENGTH, partitionname,
                                                      0, UefiConstants.UEFI_PT_LENGTH);
    byte[] pName = convertChar16tobyteArray(partitionname);
    partitionName = new String(pName, "UTF-8").trim();
   }

/**
 * Returns the partition Type GIUD.
 * @return the partition type GUID.
 */
public UefiGuid getPartitionTypeGUID() {
  return partitionTypeGUID;
}
/**
 * Returns the unique partition GUID.
 * @return the unique partition GUID.
 */
public UefiGuid getUniquePartitionGUID() {
  return uniquePartitionGUID;
}

/**
 * Returns the partition name.
 * @return the partition name.
 */
public String getName() {
  return partitionName;
  }

/**
 * Returns a description of the partition.
 * @return partition description.
 */
public String toString() {
  String partitionInfo = "";
  partitionInfo += "     Partition Name        : " + partitionName + "\n";
  partitionInfo += "     Partition Type GUID   : " + partitionTypeGUID.toString() + "\n";
  partitionInfo += "     Unique Partition GUID : " + uniquePartitionGUID.toStringNoLookup() + "\n";
  partitionInfo += "     Attributes            : " + attributes + "\n";
  return partitionInfo;
 }

/**
 * Copies character array to a byte by removing upper byte of character array.
 * @param data input char array
 * @return byte array
 */
private  byte[] convertChar16tobyteArray(final byte[] data) {
  byte[] hexdata = new byte[data.length];
  int j = 0;
  for (int i = 0; i < data.length; i = i + 2) {
    hexdata[j++] = data[i];
    }
  return hexdata;
 }
}
