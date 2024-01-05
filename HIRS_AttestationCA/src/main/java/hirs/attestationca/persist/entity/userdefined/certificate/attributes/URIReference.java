package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 *
 * Basic class that handle a URIReference object.
 * <pre>
 * URIReference ::= SEQUENCE {
 *      uniformResourceIdentifier IA5String (SIZE (1..URIMAX)),
 *      hashAlgorithm AlgorithmIdentifier OPTIONAL,
 *      hashValue BIT STRING OPTIONAL
 }
 * </pre>
 */
@Getter @Setter
@AllArgsConstructor
public class URIReference {
    private DERIA5String uniformResourceIdentifier;
    private AlgorithmIdentifier hashAlgorithm;
    @JsonIgnore
    private DERBitString hashValue;
    private ASN1Sequence sequence;

    private static final int PLATFORM_PROPERTIES_URI_MAX = 3;
    private static final int PLATFORM_PROPERTIES_URI_MIN = 1;

    /**
     * Default constructor.
     */
    public URIReference() {
        this.uniformResourceIdentifier = null;
        this.hashAlgorithm = null;
        this.hashValue = null;
    }

    /**
     * Constructor given the SEQUENCE that contains the URIReference values.
     *
     * @param sequence containing the name and value of the platform property
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public URIReference(final ASN1Sequence sequence) throws IllegalArgumentException {
        if (sequence == null) {
            return ;
        }
        //Check if the sequence contains the two values required
        if (sequence.size() > PLATFORM_PROPERTIES_URI_MAX
                || sequence.size() < PLATFORM_PROPERTIES_URI_MIN) {
            throw new IllegalArgumentException("PlatformPropertiesURI contains invalid "
                    + "number of fields.");
        }

        //Get the Platform Configuration URI values
        for (int j = 0; j < sequence.size(); j++) {
            if (sequence.getObjectAt(j) instanceof DERIA5String) {
                this.uniformResourceIdentifier = DERIA5String.getInstance(sequence.getObjectAt(j));
            } else if ((sequence.getObjectAt(j) instanceof AlgorithmIdentifier)
                    || (sequence.getObjectAt(j) instanceof ASN1Sequence)) {
                this.hashAlgorithm =
                        AlgorithmIdentifier.getInstance(sequence.getObjectAt(j));
            } else if (sequence.getObjectAt(j) instanceof DERBitString) {
                this.hashValue = DERBitString.getInstance(sequence.getObjectAt(j));
            } else {
                throw new IllegalArgumentException("Unexpected DER type found. "
                        + sequence.getObjectAt(j).getClass().getName() + " found at index " + j + ".");
            }
        }

        this.sequence = ASN1Sequence.getInstance(sequence);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("URIReference{");
        sb.append("uniformResourceIdentifier=");
        if (uniformResourceIdentifier != null) {
            sb.append(uniformResourceIdentifier.getString());
        }
        //Check of optional values are not null
        sb.append(", hashAlgorithm=");
        if (hashAlgorithm != null) {
            sb.append(hashAlgorithm.getAlgorithm().getId());
        }
        sb.append(", hashValue=");
        if (hashValue != null) {
            sb.append(hashValue.getString());
        }
        sb.append("}");
        return sb.toString();
    }
}
