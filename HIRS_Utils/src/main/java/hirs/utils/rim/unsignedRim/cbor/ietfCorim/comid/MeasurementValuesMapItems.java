package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Section 5.1.4.1.4.2 of the IETF CoRim specification
 *
 * <pre>
 * measurement-values-map = non-empty&gt;{
 *      ? &amp;(version: 0) => version-map
 *      ? &amp;(svn: 1) => svn-type-choice
 *      ? &amp;(digests: 2) => digests-type
 *      ? &amp;(flags: 3) => flags-map
 *      ? (
 *          &amp;(raw-value: 4) => $raw-value-type-choice,
 *          ? &amp;(raw-value-mask: 5) => raw-value-mask-type
 *        )
 *      ? &amp;(mac-addr: 6) => mac-addr-type-choice
 *      ? &amp;(ip-addr: 7) =>  ip-addr-type-choice
 *      ? &amp;(serial-number: 8) => text
 *      ? &amp;(ueid: 9) => ueid-type
 *      ? &amp;(uuid: 10) => uuid-type
 *      ? &amp;(name: 11) => text
 *      ? &amp;(cryptokeys: 13) => [ + $crypto-key-type-choice ]
 *      ? &amp;(integrity-registers: 14) => integrity-registers
 *      * $$measurement-values-map-extension
 *    }&lt;
 * </pre>
 */
@Getter
public enum MeasurementValuesMapItems {
    /** Corresponds to a version-map. */
    VERSION_MAP(0, "version-map"),
    /** Corresponds to an svn. */
    SVN(1, "svn"),
    /** Corresponds to digests. */
    DIGESTS(2, "digests"),
    /** Corresponds to flags. */
    FLAGS(3, "flags"),
    /** Corresponds to a raw-value. */
    RAW_VALUE(4, "raw-value"),
    /** Corresponds to a raw-value-mask <b>(deprecated).</b> */
    RAW_VALUE_MASK(5, "raw-value-mask"),
    /** Corresponds to a mac-addr. */
    MAC_ADDR(6, "mac-addr"),
    /** Corresponds to an ip-addr. */
    IP_ADDR(7, "ip-addr"),
    /** Corresponds to a serial-number. */
    SERIAL_NUMBER(8, "serial-number"),
    /** Corresponds to a ueid. */
    UEID(9, "ueid"),
    /** Corresponds to a uuid. */
    UUID(10, "uuid"),
    /** Corresponds to a name. */
    NAME(11, "name"),
    /** Corresponds to cryptokeys. */
    CRYPTOKEYS(13, "cryptokeys"),
    /** Corresponds to integrity-registers. */
    INTEGRITY_REGISTERS(14, "integrity-registers");

    private final int index;
    private final String key;

    MeasurementValuesMapItems(final int index, final String key) {
        this.index = index;
        this.key = key;
    }

    private static final Map<Integer, MeasurementValuesMapItems> LOOKUP =
            stream(values())
                    .collect(toMap(MeasurementValuesMapItems::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static MeasurementValuesMapItems fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
