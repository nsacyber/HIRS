package hirs.utils.tpm.eventlog.events;

//import hirs.attestationca.persist.util.PciIds;
import com.google.common.base.Strings;
import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.spdm.SpdmHa;
import lombok.Getter;


import com.github.marandus.pciid.model.Device;
import com.github.marandus.pciid.model.Vendor;
import com.github.marandus.pciid.service.PciIdsDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private String pciVendorId = "";
    /**
     * PCI Device ID.
     */
    @Getter
    private String pciDeviceId = "";
    /**
     * PCI Revision ID.
     */
    @Getter
    private String pciRevisionId = "";
    /**
     * PCI Class Code.
     */
    @Getter
    private String pciClassCode = "";
    /**
     * PCI Subsystem Vendor ID.
     */
    @Getter
    private String pciSubsystemVendorId = "";
    /**
     * PCI Subsystem ID.
     */
    @Getter
    private String pciSubsystemId = "";


    // TODO REMOVE
    public static final List<String> PCI_IDS_PATH =
            Collections.unmodifiableList(new ArrayList<>() {
                private static final long serialVersionUID = 1L;
                {
                    add("/usr/share/hwdata/pci.ids");
                    add("/usr/share/misc/pci.ids");
                    add("/tmp/pci.ids");
                }
            });
    public static final PciIdsDatabase DB = new PciIdsDatabase();
    static {
        if (!DB.isReady()) {
            String dbFile = null;
            for (final String path : PCI_IDS_PATH) {
                if ((new File(path)).exists()) {
//                    log.info("PCI IDs file was found {}", path);
                    dbFile = path;
                    break;
                }
            }
            if (dbFile != null) {
                InputStream is = null;
                try {
                    is = new FileInputStream(new File(dbFile));
                    DB.loadStream(is);
                } catch (IOException e) {
                    // DB will not be ready, hardware IDs will not be translated
                    dbFile = null;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            dbFile = null;
                        }
                    }
                }
            }
        }
    }


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
        pciVendorId = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciVendorIdBytes));

        byte[] pciDeviceIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 6, pciDeviceIdBytes, 0, 2);
        pciDeviceId = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciDeviceIdBytes));

        byte[] pciRevisionIdBytes = new byte[1];
        System.arraycopy(dSEDpciContextBytes, 8, pciRevisionIdBytes, 0, 1);
        pciRevisionId = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciRevisionIdBytes));

        byte[] pciClassCodeBytes = new byte[3];
        System.arraycopy(dSEDpciContextBytes, 9, pciClassCodeBytes, 0, 3);
        pciClassCode = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciClassCodeBytes));

        byte[] pciSubsystemVendorIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 12, pciSubsystemVendorIdBytes, 0, 2);
        pciSubsystemVendorId = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciSubsystemVendorIdBytes));

        byte[] pciSubsystemIdBytes = new byte[2];
        System.arraycopy(dSEDpciContextBytes, 14, pciSubsystemIdBytes, 0, 2);
        pciSubsystemId = HexUtils.byteArrayToHexString(HexUtils.leReverseByte(pciSubsystemIdBytes));

    }

    //TODO REMOVE, ALONG WITH GRADLE  implementation libs.pci
    public static String translateVendor(final String refManufacturer) {
        String manufacturer = refManufacturer;
        if (manufacturer != null && manufacturer.trim().matches("^[0-9A-Fa-f]{4}$")) {
            Vendor ven = DB.findVendor(manufacturer.toLowerCase());
            if (ven != null && !Strings.isNullOrEmpty(ven.getName())) {
                manufacturer = ven.getName();
            }
        }
        return manufacturer;
    }
    public static String translateDevice(final String refManufacturer,
                                         final String refModel) {

        String model = refModel;
        if (refManufacturer != null
                && model != null
                && refManufacturer.trim().matches("^[0-9A-Fa-f]{4}$")
                && model.trim().matches("^[0-9A-Fa-f]{4}$")) {
            Device dev = DB.findDevice(refManufacturer.toLowerCase(),
                    model.toLowerCase());
            if (dev != null && !Strings.isNullOrEmpty(dev.getName())) {
                model = dev.getName();
            }
        }
        return model;
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
        dSEDpciContextInfo += "\n      VendorID = 0x" + pciVendorId;
        dSEDpciContextInfo += "\n      DeviceID = 0x" + pciDeviceId;
        dSEDpciContextInfo += "\n      RevisionID = 0x" + pciRevisionId;
        dSEDpciContextInfo += "\n      ClassCode = 0x" + pciClassCode;
        dSEDpciContextInfo += "\n      SubsystemVendorID = 0x" + pciSubsystemVendorId;
        dSEDpciContextInfo += "\n      SubsystemID = 0x" + pciSubsystemId;

        // TODO REMOVE
        String test1 = translateVendor(pciVendorId);
        String test2 = translateDevice(pciVendorId, pciDeviceId);
        dSEDpciContextInfo += "\n      TEST1 = " + test1;
        dSEDpciContextInfo += "\n      TEST2 = " + test2;

        return dSEDpciContextInfo;
    }
}
