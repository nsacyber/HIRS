package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import hirs.utils.tpm.eventlog.uefi.UefiPartition;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to process the PC Client Firmware profile defined EV_EFI_GPT_EVENT event.
 * The EV_EFI_GPT_EVENT event data contains the UEFI_GPT_DATA  structure as defined in the PFP
 * line 2860:
 * <p>
 * typedef struct {
 * UEFI_PARTITION_TABLE_HEADER UEFIPartitionHeader;   // same as UINT64 for current x86 devices
 * UINT64                      NumberOfPartitions;
 * UEFI_PARTITION_ENTRY        Partitions [NumberOfPartitions];
 * }UEFI_GPT_DATA;
 * <p>
 * The UEFI spec defines the EFI_TABLE_HEADER and EFI_PARTITION_ENTRY
 * <p>
 * * typedef struct {
 * UINT64  Signature;    // A 64-bit signature that identifies the type of table that follows.
 * UINT32  Revision;
 * UINT32  HeaderSize;
 * UINT32  CRC32;
 * UINT32  Reserved;
 * } EFI_TABLE_HEADER;
 * <p>
 * typedef struct {
 * EFI_GUID PartitionTypeGUID;
 * EFI_GUID UniquePartitionGUID;
 * EFI_LBA StartingLBA;      // Same as UINT64.
 * EFI_LBA EndingLBA;
 * UINT64 Attributes;
 * CHAR16 PartitionName[36]; // 36 CHAR16 = 72 Bytes
 * } EFI_PARTITION_ENTRY;
 * <p>
 * EFI_SYSTEM_TABLE_SIGNATURE 0x5453595320494249
 * EFI_BOOT_SERVICES_SIGNATURE 0x56524553544f4f42
 * EFI_RUNTIME_SERVICES_SIGNATURE 0x56524553544e5552
 * <p>
 * UEFI Table 23. Defined GPT Partition Entry - Partition Type GUIDs
 * Unused Entry                             00000000-0000-0000-0000-000000000000
 * EFI System Partition                     C12A7328-F81F-11D2-BA4B-00A0C93EC93B
 * Partition containing a legacy MBR        024DEE41-33E7-11D3-9D69-0008C781F39F
 */
public class EvEfiGptPartition {
    /**
     * Header Size.
     */
    private int headerSize = 0;
    /**
     * Header bytes.
     */
    private byte[] header = new byte[UefiConstants.SIZE_8];
    /**
     * Number of partitions in this event.
     */
    private int numberOfPartitions;
    /**
     * Partition Length.
     */
    private int partitonEntryLength = UefiConstants.SIZE_128;
    /**
     * List of Partitions.
     */
    private List<UefiPartition> partitionList;

    /**
     * GPT Partition Event Type constructor.
     *
     * @param eventDataBytes GPT Event to process
     * @throws java.io.UnsupportedEncodingException if Event Data fails to parse
     */
    public EvEfiGptPartition(final byte[] eventDataBytes) throws UnsupportedEncodingException {
        //byte[] eventDataBytes = event.getEventContent();
        // Process the partition header
        partitionList = new ArrayList<>();
        System.arraycopy(eventDataBytes, 0, header, 0, UefiConstants.SIZE_8);  // Signature
        byte[] revision = new byte[UefiConstants.SIZE_4];
        System.arraycopy(eventDataBytes, UefiConstants.SIZE_8, revision, 0, UefiConstants.SIZE_4);
        byte[] hsize = new byte[UefiConstants.SIZE_4];
        System.arraycopy(eventDataBytes, UefiConstants.SIZE_12, hsize, 0, UefiConstants.SIZE_4);
        headerSize = getIntFromBytes(hsize);
        byte[] partitions = new byte[UefiConstants.SIZE_8];
        System.arraycopy(eventDataBytes, headerSize, partitions, 0, UefiConstants.SIZE_8);
        numberOfPartitions = getIntFromBytes(partitions);
        int partitionLength = numberOfPartitions * partitonEntryLength;
        byte[] partitionEntries = new byte[partitionLength];
        System.arraycopy(eventDataBytes, headerSize + UefiConstants.SIZE_8, partitionEntries,
                0, partitionLength);
        processesPartitions(partitionEntries, numberOfPartitions);
        // Mystery Structure get processed here (skipped for now), still part of the header
    }

    /**
     * Processes an individual GPT partition entry.
     *
     * @param partitions           byte array holding partition data.
     * @param numOfPartitions number of partitions included in the data.
     * @throws java.io.UnsupportedEncodingException if partition data fails to parse.
     */
    private void processesPartitions(final byte[] partitions, final int numOfPartitions)
            throws UnsupportedEncodingException {
        byte[] partitionData = new byte[UefiConstants.SIZE_128];
        for (int i = 0; i < numOfPartitions; i++) {
            System.arraycopy(partitions, i * partitonEntryLength, partitionData, 0,
                    partitonEntryLength);
            partitionList.add(new UefiPartition(partitionData));
        }
    }

    /**
     * Provides a human readable string describing the GPT Partition information.
     *
     * @return a human readable string holding the partition information.
     */
    public String toString() {
        String headerStr = HexUtils.byteArrayToHexString(header);
        StringBuilder partitionInfo = new StringBuilder();
        partitionInfo.append("GPT Header Signature = " + headerStr + " : Number of Partitions = "
                + numberOfPartitions + "\n");
        for (int i = 0; i < numberOfPartitions; i++) {
            if (i > 0) {
                partitionInfo.append("\n");
            }
            partitionInfo.append("  Partition " + i + " information\n");
            partitionInfo.append(partitionList.get(i).toString());
        }
        return partitionInfo.toString();
    }

    /**
     * Helper method for converting little Endian byte arrays into Big Endian integers.
     *
     * @param data data to convert.
     * @return an integer.
     */
    public int getIntFromBytes(final byte[] data) {
        byte[] bigEndData = HexUtils.leReverseByte(data);
        BigInteger bigInt = new BigInteger(bigEndData);
        return bigInt.intValue();
    }
}
