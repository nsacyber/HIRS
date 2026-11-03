package hirs.utils.tpm.eventlog.events;

/**
 * Class for defining constants referenced in the PC Client
 * Platform Firmware Profile specification.
 */
public final class EvConstants {

    /**
     * Each PCR bank holds 24 registers.
     */
    public static final int PCR_COUNT = 24;
    /**
     * Pre boot cert Event ID.
     */
    public static final int EV_PREBOOT_CERT = 0x00000000;
    // Event IDs
    /**
     * POST Code Event ID.
     */
    public static final int EV_POST_CODE = 0x00000001;
    /**
     * Unused Event ID.
     */
    public static final int EV_UNUSED = 0x00000002;
    /**
     * NoAction Event ID.
     */
    public static final int EV_NO_ACTION = 0x00000003;
    /**
     * NoAction Event ID.
     */
    public static final int EV_SEPARATOR = 0x00000004;
    /**
     * Action Event ID.
     */
    public static final int EV_ACTION = 0x00000005;
    /**
     * Event ID.
     */
    public static final int EV_EVENT_TAG = 0x00000006;
    /**
     * SCRTM Contents Event ID.
     */
    public static final int EV_S_CRTM_CONTENTS = 0x00000007;
    /**
     * SCRTM Version Event ID.
     */
    public static final int EV_S_CRTM_VERSION = 0x00000008;
    /**
     * CPU Microcode Event ID.
     */
    public static final int EV_CPU_MICROCODE = 0x00000009;
    /**
     * Platform Config Flags Event ID.
     */
    public static final int EV_PLATFORM_CONFIG_FLAGS = 0x0000000A;
    /**
     * Table of Devices Event ID.
     */
    public static final int EV_TABLE_OF_DEVICES = 0x0000000B;
    /**
     * Compact Hash Event ID.
     */
    public static final int EV_COMPACT_HASH = 0x0000000C;
    /**
     * IPL Event ID.
     */
    public static final int EV_IPL = 0x0000000D;
    /**
     * Partition Data Event ID.
     */
    public static final int EV_IPL_PARTITION_DATA = 0x0000000E;
    /**
     * Non Host Event ID.
     */
    public static final int EV_NONHOST_CODE = 0x0000000F;
    /**
     * Non Host Config Event ID.
     */
    public static final int EV_NONHOST_CONFIG = 0x00000010;
    /**
     * Non Host Info Event ID.
     */
    public static final int EV_NONHOST_INFO = 0x00000011;
    /**
     * Omit Boot Device Event ID.
     */
    public static final int EV_EV_OMIT_BOOT_DEVICES_EVENTS = 0x00000012;
    /**
     * EFI Event ID.
     */
    public static final int EV_EFI_EVENT_BASE = 0x80000000;
    /**
     * EFI Variable Driver Event ID.
     */
    public static final int EV_EFI_VARIABLE_DRIVER_CONFIG = 0x80000001;
    /**
     * EFI Variable Boot Driver Event ID.
     */
    public static final int EV_EFI_VARIABLE_BOOT = 0x80000002;
    /**
     * EFI Boot Services Application Event ID.
     */
    public static final int EV_EFI_BOOT_SERVICES_APPLICATION = 0x80000003;
    /**
     * EFI Boot Services Application Event ID.
     */
    public static final int EV_EFI_BOOT_SERVICES_DRIVER = 0x80000004;
    /**
     * EFI Runtime Services Driver Event ID.
     */
    public static final int EV_EFI_RUNTIME_SERVICES_DRIVER = 0x80000005;
    /**
     * EFI GPT Event ID.
     */
    public static final int EV_EFI_GPT_EVENT = 0x80000006;
    /**
     * EFI GPT Event ID.
     */
    public static final int EV_EFI_ACTION = 0x80000007;
    /**
     * Platform Firmware Blob Event ID.
     */
    public static final int EV_EFI_PLATFORM_FIRMWARE_BLOB = 0x80000008;
    /**
     * EFI Handoff Tables Event ID.
     */
    public static final int EV_EFI_HANDOFF_TABLES = 0x80000009;
    /**
     * Platform Firmware Blob2 Event ID.
     */
    public static final int EV_EFI_PLATFORM_FIRMWARE_BLOB2 = 0x8000000a;
    /**
     *  EFI Handoff Tables2 Event ID.
     */
    public static final int EV_EFI_HANDOFF_TABLES2 = 0x8000000b;
    /**
     * HRCTM Event ID.
     */
    public static final int EV_EFI_HCRTM_EVENT = 0x80000010;
    /**
     * EFI Variable Authority Event ID.
     */
    public static final int EV_EFI_VARIABLE_AUTHORITY = 0x800000E0;
    /**
     * EFI SPDM Firmware Blob Event ID.
     */
    public static final int EV_EFI_SPDM_FIRMWARE_BLOB = 0x800000E1;
    /**
     * EFI SPDM Firmware Config Event ID.
     */
    public static final int EV_EFI_SPDM_FIRMWARE_CONFIG = 0x800000E2;
    /**
     * EFI SPDM Device Policy Event ID.
     */
    public static final int EV_EFI_SPDM_DEVICE_POLICY = 0x800000E3;
    /**
     * EFI SPDM Device Authority Event ID.
     */
    public static final int EV_EFI_SPDM_DEVICE_AUTHORITY = 0x800000E4;

