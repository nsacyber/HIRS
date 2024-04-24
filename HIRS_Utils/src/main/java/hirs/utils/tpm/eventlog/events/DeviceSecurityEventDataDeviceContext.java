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
 *      DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT       PciContext;
 *      DEVICE_SECURITY_EVENT_DATA_USB_CONTEXT       UsbContext;
 * } DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT;
 * <p>
 */
public abstract class DeviceSecurityEventDataDeviceContext {

    /**
     * PCI Version.
     */
    @Getter
    private int version = 0;
    /**
     * PCI Length.
     */
    @Getter
    private int length = 0;

    /**
     * DeviceSecurityEventDataDeviceContext Constructor.
     *
     * @param dSEDdeviceContextBytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventDataDeviceContext(final byte[] dSEDdeviceContextBytes) {

        byte[] pciVersionBytes = new byte[2];
        System.arraycopy(dSEDdeviceContextBytes, 0, pciVersionBytes, 0, 2);
        version = HexUtils.leReverseInt(pciVersionBytes);

        byte[] pciLengthBytes = new byte[2];
        System.arraycopy(dSEDdeviceContextBytes, 2, pciLengthBytes, 0, 2);
        length = HexUtils.leReverseInt(pciLengthBytes);
    }

    /**
     * Returns a human readable description of the data common to device context structures.
     *
     * @return a description of this structure..
     */
    public String deviceContextCommonInfoToString() {
        String dSEDdeviceContextCommonInfo = "";

        dSEDdeviceContextCommonInfo += "\n   DeviceSecurityEventData - Device Info";
        dSEDdeviceContextCommonInfo += "\n      Device Structure Version = " + version;

        return dSEDdeviceContextCommonInfo;
    }

}

