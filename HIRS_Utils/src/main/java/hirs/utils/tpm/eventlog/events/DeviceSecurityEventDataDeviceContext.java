package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import lombok.Getter;

/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT event per PFP.
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT is a common SPDM structure which includes the
 * identification of the device, device vendor, subsystem, etc. Device can be either a PCI
 * or USB connection.
 * <p>
 * typedef union tdDEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT {
 * .     DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT       PciContext;
 * .     DEVICE_SECURITY_EVENT_DATA_USB_CONTEXT       UsbContext;
 * } DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT;
 * <p>
 */
@Getter
public abstract class DeviceSecurityEventDataDeviceContext {

    /**
     * Device Security Event Data Device Type = no device type.
     */
    public static final int DEVICE_TYPE_NONE = 0;
    /**
     * Device Security Event Data Device Type = DEVICE_TYPE_PCI.
     */
    public static final int DEVICE_TYPE_PCI = 1;
    /**
     * Device Security Event Data Device Type = DEVICE_TYPE_USB.
     */
    public static final int DEVICE_TYPE_USB = 2;
    /**
     * PCI Version.
     */
    private int version = 0;
    /**
     * PCI Length.
     */
    private int length = 0;

    /**
     * DeviceSecurityEventDataDeviceContext Constructor.
     *
     * @param dsedDeviceContextBytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventDataDeviceContext(final byte[] dsedDeviceContextBytes) {

        byte[] versionBytes = new byte[2];
        System.arraycopy(dsedDeviceContextBytes, 0, versionBytes, 0, 2);
        version = HexUtils.leReverseInt(versionBytes);

        byte[] lengthBytes = new byte[2];
        System.arraycopy(dsedDeviceContextBytes, 2, lengthBytes, 0, 2);
        length = HexUtils.leReverseInt(lengthBytes);
    }

    /**
     * Returns a human-readable description of the data common to device context structures.
     *
     * @return a description of this structure.
     */
    public String toString() {
        String dSEDdeviceContextCommonInfo = "";

        dSEDdeviceContextCommonInfo += "   DeviceSecurityEventData Device Context:\n";

        return dSEDdeviceContextCommonInfo;
    }
}

