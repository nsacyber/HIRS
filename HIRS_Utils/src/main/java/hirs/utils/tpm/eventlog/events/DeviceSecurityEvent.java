package hirs.utils.tpm.eventlog.events;

import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base class to process the DEVICE_SECURITY_EVENT_DATA or ..DATA2 event.
 * Parses event data per PFP v1.06 Rev52 Tables 20 and 26.
 * The event data comes in 2 forms:
 *    1) DEVICE_SECURITY_EVENT_DATA or
 *    2) DEVICE_SECURITY_EVENT_DATA2
 * The first 2 fields of the respective headers are the same in both ..DATA and ..DATA2.
 * Field 1:
 *    The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 *    per PFP. The only currently defined Signatures are "SPDM Device Sec" and "SPDM Device Sec2",
 *    which implies the data is a DEVICE_SECURITY_EVENT_DATA or ..DATA2, respectively.
 * Field 2:
 *    The Version field also indicates whether the Device Security Event is ..DATA or ..DATA2.
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
 *      (The rest of the components are different for HEADER vs HEADER2)
 * }
 * <p>
 */
public abstract class DeviceSecurityEvent {

    /**
     * DeviceSecurityEventDataContext Object.
     */
    @Getter
    private DeviceSecurityEventDataDeviceContext dsedDevContext = null;

    /**
     * Device type.
     */
    @Getter
    @Setter
    private int deviceType = -1;

    /**
     * Human-readable description of the data within the
     * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT. DEVICE can be either PCI or USB.
     */
    @Getter
    private String deviceContextInfo = "";

    /**
     * DeviceSecurityEventData Default Constructor.
     *
     */
    public DeviceSecurityEvent() {

    }

    /**
     * Parse the Device Context structure, can be PCI or USB based on device type field.
     *
     * @param dsedDeviceContextBytes byte array holding the DeviceSecurityEventData.
     *
     */
    public void instantiateDeviceContext(final byte[] dsedDeviceContextBytes) {

        if (dsedDeviceContextBytes.length == 0) {
            deviceContextInfo = "\n    DeviceSecurityEventDataDeviceContext object is empty";
        } else {
            if (deviceType == DeviceSecurityEventDataDeviceContext.DEVICE_TYPE_NONE) {
                deviceContextInfo = "\n    No Device Context (indicated by device type value of 0)";
            } else if (deviceType == DeviceSecurityEventDataDeviceContext.DEVICE_TYPE_PCI) {
                dsedDevContext = new DeviceSecurityEventDataPciContext(dsedDeviceContextBytes);
                deviceContextInfo = dsedDevContext.toString();
            } else if (deviceType == DeviceSecurityEventDataDeviceContext.DEVICE_TYPE_USB) {
                deviceContextInfo = "    Device Type: USB - To be implemented";
            } else {
                deviceContextInfo = "    Unknown device type; cannot process device context";
            }
        }
    }
}
