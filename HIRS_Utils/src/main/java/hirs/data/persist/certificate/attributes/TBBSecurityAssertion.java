package hirs.data.persist.certificate.attributes;

import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;

import com.fasterxml.jackson.annotation.JsonIgnore;

import hirs.utils.HexUtils;

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
 *      iso9000Uri IA5STRING (SIZE (1..URIMAX)) OPTIONAL,
 *      platformFwSignatureVerification [3] IMPLICIT BIT STRING OPTIONAL,
 *      platformFirmwareUpdateCompliance [4] IMPLICIT BIT STRING OPTIONAL,
 *      firmwareCapabilities [5] IMPLICIT BIT STRING OPTIONAL,
 *      hardwareCapabilities [6] IMPLICIT BIT STRING OPTIONAL }
 * </pre>
 */
public class TBBSecurityAssertion {

    private static final int CCINFO = 0;
    private static final int FIPSLEVEL = 1;
    private static final int RTMTYPE = 2;
    private static final int PLATFORMFWSIGNATUREVERIFICATION = 3;
    private static final int PLATFORMFIRMWAREUPDATECOMPLIANCE = 4;
    private static final int FIRMWARECAPABILITIES = 5;
    private static final int HARDWARECAPABILITIES = 6;

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

        private final String value;

        /**
         * Basic constructor.
         * @param value string containing the value.
         */
        MeasurementRootType(final String value) {
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

    private ASN1Integer version;
    private CommonCriteriaMeasures ccInfo;
    private FIPSLevel fipsLevel;
    private MeasurementRootType rtmType;
    private ASN1Boolean iso9000Certified;
    private DERIA5String iso9000Uri;
    @JsonIgnore
    private DERBitString platformFwSignatureVerification;
    @JsonIgnore
    private DERBitString platformFirmwareUpdateCompliance;
    @JsonIgnore
    private DERBitString firmwareCapabilities;
    @JsonIgnore
    private DERBitString hardwareCapabilities;
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
        platformFwSignatureVerification = null;
        platformFirmwareUpdateCompliance = null;
        firmwareCapabilities = null;
        hardwareCapabilities = null;
    }

    /**
     * Constructor given the components values.
     *
     * @param version represents the version of the TBB Security Assertion
     * @param ccInfo represents the common criteria measures
     * @param fipsLevel represent the FIPSLevel
     * @param rtmType represent the measurement root type
     * @param iso9000Certified indicate if is iso9000 certifies
     * @param iso9000Uri URI string for the iso9000
     */
    public TBBSecurityAssertion(final ASN1Integer version,
            final CommonCriteriaMeasures ccInfo,
            final FIPSLevel fipsLevel,
            final MeasurementRootType rtmType,
            final ASN1Boolean iso9000Certified,
            final DERIA5String iso9000Uri) {
        this(version, ccInfo, fipsLevel, rtmType, iso9000Certified, iso9000Uri,
                null, null, null, null);
    }

    /**
     * Constructor given the components values.
     *
     * @param version represents the version of the TBB Security Assertion
     * @param ccInfo represents the common criteria measures
     * @param fipsLevel represent the FIPSLevel
     * @param rtmType represent the measurement root type
     * @param iso9000Certified indicate if is iso9000 certifies
     * @param iso9000Uri URI string for the iso9000
     * @param platformFwSignatureVerification represent the FIM platform
     *            firmware signature verification method
     * @param platformFirmwareUpdateCompliance represent the FIM platform
     *            firmware update compliance level
     * @param firmwareCapabilities represent the security capabilities of
     *            the firmware as defined in the FIM
     * @param hardwareCapabilities represent the security capabilities of
     *            the platform motherboard or its attached components
     */
    public TBBSecurityAssertion(final ASN1Integer version,
            final CommonCriteriaMeasures ccInfo,
            final FIPSLevel fipsLevel,
            final MeasurementRootType rtmType,
            final ASN1Boolean iso9000Certified,
            final DERIA5String iso9000Uri,
            final DERBitString platformFwSignatureVerification,
            final DERBitString platformFirmwareUpdateCompliance,
            final DERBitString firmwareCapabilities,
            final DERBitString hardwareCapabilities) {
        this.version = version;
        this.ccInfo = ccInfo;
        this.fipsLevel = fipsLevel;
        this.rtmType = rtmType;
        this.iso9000Certified = iso9000Certified;
        this.iso9000Uri = iso9000Uri;
        this.platformFwSignatureVerification = null;
        this.platformFirmwareUpdateCompliance = null;
        this.firmwareCapabilities = null;
        this.hardwareCapabilities = null;
    }

