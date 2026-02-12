package hirs.utils.tpm.eventlog.events;

import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base class to process the DEVICE_SECURITY_EVENT_DATA or ..DATA2 event.
 * Parses event data per PFP v1.06 Rev52 Tables 20 and 26.
 * The event data comes in 2 forms:
 * 1) DEVICE_SECURITY_EVENT_DATA or
 * 2) DEVICE_SECURITY_EVENT_DATA2
 * <p>
 * The first 2 fields of the respective headers are the same in both ..DATA and ..DATA2.
 * <p>
 * Field 1:
 * The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 * per PFP. The only currently defined Signatures are "SPDM Device Sec" and "SPDM Device Sec2",
 * which implies the data is a DEVICE_SECURITY_EVENT_DATA or ..DATA2, respectively.
 * <p>
 * Field 2:
 * The Version field also indicates whether the Device Security Event is ..DATA or ..DATA2.
 * <p>
 * DEVICE SECURITY EVENT structures defined by PFP v1.06 Rev 52:
 *
 * <pre>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA {
 * .     DEVICE_SECURITY_EVENT_DATA_HEADER            EventDataHeader;
 * .     DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT    DeviceContext;
 * } DEVICE_SECURITY_EVENT_DATA;
 * </pre>
 *
 * <pre>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA2 {
 * .     DEVICE_SECURITY_EVENT_DATA_HEADER2           EventDataHeader;
 * .     DEVICE_SECURITY_EVENT_DATA_SUB_HEADER        EventDataSubHeader;
 * .     DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT    DeviceContext;
 * } DEVICE_SECURITY_EVENT_DATA2;
 * </pre>
 *
 * <pre>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER or HEADER2 {
 * .     UINT8                           Signature[16];
 * .     UINT16                          Version;
 * .     ...                             ...
 * .     (The rest of the components are different for HEADER vs HEADER2)
 * }
 * </pre>
 */
@Getter
public abstract class DeviceSecurityEvent {

    /**
     * DeviceSecurityEventDataContext Object.
     */
    private DeviceSecurityEventDataPciContext dsedPciContext = null;

    /**
     * Device type.
     */
    @Setter
    private int deviceType = -1;

    /**
     * Human-readable description of the data within the
     * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT. DEVICE can be either PCI or USB.
     */
    private String deviceContextInfo = "";

    /**
     * Track status of pci.ids
     * This is only used for events that access the pci.ids file.
     * (In this class, this is only needed if DeviceSecurityEvent includes
     * a DeviceSecurityEventDataPciContext)
     * Default is normal status (normal status is from-filesystem).
     * Status will only change IF this is an event that uses this file,
     * and if that event causes a different status.
     */
    private String pciidsFileStatus = UefiConstants.FILESTATUS_FROM_FILESYSTEM;

    /**
     * DeviceSecurityEventData Default Constructor.
     */
    public DeviceSecurityEvent() {

    }

    /**
     * Parse the Device Context structure, can be PCI or USB based on device type field.
     *
     * @param dsedDeviceContextBytes byte array holding the DeviceSecurityEventData.
     */
    public void instantiateDeviceContext(final byte[] dsedDeviceContextBytes) {

        if (dsedDeviceContextBytes.length == 0) {
            deviceContextInfo = "\n    DeviceSecurityEventDataDeviceContext object is empty";
        } else {
            if (deviceType == DeviceSecurityEventDataDeviceContext.DEVICE_TYPE_NONE) {
                deviceContextInfo = "\n    No Device Context (indicated by device type value of 0)";
            } else if (deviceType == DeviceSecurityEventDataDeviceContext.DEVICE_TYPE_PCI) {
                dsedPciContext = new DeviceSecurityEventDataPciContext(dsedDeviceContextBytes);
                deviceContextInfo = dsedPciContext.toString();
                // getPciidsFileStatus() must be called after DeviceSecurityEventDataPciContext.toString(),
                // because the toString function is where the pciids db gets set up and used
                pciidsFileStatus = dsedPciContext.getPciidsFileStatus();
            } else if (deviceType == DeviceSecurityEventDataDeviceContext.DEVICE_TYPE_USB) {
                deviceContextInfo = "    Device Type: USB - To be implemented";
            } else {
                deviceContextInfo = "    Unknown device type; cannot process device context";
            }
        }
    }
}
