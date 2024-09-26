package hirs.utils.tpm.eventlog.uefi;

import com.eclipsesource.json.JsonObject;
import hirs.utils.HexUtils;
import hirs.utils.JsonUtils;
import lombok.Getter;

import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Class to process GUID per the UEFI specification
 * GUIDs are essentially UUID as defined by RFC-1422, however Microsoft refers to GUIDS.
 */
public class UefiGuid {
    /**
     * number of 100ns intervals since UUID Epoch.
     */
    private static final long UUID_EPOCH_INTERVALS = 0x01b21dd213814000L;
    /**
     * used for conversion to uuid time.
     */
    private static final int UUID_EPOCH_DIVISOR = 10000;
    /**
     * Filesystem path of vendor-table.json.
     */
    private static final Path JSON_PATH = FileSystems.getDefault().getPath("/etc",
            "hirs", "aca", "default-properties", "vendor-table.json");
    /**
     * Name of vendor-table file in code.
     */
    private static final String JSON_FILENAME = "vendor-table.json";
    /**
     * Reference to the vendor-table json object.
     */
    private JsonObject uefiVendorRef;
    /**
     * Track status of vendor-table.json.
     */
    @Getter
    private String vendorTableFileStatus = UefiConstants.FILESTATUS_NOT_ACCESSIBLE;

    /**
     * guid byte array.
     */
    private byte[] guid;
    /**
     * UUID object.
     */
    private UUID uuid;

    /**
     * UefiGUID constructor.
     *
     * @param guidBytes byte array holding a valid guid.
     */
    public UefiGuid(final byte[] guidBytes) {
        this(guidBytes, JSON_PATH);
    }

    /**
     * UefiGUID constructor.
     *
     * @param guidBytes byte array holding a valid guid.
     * @param vendorPathString string path for vendor
     */
    public UefiGuid(final byte[] guidBytes, final Path vendorPathString) {
        guid = new byte[UefiConstants.SIZE_16];
        System.arraycopy(guidBytes, 0, guid, 0, UefiConstants.SIZE_16);
        uuid = processGuid(guidBytes);
        uefiVendorRef = JsonUtils.getSpecificJsonObject(vendorPathString,
                "VendorTable");

        if (!isVendorTableReferenceHandleEmpty()) {
            vendorTableFileStatus = UefiConstants.FILESTATUS_FROM_FILESYSTEM;
        } else {
            // could not access vendor-table.json from filesystem, so attempt to access from code
            uefiVendorRef = JsonUtils.getSpecificJsonObject(JSON_FILENAME, "VendorTable");
            if (!isVendorTableReferenceHandleEmpty()) {
                vendorTableFileStatus = UefiConstants.FILESTATUS_FROM_CODE;
            }
        }
    }

    /**
     * Converts a GUID with a byte array to a RFC-1422 UUID object.
     * Assumes a MS format and converts to Big Endian format used by most others , including Linux
     * Matched uuids found in /sys/firmware/efi/efivars on Centos 7.
     * @param guid byte array holding the guid data.
     * @return UUID processed from the passed in guid
     */
    private static UUID processGuid(final byte[] guid) {
        byte[] msb1 = new byte[UefiConstants.SIZE_4];
        System.arraycopy(guid, 0, msb1, 0, UefiConstants.SIZE_4);
        byte[] msb1r = HexUtils.leReverseByte(msb1);
        byte[] msb2 = new byte[UefiConstants.SIZE_4];
        System.arraycopy(guid, UefiConstants.OFFSET_4, msb2, 0, UefiConstants.SIZE_4);
        byte[] msb2r = HexUtils.leReverseByte(msb2);
        byte[] msb2rs = new byte[UefiConstants.SIZE_4];
        System.arraycopy(msb2r, 0, msb2rs, UefiConstants.OFFSET_2, UefiConstants.SIZE_2);
        System.arraycopy(msb2r, UefiConstants.OFFSET_2, msb2rs, 0, UefiConstants.SIZE_2);
        byte[] msbt = new byte[UefiConstants.SIZE_8];
        System.arraycopy(msb1r, 0, msbt, 0, UefiConstants.SIZE_4);
        System.arraycopy(msb2rs, 0, msbt, UefiConstants.OFFSET_4, UefiConstants.SIZE_4);
        long msbl = new BigInteger(msbt).longValue();
        byte[] lsb = new byte[UefiConstants.SIZE_8];
        System.arraycopy(guid, UefiConstants.OFFSET_8, lsb, 0, UefiConstants.SIZE_8);
        long lsbl = new BigInteger(lsb).longValue();
        return new UUID(msbl, lsbl);
    }

