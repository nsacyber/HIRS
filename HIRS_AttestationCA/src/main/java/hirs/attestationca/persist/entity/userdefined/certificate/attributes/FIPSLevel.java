package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1IA5String;
import org.bouncycastle.asn1.ASN1Sequence;

import java.io.Serializable;

/**
 * Basic class that handle FIPS Level.
 * <pre>
 * FIPSLevel ::= SEQUENCE {
 *      version IA5STRING (SIZE (1..STRMAX)), -- "140-1" or "140-2"
 *      level SecurityLevel,
 *      plus BOOLEAN DEFAULT FALSE }
 * </pre>
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
public class FIPSLevel implements Serializable {

    private static final int MAX_SEQUENCE_SIZE = 3;

    private String fipsVersion;

    private SecurityLevel securityLevel;

    private Boolean fipsPlus;

    /**
     * Default constructor.
     */
    public FIPSLevel() {
        fipsVersion = null;
        securityLevel = null;
        fipsPlus = null;
    }

    /**
     * Constructor given the SEQUENCE that contains the FIPLevel Object.
     *
     * @param sequence containing the FIPS Level Object
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public FIPSLevel(final ASN1Sequence sequence) throws IllegalArgumentException {
        //Get version
        fipsVersion = ASN1IA5String.getInstance(sequence.getObjectAt(0)).getString();
        //Get and validate level
        ASN1Enumerated enumerated = ASN1Enumerated.getInstance(sequence.getObjectAt(1));
        //Throw exception when is not between 1 and 7
        if (enumerated.getValue().intValue() <= 0
                || enumerated.getValue().intValue() > SecurityLevel.values().length) {
            throw new IllegalArgumentException("Invalid security level on FIPSLevel.");
        }
        securityLevel = SecurityLevel.values()[enumerated.getValue().intValue() - 1];

        //Check if there is another value on the sequence for the plus
        fipsPlus = Boolean.FALSE;   //Default to false
        if (sequence.size() == MAX_SEQUENCE_SIZE) {
            fipsPlus = ASN1Boolean.getInstance(sequence.getObjectAt(2)).isTrue();
        }
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
    @Getter
    @AllArgsConstructor
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
    }
}
