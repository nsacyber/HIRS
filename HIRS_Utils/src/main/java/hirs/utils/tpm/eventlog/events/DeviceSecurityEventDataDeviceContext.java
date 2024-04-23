package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.spdm.SpdmMeasurementBlock;
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
 * } DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT;
 * <p>
 */
public class DeviceSecurityEventDataDeviceContext {

    /**
     * SPDM Measurement Block.
     */
    private DeviceSecurityEventDataPciContext deviceSecurityEventDataPciContext = null;

    /**
     * DeviceSecurityEventDataDeviceContext Constructor.
     *
     * @param dSEDdeviceContextBytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventDataDeviceContext(final byte[] dSEDdeviceContextBytes) {

        byte[] dSEDpciContextLengthBytes = new byte[2];
        System.arraycopy(dSEDdeviceContextBytes, 2, dSEDpciContextLengthBytes, 0, 2);
        int dSEDpciContextLength = HexUtils.leReverseInt(dSEDpciContextLengthBytes);

        byte[] dSEDpciContextBytes = new byte[dSEDpciContextLength];
        System.arraycopy(dSEDdeviceContextBytes, 0, dSEDpciContextBytes, 0, dSEDpciContextLength);
        deviceSecurityEventDataPciContext = new DeviceSecurityEventDataPciContext(dSEDpciContextBytes);

        //TODO add USB context
    }

    /**
     * Returns a human readable description of the data within this structure.
     *
     * @return a description of this structure..
     */
    public String toString() {
        String dSEDdeviceContextInfo = "";

        dSEDdeviceContextInfo += deviceSecurityEventDataPciContext.toString();

        return dSEDdeviceContextInfo;
    }
}

