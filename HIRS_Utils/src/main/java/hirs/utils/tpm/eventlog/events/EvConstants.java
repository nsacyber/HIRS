package hirs.utils.tpm.eventlog.events;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class for defining constants referenced in the PC Client
 * Platform Firmware Profile specification.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EvConstants {

    /**
     * Type length = 4 bytes.
     */
    public static final int EV_TYPE_SIZE = 4;
    /**
     * Event Log spec version.
     */
    public static final int MIN_SIZE = 32;
    /**
     * Event Type (byte array).
     */
    public static final int INT_LENGTH = 4;
    /**
     * Event Type (byte array).
     */
    public static final int SHA1_LENGTH = 20;
    /**
     * Event Type (byte array).
     */
    public static final int SHA256_LENGTH = 32;
    /**
     * Event Type (byte array).
     */
    public static final int SHA384_LENGTH = 48;
    /**
     * Each PCR bank holds 24 registers.
     */
    public static final int PCR_COUNT = 24;
    // Event IDs
    /**
     * Pre boot cert Event ID.
     */
    public static final int EV_PREBOOT_CERT = 0x00000000;
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
}
