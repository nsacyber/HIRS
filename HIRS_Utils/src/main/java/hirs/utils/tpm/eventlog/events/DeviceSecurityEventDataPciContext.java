package hirs.utils.tpm.eventlog.events;


/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT event per PFP.
 * DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT is an SPDM structure which includes the
 * identification of the device, device vendor, subsystem, etc. for a PCI device.
 * <p>
 * typedef struct DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT {
 *      UINT16                          Version;
 *      UINT16                          Length;
 *      UINT16                          VendorId;
 *      UINT16                          DeviceId;
 *      UINT8                           RevisionID;
 *      UINT8                           ClassCode[3];
 *      UINT16                          SubsystemVendorID;
 *      UINT16                          SubsystemID;
 * } DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT;
 * <p>
 */
public class DeviceSecurityEventDataPciContext {
}
