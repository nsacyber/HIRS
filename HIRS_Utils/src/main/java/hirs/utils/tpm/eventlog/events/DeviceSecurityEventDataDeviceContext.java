package hirs.utils.tpm.eventlog.events;

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
}

