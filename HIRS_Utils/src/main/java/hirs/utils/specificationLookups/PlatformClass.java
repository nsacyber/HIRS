package hirs.utils.specificationLookups;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum PlatformClass {
    UNCLASSIFIED(0, "Unclassified (not platform specific)"),
    PCCLIENT(1, "PC Client"),
    PDA(2, "PDA (includes all mobile devices that are not specifically cell phones)"),
    CELLPHONE(3, "Cell Phone"),
    SERVER(4, "Server WG"),
    PERIPHERAL(5, "Peripheral WG"),
    TSS(6, "Deprecated"),
    STORAGE(7, "Storage WG"),
    AUTHENTICATION(8, "Authentication WG"),
    EMBEDDED(9, "Embedded WG"),
    HARDCOPY(0xA, "Hardcopy WG "),
    INFRASTRUCTURE(0xB, "Infrastructure (Deprecated)"),
    VIRTUALIZATION(0xC, "Virtualization WG"),
    TNC(0xD, "TNC (Deprecated)"),
    MULTITENANT(0xE, "Multi Tenant (Deprecated)"),
    TC(0xF, "TC (Deprecated)");

    private final int platformClassId;
    private final String comments;

    /**
     * Map of PlatformClass values.
     */
    private static final Map<Integer, PlatformClass> ID_MAP = new HashMap<>();

    static {
        for (PlatformClass platformClass : values()) {
            ID_MAP.put(platformClass.getPlatformClassId(), platformClass);
        }
    }

    /**
     * Searches platform class array for match to an enum value.
     * @param platformClassId int id of the platform class you want to look up
     * @return the corresponding platform class
     */
    public static PlatformClass getPlatClassFromId(final int platformClassId) {
        return ID_MAP.get(platformClassId);
    }
}
