package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import lombok.Getter;

import java.util.List;

import static hirs.utils.PciIds.translateDevice;
import static hirs.utils.PciIds.translateDeviceClass;
import static hirs.utils.PciIds.translateVendor;

/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT event per PFP.
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT {
 *      UINT16       Version;
 *      UINT16       Length;
 *      UINT16       VendorId;
 *      UINT16       DeviceId;
 *      UINT16       RevisionId;
 *      UINT16       ClassCode[3];
 *      UINT16       SubsystemVendorId;
 *      UINT16       SubsystemId;
 * <p>
 * The following fields are defined by the PCI Express Base Specification rev4.0 v1.0.
 *    VendorId
 *    DeviceId
 *    RevisionId
 *    ClassCode
 *    SubsystemVendorId
 *    SubsystemId
 * Vendor id and device id are registered to specific manufacturers.
 *    https://admin.pci-ids.ucw.cz/read/PC/
 *    Ex. vendor id 8086 and device id 0b60: https://admin.pci-ids.ucw.cz/read/PC/8086/0b60
 * Class code can be looked up on the web.
 *    https://admin.pci-ids.ucw.cz/read/PD/
 * The revision ID is controlled by the vendor and cannot be looked up.
 */
public class DeviceSecurityEventDataPciContext extends DeviceSecurityEventDataDeviceContext {

    /**
     * PCI Vendor ID.
     */
    @Getter
    private String vendorId = "";
    /**
     * PCI Device ID.
     */
    @Getter
    private String deviceId = "";
    /**
     * PCI Revision ID.
     */
    @Getter
    private String revisionId = "";
    /**
     * PCI Class Code.
     */
    @Getter
    private String classCode = "";
    /**
     * PCI Subsystem Vendor ID.
     */
    @Getter
    private String subsystemVendorId = "";
    /**
     * PCI Subsystem ID.
     */
    @Getter
    private String subsystemId = "";

    /**
     * DeviceSecurityEventDataPciContext Constructor.
     *
     * @param dSEDpciContextBytes byte array holding the DeviceSecurityEventDataPciContext.
     */
    public DeviceSecurityEventDataPciContext(final byte[] dSEDpciContextBytes) {

        super(dSEDpciContextBytes);

        byte[] pciVendorIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 4, pciVendorIdBytes, 0, 2);
        vendorId = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciVendorIdBytes));

        byte[] pciDeviceIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 6, pciDeviceIdBytes, 0, 2);
        deviceId = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciDeviceIdBytes));

        byte[] pciRevisionIdBytes = new byte[1];
        System.arraycopy(dSEDpciContextBytes, 8, pciRevisionIdBytes, 0, 1);
        revisionId = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciRevisionIdBytes));

        byte[] pciClassCodeBytes = new byte[3];
        System.arraycopy(dSEDpciContextBytes, 9, pciClassCodeBytes, 0, 3);
        classCode = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciClassCodeBytes));

        byte[] pciSubsystemVendorIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 12, pciSubsystemVendorIdBytes, 0, 2);
        subsystemVendorId = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciSubsystemVendorIdBytes));

        byte[] pciSubsystemIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 14, pciSubsystemIdBytes, 0, 2);
        subsystemId = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciSubsystemIdBytes));
    }

    /**
     * Returns a human-readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {
        String dSEDpciContextInfo = "";

        dSEDpciContextInfo += super.toString();
        dSEDpciContextInfo += "      Device Type = PCI\n";
        dSEDpciContextInfo += "      Vendor = " + translateVendor(vendorId) + "\n";
        dSEDpciContextInfo += "      Device = " + translateDevice(vendorId, deviceId) + "\n";
        dSEDpciContextInfo += "      RevisionID = " + revisionId + "\n";

        List<String> classCodeList = translateDeviceClass(classCode);
        dSEDpciContextInfo += "      Device Class: \n";
        if (classCodeList.size() == 3) {
            dSEDpciContextInfo += "        Class = " + classCodeList.get(0) + "\n";
            dSEDpciContextInfo += "        Subclass = " + classCodeList.get(1) + "\n";
            dSEDpciContextInfo += "        Programming Interface = " + classCodeList.get(2) + "\n";
        } else {
            dSEDpciContextInfo += " ** Class code could not be determined **";
        }
        dSEDpciContextInfo += "      SubsystemVendor = " + translateVendor(subsystemVendorId) + "\n";
        dSEDpciContextInfo += "      Subsystem = " + translateDevice(subsystemVendorId, subsystemId) + "\n";

        return dSEDpciContextInfo;
    }
}
