package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;

import java.nio.charset.StandardCharsets;

/**
 * Class to process the NV_INDEX_DYNAMIC_EVENT_LOG_DATA per PFP.
 * Per PFP, the first 16 bytes of the structure are a String based identifier (Signature),
 * which are a NULL-terminated ASCII string "NvIndexInstance".
 *
 * HEADERS defined by PFP v1.06 Rev 52.
 * Certain fields are common to both ..HEADER and ..HEADER2, and are noted below the structures.
 * <p>
 * typedef struct tdNV_INDEX_DYNAMIC_EVENT_LOG_DATA {
 *      BYTE                            Signature[16];
 *      UINT16                          Version;
 *      UINT8[6]                        Reserved;
 *      UINT64                          UID;
 *      UINT16                          DescriptionSize;
 *      UINT8                           Description[DescriptionSize];
 *      UINT16                          DataSize;
 *      DEVICE_SECURITY_EVENT_DATA2     Data[DataSize];
 * } NV_INDEX_DYNAMIC_EVENT_LOG_DATA;
 * <p>
 */
public class NvIndexDynamicEventLogData {

    /**
     * Signature (text) data.
     */
    private String signature = "";

    /**
     * Human-readable description of the data within this DEVICE_SECURITY_EVENT_DATA/..DATA2 event.
     */
    String nvIndexDynamicInfo = "";

    /**
     * NvIndexInstanceEventLogData constructor.
     *
     * @param eventData byte array holding the event to process.
     */
    public NvIndexDynamicEventLogData(final byte[] eventData) {

        byte[] signatureBytes = new byte[16];
        System.arraycopy(eventData, 0, signatureBytes, 0, 16);
        signature = new String(signatureBytes, StandardCharsets.UTF_8);
        signature = signature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters

        byte[] versionBytes = new byte[2];
        System.arraycopy(eventData, 16, versionBytes, 0, 2);
        String nvIndexVersion = HexUtils.byteArrayToHexString(versionBytes);
        if (nvIndexVersion.isEmpty()) {
            nvIndexVersion = "version not readable";
        }
        nvIndexDynamicInfo = "   Nv Index Dynamic Signature = " + signature + "\n";
        nvIndexDynamicInfo += "   Nv Index Dynamic Version = " + nvIndexVersion + "\n";

        // 6 bytes of Reserved data

        byte[] uidBytes = new byte[8];
        System.arraycopy(eventData, 24, uidBytes, 0, 8);
        String uid = HexUtils.byteArrayToHexString(uidBytes);
        nvIndexDynamicInfo += "   UID = " + uid + "\n";

        byte[] descriptionSizeBytes = new byte[2];
        System.arraycopy(eventData, 32, descriptionSizeBytes, 0, 2);
        int descriptionSize = HexUtils.leReverseInt(descriptionSizeBytes);

        byte[] descriptionBytes = new byte[descriptionSize];
        System.arraycopy(eventData, 34, descriptionBytes, 0, descriptionSize);
        String description = new String(descriptionBytes, StandardCharsets.UTF_8);
        description = description.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters
        nvIndexDynamicInfo += "   Description = " + description + "\n";

        int dataSizeStartByte = 34 + descriptionSize;
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
