package hirs.data.persist.certificate.attributes;

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
public class URIReference {
    private DERIA5String uniformResourceIdentifier;
    private AlgorithmIdentifier hashAlgorithm;
    private DERBitString hashValue;

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
    public URIReference(final DERIA5String uniformResourceIdentifier,
                    final AlgorithmIdentifier hashAlgorithm,
                    final DERBitString hashValue) {
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
            if (sequence.getObjectAt(j) instanceof DERIA5String) {
                this.uniformResourceIdentifier = DERIA5String.getInstance(sequence.getObjectAt(j));
            } else if (sequence.getObjectAt(j) instanceof AlgorithmIdentifier) {
                this.hashAlgorithm =
                        AlgorithmIdentifier.getInstance(sequence.getObjectAt(j));
            } else if (sequence.getObjectAt(j) instanceof DERBitString) {
                this.hashValue = DERBitString.getInstance(sequence.getObjectAt(j));
            } else {
                throw new IllegalArgumentException("PlatformPropertiesURI contains invalid type.");
            }
        }
    }

    /**
     * @return the uniformResourceIdentifier
     */
    public DERIA5String getUniformResourceIdentifier() {
        return uniformResourceIdentifier;
    }

    /**
     * @param uniformResourceIdentifier the uniformResourceIdentifier to set
     */
    public void setUniformResourceIdentifier(final DERIA5String uniformResourceIdentifier) {
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
    public DERBitString getHashValue() {
        return hashValue;
    }

    /**
     * @param hashValue the hashValue to set
     */
    public void setHashValue(final DERBitString hashValue) {
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
