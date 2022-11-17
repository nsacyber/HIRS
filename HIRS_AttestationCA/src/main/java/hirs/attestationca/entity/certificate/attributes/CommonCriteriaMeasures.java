package hirs.attestationca.entity.certificate.attributes;

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

        private final String value;

        /**
         * Basic constructor.
         * @param value string containing the value.
         */
        EvaluationStatus(final String value) {
            this.value = value;
        }

        /**
         * Get the string value from the EvaluationStatus.
         * @return the string containing the value.
         */
        public String getValue() {
            return this.value;
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

        private final String value;

        /**
         * Basic constructor.
         * @param value string containing the value.
         */
        StrengthOfFunction(final String value) {
            this.value = value;
        }

        /**
         * Get the string value from the StrengthOfFunction.
         * @return the string containing the value.
         */
        public String getValue() {
            return this.value;
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

        private final String value;
        /**
         * Basic constructor.
         * @param value string containing the value.
         */
        EvaluationAssuranceLevel(final String value) {
            this.value = value;
        }

        /**
         * Get the string value from the StrengthOfFunction.
         * @return the string containing the value.
         */
        public String getValue() {
            return this.value;
        }
    }

    private ASN1IA5String version;
    private EvaluationAssuranceLevel assurancelevel;
    private EvaluationStatus evaluationStatus;
    private ASN1Boolean plus;
    private StrengthOfFunction strengthOfFunction;
    private ASN1ObjectIdentifier profileOid;
    private URIReference profileUri;
    private ASN1ObjectIdentifier targetOid;
    private URIReference targetUri;

    /**
     * Default constructor.
     */
    public CommonCriteriaMeasures() {
        this.version = null;
        this.assurancelevel = null;
        this.evaluationStatus = null;
        this.plus = ASN1Boolean.FALSE;
        this.strengthOfFunction = null;
        this.profileOid = null;
        this.profileUri = null;
        this.targetOid = null;
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
        version = ASN1IA5String.getInstance(sequence.getObjectAt(index));
        ++index;
        ASN1Enumerated enumarated = ASN1Enumerated.getInstance(sequence.getObjectAt(index));
        ++index;
        //Throw exception when is not between 1 and 7
        if (enumarated.getValue().intValue() <= 0
                || enumarated.getValue().intValue() > EvaluationAssuranceLevel.values().length) {
            throw new IllegalArgumentException("Invalid assurance level.");
        }
        assurancelevel = EvaluationAssuranceLevel.values()[enumarated.getValue().intValue() - 1];
        enumarated = ASN1Enumerated.getInstance(sequence.getObjectAt(index));
        ++index;
        evaluationStatus = EvaluationStatus.values()[enumarated.getValue().intValue()];
        //Default plus value
        plus = ASN1Boolean.FALSE;

        //Current sequence index
        if (sequence.getObjectAt(index).toASN1Primitive() instanceof ASN1Boolean) {
            plus = ASN1Boolean.getInstance(sequence.getObjectAt(index));
            index++;
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
                    enumarated = ASN1Enumerated.getInstance(taggedObj, false);
                    strengthOfFunction
                            = StrengthOfFunction.values()[enumarated.getValue().intValue()];
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

     /**
     * @return the version
     */
    public ASN1IA5String getVersion() {
        return version;
    }

    /**
     * Set the version.
     * @param version the version to set
     */
    public void setVersion(final ASN1IA5String version) {
        this.version = version;
    }

    /**
     * @return the assurancelevel
     */
    public EvaluationAssuranceLevel getAssurancelevel() {
        return assurancelevel;
    }

    /**
     * @param assurancelevel the assurancelevel to set
     */
    public void setAssurancelevel(final EvaluationAssuranceLevel assurancelevel) {
        this.assurancelevel = assurancelevel;
    }

    /**
     * @return the evaluationStatus
     */
    public EvaluationStatus getEvaluationStatus() {
        return evaluationStatus;
    }

    /**
     * @param evaluationStatus the evaluationStatus to set
     */
    public void setEvaluationStatus(final EvaluationStatus evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }

    /**
     * @return the plus
     */
    public ASN1Boolean getPlus() {
        return plus;
    }

    /**
     * @param plus the plus to set
     */
    public void setPlus(final ASN1Boolean plus) {
        this.plus = plus;
    }

    /**
     * @return the strengthOfFunction
     */
    public StrengthOfFunction getStrengthOfFunction() {
        return strengthOfFunction;
    }

    /**
     * @param strengthOfFunction the strengthOfFunction to set
     */
    public void setStrengthOfFunction(final StrengthOfFunction strengthOfFunction) {
        this.strengthOfFunction = strengthOfFunction;
    }

    /**
     * @return the profileOid
     */
    public ASN1ObjectIdentifier getProfileOid() {
        return profileOid;
    }

    /**
     * @param profileOid the profileOid to set
     */
    public void setProfileOid(final ASN1ObjectIdentifier profileOid) {
        this.profileOid = profileOid;
    }

    /**
     * @return the profileUri
     */
    public URIReference getProfileUri() {
        return profileUri;
    }

    /**
     * @param profileUri the profileUri to set
     */
    public void setProfileUri(final URIReference profileUri) {
        this.profileUri = profileUri;
    }

    /**
     * @return the targetOid
     */
    public ASN1ObjectIdentifier getTargetOid() {
        return targetOid;
    }

    /**
     * @param targetOid the targetOid to set
     */
    public void setTargetOid(final ASN1ObjectIdentifier targetOid) {
        this.targetOid = targetOid;
    }

    /**
     * @return the targetUri
     */
    public URIReference getTargetUri() {
        return targetUri;
    }

    /**
     * @param targetUri the targetUri to set
     */
    public void setTargetUri(final URIReference targetUri) {
        this.targetUri = targetUri;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ComponentIdentifier{");
        sb.append("version=").append(version.toString());
        sb.append(", assurancelevel=").append(assurancelevel.getValue());
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
