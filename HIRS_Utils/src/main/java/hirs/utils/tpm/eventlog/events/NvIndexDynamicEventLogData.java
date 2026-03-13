package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;

import java.nio.charset.StandardCharsets;

/**
 * Class to process the NV_INDEX_DYNAMIC_EVENT_LOG_DATA per PFP.
 * Per PFP, the first 16 bytes of the structure are a String based identifier (Signature),
 * which are a NULL-terminated ASCII string "NvIndexDynamic".
 * <p>
 * HEADERS defined by PFP v1.06 Rev 52.
 * Certain fields are common to both ..HEADER and ..HEADER2, and are noted below the structures.
 * <p>
 * typedef struct tdNV_INDEX_DYNAMIC_EVENT_LOG_DATA {
 * .     BYTE                            Signature[16];
 * .     UINT16                          Version;
 * .     UINT8[6]                        Reserved;
 * .     UINT64                          UID;
 * .     UINT16                          DescriptionSize;
 * .     UINT8                           Description[DescriptionSize];
 * .     UINT16                          DataSize;
 * .     UINT8                           Data[DataSize];
 * } NV_INDEX_DYNAMIC_EVENT_LOG_DATA;
 */
public class NvIndexDynamicEventLogData {

    /**
     * Signature (text) data.
     */
    private String signature = "";

    /**
     * Human-readable description of the data within this DEVICE_SECURITY_EVENT_DATA/..DATA2 event.
     */
    private String nvIndexDynamicInfo = "";

    /**
     * NvIndexInstanceEventLogData constructor.
     *
     * @param eventData byte array holding the event to process.
     */
    public NvIndexDynamicEventLogData(final byte[] eventData) {

        final int signatureBytesSize = 16;
        byte[] signatureBytes = new byte[signatureBytesSize];
        System.arraycopy(eventData, 0, signatureBytes, 0, signatureBytesSize);
        signature = new String(signatureBytes, StandardCharsets.UTF_8);
        signature = signature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters

        final int versionBytesSize = 2;
        final int eventDataSrcIndex1 = 16;
        byte[] versionBytes = new byte[versionBytesSize];
        System.arraycopy(eventData, eventDataSrcIndex1, versionBytes, 0, versionBytesSize);
        String nvIndexVersion = HexUtils.byteArrayToHexString(versionBytes);
        if (nvIndexVersion.isEmpty()) {
            nvIndexVersion = "version not readable";
        }
        nvIndexDynamicInfo = "   Nv Index Dynamic Signature = " + signature + "\n";
        nvIndexDynamicInfo += "   Nv Index Dynamic Version = " + nvIndexVersion + "\n";

        // 6 bytes of Reserved data

        final int uidBytesSize = 8;
        final int eventDataSrcIndex2 = 24;
        byte[] uidBytes = new byte[uidBytesSize];
        System.arraycopy(eventData, eventDataSrcIndex2, uidBytes, 0, uidBytesSize);
        String uid = HexUtils.byteArrayToHexString(uidBytes);
        nvIndexDynamicInfo += "   UID = " + uid + "\n";

        final int descriptionSizeBytesLength = 2;
        final int eventDataSrcIndex3 = 32;
        byte[] descriptionSizeBytes = new byte[descriptionSizeBytesLength];
        System.arraycopy(eventData, eventDataSrcIndex3, descriptionSizeBytes, 0, descriptionSizeBytesLength);
        int descriptionSize = HexUtils.leReverseInt(descriptionSizeBytes);

        final int eventDataSrcIndex4 = 34;
        byte[] descriptionBytes = new byte[descriptionSize];
        System.arraycopy(eventData, eventDataSrcIndex4, descriptionBytes, 0, descriptionSize);
        String description = new String(descriptionBytes, StandardCharsets.UTF_8);
        description = description.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters
        nvIndexDynamicInfo += "   Description = " + description + "\n";

        final int dataSizeOffset = 34;
        int dataSizeStartByte = dataSizeOffset + descriptionSize;
        byte[] dataSizeBytes = new byte[2];
        System.arraycopy(eventData, dataSizeStartByte, dataSizeBytes, 0, 2);
        int dataSize = HexUtils.leReverseInt(dataSizeBytes);

        int dataStartByte = dataSizeStartByte + 2;
        byte[] dataBytes = new byte[dataSize];
        System.arraycopy(eventData, dataStartByte, dataBytes, 0, dataSize);
        String data = HexUtils.byteArrayToHexString(dataBytes);
        nvIndexDynamicInfo += "   Data = " + data + "\n";
    }

    /**
     * Returns a description of this event.
     *
     * @return Human-readable description of this event.
     */
    public String toString() {
        return nvIndexDynamicInfo;
    }
}
