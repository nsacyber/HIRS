package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.nio.charset.StandardCharsets;


/**
 * Abstract base class to process the DEVICE_SECURITY_EVENT_DATA or ..DATA2 event.
 * Parses event data for DEVICE_SECURITY_EVENT_DATA per PFP v1.06 Rev52 Table 20.
 * The event data comes in 2 forms:
 *    1) DEVICE_SECURITY_EVENT_DATA or
 *    2) DEVICE_SECURITY_EVENT_DATA2
 * The first 2 fields of the respective headers are the same in both ..DATA and ..DATA2.
 * Field 1:
 *    The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 *    NUL-terminated, per PFP. The only currently defined Signature is "SPDM Device Sec", which
 *    implies the data is a DEVICE_SECURITY_EVENT_DATA or ..DATA2.
 * Field 2:
 *    The Version field indicates whether the Device Security Event is ..DATA or ..DATA2.
 *
 * DEVICE SECURITY EVENT structures defined by PFP v1.06 Rev 52:
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA {
 * DEVICE_SECURITY_EVENT_DATA_HEADER            EventDataHeader;
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT    DeviceContext;
 * } DEVICE_SECURITY_EVENT_DATA;
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA2 {
 * DEVICE_SECURITY_EVENT_DATA_HEADER2           EventDataHeader;
 * DEVICE_SECURITY_EVENT_DATA_SUB_HEADER        EventDataSubHeader;
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT    DeviceContext;
 * } DEVICE_SECURITY_EVENT_DATA;
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER or HEADER2 {
 *      UINT8                           Signature[16];
 *      UINT16                          Version;
 *      ...                             ...
 * }
 * <p>
 * Notes:
 * 1. Has an EventType of EV_EFI_SPDM_FIRMWARE_BLOB (0x800000E1)
 * 2. Event content defined as DEVICE_SECURITY_EVENT_DATA Struct.
 * 3. First 16 bytes of the structure header is an ASCII "SPDM Device Sec"
 * <p>
 * Only a few of the Device Security Event Data events have been implemented as there are many,
 * but only those that were reported using the test devices at hand.
 * Without test patterns, the processing may lead to an un-handled exception.
 * For now, the only test pattern uses ..DeviceContext with PCI only, without USB -> assume only 1
 * even though the spec says both are in the data structure. If it is only 1, though, there's no
 * method to tell them apart.
 */
public abstract class DeviceSecurityEventDataBase {

    /**
     * DeviceSecurityEventDataDeviceContext Object.
     */
    @Getter
    private DeviceSecurityEventDataDeviceContext dsedDeviceContext = null;

    /**
     * DeviceSecurityEventData Default Constructor.
     *
     */
    public DeviceSecurityEventDataBase() {

    }

    public void extractDeviceContext(final byte[] dSEDbytes, int startByte) {

        int deviceContextLength = dSEDbytes.length - startByte;

        // get the device type ID
        byte[] deviceContextBytes = new byte[deviceContextLength];
        System.arraycopy(dSEDbytes, startByte, deviceContextBytes, 0,
                deviceContextLength);
        dsedDeviceContext = new DeviceSecurityEventDataDeviceContext(deviceContextBytes);

    }

}
