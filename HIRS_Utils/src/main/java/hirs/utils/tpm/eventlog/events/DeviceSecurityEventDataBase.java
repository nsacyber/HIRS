package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.nio.charset.StandardCharsets;


/**
 * Abstract base class to process the DEVICE_SECURITY_EVENT_DATA or ..DATA2 event.
 * Parses event data per PFP v1.06 Rev52 Tables 20 and 26.
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
 * } DEVICE_SECURITY_EVENT_DATA2;
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
     * Human readable description of the data within the
     * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT. DEVICE can be either PCI or USB.
     */
    @Getter
    String deviceContextInfo = "";

    /**
     * DeviceSecurityEventData Default Constructor.
     *
     */
    public DeviceSecurityEventDataBase() {

    }

    /**
     * Parse the Device Context structure, can be PCI or USB based on device type field.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData.
     * @param startByte starting byte of the device structure (depends on length of header).
     * @param deviceType device type either PCI or USB.
     *
     */
    public void parseDeviceContext(final byte[] dSEDbytes, int startByte, int deviceType) {

        int deviceContextLength = dSEDbytes.length - startByte;

        // get the device context bytes
        byte[] deviceContextBytes = new byte[deviceContextLength];
        System.arraycopy(dSEDbytes, startByte, deviceContextBytes, 0,
                deviceContextLength);

        if (deviceType == 0) {
            deviceContextInfo = "No Device Context (indicated by device type value of 0";
        }
        else if (deviceType == 1) {
            DeviceSecurityEventDataPciContext dSEDpciContext
                    = new DeviceSecurityEventDataPciContext(deviceContextBytes);
            deviceContextInfo = dSEDpciContext.toString();
        }
        //else if (deviceType == 2) {
            //DeviceSecurityEventDataUsbContext dSEDusbContext
            //        = new DeviceSecurityEventDataUsbContext(deviceContextBytes);
            //deviceContextInfo = dSEDusbContext.toString();
            //deviceContextInfo = "Device type is USB - to be implemented in future";
        //}
        else {
            deviceContextInfo = "    Unknown device type; cannot process device context";
        }
    }
}
