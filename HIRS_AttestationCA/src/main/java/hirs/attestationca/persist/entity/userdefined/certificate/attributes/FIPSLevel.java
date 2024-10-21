package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1IA5String;
import org.bouncycastle.asn1.ASN1Sequence;

/**
 * Basic class that handle FIPS Level.
 * <pre>
 * FIPSLevel ::= SEQUENCE {
 *      version IA5STRING (SIZE (1..STRMAX)), -- "140-1" or "140-2"
 *      level SecurityLevel,
 *      plus BOOLEAN DEFAULT FALSE }
 * </pre>
 */
@AllArgsConstructor
public class FIPSLevel {

    private static final int MAX_SEQUENCE_SIZE = 3;
    @Getter
    @Setter
    private ASN1IA5String version;
    @Getter
    @Setter
    private SecurityLevel level;
    @Getter
    @Setter
    private ASN1Boolean plus;

    /**
     * Default constructor.
     */
    public FIPSLevel() {
        version = null;
        level = null;
        plus = null;
    }

    /**
     * Constructor given the SEQUENCE that contains the FIPLevel Object.
     *
     * @param sequence containing the FIPS Level Object
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public FIPSLevel(final ASN1Sequence sequence) throws IllegalArgumentException {
        //Get version
        version = ASN1IA5String.getInstance(sequence.getObjectAt(0));
        //Get and validate level
        ASN1Enumerated enumerated = ASN1Enumerated.getInstance(sequence.getObjectAt(1));
        //Throw exception when is not between 1 and 7
        if (enumerated.getValue().intValue() <= 0
                || enumerated.getValue().intValue() > SecurityLevel.values().length) {
            throw new IllegalArgumentException("Invalid security level on FIPSLevel.");
        }
        level = SecurityLevel.values()[enumerated.getValue().intValue() - 1];

        //Check if there is another value on the sequence for the plus
        plus = ASN1Boolean.FALSE;   //Default to false
        if (sequence.size() == MAX_SEQUENCE_SIZE) {
            plus = ASN1Boolean.getInstance(sequence.getObjectAt(2));
        }
    }

    @Override
    public String toString() {
        return "FIPSLevel{"
                + "version=" + version.getString()
                + ", level=" + level.getValue()
                + ", plus=" + plus.toString()
                + '}';
    }

    /**
     * A type to handle the security Level used in the FIPS Level.
     * Ordering of enum types is intentional and their ordinal values correspond to enum
     * values in the TCG spec.
     *
     * <pre>
     * SecurityLevel ::= ENUMERATED {
     *      level1 (1),
     *      level2 (2),
     *      level3 (3),
     *      level4 (4) }
     * </pre>
     */
    public enum SecurityLevel {
        /**
         * Security Level 1.
         */
        LEVEL1("level 1"),
        /**
         * Security Level 2.
         */
        LEVEL2("level 2"),
        /**
         * Security Level 3.
         */
        LEVEL3("level 3"),
        /**
         * Security Level 4.
         */
        LEVEL4("level 4");

        private final String value;

        /**
         * Basic constructor.
         *
         * @param value string containing the value.
         */
        SecurityLevel(final String value) {
            this.value = value;
        }

        /**
         * Get the string value from the StrengthOfFunction.
         *
         * @return the string containing the value.
         */
        public String getValue() {
            return this.value;
        }
    }
}
