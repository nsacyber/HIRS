package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;

import java.nio.charset.StandardCharsets;

/**
 * Class to process the NV_INDEX_INSTANCE_EVENT_LOG_DATA per PFP.
 * Per PFP, the first 16 bytes of the structure are a String based identifier (Signature),
 * which are a NULL-terminated ASCII string "NvIndexInstance".
 *
 * HEADERS defined by PFP v1.06 Rev 52.
 * Certain fields are common to both ..HEADER and ..HEADER2, and are noted below the structures.
 * <p>
 * typedef struct tdNV_INDEX_INSTANCE_EVENT_LOG_DATA {
 *      BYTE                            Signature[16];
 *      UINT16                          Version;
 *      UINT8[6]                        Reserved;
 *      DEVICE_SECURITY_EVENT_DATA2     Data;
 * } NV_INDEX_INSTANCE_EVENT_LOG_DATA;
 * <p>
 */
public class NvIndexInstanceEventLogData {

    /**
     * DeviceSecurityEventData2 Object.
     */
//    private DeviceSecurityEventData2 dsed = null;
    private DeviceSecurityEvent dsed = null;

    /**
     * Signature (text) data.
     */
    private String signature = "";

    /**
     * Human-readable description of the data within this DEVICE_SECURITY_EVENT_DATA/..DATA2 event.
     */
    String nvIndexInstanceInfo = "";

    /**
     * NvIndexInstanceEventLogData constructor.
     *
     * @param eventData byte array holding the event to process.
     */
    public NvIndexInstanceEventLogData(final byte[] eventData) {

        byte[] signatureBytes = new byte[16];
        System.arraycopy(eventData, 0, signatureBytes, 0, 16);
        signature = new String(signatureBytes, StandardCharsets.UTF_8);
        signature = signature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters

        byte[] versionBytes = new byte[2];
        System.arraycopy(eventData, 16, versionBytes, 0, 2);
        String nvIndexVersion = HexUtils.byteArrayToHexString(versionBytes);
        if (nvIndexVersion == "") {
            nvIndexVersion = "version not readable";
        }
        nvIndexInstanceInfo = "   Nv Index Instance Signature = " + signature + "\n";
        nvIndexInstanceInfo += "   Nv Index Instance Version = " + nvIndexVersion + "\n";

        // 6 bytes of Reserved data

        byte[] dsedSignatureBytes = new byte[16];
        System.arraycopy(eventData, 24, dsedSignatureBytes, 0, 16);
        String dsedSignature = new String(dsedSignatureBytes, StandardCharsets.UTF_8);
        dsedSignature = dsedSignature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters

        byte[] dsedVersionBytes = new byte[2];
        System.arraycopy(eventData, 40, dsedVersionBytes, 0, 2);
        String dsedVersion = HexUtils.byteArrayToHexString(dsedVersionBytes);
        if (dsedVersion == "") {
            dsedVersion = "version not readable";
        }

        if (dsedSignature.contains("SPDM Device Sec2")) {

            int dsedEventDataSize = eventData.length - 24;
            byte[] dsedEventData = new byte[dsedEventDataSize];
            System.arraycopy(eventData, 24, dsedEventData, 0, dsedEventDataSize);

            nvIndexInstanceInfo += "   Signature = SPDM Device Sec2\n";

            if (dsedVersion.equals("0200")) {
                dsed = new DeviceSecurityEventData2(dsedEventData);
                nvIndexInstanceInfo += dsed.toString();
            }
            else {
                nvIndexInstanceInfo += "    Incompatible version for DeviceSecurityEventData2: "
                        + dsedVersion + "\n";
            }
        }
        else {
            nvIndexInstanceInfo = "   Signature error: should be \'SPDM Device Sec2\' but is "
                    + signature + "\n";
        }
    }

    /**
     * Returns a description of this event.
     *
     * @return Human-readable description of this event.
     */
    public String toString() {
        return nvIndexInstanceInfo;
    }
}
