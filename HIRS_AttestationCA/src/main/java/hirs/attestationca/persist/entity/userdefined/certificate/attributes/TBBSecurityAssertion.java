package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERIA5String;

import java.math.BigInteger;

/**
 * Basic class that handle component identifiers from the Platform Configuration
 * Attribute.
 * <pre>
 * TBBSecurityAssertions ::= SEQUENCE {
 *      version Version DEFAULT v1,
 *      ccInfo [0] IMPLICIT CommonCriteriaMeasures OPTIONAL,
 *      fipsLevel [1] IMPLICIT FIPSLevel OPTIONAL,
 *      rtmType [2] IMPLICIT MeasurementRootType OPTIONAL,
 *      iso9000Certified BOOLEAN DEFAULT FALSE,
 *      iso9000Uri IA5STRING (SIZE (1..URIMAX)) OPTIONAL }
 * </pre>
 */
@AllArgsConstructor
public class TBBSecurityAssertion {

    private static final int CCINFO = 0;
    private static final int FIPSLEVEL = 1;
    private static final int RTMTYPE = 2;

    /**
     * A type to handle the evaluation status used in the Common Criteria Measurement.
     * Ordering of enum types is intentional and their ordinal values correspond to enum
     * values in the TCG spec.
     *
     * <pre>
     * MeasurementRootType ::= ENUMERATED {
     *    static (0),
     *    dynamic (1),
     *    nonHost (2),
     *    hybrid (3),
     *    physical (4),
     *    virtual (5) }
     * </pre>
     */
    public enum MeasurementRootType {
        /**
         * Static measurement root type.
         */
        STATIC("static"),
        /**
         * Dynamic  measurement root type.
         */
        DYNAMIC("dynamic"),
        /**
         * Non-Host measurement root type.
         */
        NONHOST("nonHost"),
        /**
         * Hybrid measurement root type.
         */
        HYBRID("hybrid"),
        /**
         * Physical measurement root type.
         */
        PHYSICAL("physical"),
        /**
         * Virtual measurement root type.
         */
        VIRTUAL("virtual");

        @Getter
        private final String value;

        /**
         * Basic constructor.
         * @param value string containing the value.
         */
        MeasurementRootType(final String value) {
            this.value = value;
        }
    }

    private ASN1Integer version;
    @Getter @Setter
    private CommonCriteriaMeasures ccInfo;
    @Getter @Setter
    private FIPSLevel fipsLevel;
    private MeasurementRootType rtmType;
    private ASN1Boolean iso9000Certified;
    private DERIA5String iso9000Uri;

    /**
     * Default constructor.
     */
    public TBBSecurityAssertion() {
        version = null;
        ccInfo = null;
        fipsLevel = null;
        rtmType = null;
        iso9000Certified = null;
        iso9000Uri = null;
    }

    /**
     * Constructor given the SEQUENCE that contains a TBBSecurityAssertion Object.
     * @param sequence containing the the TBB Security Assertion
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public TBBSecurityAssertion(final ASN1Sequence sequence) throws IllegalArgumentException {
        int index = 0;
        //sequence size
        int sequenceSize = sequence.size();

        //Default values
        version = new ASN1Integer(BigInteger.valueOf(0));   //Default v1 (0)
        ccInfo = null;
        fipsLevel = null;
        rtmType = null;
        iso9000Certified = ASN1Boolean.FALSE;
        iso9000Uri = null;

        // Only contains defaults
        if (sequence.size() == 0) {
            return;
        }

        // Get version if present
        if (sequence.getObjectAt(index).toASN1Primitive() instanceof ASN1Integer) {
            version = ASN1Integer.getInstance(sequence.getObjectAt(index));
            index++;
        }

        // Check if it's a tag value
        while (index < sequenceSize
                && sequence.getObjectAt(index).toASN1Primitive() instanceof ASN1TaggedObject) {
            ASN1TaggedObject taggedObj = ASN1TaggedObject.getInstance(sequence.getObjectAt(index));
            switch (taggedObj.getTagNo()) {
                case CCINFO:
                    ASN1Sequence cciSequence = ASN1Sequence.getInstance(taggedObj, false);
                    ccInfo = new CommonCriteriaMeasures(cciSequence);
                    break;
                case FIPSLEVEL:
                    ASN1Sequence fipsSequence = ASN1Sequence.getInstance(taggedObj, false);
                    fipsLevel = new FIPSLevel(fipsSequence);
                    break;
                case RTMTYPE:
                    ASN1Enumerated enumerated = ASN1Enumerated.getInstance(taggedObj, false);
                    rtmType = MeasurementRootType.values()[enumerated.getValue().intValue()];
                    break;
                default:
                    throw new IllegalArgumentException("TBB Security Assertion contains "
                            + "invalid tagged object.");
            }
            index++;
        }
        // Check if it's a boolean
        if (index < sequenceSize
                && sequence.getObjectAt(index).toASN1Primitive() instanceof ASN1Boolean) {
            iso9000Certified = ASN1Boolean.getInstance(sequence.getObjectAt(index));
            index++;
        }
        // Check if it's a IA5String
        if (index < sequenceSize
                && sequence.getObjectAt(index).toASN1Primitive() instanceof DERIA5String) {
            iso9000Uri = DERIA5String.getInstance(sequence.getObjectAt(index));
        }
    }

    /**
     * @return the version
     */
    public ASN1Integer getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(final ASN1Integer version) {
        this.version = version;
    }

    /**
     * @return the rtmType
     */
    public MeasurementRootType getRtmType() {
        return rtmType;
    }

    /**
     * @param rtmType the rtmType to set
     */
    public void setRtmType(final MeasurementRootType rtmType) {
        this.rtmType = rtmType;
    }

    /**
     * @return the iso9000Certified
     */
    public ASN1Boolean getIso9000Certified() {
        return iso9000Certified;
    }

    /**
     * @param iso9000Certified the iso9000Certified to set
     */
    public void setIso9000Certified(final ASN1Boolean iso9000Certified) {
        this.iso9000Certified = iso9000Certified;
    }

    /**
     * @return the iso9000Uri
     */
    public DERIA5String getIso9000Uri() {
        return iso9000Uri;
    }

    /**
     * @param iso9000Uri the iso9000Uri to set
     */
    public void setIso9000Uri(final DERIA5String iso9000Uri) {
        this.iso9000Uri = iso9000Uri;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TBBSecurityAssertion{");
        sb.append("version=").append(version.toString());
        //Optional values not null
        sb.append(", ccInfo=");
        if (ccInfo != null) {
            sb.append(ccInfo.toString());
        }
        sb.append(", fipsLevel=");
        if (fipsLevel != null) {
            sb.append(fipsLevel.toString());
        }
        sb.append(", rtmType=");
        if (rtmType != null) {
            sb.append(rtmType.getValue());
        }
        sb.append(", iso9000Certified=").append(iso9000Certified.toString());
        sb.append(", iso9000Uri=");
        if (iso9000Uri != null) {
            sb.append(iso9000Uri.getString());
        }
        sb.append("}");

        return sb.toString();
    }
}
