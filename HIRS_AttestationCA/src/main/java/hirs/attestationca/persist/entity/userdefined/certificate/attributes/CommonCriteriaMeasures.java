package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1IA5String;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;

/**
 * Basic class that handle CommonCriteriaMeasures for the Platform Certificate
 * Attribute.
 * <pre>
 * CommonCriteriaMeasures ::= SEQUENCE {
 *      version IA5STRING (SIZE (1..STRMAX)), "2.2" or "3.1";
 *      assurancelevel EvaluationAssuranceLevel,
 *      evaluationStatus EvaluationStatus,
 *      plus BOOLEAN DEFAULT FALSE,
 *      strengthOfFunction [0] IMPLICIT StrengthOfFunction OPTIONAL,
 *      profileOid [1] IMPLICIT OBJECT IDENTIFIER OPTIONAL,
 *      profileUri [2] IMPLICIT URIReference OPTIONAL,
 *      targetOid [3] IMPLICIT OBJECT IDENTIFIER OPTIONAL,
 *      targetUri [4] IMPLICIT URIReference OPTIONAL }
 * </pre>
 */
@Getter @Setter
public class CommonCriteriaMeasures {

    private static final int STRENGTH_OF_FUNCTION = 0;
    private static final int PROFILE_OID = 1;
    private static final int PROFILE_URI = 2;
    private static final int TARGET_OID = 3;
    private static final int TARGET_URI = 4;

    /**
     * A type to handle the evaluation status used in the Common Criteria Measurement.
     * Ordering of enum types is intentional and their ordinal values correspond to enum
     * values in the TCG spec.
     *
     * <pre>
     * EvaluationStatus ::= ENUMERATED {
     *      designedToMeet (0),
     *      evaluationInProgress (1),
     *      evaluationCompleted (2) }
     * </pre>
     */
    public enum EvaluationStatus {
        /**
         * Evaluation designed to meet.
         */
        DESIGNEDTOMEET("designed To Meet"),
        /**
         * Evaluation in progress.
         */
        EVALUATIONINPROGRESS("evaluation In Progress"),
        /**
         * Evaluation completed.
         */
        EVALUATIONCOMPLETED("evaluation Completed");

        @Getter
        private final String value;

        /**
         * Basic constructor.
         * @param value string containing the value.
         */
        EvaluationStatus(final String value) {
            this.value = value;
        }
    }

    /**
     * A type to handle the strength of function used in the Common Criteria Measurement.
     * Ordering of enum types is intentional and their ordinal values correspond to enum
     * values in the TCG spec.
     *
     * <pre>
     * StrengthOfFunction ::= ENUMERATED {
     *      basic (0),
     *      medium (1),
     *      high (2) }
     * </pre>
     */
    public enum StrengthOfFunction {
        /**
         * Basic function.
         */
        BASIC("basic"),
        /**
         * Medium function.
         */
        MEDIUM("medium"),
        /**
         * Hight function.
         */
        HIGH("high");

        @Getter
        private final String value;

        /**
         * Basic constructor.
         * @param value string containing the value.
         */
        StrengthOfFunction(final String value) {
            this.value = value;
        }
    }

    /**
     * A type to handle the evaluation assurance aevel used in the Common Criteria Measurement.
     * Ordering of enum types is intentional and their ordinal values correspond to enum
     * values in the TCG spec.
     *
     * <pre>
     * EvaluationAssuranceLevel ::= ENUMERATED {
     *      levell (1),
     *      level2 (2),
     *      level3 (3),
     *      level4 (4),
     *      level5 (5),
     *      level6 (6),
     *      level7 (7) }
     * </pre>
     */
    public enum EvaluationAssuranceLevel {
        /**
         * Evaluation Assurance Level 1.
         */
        LEVEL1("level 1"),
        /**
         * Evaluation Assurance Level 2.
         */
        LEVEL2("level 2"),
        /**
         * Evaluation Assurance Level 3.
         */
        LEVEL3("level 3"),
        /**
         * Evaluation Assurance Level 4.
         */
        LEVEL4("level 4"),
        /**
         * Evaluation Assurance Level 5.
         */
        LEVEL5("level 5"),
        /**
         * Evaluation Assurance Level 6.
         */
        LEVEL6("level 6"),
        /**
         * Evaluation Assurance Level 7.
         */
        LEVEL7("level 7");

        @Getter
        private final String value;
        /**
         * Basic constructor.
         * @param value string containing the value.
         */
        EvaluationAssuranceLevel(final String value) {
            this.value = value;
        }
    }

