package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Class to process the NV_INDEX_INSTANCE_EVENT_LOG_DATA per PFP.
 * Per PFP, the first 16 bytes of the structure are a String based identifier (Signature),
 * which are a NULL-terminated ASCII string "NvIndexInstance".
 * <p>
 * HEADERS defined by PFP v1.06 Rev 52.
 * Certain fields are common to both ..HEADER and ..HEADER2, and are noted below the structures.
 * <p>
 * typedef struct tdNV_INDEX_INSTANCE_EVENT_LOG_DATA {
 * .    BYTE                            Signature[16];
 * .    UINT16                          Version;
 * .    UINT8[6]                        Reserved;
 * .    DEVICE_SECURITY_EVENT_DATA2     Data;
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
    private String nvIndexInstanceInfo = "";

    /**
     * Track status of pci.ids
     * This is only used for events that access the pci.ids file.
     * Default is normal status (normal status is from-filesystem).
     * Status will only change IF this is an event that uses this file,
     * and if that event causes a different status.
     */
    @Getter
    private String pciidsFileStatus = UefiConstants.FILESTATUS_FROM_FILESYSTEM;

    /**
     * NvIndexInstanceEventLogData constructor.
     *
     * @param eventData byte array holding the event to process.
     */
    public NvIndexInstanceEventLogData(final byte[] eventData) {

        final int signatureBytesSize = 16;
        byte[] signatureBytes = new byte[signatureBytesSize];
        System.arraycopy(eventData, 0, signatureBytes, 0, signatureBytesSize);
        signature = new String(signatureBytes, StandardCharsets.UTF_8);
        signature = signature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters

        final int eventDataSrcIndex1 = 16;
        byte[] versionBytes = new byte[2];
        System.arraycopy(eventData, eventDataSrcIndex1, versionBytes, 0, 2);
        String nvIndexVersion = HexUtils.byteArrayToHexString(versionBytes);
        if (nvIndexVersion == "") {
            nvIndexVersion = "version not readable";
        }
        nvIndexInstanceInfo = "   Nv Index Instance Signature = " + signature + "\n";
        nvIndexInstanceInfo += "   Nv Index Instance Version = " + nvIndexVersion + "\n";

        // 6 bytes of Reserved data
        final int eventDataSrcIndex2 = 24;
        final int dsedSignatureBytesSize = 16;
        byte[] dsedSignatureBytes = new byte[dsedSignatureBytesSize];
        System.arraycopy(eventData, eventDataSrcIndex2, dsedSignatureBytes, 0, dsedSignatureBytesSize);
        String dsedSignature = new String(dsedSignatureBytes, StandardCharsets.UTF_8);
        dsedSignature = dsedSignature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters

        final int eventDataSrcIndex3 = 40;
        byte[] dsedVersionBytes = new byte[2];
        System.arraycopy(eventData, eventDataSrcIndex3, dsedVersionBytes, 0, 2);
        String dsedVersion = HexUtils.byteArrayToHexString(dsedVersionBytes);
        if (dsedVersion == "") {
            dsedVersion = "version not readable";
        }

        if (dsedSignature.contains("SPDM Device Sec2")) {

            final int eventDataSrcIndex4 = 24;
            final int dsedEventDataSize = eventData.length - eventDataSrcIndex4;
            byte[] dsedEventData = new byte[dsedEventDataSize];
            System.arraycopy(eventData, eventDataSrcIndex4, dsedEventData, 0, dsedEventDataSize);

            nvIndexInstanceInfo += "   Signature = SPDM Device Sec2\n";

            if (dsedVersion.equals("0200")) {
                dsed = new DeviceSecurityEventData2(dsedEventData);
                nvIndexInstanceInfo += dsed.toString();
                pciidsFileStatus = dsed.getPciidsFileStatus();
            } else {
                nvIndexInstanceInfo += "    Incompatible version for DeviceSecurityEventData2: "
                        + dsedVersion + "\n";
            }
        } else {
            nvIndexInstanceInfo = "   Signature error: should be 'SPDM Device Sec2' but is "
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
