package hirs.tpm.eventlog.uefi;

/**
 * This class contains the String constants that are referenced by UEFI.
 * It is expected that member properties of this class will expand as
 * more functionality is added.
 */
public final class UefiConstants {
/**
 * Constructor.
 */
    private UefiConstants() {
    }
    /** 2 byte size. */
    public static final int SIZE_2 = 2;
    /** 4 byte size. */
    public static final int SIZE_4 = 4;
    /** 5 byte size. */
    public static final int SIZE_5 = 5;
    /** 8 byte size. */
    public static final int SIZE_8 = 8;
    /** 16 byte size. */
    public static final int SIZE_16 = 16;
    /** 20 byte size. */
    public static final int SIZE_20 = 20;
    /** 28 byte size. */
    public static final int SIZE_28 = 28;
    /** 32 byte size. */
    public static final int SIZE_32 = 32;
    /** 40 byte size. */
    public static final int SIZE_40 = 40;
    /** 256 byte size. */
    public static final int SIZE_256 = 256;
    /** 1 byte offset. */
    public static final int OFFSET_1 = 1;
    /** 2 byte offset. */
    public static final int OFFSET_2 = 2;
    /** 3 byte offset. */
    public static final int OFFSET_3 = 3;
    /** 4 byte offset. */
    public static final int OFFSET_4 = 4;
    /** 6 byte offset. */
    public static final int OFFSET_6 = 4;
    /** 8 byte offset. */
    public static final int OFFSET_8 = 8;
    /** 16 byte offset. */
    public static final int OFFSET_16 = 16;
    /** 20 byte offset. */
    public static final int OFFSET_20 = 20;
    /** 24 byte offset. */
    public static final int OFFSET_24 = 24;
    /** 28 byte offset. */
    public static final int OFFSET_28 = 28;
    /** 28 byte offset. */
    public static final int OFFSET_32 = 32;
    /** 40 byte offset. */
    public static final int OFFSET_40 = 40;
    /** 41 byte offset. */
    public static final int OFFSET_41 = 41;
    /** Device path terminator. */
    public static final int TERMINATOR = 0x7f;
    /** Device path end flag. */
    public static final int END_FLAG = 0xff;
    /** Device Type Hardware. */
    public static final int DEV_HW = 0x01;
    /** Device Type ACPI. */
    public static final int DEV_ACPI = 0x02;
    /** Device Type Messaging. */
    public static final int DEV_MSG = 0x03;
    /** Device Type Media. */
    public static final int DEV_MEDIA = 0x04;
    /** Device Type Hardware. */
    public static final int DEV_BIOS = 0x05;
    /** Device Sub-Type Sata. */
    public static final int DEV_SUB_SATA = 0x12;
   /** Device Sub-Type nvm. */
    public static final int DEV_SUB_NVM = 0x17;
    /** BIOS Device Path reserved. */
    public static final int DEVPATH_BIOS_RESERVED = 0x0;
    /** BIOS Device Path for Floppy disks. */
    public static final int DEVPATH_BIOS_FLOPPY = 0x01;
    /** BIOS Device Path Hard drives. */
    public static final int DEVPATH_BIOS_HD = 0x02;
    /** BIOS Device Path for CD Drives. */
    public static final int DEVPATH_BIOS_CD = 0x03;
    /** BIOS Device Path for PCM CIA drives. */
    public static final int DEVPATH_BIOS_PCM = 0x04;
    /** BIOS Device Path for USB Drives. */
    public static final int DEVPATH_BIOS_USB = 0x05;
    /** BIOS Device Path for embedded network. */
    public static final int DEVPATH_BIOS_EN = 0x06;
    /** BIOS Device Path for a Bootstrap Entry Vector (BEV) from an option ROM. */
    public static final int DEVPATH_BIOS_BEV = 0x80;
    /** Hardware Device Path. */
    public static final int DEVPATH_HARWARE = 0x1;
    /** 2 byte size. */
    public static final int DEVPATH_VENDOR = 0x03;
    /** 2 byte size. */
    public static final int DEVPATH_FILE = 0x04;
    /** PIWG File device path type. */
    public static final int DEVPATH_PWIG_FILE = 0x06;
    /** PIWG Volume device path type. */
    public static final int DEVPATH_PWIG_VOL = 0x07;
    /** PC-AT compatible legacy MBR. */
    public static final int DRIVE_TYPE_PC_AT = 0x01;
    /** GUID Partition Table type. */
    public static final int DRIVE_TYPE_GPT = 0x02;
    /** Drive Signature type. */
    public static final int DRIVE_SIG_NONE = 0x00;
    /** Drive Signature type. */
    public static final int DRIVE_SIG_32BIT = 0x01;
    /** Drive Signature type. */
    public static final int DRIVE_SIG_GUID = 0x02;
    /** standard byte length. */
    public static final int BYTE_LENGTH = 8;
    /** standard byte length. */
    public static final int ATTRIBUTE_LENGTH = 48;
    /** standard byte length. */
    public static final int PART_NAME_LENGTH = 56;
    /** standard UEFI partition table lengh. */
    public static final int UEFI_PT_LENGTH = 72;
}
