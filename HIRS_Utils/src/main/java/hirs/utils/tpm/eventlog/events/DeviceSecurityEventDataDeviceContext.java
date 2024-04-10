package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT event per PFP.
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT is a common SPDM structure which includes the
 * identification of the device, device vendor, subsystem, etc. Device can be either a PCI
 * or USB connection.
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT {
 *      DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT       PciContext;
 *      DEVICE_SECURITY_EVENT_DATA_USB_CONTEXT       UsbContext;
 * } tdDEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT;
 * <p>
 */
public class DeviceSecurityEventDataDeviceContext {

    /**
     * Contains the human-readable info inside the Device Security Event Data Device Context structure.
     */
    @Getter
    private String dSEDdeviceContextInfo = "";
    /**
     * PCI Version.
     */
    @Getter
    private String pciVersion = "";
    /**
     * PCI Length.
     */
    @Getter
    private String pciLength = "";

    /**
     * DeviceSecurityEventDataDeviceContext Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventDataDeviceContext(final byte[] dSEDbytes, int byteStartOffset) {

        int byteOffset = byteStartOffset;

        byte[] pciVersionBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(dSEDbytes, byteOffset, pciVersionBytes, 0, UefiConstants.SIZE_16);
        pciVersion = new String(pciVersionBytes, StandardCharsets.UTF_8)
                .substring(0, UefiConstants.SIZE_15);

        byteOffset += UefiConstants.SIZE_16;
        byte[] pciLengthBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(dSEDbytes, byteOffset, pciLengthBytes, 0,
                UefiConstants.SIZE_16);
        pciLength = HexUtils.byteArrayToHexString(pciLengthBytes);


    }
}

