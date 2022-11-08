package hirs.data.persist.certificate.attributes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1IA5String;
import org.bouncycastle.asn1.ASN1Sequence;
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
public class URIReference {
    private ASN1IA5String uniformResourceIdentifier;
    private AlgorithmIdentifier hashAlgorithm;
    @JsonIgnore
    private ASN1BitString hashValue;

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
     * Constructor given the resources identifier, algorithm identifier and hash value.
     *
     * @param uniformResourceIdentifier string containing the resource identifier
     * @param hashAlgorithm algorithm identifier
     * @param hashValue string containing the hash value
     */
    public URIReference(final ASN1IA5String uniformResourceIdentifier,
                    final AlgorithmIdentifier hashAlgorithm,
                    final ASN1BitString hashValue) {
        this.uniformResourceIdentifier = uniformResourceIdentifier;
        this.hashAlgorithm = hashAlgorithm;
        this.hashValue = hashValue;
    }

    /**
     * Constructor given the SEQUENCE that contains the URIReference values.
     *
     * @param sequence containing the name and value of the platform property
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public URIReference(final ASN1Sequence sequence) throws IllegalArgumentException {
         //Check if the sequence contains the two values required
        if (sequence.size() > PLATFORM_PROPERTIES_URI_MAX
                || sequence.size() < PLATFORM_PROPERTIES_URI_MIN) {
            throw new IllegalArgumentException("PlatformPropertiesURI contains invalid "
                    + "number of fields.");
        }

        //Get the Platform Configuration URI values
        for (int j = 0; j < sequence.size(); j++) {
            if (sequence.getObjectAt(j) instanceof ASN1IA5String
                    || sequence.getObjectAt(j) instanceof ASN1IA5String) {
                this.uniformResourceIdentifier = ASN1IA5String.getInstance(sequence.getObjectAt(j));
            } else if ((sequence.getObjectAt(j) instanceof AlgorithmIdentifier)
                    || (sequence.getObjectAt(j) instanceof ASN1Sequence)) {
                this.hashAlgorithm =
                        AlgorithmIdentifier.getInstance(sequence.getObjectAt(j));
            } else if (sequence.getObjectAt(j) instanceof ASN1BitString) {
                this.hashValue = ASN1BitString.getInstance(sequence.getObjectAt(j));
            } else {
                throw new IllegalArgumentException("Unexpected DER type found. "
                    + sequence.getObjectAt(j).getClass().getName() + " found at index " + j + ".");
            }
        }
    }

    /**
     * @return the uniformResourceIdentifier
     */
    public ASN1IA5String getUniformResourceIdentifier() {
        return uniformResourceIdentifier;
    }

    /**
     * @param uniformResourceIdentifier the uniformResourceIdentifier to set
     */
    public void setUniformResourceIdentifier(final ASN1IA5String uniformResourceIdentifier) {
        this.uniformResourceIdentifier = uniformResourceIdentifier;
    }

    /**
     * @return the hashAlgorithm
     */
    public AlgorithmIdentifier getHashAlgorithm() {
        return hashAlgorithm;
    }

    /**
     * @param hashAlgorithm the hashAlgorithm to set
     */
    public void setHashAlgorithm(final AlgorithmIdentifier hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    /**
     * @return the hashValue
     */
    public ASN1BitString getHashValue() {
        return hashValue;
    }

    /**
     * @param hashValue the hashValue to set
     */
    public void setHashValue(final ASN1BitString hashValue) {
        this.hashValue = hashValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("URIReference{");
        sb.append("uniformResourceIdentifier=").append(uniformResourceIdentifier.getString());
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
