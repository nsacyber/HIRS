package hirs.utils.rim.unsignedRim.common;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum to lookup values for hash algorithm id name and length specified by rfc9393 section 2.9.1.
 * @see <a href="https://www.iana.org/assignments/named-information/named-information.xhtml">
 *     IANA Hash Algorithm Registry</a>
 */
@SuppressWarnings("JavadocVariable")
@Getter
public enum IanaHashAlg {
    RESERVED(0, "reserved", "n/a"),
    SHA_256(1, "sha-256", "256"),
    SHA_256_128(2, "sha-256-128", "128"),
    SHA_256_120(3, "sha-256-120", "120"),
    SHA_256_96(4, "sha-256-96", "96"),
    SHA_256_64(5, "sha-256-64", "64"),
    SHA_256_32(6, "sha-256-32", "32"),
    SHA_384(7, "sha-384", "384"),
    SHA_512(8, "sha-512", "512"),
    SHA3_224(9, "sha3-224", "224"),
    SHA3_256(10, "sha3-256", "256"),
    SHA3_384(11, "sha3-384", "384"),
    SHA3_512(12, "sha3-512", "512");

    private final int algId;
    @JsonValue
    private final String algName;
    private final String algLength;

    /**
     * Map of alg and values.
     */
    private static final Map<Integer, IanaHashAlg> ID_MAP = new HashMap<>();

    static {
        for (IanaHashAlg alg : values()) {
            ID_MAP.put(alg.getAlgId(), alg);
        }
    }

    /**
     * Constructor to set parameters.
     * @param algId int id of algorithm
     * @param algName name of algorithm
     * @param algLength length of algorithm
     */
    IanaHashAlg(final int algId, final String algName, final String algLength) {
        this.algId = algId;
        this.algName = algName;
        this.algLength = algLength;
    }

    /**
     * Searches algorithm array for match to an alg enum value.
     * @param alg int id of algorithm
     * @return the corresponding algorithm for the IANA reference page
     */
    public static IanaHashAlg getAlgFromId(final int alg) {
        return ID_MAP.get(alg);
    }

    /**
     * Searches algorithm array for match to an alg enum value.
     * @param algName String name of algorithm
     * @return the corresponding algorithm for the IANA reference page, or null if none found
     */
    public static IanaHashAlg getAlgFromName(final String algName) {
        for (IanaHashAlg alg : values()) {
            if (alg.getAlgName().equals(algName)) {
                return alg;
            }
        }
        return null;
    }
}

