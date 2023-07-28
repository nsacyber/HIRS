package hirs.utils.enums;

public enum ComponentType {

    /**
     * Baseboard.
     */
    BASEBOARD(Values.BASEBOARD),
    /**
     * BIOS or UEFI.
     */
    BIOS_UEFI(Values.BIOS_UEFI),
    /**
     * Chassis.
     */
    CHASSIS(Values.CHASSIS),
    /**
     * Hard Drive.
     */
    HARD_DRIVE(Values.HARD_DRIVE),
    /**
     * Memory.
     */
    MEMORY(Values.MEMORY),
    /**
     * Network Interface Card.
     */
    NIC(Values.NIC),
    /**
     * Processor.
     */
    PROCESSOR(Values.PROCESSOR);

    /**
     * Constructor.
     *
     * @param val string value
     */
    ComponentType(final String val) {
        if (!this.name().equals(val)) {
            throw new IllegalArgumentException("Incorrect use of ComponentType");
        }
    }

    /**
     * String values for use in {@link ComponentType}.
     */
    public static class Values {

        /**
         * Baseboard.
         */
        public static final String BASEBOARD = "BASEBOARD";

        /**
         * BIOS or UEFI.
         */
        public static final String BIOS_UEFI = "BIOS_UEFI";

        /**
         * Chassis.
         */
        public static final String CHASSIS = "CHASSIS";

        /**
         * Hard Drive.
         */
        public static final String HARD_DRIVE = "HARD_DRIVE";

        /**
         * Memory.
         */
        public static final String MEMORY = "MEMORY";

        /**
         * Network Interface Card.
         */
        public static final String NIC = "NIC";

        /**
         * Processor.
         */
        public static final String PROCESSOR = "PROCESSOR";
    }
}
