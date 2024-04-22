package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.spdm.SpdmHa;
import lombok.Getter;

public class DeviceSecurityEventDataPciContext {

    /**
     * PCI Version.
     */
    @Getter
    private int pciVersion = 0;
    /**
     * PCI Length.
     */
    @Getter
    private int pciLength = 0;
    /**
     * PCI Vendor ID.
     */
    @Getter
    private int pciVendorId = 0;
    /**
     * PCI Device ID.
     */
    @Getter
    private int pciDeviceId = 0;
    /**
     * PCI Revision ID.
     */
    @Getter
    private int pciRevisionId = 0;
    /**
     * PCI Class Code.
     */
    @Getter
    private int pciClassCode = 0;
    /**
     * PCI Subsystem Vendor ID.
     */
    @Getter
    private int pciSubsystemVendorId = 0;
    /**
     * PCI Subsystem ID.
     */
    @Getter
    private int pciSubsystemId = 0;

    /**
     * DeviceSecurityEventDataPciContext Constructor.
     *
     * @param dSEDpciContextBytes byte array holding the DeviceSecurityEventDataPciContext.
     */
    public DeviceSecurityEventDataPciContext(final byte[] dSEDpciContextBytes) {

        byte[] pciVersionBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 0, pciVersionBytes, 0, 2);
        pciVersion = HexUtils.leReverseInt(pciVersionBytes);

        byte[] pciLengthBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 2, pciLengthBytes, 0, 2);
        pciLength = HexUtils.leReverseInt(pciLengthBytes);

        byte[] pciVendorIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 4, pciVendorIdBytes, 0, 2);
        pciVendorId = HexUtils.leReverseInt(pciVendorIdBytes);

        byte[] pciDeviceIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 6, pciDeviceIdBytes, 0, 2);
        pciDeviceId = HexUtils.leReverseInt(pciDeviceIdBytes);

        byte[] pciRevisionIdBytes = new byte[1];
        System.arraycopy(dSEDpciContextBytes, 8, pciRevisionIdBytes, 0, 1);
        pciRevisionId = HexUtils.leReverseInt(pciRevisionIdBytes);

        byte[] pciClassCodeBytes = new byte[3];
        System.arraycopy(dSEDpciContextBytes, 9, pciClassCodeBytes, 0, 3);
        pciClassCode = HexUtils.leReverseInt(pciClassCodeBytes);

        byte[] pciSubsystemVendorIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 12, pciSubsystemVendorIdBytes, 0, 2);
        pciSubsystemVendorId = HexUtils.leReverseInt(pciSubsystemVendorIdBytes);

        byte[] pciSubsystemIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 14, pciSubsystemIdBytes, 0, 2);
        pciSubsystemId = HexUtils.leReverseInt(pciSubsystemIdBytes);

    }

    /**
     * Returns a human readable description of the data within this structure.
     *
     * @return a description of this structure..
     */
    public String toString() {
        String dSEDpciContextInfo = "";

        dSEDpciContextInfo += "\n   DeviceSecurityEventData - PCI Context";
        dSEDpciContextInfo += "\n      Version = " + pciVersion;
        dSEDpciContextInfo += "\n      Length = " + pciLength;
        dSEDpciContextInfo += "\n      VendorID = " + pciVendorId;
        dSEDpciContextInfo += "\n      DeviceID = " + pciDeviceId;
        dSEDpciContextInfo += "\n      RevisionID = " + pciRevisionId;
        dSEDpciContextInfo += "\n      ClassCode = " + pciClassCode;
        dSEDpciContextInfo += "\n      SubsystemVendorID = " + pciSubsystemVendorId;
        dSEDpciContextInfo += "\n      SubsystemID = " + pciSubsystemId;

        return dSEDpciContextInfo;
    }
}