    // The following ranges are from the TCG PC Client Event Registry
    /**
     * Min of range 1 reserved for BIOS.
     */
    public static final int EV_RESERVED_TCG_BIOS1_MIN = 0x00000000;
    /**
     * Max of range 1 reserved for BIOS.
     */
    public static final int EV_RESERVED_TCG_BIOS1_MAX = 0x000003FF;
    /**
     * Min of range reserved for Intel - DRTM.
     */
    public static final int EV_RESERVED_INTEL_DRTM_MIN = 0x00000400;
    /**
     * Max of range reserved for Intel - DRTM.
     */
    public static final int EV_RESERVED_INTEL_DRTM_MAX = 0x000004FF;
    /**
     * Min of range reserved for TBoot - DRTM.
     */
    public static final int EV_RESERVED_TBOOT_DRTM_MIN = 0x00000500;
    /**
     * Max of range reserved for TBoot - DRTM.
     */
    public static final int EV_RESERVED_TBOOT_DRTM_MAX = 0x000005FF;
    /**
     * Min of range reserved for Microsoft Custom BIOS Events.
     */
    public static final int EV_RESERVED_MICROSOFT_BIOS_MIN = 0x00000600;
    /**
     * Max of range reserved for Microsoft Custom BIOS Events.
     */
    public static final int EV_RESERVED_MICROSOFT_BIOS_MAX = 0x000006FF;
    /**
     * Min of range 2 reserved for BIOS.
     */
    public static final int EV_RESERVED_TCG_BIOS2_MIN = 0x00000700;
    /**
     * Max of range 2 reserved for BIOS.
     */
    public static final int EV_RESERVED_TCG_BIOS2_MAX = 0x00007FFF;
    /**
     * Min of range 2 reserved for BIOS.
     */
    public static final int EV_RESERVED_AMD_DRTM_MIN = 0x00008000;
    /**
     * Max of range 2 reserved for BIOS.
     */
    public static final int EV_RESERVED_AMD_DRTM_MAX = 0x000080FF;
    /**
     * Min of range 2 reserved for BIOS.
     */
    public static final int EV_RESERVED_ARM_MIN = 0x00009000;
    /**
     * Max of range 2 reserved for BIOS.
     */
    public static final int EV_RESERVED_ARM_MAX = 0x00009FFF;

    /**
     * Default private constructor so checkstyles doesn't complain.
     */
    private EvConstants() {
    }
}