    /**
     * Returns the standard GUID length.
     *
     * @return guid length
     */
    public static int getGuidLength() {
        return UefiConstants.SIZE_16;
    }

    /**
     * Checks whether the handle to the file needed to look up the UUID is valid. If empty,
     * this likely means the file was not accessible to due to existence or permissions.
     *
     * @return true if the reference to the file handle needed to look up the UUID is empty
     */
    public boolean isVendorTableReferenceHandleEmpty() {
        return uefiVendorRef.isEmpty();
    }

    /**
     * Returns a String that represents a specification name referenced by the
     * EFI_CONFIGURATION_TABLE VendorGUID field.  For structure of
     * EFI_CONFIGURATION_TABLE type, the UEFI specification has set of GUIDs
     * published that represent standards that one can find further information on
     * the configuration table being referenced.
     * Refer to section 4.6 of UEFI spec v 2.8, page 101.
     *
     * @return A String of major UUID parameters
     */
    public String getVendorTableReference() {
        return getVendorTableReference(uuid.toString().toLowerCase());
    }

    /**
     * Returns a String that represents a specification name referenced by the
     * EFI_CONFIGURATION_TABLE VendorGUID field.  For structure of
     * EFI_CONFIGURATION_TABLE type, the UEFI specification has set of GUIDs
     * published that represent standards that one can find further
     * information on the configuration table being referenced.
     * Refer to section 4.6 of UEFI spec v 2.8, page 101.
     *
     * @param lookupValue specific value to look up
     * @return A String of major UUID parameters
     */
    public String getVendorTableReference(final String lookupValue) {
        return uefiVendorRef.getString(lookupValue, "Unknown GUID reference");
    }

    /**
     * Returns a string of the entity that the UUID represents.
     * Contains a Vendor String lookup on the UUID.
     *
     * @return UUID description.
     */
    public String toString() {
        return String.format("%s : %s", uuid.toString(), getVendorTableReference());
    }

    /**
     * Returns a string of the entity that the UUID represents.
     * Does not contain a vendor lookup on the UUID.
     *
     * @return UUID description.
     */
    public String toStringNoLookup() {
        return uuid.toString();
    }

    /**
     * Returns a string of the entity that the UUID represents.
     * Does not contain a vendor lookup on the UUID.
     *
     * @param guid byte array holding the guid data.
     * @return true if the UUID has a valid structure.
     */
    public static boolean isValidUUID(final byte[] guid) {
        boolean valid = false;
        UUID tmpUuid = processGuid(guid);
        if (tmpUuid.toString().length() != 0) {
            valid = true;
        }
        return valid;
    }

    /**
     * Checks to see if the uuid is the test or Empty UUID ("00000000-0000-0000-0000-000000000000").
     *
     * @return true if the uuid is the Empty UUID, false if not
     */
    public boolean isEmptyUUID() {
        return uuid.toString().equals("00000000-0000-0000-0000-000000000000");
    }

    /**
     * Checks to see if the uuid is the Empty UUID or an unknown.
     *
     * @return true if the uuid is the Empty UUID, false if not
     */
    public boolean isUnknownUUID() {
        return getVendorTableReference().equals("Unknown GUID reference");
    }

    /**
     * Retrieves the timestamp within a time based GUID.
     *
     * @param uuidTimeStamp uuid object
     * @return long representing the time stamp from the GUID
     */
    public long getTimeFromUUID(final UUID uuidTimeStamp) {
        return (uuidTimeStamp.timestamp() - UUID_EPOCH_INTERVALS) / UUID_EPOCH_DIVISOR;
    }
}
