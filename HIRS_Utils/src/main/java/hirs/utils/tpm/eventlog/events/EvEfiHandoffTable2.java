package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import hirs.utils.tpm.eventlog.uefi.UefiGuid;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Class to process the PC Client Firmware profile defined EV_EFI_HANDOFF_TABLES2 event.
 * The Event data holds a structure called UEFI_HANDOFF_TABLE_POINTERS:
 * <p>
 * typedef struct tdUEFI_HANDOFF_TABLE_POINTERS2 {
 * UINT8 TableDescriptionSize;
 * BYTE[TableDescriptionSize] TableDescription;
 * UINT64 NumberOfTables;
 * UEFI_CONFIGURATION_TABLE TableEntry[NumberOfTables];
 * } UEFI_HANDOFF_TABLE_POINTERS2;
 * <p>
 * The UEFI_CONFIGURATION_TABLE id defined in the UEFI spec as:
 * <p>
 * typedef struct{
 * EFI_GUID            VendorGuid;
 * VOID               *VendorTable;
 * } EFI_CONFIGURATION_TABLE;
 * Where the definitions:
 * VendorGuid: The 128-bit GUID value that uniquely identifies the system configuration table.
 * VendorTable: A pointer to the table associated with VendorGuid.
 * Section 4.6 of the UEFI spec has a listing of some of the industry defined
 * standard that define the particular table.
 */
public class EvEfiHandoffTable2 {

    /**
     * string holding the description.
     */
    private String tableDescription = "";
    /**
     * List of Vendor GUIDs.
     */
    private final ArrayList<UefiGuid> vendorGuids = new ArrayList<>();
    /**
     * List of Vendors.
     */
    private final ArrayList<byte[]> vendorTables = new ArrayList<>();
    /**
     * Number of Tables.
     */
    @Getter
    private int numberOfTables = 0;

    /**
     * EvEFIHandoffTable2 constructor.
     *
     * @param tpmEventData byte array holding the Handoff table data.
     */
    public EvEfiHandoffTable2(final byte[] tpmEventData) {

        int tableDescriptionSize = 0;       // size of description bytes
        int offset = 0;

        // Get description size
        byte[] tableDescriptionSizeBytes = new byte[1];
        System.arraycopy(tpmEventData, offset, tableDescriptionSizeBytes, 0, 1);
        tableDescriptionSize = HexUtils.leReverseInt(tableDescriptionSizeBytes);
        offset += 1;

        // Get description
        byte[] tableDescriptionBytes = new byte[tableDescriptionSize];
        System.arraycopy(tpmEventData, offset, tableDescriptionBytes, 0, tableDescriptionSize);
        tableDescription = new String(tableDescriptionBytes, StandardCharsets.UTF_8);
        offset += tableDescriptionSize;

        // Get NumberOfTables
        byte[] count = new byte[UefiConstants.SIZE_8];
        System.arraycopy(tpmEventData, offset, count, 0, UefiConstants.SIZE_8);
        numberOfTables = HexUtils.leReverseInt(count);
        offset += UefiConstants.OFFSET_8;

        // process each UEFI_CONFIGURATION_TABLE table
        for (int tables = 0; tables < numberOfTables; tables++) {
            vendorGuids.add(getNextGUID(tpmEventData, offset));
            vendorTables.add(getNextTable(tpmEventData, offset + UefiConstants.OFFSET_16));
            offset += UefiConstants.OFFSET_24;
        }
    }

    /**
     * Returns the next GUI in the table.
     *
     * @param eventData byte array holding the guids.
     * @param offset    offset to the guid.
     * @return Vendor Guid
     */
    private UefiGuid getNextGUID(final byte[] eventData, final int offset) {
        byte[] guid = new byte[UefiConstants.SIZE_16];
        System.arraycopy(eventData, offset, guid, 0, UefiConstants.SIZE_16);
        return new UefiGuid(guid);
    }

    /**
     * Copies the next table to a new array.
     *
     * @param eventData byte array holding the next table.
     * @param offset    offset within the table to fond the data.
     * @return a  byte array holding the new table.
     */
    private byte[] getNextTable(final byte[] eventData, final int offset) {
        byte[] table = new byte[UefiConstants.SIZE_8];
        System.arraycopy(eventData, offset, table, 0, UefiConstants.SIZE_8);
        return table;
    }

    /**
     * Returns a human-readable description of the hand off tables.
     *
     * @return a human-readable description.
     */
    public String toString() {
        StringBuilder tableInfo = new StringBuilder();
        tableInfo.append(String.format("   Description: %s%n", tableDescription));
        tableInfo.append(String.format("   Number of UEFI_CONFIGURATION_TABLEs = " + numberOfTables + "%n"));
        for (int i = 0; i < numberOfTables; i++) {
            UefiGuid currentGuid = vendorGuids.get(i);
            tableInfo.append("      Table " + i + ":\n");
            tableInfo.append("        GUID = " + currentGuid.toString() + "\n");
            tableInfo.append("        UEFI industry standard table type = "
                    + currentGuid.getVendorTableReference() + "\n");
            tableInfo.append("        VendorTable " + i + " address: "
                    + HexUtils.byteArrayToHexString(vendorTables.get(i)) + "\n");
        }
        return tableInfo.toString();
    }
}