    private ASN1IA5String version;
    private EvaluationAssuranceLevel assuranceLevel;
    private EvaluationStatus evaluationStatus;
    private ASN1Boolean plus;
    private StrengthOfFunction strengthOfFunction;
    private ASN1ObjectIdentifier profileOid;
    private ASN1ObjectIdentifier targetOid;
    private URIReference profileUri;
    private URIReference targetUri;

    /**
     * Default constructor.
     */
    public CommonCriteriaMeasures() {
        this.version = null;
        this.assuranceLevel = null;
        this.evaluationStatus = null;
        this.plus = ASN1Boolean.FALSE;
        this.strengthOfFunction = null;
        this.profileOid = null;
        this.targetOid = null;
        this.profileUri = null;
        this.targetUri = null;
    }

    /**
     * Constructor given the SEQUENCE that contains Common Criteria Measures.
     * @param sequence containing the the common criteria measures
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public CommonCriteriaMeasures(final ASN1Sequence sequence) throws IllegalArgumentException {
        //Get all the mandatory values
        int index = 0;
        version = ASN1IA5String.getInstance(sequence.getObjectAt(index++));
        ASN1Enumerated enumerated = ASN1Enumerated.getInstance(sequence.getObjectAt(index++));
        //Throw exception when is not between 1 and 7
        if (enumerated.getValue().intValue() <= 0
                || enumerated.getValue().intValue() > EvaluationAssuranceLevel.values().length) {
            throw new IllegalArgumentException("Invalid assurance level.");
        }
        assuranceLevel = EvaluationAssuranceLevel.values()[enumerated.getValue().intValue() - 1];
        enumerated = ASN1Enumerated.getInstance(sequence.getObjectAt(index++));
        evaluationStatus = EvaluationStatus.values()[enumerated.getValue().intValue()];
        //Default plus value
        plus = ASN1Boolean.FALSE;

        //Current sequence index
        if (sequence.getObjectAt(index).toASN1Primitive() instanceof ASN1Boolean) {
            plus = ASN1Boolean.getInstance(sequence.getObjectAt(index++));
        }

        //Optional values (default to null or empty)
        strengthOfFunction = null;
        profileOid = null;
        profileUri = null;
        targetOid = null;
        targetUri = null;

        //Sequence for the URIReference
        ASN1Sequence uriSequence;

        //Continue reading the sequence
        for (; index < sequence.size(); index++) {
            ASN1TaggedObject taggedObj = ASN1TaggedObject.getInstance(sequence.getObjectAt(index));
            switch (taggedObj.getTagNo()) {
                case STRENGTH_OF_FUNCTION:
                    enumerated = ASN1Enumerated.getInstance(taggedObj, false);
                    strengthOfFunction
                            = StrengthOfFunction.values()[enumerated.getValue().intValue()];
                    break;
                case PROFILE_OID:
                    profileOid = ASN1ObjectIdentifier.getInstance(taggedObj, false);
                    break;
                case PROFILE_URI:
                    uriSequence = ASN1Sequence.getInstance(taggedObj, false);
                    profileUri = new URIReference(uriSequence);
                    break;
                case TARGET_OID:
                    targetOid = ASN1ObjectIdentifier.getInstance(taggedObj, false);
                    break;
                case TARGET_URI:
                    uriSequence = ASN1Sequence.getInstance(taggedObj, false);
                    targetUri = new URIReference(uriSequence);
                    break;
                default:
                    throw new IllegalArgumentException("Common criteria measures contains "
                            + "invalid tagged object.");
            }
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ComponentIdentifier{");
        sb.append("version=").append(version.toString());
        sb.append(", assuranceLevel=").append(assuranceLevel.getValue());
        sb.append(", evaluationStatus=").append(evaluationStatus.getValue());
        sb.append(", plus=").append(plus.toString());
        //Not null optional objects
        sb.append(", strengthOfFunction=");
        if (strengthOfFunction != null) {
            sb.append(strengthOfFunction.getValue());
        }
        sb.append(", profileOid=");
        if (profileOid != null) {
            sb.append(profileOid.getId());
        }
        sb.append(", profileUri=");
        if (profileUri != null) {
            sb.append(profileUri.toString());
        }
        sb.append(", targetOid=");
        if (targetOid != null) {
            sb.append(targetOid.getId());
        }
        sb.append(", targetUri=");
        if (targetUri != null) {
            sb.append(targetUri.toString());
        }
        sb.append("}");

        return sb.toString();
    }
}
