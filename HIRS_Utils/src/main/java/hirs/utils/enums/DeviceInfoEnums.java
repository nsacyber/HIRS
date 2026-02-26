package hirs.utils.enums;

/**
 * Enum values that are used for the embedded info objects.
 */
public final class DeviceInfoEnums {

    /**
     * A variable used to describe unavailable hardware, firmware, or OS info.
     */
    public static final String NOT_SPECIFIED = "Not Specified";

    /**
     * Constant variable representing the various Short sized strings.
     */
    public static final int SHORT_STRING_LENGTH = 32;

    /**
     * Constant variable representing the various Medium sized strings.
     */
    public static final int MED_STRING_LENGTH = 64;

    /**
     * Constant variable representing the various Long sized strings.
     */
    public static final int LONG_STRING_LENGTH = 255;

    /**
     * Default private constructor so checkstyles doesn't complain.
     */
    private DeviceInfoEnums() {
    }
}
