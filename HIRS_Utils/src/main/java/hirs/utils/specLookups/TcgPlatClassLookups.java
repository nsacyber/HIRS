package hirs.utils.specLookups;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum to lookup values for platform class specified by section 4 of the following registry.
 *
 * @see <a href="https://trustedcomputinggroup.org/resource/tcg-platform-certificate-profile/">
 * Registry of Reserved TPM 2.0 Handles and Localities</a>
 */
@Getter
@AllArgsConstructor
public enum TcgPlatClassLookups {
    /** UNCLASSIFIED. **/
    UNCLASSIFIED(0, "Unclassified (not platform specific)"),
    /** PCCLIENT. **/
    PCCLIENT(1, "PC Client"),
    /** PDA. **/
    PDA(2, "PDA (includes all mobile devices that are not specifically cell phones)"),
    /** CELLPHONE. **/
    CELLPHONE(3, "Cell Phone"),
    /** SERVER. **/
    SERVER(4, "Server WG"),
    /** PERIPHERAL. **/
    PERIPHERAL(5, "Peripheral WG"),
    /** TSS. **/
    TSS(6, "Deprecated"),
    /** STORAGE. **/
    STORAGE(7, "Storage WG"),
    /** AUTHENTICATION. **/
    AUTHENTICATION(8, "Authentication WG"),
    /** EMBEDDED. **/
    EMBEDDED(9, "Embedded WG"),
    /** HARDCOPY. **/
    HARDCOPY(0xA, "Hardcopy WG "),
    /** INFRASTRUCTURE. **/
    INFRASTRUCTURE(0xB, "Infrastructure (Deprecated)"),
    /** VIRTUALIZATION. **/
    VIRTUALIZATION(0xC, "Virtualization WG"),
    /** TNC. **/
    TNC(0xD, "TNC (Deprecated)"),
    /** MULTITENANT. **/
    MULTITENANT(0xE, "Multi Tenant (Deprecated)"),
    /** TC. **/
    TC(0xF, "TC (Deprecated)");

    /**
     * Map of TcgPlatClassLookups values.
     */
    private static final Map<Integer, TcgPlatClassLookups> ID_MAP = new HashMap<>();

    static {
        for (TcgPlatClassLookups tcgPlatClassLookups : values()) {
            ID_MAP.put(tcgPlatClassLookups.getPlatformClassId(), tcgPlatClassLookups);
        }
    }

    private final int platformClassId;
    private final String comments;

    /**
     * Searches platform class array for match to an enum value.
     *
     * @param platformClassId int id of the platform class you want to look up
     * @return the corresponding platform class
     */
    public static TcgPlatClassLookups getPlatClassFromId(final int platformClassId) {
        return ID_MAP.get(platformClassId);
    }
}