    /**
     * Constructor given the SEQUENCE that contains a TBBSecurityAssertion Object.
     * @param sequence containing the the TBB Security Assertion
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public TBBSecurityAssertion(final ASN1Sequence sequence) throws IllegalArgumentException {
        //sequence size
        int sequenceSize = sequence.size();

        //Default values
        version = new ASN1Integer(BigInteger.valueOf(0));   //Default v1 (0)
        ccInfo = null;
        fipsLevel = null;
        rtmType = null;
        iso9000Certified = ASN1Boolean.FALSE;
        iso9000Uri = null;
        platformFwSignatureVerification = null;
        platformFirmwareUpdateCompliance = null;
        firmwareCapabilities = null;
        hardwareCapabilities = null;

        // Only contains defaults
        if (sequence.size() == 0) {
            return;
        }

        // Parse sequence elements
        int nonTaggedPosition = 0;
        for (int index = 0; index < sequenceSize; index++) {
            // Get version if present
            if (sequence.getObjectAt(index).toASN1Primitive() instanceof ASN1Integer
                    && nonTaggedPosition == 0) {
                version = ASN1Integer.getInstance(sequence.getObjectAt(index));
                nonTaggedPosition++;
            }

            // Check if it's a tag value
            if (sequence.getObjectAt(index).toASN1Primitive() instanceof ASN1TaggedObject) {
                ASN1TaggedObject taggedObj =
                        ASN1TaggedObject.getInstance(sequence.getObjectAt(index));
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
                        ASN1Enumerated enumerated =
                            ASN1Enumerated.getInstance(taggedObj, false);
                        rtmType =
                            MeasurementRootType.values()[enumerated.getValue().intValue()];
                        break;
                    case PLATFORMFWSIGNATUREVERIFICATION:
                        if (taggedObj.getObject() instanceof DEROctetString) {
                            // workaround for an issue with DERBitString.fromOctetString
                            // in BC 1.59. Looks fixed as of BC 1.69.
                            platformFwSignatureVerification =
                                new DERBitString(
                                    ((DEROctetString) taggedObj.getObject()).getOctets());
                        } else {
                            platformFwSignatureVerification =
                                DERBitString.getInstance(taggedObj.getObject());
                        }
                        break;
                    case PLATFORMFIRMWAREUPDATECOMPLIANCE:
                        if (taggedObj.getObject() instanceof DEROctetString) {
                            // workaround for an issue with DERBitString.fromOctetString
                            // in BC 1.59. Looks fixed as of BC 1.69.
                            platformFirmwareUpdateCompliance =
                                new DERBitString(
                                    ((DEROctetString) taggedObj.getObject()).getOctets());
                        } else {
                            platformFirmwareUpdateCompliance =
                                DERBitString.getInstance(taggedObj.getObject());
                        }
                        break;
                    case FIRMWARECAPABILITIES:
                        if (taggedObj.getObject() instanceof DEROctetString) {
                            // workaround for an issue with DERBitString.fromOctetString
                            // in BC 1.59. Looks fixed as of BC 1.69.
                            firmwareCapabilities =
                                new DERBitString(
                                    ((DEROctetString) taggedObj.getObject()).getOctets());
                        } else {
                            firmwareCapabilities =
                                DERBitString.getInstance(taggedObj.getObject());
                        }
                        break;
                    case HARDWARECAPABILITIES:
                        if (taggedObj.getObject() instanceof DEROctetString) {
                            // workaround for an issue with DERBitString.fromOctetString
                            // in BC 1.59. Looks fixed as of BC 1.69.
                            hardwareCapabilities =
                                new DERBitString(
                                    ((DEROctetString) taggedObj.getObject()).getOctets());
                        } else {
                            hardwareCapabilities =
                                DERBitString.getInstance(taggedObj.getObject());
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("TBB Security Assertion contains "
                                + "invalid tagged object.");
                }
            }
            // Check if it's a boolean
            if (sequence.getObjectAt(index).toASN1Primitive() instanceof ASN1Boolean
                    && nonTaggedPosition == 1) {
                iso9000Certified = ASN1Boolean.getInstance(sequence.getObjectAt(index));
                nonTaggedPosition++;
            }
            // Check if it's a IA5String
            if (sequence.getObjectAt(index).toASN1Primitive() instanceof DERIA5String
                    && nonTaggedPosition == 2) {
                iso9000Uri = DERIA5String.getInstance(sequence.getObjectAt(index));
                nonTaggedPosition++;
            }
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
     * @return the ccInfo
     */
    public CommonCriteriaMeasures getCcInfo() {
        return ccInfo;
    }

    /**
     * @param ccInfo the ccInfo to set
     */
    public void setCcInfo(final CommonCriteriaMeasures ccInfo) {
        this.ccInfo = ccInfo;
    }

    /**
     * @return the fipsLevel
     */
    public FIPSLevel getFipsLevel() {
        return fipsLevel;
    }

    /**
     * @param fipsLevel the fipsLevel to set
     */
    public void setFipsLevel(final FIPSLevel fipsLevel) {
        this.fipsLevel = fipsLevel;
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

    /**
     * @return the platformFwSignatureVerification byte array
     */
    public DERBitString getPlatformFwSignatureVerification() {
        return platformFwSignatureVerification;
    }

    /**
     * @return the platformFwSignatureVerification as a string
     */
    public String getPlatformFwSignatureVerificationStr() {
        if (platformFwSignatureVerification == null) {
            return null;
        }
        return HexUtils.byteArrayToHexString(platformFwSignatureVerification.getOctets());
    }

    /**
     * @return the platformFirmwareUpdateCompliance byte array
     */
    public DERBitString getPlatformFirmwareUpdateCompliance() {
        return platformFirmwareUpdateCompliance;
    }

    /**
     * @return the platformFirmwareUpdateCompliance as a string
     */
    public String getPlatformFirmwareUpdateComplianceStr() {
        if (platformFirmwareUpdateCompliance == null) {
            return null;
        }
        return HexUtils.byteArrayToHexString(platformFirmwareUpdateCompliance.getOctets());
    }

    /**
     * @return the firmwareCapabilities byte array
     */
    public DERBitString getFirmwareCapabilities() {
        return firmwareCapabilities;
    }

    /**
     * @return the firmwareCapabilities as a string
     */
    public String getFirmwareCapabilitiesStr() {
        if (firmwareCapabilities == null) {
            return null;
        }
        return HexUtils.byteArrayToHexString(firmwareCapabilities.getOctets());
    }

    /**
     * @return the hardwareCapabilities byte array
     */
    public DERBitString getHardwareCapabilities() {
        return hardwareCapabilities;
    }

    /**
     * @return the hardwareCapabilities as a string
     */
    public String getHardwareCapabilitiesStr() {
        if (hardwareCapabilities == null) {
            return null;
        }
        return HexUtils.byteArrayToHexString(hardwareCapabilities.getOctets());
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
