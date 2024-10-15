package hirs.utils;

import com.github.marandus.pciid.model.Device;
import com.github.marandus.pciid.model.DeviceClass;
import com.github.marandus.pciid.model.DeviceSubclass;
import com.github.marandus.pciid.model.ProgramInterface;
import com.github.marandus.pciid.model.Vendor;
import com.github.marandus.pciid.service.PciIdsDatabase;
import com.google.common.base.Strings;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.ASN1UTF8String;
import org.bouncycastle.asn1.DERUTF8String;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provide Java access to PCI IDs.
 */
@Log4j2
public final class PciIds {

    /**
     * This pci ids file can be in different places on different distributions.
     */
    public static final List<String> PCI_IDS_PATH =
            Collections.unmodifiableList(new ArrayList<>() {
                private static final long serialVersionUID = 1L;

                {
                    add("/usr/share/hwdata/pci.ids");
                    add("/usr/share/misc/pci.ids");
                    add("/tmp/pci.ids");
                }
            });
    /**
     * The PCI IDs Database object.
     * <p>
     * This only needs to be loaded one time.
     * <p>
     * The pci ids library protects the data inside the object by making it immutable.
     */
    public static final PciIdsDatabase DB = new PciIdsDatabase();

    static {
        if (!DB.isReady()) {
            String dbFile = null;
            for (final String path : PCI_IDS_PATH) {
                if ((new File(path)).exists()) {
                    log.info("PCI IDs file was found {}", path);
                    dbFile = path;
                    break;
                }
            }
            if (dbFile != null) {
                InputStream is = null;
                try {
                    is = new FileInputStream(dbFile);
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
     * Default private constructor so checkstyles doesn't complain.
     */
    private PciIds() {
    }

    /**
     * Look up the vendor name from the PCI IDs list, if the input string contains an ID.
     * If any part of this fails, return the original manufacturer value.
     *
     * @param refManufacturer DERUTF8String, likely from a ComponentIdentifier
     * @return DERUTF8String with the discovered vendor name, or the original manufacturer value.
     */
    public static ASN1UTF8String translateVendor(final ASN1UTF8String refManufacturer) {
        ASN1UTF8String manufacturer = refManufacturer;
        if (manufacturer != null && manufacturer.getString().trim().matches("^[0-9A-Fa-f]{4}$")) {
            Vendor ven = DB.findVendor(manufacturer.getString().toLowerCase());
            if (ven != null && !Strings.isNullOrEmpty(ven.getName())) {
                manufacturer = new DERUTF8String(ven.getName());
            }
        }
        return manufacturer;
    }

    /**
     * Look up the vendor name from the PCI IDs list, if the input string contains an ID.
     * If any part of this fails, return the original manufacturer value.
     *
     * @param refManufacturer String, likely from a ComponentResult
     * @return String with the discovered vendor name, or the original manufacturer value.
     */
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

    /**
     * Look up the device name from the PCI IDs list, if the input strings contain IDs.
     * The Device lookup requires the Vendor ID AND the Device ID to be valid values.
     * If any part of this fails, return the original model value.
     *
     * @param refManufacturer ASN1UTF8String, likely from a ComponentIdentifier
     * @param refModel        ASN1UTF8String, likely from a ComponentIdentifier
     * @return ASN1UTF8String with the discovered device name, or the original model value.
     */
    public static ASN1UTF8String translateDevice(final ASN1UTF8String refManufacturer,
                                                 final ASN1UTF8String refModel) {
        ASN1UTF8String manufacturer = refManufacturer;
        ASN1UTF8String model = refModel;
        if (manufacturer != null
                && model != null
                && manufacturer.getString().trim().matches("^[0-9A-Fa-f]{4}$")
                && model.getString().trim().matches("^[0-9A-Fa-f]{4}$")) {
            Device dev = DB.findDevice(manufacturer.getString().toLowerCase(),
                    model.getString().toLowerCase());
            if (dev != null && !Strings.isNullOrEmpty(dev.getName())) {
                model = new DERUTF8String(dev.getName());
            }
        }
        return model;
    }

    /**
     * Look up the device name from the PCI IDs list, if the input strings contain IDs.
     * The Device lookup requires the Vendor ID AND the Device ID to be valid values.
     * If any part of this fails, return the original model value.
     *
     * @param refManufacturer String, likely from a ComponentResult
     * @param refModel        String, likely from a ComponentResult
     * @return String with the discovered device name, or the original model value.
     */
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
     * Look up the device class name from the PCI IDs list, if the input string contains an ID.
     * If any part of this fails, return the original manufacturer value.
     *
     * @param refClassCode String, formatted as 2 characters (1 byte) for each of the 3 categories
     *                     Example "010802":
     *                     Class: "01"
     *                     Subclass: "08"
     *                     Programming Interface: "02"
     * @return List<String> 3-element list with the class code
     * 1st element: human-readable description of Class
     * 2nd element: human-readable description of Subclass
     * 3rd element: human-readable description of Programming Interface
     */
    public static List<String> translateDeviceClass(final String refClassCode) {
        List<String> translatedClassCode = new ArrayList<>();

        String classCode = refClassCode;
        if (classCode != null && classCode.trim().matches("^[0-9A-Fa-f]{6}$")) {
            final int startIndexOfDeviceClass = 0;
            final int endIndexOfDeviceClass = 2;
            String deviceClass =
                    classCode.substring(startIndexOfDeviceClass, endIndexOfDeviceClass).toLowerCase();

            final int startIndexOfDeviceSubclass = 2;
            final int endIndexOfDeviceSubclass = 4;
            String deviceSubclass =
                    classCode.substring(startIndexOfDeviceSubclass, endIndexOfDeviceSubclass)
                            .toLowerCase();

            final int startIndexOfProgramInterface = 4;
            final int endIndexOfProgramInterface = 6;
            final String programInterface =
                    classCode.substring(startIndexOfProgramInterface, endIndexOfProgramInterface)
                            .toLowerCase();

            translatedClassCode.add(deviceClass);
            translatedClassCode.add(deviceSubclass);
            translatedClassCode.add(programInterface);
            DeviceClass devC = DB.findDeviceClass(deviceClass);
            DeviceSubclass devSc = DB.findDeviceSubclass(deviceClass, deviceSubclass);
            ProgramInterface progI = DB.findProgramInterface(deviceClass, deviceSubclass, programInterface);
            if (devC != null && !Strings.isNullOrEmpty(devC.getName())) {
                translatedClassCode.set(0, devC.getName());
            }
            if (devSc != null && !Strings.isNullOrEmpty(devSc.getName())) {
                translatedClassCode.set(1, devSc.getName());
            }
            if (progI != null && !Strings.isNullOrEmpty(progI.getName())) {
                translatedClassCode.set(2, progI.getName());
            }
        }
        return translatedClassCode;
    }
}
