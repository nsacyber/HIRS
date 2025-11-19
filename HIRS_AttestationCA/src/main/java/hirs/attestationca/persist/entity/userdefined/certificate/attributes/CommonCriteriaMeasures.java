package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1IA5String;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import static hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential.parseUriReference;

/**
 * Basic class that handle CommonCriteriaMeasures for the Platform Certificate
 * Attribute.
 * The URIReference and algorithm identifier fields
 * (profileUri, profileAlgOid, profileAlgParameters, profileHashValue,
 * targetUri, targetAlgOid, targetAlgParameters, targetHashValue) have been flattened
 * into this class. They do not make use of HIRS' URIReference.java
 * or BouncyCastle's AlgorithmIdentifier.
 *
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
@Getter
@Setter
public class CommonCriteriaMeasures implements Serializable {

    private static final int STRENGTH_OF_FUNCTION = 0;
    private static final int PROFILE_OID = 1;
    private static final int PROFILE_URI = 2;
    private static final int TARGET_OID = 3;
    private static final int TARGET_URI = 4;
    private String ccVersion;
    private EvaluationAssuranceLevel assuranceLevel;
    private EvaluationStatus evaluationStatus;
    private boolean ccPlus;
    private StrengthOfFunction strengthOfFunction;
    private String profileOid;
    private String profileUri;
    private String profileAlgOid;
    private byte[] profileAlgParameters;
    private byte[] profileHashValue;
    private String targetOid;
    private String targetUri;
    private String targetAlgOid;
    private byte[] targetAlgParameters;
    private byte[] targetHashValue;

    /**
     * Default constructor.
     */
    public CommonCriteriaMeasures() {
        this.ccVersion = null;
        this.assuranceLevel = null;
        this.evaluationStatus = null;
        this.ccPlus = Boolean.FALSE;
        this.strengthOfFunction = null;
        this.profileOid = null;
        this.profileUri = null;
        this.profileAlgOid = null;
        this.profileAlgParameters = null;
        this.profileHashValue = null;
        this.targetOid = null;
        this.targetUri = null;
        this.targetAlgOid = null;
        this.targetAlgParameters = null;
        this.targetHashValue = null;
    }

    /**
     * Constructor given the SEQUENCE that contains Common Criteria Measures.
     *
     * @param sequence containing the the common criteria measures
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public CommonCriteriaMeasures(final ASN1Sequence sequence) throws IllegalArgumentException {
        //Get all the mandatory values
        int index = 0;
        ccVersion = ASN1IA5String.getInstance(sequence.getObjectAt(index++)).getString();
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
        ccPlus = Boolean.FALSE;

        //Current sequence index
        if (sequence.getObjectAt(index).toASN1Primitive() instanceof ASN1Boolean) {
            ccPlus = ASN1Boolean.getInstance(sequence.getObjectAt(index++)).isTrue();
        }

        //Optional values (default to null or empty)
        strengthOfFunction = null;
        profileOid = null;
        profileUri = null;
        profileAlgOid = null;
        profileAlgParameters = null;
        profileHashValue = null;
        targetOid = null;
        targetUri = null;
        targetAlgOid = null;
        targetAlgParameters = null;
        targetHashValue = null;

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
                    profileOid = ASN1ObjectIdentifier.getInstance(taggedObj, false).getId();
                    break;
                case PROFILE_URI:
                    uriSequence = ASN1Sequence.getInstance(taggedObj, false);
                    Map<String, Object> profileUriMap = parseUriReference(uriSequence);
                    profileUri = (String) profileUriMap.get("uri");
                    profileAlgOid = (String) profileUriMap.get("algOid");
                    profileAlgParameters = (byte[]) profileUriMap.get("algParams");
                    profileHashValue = (byte[]) profileUriMap.get("hashValue");
                    break;
                case TARGET_OID:
                    targetOid = ASN1ObjectIdentifier.getInstance(taggedObj, false).getId();
                    break;
                case TARGET_URI:
                    uriSequence = ASN1Sequence.getInstance(taggedObj, false);
                    Map<String, Object> targetUriMap = parseUriReference(uriSequence);
                    targetUri = (String) targetUriMap.get("uri");
                    targetAlgOid = (String) targetUriMap.get("algOid");
                    targetAlgParameters = (byte[]) targetUriMap.get("algParams");
                    targetHashValue = (byte[]) targetUriMap.get("hashValue");
                    break;
                default:
                    throw new IllegalArgumentException("Common criteria measures contains "
                            + "invalid tagged object.");
            }
        }
    }

    /**
     * Creates a custom string representation of the Common Criteria Measures object.
     *
     * @return a string representation of Common Criteria Measures
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CommonCriteriaMeasures{");
        sb.append("ccVersion=").append(ccVersion);
        sb.append(", assuranceLevel=").append(assuranceLevel != null ? assuranceLevel.getValue() : "");
        sb.append(", evaluationStatus=").append(evaluationStatus != null ? evaluationStatus.getValue() : "");
        sb.append(", ccPlus=").append(ccPlus);
        //Not null optional objects
        sb.append(", strengthOfFunction=").append(strengthOfFunction != null ? strengthOfFunction.getValue() : "");
        sb.append(", profileOid=").append(profileOid != null ? profileOid : "");
        sb.append(", profileUri=").append(profileUri != null ? profileUri : "");
        sb.append(", profileAlgOid=").append(profileAlgOid != null ? profileAlgOid : "");
        sb.append(", profileAlgParameters=")
                .append(profileAlgParameters != null ? Arrays.toString(profileAlgParameters).replace(",", " ") : "");
        sb.append(", profileHashValue=")
                .append(profileHashValue != null ? Arrays.toString(profileHashValue).replace(",", " ") : "");
        sb.append(", targetOid=").append(targetOid != null ? targetOid : "");
        sb.append(", targetUri=").append(targetUri != null ? targetUri : "");
        sb.append(", targetAlgOid=").append(targetAlgOid != null ? targetAlgOid : "");
        sb.append(", targetAlgParameters=")
                .append(targetAlgParameters != null ? Arrays.toString(targetAlgParameters).replace(",", " ") : "");
        sb.append(", targetHashValue=")
                .append(targetHashValue != null ? Arrays.toString(targetHashValue).replace(",", " ") : "");
        sb.append("}");

        return sb.toString();
    }

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
         *
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
         *
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
         *
         * @param value string containing the value.
         */
        EvaluationAssuranceLevel(final String value) {
            this.value = value;
        }
    }
}
