package hirs.data.persist.certificate;

import com.google.common.base.Preconditions;
import hirs.data.persist.certificate.attributes.ComponentIdentifier;
import hirs.data.persist.certificate.attributes.PlatformConfiguration;
import hirs.data.persist.certificate.attributes.PlatformConfigurationV1;
import hirs.data.persist.certificate.attributes.TBBSecurityAssertion;
import hirs.data.persist.certificate.attributes.URIReference;
import hirs.data.persist.certificate.attributes.V2.PlatformConfigurationV2;
import hirs.persist.CertificateManager;
import hirs.persist.CertificateSelector;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.AttributeCertificate;
import org.bouncycastle.asn1.x509.AttributeCertificateInfo;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.PolicyQualifierInfo;
import org.bouncycastle.asn1.x509.UserNotice;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class persists Platform credentials by extending the base Certificate
 * class with fields unique to a Platform credentials, as defined in the Trusted
 * Computing Group Credential Profiles, specification v.1.2.
 */
@Entity
public class PlatformCredential extends DeviceAssociatedCertificate {
    private static final Logger LOGGER = LogManager.getLogger(PlatformCredential.class);
    private static final int TCG_SPECIFICATION_LENGTH = 3;
    // These are Object Identifiers (OIDs) for sections in the credentials
    private static final String POLICY_QUALIFIER_CPSURI = "1.3.6.1.5.5.7.2.1";
    private static final String POLICY_QUALIFIER_USER_NOTICE = "1.3.6.1.5.5.7.2.2";

    // OID for TCG Attributes
    private static final String PLATFORM_MANUFACTURER = "2.23.133.2.4";
    private static final String PLATFORM_MODEL = "2.23.133.2.5";
    private static final String PLATFORM_VERSION = "2.23.133.2.6";
    private static final String PLATFORM_SERIAL = "2.23.133.2.23";
    private static final String PLATFORM_BASEBOARD_CHASSIS_COMBINED = "2.23.133.5.1.6";

    // OID for TCG Platform Class Common Attributes
    private static final String PLATFORM_MANUFACTURER_2_0 = "2.23.133.5.1.1";
    private static final String PLATFORM_MODEL_2_0 = "2.23.133.5.1.4";
    private static final String PLATFORM_VERSION_2_0 = "2.23.133.5.1.5";
    private static final String PLATFORM_SERIAL_2_0 = "2.23.133.5.1.6";

    // OID for Certificate Attributes
    private static final String TCG_PLATFORM_SPECIFICATION = "2.23.133.2.17";
    private static final String TPM_SECURITY_ASSERTION = "2.23.133.2.18";
    private static final String TBB_SECURITY_ASSERTION = "2.23.133.2.19";
    private static final String TCG_CREDENTIAL_SPECIFICATION = "2.23.133.2.23";
    private static final String PLATFORM_CONFIGURATION_URI = "2.23.133.5.1.3";
    private static final String PLATFORM_CONFIGURATION = "2.23.133.5.1.7.1";
    private static final String PLATFORM_CONFIGURATION_V2 = "2.23.133.5.1.7.2";
    private static final String PLATFORM_CREDENTIAL_TYPE = "2.23.133.2.25";
    private static final String PLATFORM_BASE_CERT = "2.23.133.8.2";
    private static final String PLATFORM_DELTA_CERT = "2.23.133.8.5";

    /**
     * TCG Platform Specification values
     * At this time these are placeholder values.
     */
    private static final Map<String, String> TCG_PLATFORM_MAP = new HashMap<String, String>() {{
        put("#00000000", "Unclassified");
        put("#00000001", "PC Client");
        put("#00000002", "PDA");
        put("#00000003", "CELLPHONE");
        put("#00000004", "SERVER");
        put("#00000005", "PERIPHERAL");
        put("#00000006", "TSS");
        put("#00000007", "STORAGE");
        put("#00000008", "AUTHENTICATION");
        put("#00000009", "EMBEDDED");
        put("#00000010", "HARD COPY");
        put("#00000011", "INFRASTRUCTURE");
        put("#00000012", "VIRTUALIZATION");
        put("#00000013", "TNC");
        put("#00000014", "MULTI-TENANT");
    }};

    // number of extra bytes potentially present in a cert header.
    private static final int PC_CERT_HEADER_BYTE_COUNT = 8;

    /**
     * TCPA Trusted Platform Endorsement.
     */
    public static final String CERTIFICATE_TYPE_1_2 = "TCPA Trusted Platform Endorsement";

    /**
     * TCG Trusted Platform Endorsement.
     */
    public static final String CERTIFICATE_TYPE_2_0 = "TCG Trusted Platform Endorsement";

    /**
     * This class enables the retrieval of PlatformCredentials by their attributes.
     */
    public static class Selector extends CertificateSelector<PlatformCredential> {
        /**
         * Construct a new CertificateSelector that will use the given {@link CertificateManager} to
         * retrieve one or many PlatformCredentials.
         *
         * @param certificateManager the certificate manager to be used to retrieve certificates
         */
        public Selector(final CertificateManager certificateManager) {
            super(certificateManager, PlatformCredential.class);
        }

        /**
         * Specify a manufacturer that certificates must have to be considered as matching.
         * @param manufacturer the manufacturer to query, not empty or null
         * @return this instance (for chaining further calls)
         */
        public Selector byManufacturer(final String manufacturer) {
            setFieldValue(MANUFACTURER_FIELD, manufacturer);
            return this;
        }

        /**
         * Specify a model that certificates must have to be considered as matching.
         * @param model the model to query, not empty or null
         * @return this instance (for chaining further calls)
         */
        public Selector byModel(final String model) {
            setFieldValue(MODEL_FIELD, model);
            return this;
        }

        /**
         * Specify a version that certificates must have to be considered as matching.
         * @param version the version to query, not empty or null
         * @return this instance (for chaining further calls)
         */
        public Selector byVersion(final String version) {
            setFieldValue(VERSION_FIELD, version);
            return this;
        }

        /**
         * Specify a serial number that certificates must have to be considered as matching.
         * @param serialNumber the serial number to query, not empty or null
         * @return this instance (for chaining further calls)
         */
        public Selector bySerialNumber(final String serialNumber) {
            setFieldValue(SERIAL_NUMBER_FIELD, serialNumber);
            return this;
        }

        /**
         * Specify a board serial number that certificates must have to be considered as matching.
         * @param boardSerialNumber the board serial number to query, not empty or null
         * @return this instance (for chaining further calls)
         */
        public Selector byBoardSerialNumber(final String boardSerialNumber) {
            setFieldValue(PLATFORM_SERIAL_FIELD, boardSerialNumber);
            return this;
        }

        /**
         * Specify a chassis serial number that certificates must have to be considered as matching.
         * @param chassisSerialNumber the board serial number to query, not empty or null
         * @return this instance (for chaining further calls)
         */
        public Selector byChassisSerialNumber(final String chassisSerialNumber) {
            setFieldValue(CHASSIS_SERIAL_NUMBER_FIELD, chassisSerialNumber);
            return this;
        }

        /**
         * Specify a device id that certificates must have to be considered
         * as matching.
         *
         * @param device the device id to query
         * @return this instance (for chaining further calls)
         */
        public Selector byDeviceId(final UUID device) {
            setFieldValue(DEVICE_ID_FIELD, device);
            return this;
        }
    }

    @Column
    private String credentialType = null;

    @Column
    private boolean platformBase = false;

    private static final String MANUFACTURER_FIELD = "manufacturer";
    @Column
    private String manufacturer = null;

    private static final String MODEL_FIELD = "model";
    @Column
    private String model = null;

    private static final String VERSION_FIELD = "version";
    @Column
    private String version = null;

    private static final String PLATFORM_SERIAL_FIELD = "platformSerial";
    @Column
    private String platformSerial = null;

    private static final String CHASSIS_SERIAL_NUMBER_FIELD = "chassisSerialNumber";
    @Column
    private String chassisSerialNumber;

    @Column
    private int majorVersion = 0;

    @Column
    private int minorVersion = 0;

    @Column
    private int revisionLevel = 0;

    @Column
    private int tcgCredentialMajorVersion = 0;

    @Column
    private int tcgCredentialMinorVersion = 0;

    @Column
    private int tcgCredentialRevisionLevel = 0;

    @Column
    private String platformClass = null;

    @Column(length = MAX_MESSAGE_LENGTH)
    private String componentFailures = Strings.EMPTY;
    @Column(length = MAX_MESSAGE_LENGTH)
    private String componentFailureMessage = Strings.EMPTY;

    @Transient
    private EndorsementCredential endorsementCredential = null;

    private String platformChainType = Strings.EMPTY;
    private boolean isDeltaChain = false;


    /**
     * Get a Selector for use in retrieving PlatformCredentials.
     *
     * @param certMan the CertificateManager to be used to retrieve persisted certificates
     * @return a PlatformCredential.Selector instance to use for retrieving certificates
     */
    public static Selector select(final CertificateManager certMan) {
        return new Selector(certMan);
    }

    /**
     * Construct a new PlatformCredential given its binary contents.  ParseFields is
     * optionally run.  The given certificate should represent either an X509 certificate
     * or X509 attribute certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @param parseFields boolean True to parse fields
     * @throws IOException if there is a problem extracting information from the certificate\
     */
    public PlatformCredential(final byte[] certificateBytes,
                              final boolean parseFields) throws IOException {
        super(certificateBytes);
        if (parseFields) {
            parseFields();
        }
    }

    /**
     * Construct a new PlatformCredential given its binary contents.  The given
     * certificate should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public PlatformCredential(final byte[] certificateBytes) throws IOException {
        this(certificateBytes, true);
    }

    /**
     * Construct a new PlatformCredential by parsing the file at the given path.  The given
     * certificate should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificatePath the path on disk to a certificate
     * @throws IOException if there is a problem reading the file
     */
    public PlatformCredential(final Path certificatePath) throws IOException {
        this(readBytes(certificatePath), true);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected PlatformCredential() {

    }

    /**
     * Validate the signature on the attribute certificate in this holder.
     *
     * @param verifierProvider a ContentVerifierProvider that can generate a
     * verifier for the signature.
     * @return true if the signature is valid, false otherwise.
     * @throws IOException if the signature cannot be processed or is inappropriate.
     */
    public boolean isSignatureValid(final ContentVerifierProvider verifierProvider)
        throws IOException {
            AttributeCertificate attCert = getAttributeCertificate();
            AttributeCertificateInfo acinfo = getAttributeCertificate().getAcinfo();

            // Check if the algorithm identifier is the same
            if (!isAlgIdEqual(acinfo.getSignature(), attCert.getSignatureAlgorithm())) {
                throw new IOException("signature invalid - algorithm identifier mismatch");
            }

            ContentVerifier verifier;

            try {
                // Set ContentVerifier with the signature that will verify
                verifier = verifierProvider.get((acinfo.getSignature()));

            } catch (Exception e) {
                throw new IOException("unable to process signature: " + e.getMessage(), e);
            }

            return verifier.verify(attCert.getSignatureValue().getOctets());
    }
    /**
     * Parses the bytes as an PC. If parsing fails initially, the optionally present header
     * is removed and tried again. The cert header, if present, contains some certificate length
     * information which isn't needed for parsing.
     * @param certificateBytes the bytes of the PC
     * @return the PC if a valid credential, null otherwise
     */
    public static PlatformCredential parseWithPossibleHeader(final byte[] certificateBytes) {
        PlatformCredential credential = null;

        try {
            // first, attempt parsing as is
            credential = new PlatformCredential(certificateBytes);
        } catch (Exception e) {
            // attempt parsing again after removing extra header bytes.
            if (certificateBytes.length > PC_CERT_HEADER_BYTE_COUNT) {
                LOGGER.debug("Attempting parse after removing extra header bytes");
                try {
                    byte[] truncatedBytes = ArrayUtils.subarray(
                            certificateBytes, PC_CERT_HEADER_BYTE_COUNT,
                            certificateBytes.length);
                    credential = new PlatformCredential(truncatedBytes);
                } catch (Exception e1) {
                    LOGGER.warn("Failed to parse PC after multiple attempts", e1);
                }
            } else {
                LOGGER.warn("EK parsing failed (only one attempt possible)", e);
            }
        }
        return credential;
    }

    /**
     * Get the Platform Credential type.
     *
     * @return the credential type
     */
    public String getCredentialType() {
        return credentialType;
    }

    /**
     * Get the type of platform certificate.
     *
     * @return flag for base certificate
     */
    public boolean isBase() {
        return platformBase;
    }

    /**
     * Getter for the string representation of the platform type.
     *
     * @return the TCG platform type { base | delta }
     */
    public String getPlatformType() {
        return platformChainType;
    }

    /**
     * Get the Platform Manufacturer.
     *
     * @return the manufacturer
     */
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * Get the Platform Model.
     *
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * Get the Platform Version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the Platform Board Serial number.
     *
     * @return the board serial number
     */
    public String getPlatformSerial() {
        return platformSerial;
    }

    /**
     * Get the Platform Chassis Serial number.
     *
     * @return the chassis serial number
     */
    public String getChassisSerialNumber() {
        return chassisSerialNumber;
    }

    /**
     * Gets the platform specification major version.
     *
     * @return the platform specification major version
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Gets the platform specification minor version.
     *
     * @return the platform specification minor version
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Gets the platform specification revision level.
     *
     * @return the platform specification revision level
     */
    public int getRevisionLevel() {
        return revisionLevel;
    }

    /**
     * Gets the TCG Credential major version.
     *
     * @return the TCG Credential Major Version
     */
    public int getTcgCredentialMajorVersion() {
        return tcgCredentialMajorVersion;
    }

    /**
     * Gets the TCG Credential minor version.
     *
     * @return the TCG Credential minor version
     */
    public int getTcgCredentialMinorVersion() {
        return tcgCredentialMinorVersion;
    }

    /**
     * Gets the TCG Credential revision level.
     *
     * @return the TCG Credential revision level
     */
    public int getTcgCredentialRevisionLevel() {
        return tcgCredentialRevisionLevel;
    }

    /**
     * Gets the platform specification platform class.
     *
     * @return the platform specification platform class
     */
    public String getPlatformClass() {
        return TCG_PLATFORM_MAP.get(platformClass);
    }

    /**
     * Gets the endorsement credential the PC refers to.
     *
     * @return the platform specification platform class
     */
    public EndorsementCredential getEndorsementCredential() {
        return endorsementCredential;
    }

    /**
     * Sets the endorsement credential the PC refers to.
     *
     * @param ec The Endorsement Credential object associated with the Plat Cred object
     */
    public void setEndorsementCredential(final EndorsementCredential ec) {
        endorsementCredential = ec;
    }

    private void parseFields() throws IOException {
        AttributeCertificateInfo certificate = getAttributeCertificate().getAcinfo();
        Map<String, String> policyQualifier = getPolicyQualifier(certificate);
        credentialType = policyQualifier.get("userNotice");

        // Parse data based on certificate type (1.2 vs 2.0)
        switch (credentialType) {
            case CERTIFICATE_TYPE_1_2:
                parseAttributeCert(certificate);
                break;
            case CERTIFICATE_TYPE_2_0:
                parseAttributeCert2(certificate);
                break;
            default:
                throw new IOException("Invalid Attribute Credential Type: " + credentialType);
        }

        // Get TCG Platform Specification Information
        for (ASN1Encodable enc : certificate.getAttributes().toArray()) {
            Attribute attr = Attribute.getInstance(enc);
            if (attr.getAttrType().toString().equals(TCG_PLATFORM_SPECIFICATION)) {
                ASN1Sequence tcgPlatformSpecification
                        = ASN1Sequence.getInstance(attr.getAttrValues().getObjectAt(0));
                ASN1Sequence tcgSpecificationVersion
                        = ASN1Sequence.getInstance(tcgPlatformSpecification.getObjectAt(0));

                this.majorVersion = Integer.parseInt(
                                        tcgSpecificationVersion.getObjectAt(0).toString());
                this.minorVersion = Integer.parseInt(
                                        tcgSpecificationVersion.getObjectAt(1).toString());
                this.revisionLevel = Integer.parseInt(
                                        tcgSpecificationVersion.getObjectAt(2).toString());

                this.platformClass = tcgPlatformSpecification.getObjectAt(1).toString();
            } else if (attr.getAttrType().toString().equals(PLATFORM_CREDENTIAL_TYPE)) {
                ASN1Sequence tcgPlatformType = ASN1Sequence.getInstance(
                        attr.getAttrValues().getObjectAt(0));
                ASN1ObjectIdentifier platformOid = ASN1ObjectIdentifier.getInstance(
                        tcgPlatformType.getObjectAt(0));

                if (platformOid.getId().equals(PLATFORM_BASE_CERT)) {
                    this.platformBase = true;
                    this.platformChainType = "Base";
                    this.isDeltaChain = true;
                } else if (platformOid.getId().equals(PLATFORM_DELTA_CERT)) {
                    this.platformBase = false;
                    this.platformChainType = "Delta";
                    this.isDeltaChain = true;
                }
            }
        }
    }

    /**
     * Parse a 1.2 Platform Certificate (Attribute Certificate).
     * @param certificate Attribute Certificate
     */
    private void parseAttributeCert(final AttributeCertificateInfo certificate) {
        Extension subjectAlternativeNameExtension
                = certificate.getExtensions().getExtension(Extension.subjectAlternativeName);
        // It contains a Subject Alternative Name Extension
        if (subjectAlternativeNameExtension != null) {
            GeneralNames gnames =  GeneralNames.getInstance(
                                        subjectAlternativeNameExtension.getParsedValue());
            for (GeneralName gname : gnames.getNames()) {
                // Check if it's a directoryName [4] Name type
                if (gname.getTagNo() == GeneralName.directoryName) {
                    X500Name name = X500Name.getInstance(gname.getName());
                    for (RDN rdn: name.getRDNs()) {
                        for (AttributeTypeAndValue attTV: rdn.getTypesAndValues()) {
                            switch (attTV.getType().toString()) {
                                case PLATFORM_MANUFACTURER:
                                    this.manufacturer = attTV.getValue().toString();
                                    break;
                                case PLATFORM_MODEL:
                                    this.model = attTV.getValue().toString();
                                    break;
                                case PLATFORM_VERSION:
                                    this.version = attTV.getValue().toString();
                                    break;
                                case PLATFORM_SERIAL:
                                    this.platformSerial = attTV.getValue().toString();
                                    break;
                                case PLATFORM_BASEBOARD_CHASSIS_COMBINED:
                                    String[] combinedValues = attTV.getValue()
                                                                    .toString()
                                                                    .split(",");
                                    if (combinedValues.length != 2) {
                                        LOGGER.warn("Unable to parse combined "
                                                + "baseboard/chassis SN field");
                                    } else {
                                        this.chassisSerialNumber = combinedValues[0];
                                        this.platformSerial = combinedValues[1];
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Parse a 2.0 Platform Certificate (Attribute Certificate).
     * @param certificate Attribute Certificate
     */
    private void parseAttributeCert2(final AttributeCertificateInfo certificate)
            throws IOException {
        Extension subjectAlternativeNameExtension
                = certificate.getExtensions().getExtension(Extension.subjectAlternativeName);

        // It contains a Subject Alternative Name Extension
        if (subjectAlternativeNameExtension != null) {
            GeneralNames gnames = GeneralNames.getInstance(
                                    subjectAlternativeNameExtension.getParsedValue());
            for (GeneralName gname : gnames.getNames()) {
                // Check if it's a directoryName [4] Name type
                if (gname.getTagNo() == GeneralName.directoryName) {
                    X500Name name = X500Name.getInstance(gname.getName());
                    for (RDN rdn: name.getRDNs()) {
                        for (AttributeTypeAndValue attTV: rdn.getTypesAndValues()) {
                            switch (attTV.getType().toString()) {
                                case PLATFORM_MANUFACTURER_2_0:
                                    this.manufacturer = attTV.getValue().toString();
                                    break;
                                case PLATFORM_MODEL_2_0:
                                    this.model = attTV.getValue().toString();
                                    break;
                                case PLATFORM_VERSION_2_0:
                                    this.version = attTV.getValue().toString();
                                    break;
                                case PLATFORM_SERIAL_2_0:
                                    this.platformSerial = attTV.getValue().toString();
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
        // Get all the attributes map to check for validity
        try {
            getAllAttributes();
        } catch (IllegalArgumentException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Get the x509 Platform Certificate version.
     * @return a big integer representing the certificate version.
     */
    @Override
    public int getX509CredentialVersion() {
        try {
            return getAttributeCertificate()
                    .getAcinfo()
                    .getVersion()
                    .getValue().intValue();
        } catch (IOException ex) {
            LOGGER.warn("X509 Credential Version not found.");
            LOGGER.error(ex);
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Get the cPSuri from the Certificate Policies.
     * @return cPSuri from the CertificatePolicies.
     * @throws IOException when reading the certificate.
     */
    public String getCPSuri() throws IOException {
        Map<String, String> policyQualifier
                = getPolicyQualifier(getAttributeCertificate().getAcinfo());
        if (policyQualifier.get("cpsURI") != null && !policyQualifier.get("cpsURI").isEmpty()) {
            return policyQualifier.get("cpsURI");
        }

        return null;
    }

    /**
     * Getter for the component failures.
     * @return string of failures.
     */
    public String getComponentFailures() {
        return componentFailures;
    }

    /**
     * Setter for the component failure instance.
     * @param componentFailures a string of failures.
     */
    public void setComponentFailures(final String componentFailures) {
        this.componentFailures = componentFailures;
    }

    /**
     * Getter for the component failures message.
     * @return string of failures.
     */
    public String getComponentFailureMessage() {
        return componentFailureMessage;
    }

    /**
     * Setter for the component failure message instance.
     * @param componentFailureMessage a string of failures.
     */
    public void setComponentFailureMessage(final String componentFailureMessage) {
        this.componentFailureMessage = componentFailureMessage;
    }

    /**
     * Get the Platform Configuration Attribute from the Platform Certificate.
     * @return a map with all the attributes
     * @throws IllegalArgumentException when there is a parsing error
     * @throws IOException when reading the certificate.
     */
    public Map<String, Object> getAllAttributes()
            throws IllegalArgumentException, IOException {
        Map<String, Object> attributes = new HashMap<>();
        ASN1Sequence attributeSequence;
        // Check all attributes for Platform Configuration
        for (ASN1Encodable enc: getAttributeCertificate().getAcinfo().getAttributes().toArray()) {
            Attribute attr = Attribute.getInstance(enc);
            attributeSequence
                        = ASN1Sequence.getInstance(attr.getAttrValues().getObjectAt(0));
            // Parse sequence based on the attribute OID
            switch (attr.getAttrType().getId()) {
                case TBB_SECURITY_ASSERTION:
                    attributes.put("tbbSecurityAssertion",
                            new TBBSecurityAssertion(attributeSequence));
                    break;
                case PLATFORM_CONFIGURATION_URI:
                    attributes.put("platformConfigurationURI",
                            new URIReference(attributeSequence));
                    break;
                case PLATFORM_CONFIGURATION:
                    attributes.put("platformConfiguration",
                            new PlatformConfigurationV1(attributeSequence));
                    break;
                case PLATFORM_CONFIGURATION_V2:
                    attributes.put("platformConfiguration",
                            new PlatformConfigurationV2(attributeSequence));
                    break;
                case TCG_PLATFORM_SPECIFICATION:
                case PLATFORM_CREDENTIAL_TYPE:
                    // handled in parseFields
                    break;
                case TCG_CREDENTIAL_SPECIFICATION:
                    getTCGCredentialSpecification(attributeSequence);
                    break;
                default:
                    // No class defined for this attribute
                    LOGGER.warn("No class defined for attribute with OID: "
                            + attr.getAttrType().getId());
                    break;
            }
        }
        return attributes;
    }

    /**
     * Get the specified attribute from the Platform Certificate.
     * @param attributeName to retrieve from the map.
     * @return an Object with the attribute.
     * @throws IllegalArgumentException when there is a parsing error
     * @throws IOException when reading the certificate.
     */
    public Object getAttribute(final String attributeName)
            throws IllegalArgumentException, IOException {
        return getAllAttributes().get(attributeName);
    }

    /**
     * Get the Platform Configuration Attribute from the Platform Certificate.
     * @return a map with the Platform Configuration information.
     * @throws IllegalArgumentException when there is a parsing error
     * @throws IOException when reading the certificate.
     */
    public PlatformConfiguration getPlatformConfiguration()
            throws IllegalArgumentException, IOException {

        if (getAttribute("platformConfiguration") != null
                && getAttribute("platformConfiguration") instanceof PlatformConfiguration) {
            return (PlatformConfiguration) getAttribute("platformConfiguration");
        }

        return null;
    }

    /**
     * Get the Platform Configuration URI Attribute from the Platform Certificate.
     * @return an URIReference object to the Platform Configuration URI.
     * @throws IllegalArgumentException when there is a parsing error
     * @throws IOException when reading the certificate.
     */
    public URIReference getPlatformConfigurationURI()
            throws IllegalArgumentException, IOException {
        if (getAttribute("platformConfigurationURI") != null
                && getAttribute("platformConfigurationURI") instanceof URIReference) {
            return (URIReference) getAttribute("platformConfigurationURI");
        }
        return null;
    }

    /**
     * Get the TBB Security Assertion from the Platform Certificate.
     * @return a TBBSecurityAssertion object.
     * @throws IllegalArgumentException when there is a parsing error
     * @throws IOException when reading the certificate.
     */
    public TBBSecurityAssertion getTBBSecurityAssertion()
            throws IllegalArgumentException, IOException {
        if (getAttribute("tbbSecurityAssertion") != null
                && getAttribute("tbbSecurityAssertion") instanceof TBBSecurityAssertion) {
            return (TBBSecurityAssertion) getAttribute("tbbSecurityAssertion");
        }
        return null;
    }

    /**
     * This method sets the TCG Credential fields from a certificate, if provided.
     *
     * @param attributeSequence The sequence associated with 2.23.133.2.23
     */
    private void getTCGCredentialSpecification(final ASN1Sequence attributeSequence) {
        try {
            this.tcgCredentialMajorVersion = Integer.parseInt(
                    attributeSequence.getObjectAt(0).toString());
            this.tcgCredentialMinorVersion = Integer.parseInt(
                    attributeSequence.getObjectAt(1).toString());
            this.tcgCredentialRevisionLevel = Integer.parseInt(
                    attributeSequence.getObjectAt(2).toString());
        } catch (NumberFormatException nfEx) {
            // ill-formed ASN1
            String fieldContents = attributeSequence.toString();

            if (fieldContents != null && fieldContents.contains(",")) {
                fieldContents = fieldContents.replaceAll("[^a-zA-Z0-9,]", "");
                String[] fields = fieldContents.split(",");

                if (fields.length == TCG_SPECIFICATION_LENGTH) {
                    this.tcgCredentialMajorVersion = Integer.parseInt(fields[0]);
                    this.tcgCredentialMinorVersion = Integer.parseInt(fields[1]);
                    this.tcgCredentialRevisionLevel = Integer.parseInt(fields[2]);
                }
            }
        }
    }

    /**
     * Get the list of component identifiers if there are any.
     * @return the list of component identifiers if there are any
     */
    public List<ComponentIdentifier> getComponentIdentifiers() {
        try {
            PlatformConfiguration platformConfig = getPlatformConfiguration();
            if (platformConfig != null) {
                return platformConfig.getComponentIdentifier();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to parse Platform Configuration from Credential or find"
                    + "component identifiers");
        }
        return Collections.emptyList();
    }

    //This method was auto generated and lightly edited for style
    @Override
    @SuppressWarnings({"checkstyle:avoidinlineconditionals", "checkstyle:needbraces" })
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PlatformCredential that = (PlatformCredential) o;

        if (majorVersion != that.majorVersion)
            return false;
        if (minorVersion != that.minorVersion)
            return false;
        if (revisionLevel != that.revisionLevel)
            return false;
        if (credentialType != null ? !credentialType.equals(that.credentialType)
                : that.credentialType != null)
            return false;
        if (manufacturer != null ? !manufacturer.equals(that.manufacturer)
                : that.manufacturer != null)
            return false;
        if (model != null ? !model.equals(that.model) : that.model != null)
            return false;
        if (version != null ? !version.equals(that.version) : that.version != null)
            return false;
        if (platformSerial != null ? !platformSerial.equals(that.platformSerial)
                : that.platformSerial != null)
            return false;
        if (platformClass != null ? !platformClass.equals(that.platformClass)
                : that.platformClass != null)
            return false;

        return true;
    }

    //This method was auto generated and lightly edited for style
    @Override
    @SuppressWarnings({"checkstyle:avoidinlineconditionals", "checkstyle:magicnumber" })
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (credentialType != null ? credentialType.hashCode() : 0);
        result = 31 * result + (manufacturer != null ? manufacturer.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (platformSerial != null ? platformSerial.hashCode() : 0);
        result = 31 * result + majorVersion;
        result = 31 * result + minorVersion;
        result = 31 * result + revisionLevel;
        result = 31 * result + (platformClass != null ? platformClass.hashCode() : 0);
        return result;
    }

    /**
     * Verify if the AlgorithmIdentifiers are equal.
     *
     * @param id1 AlgorithIdentifier one
     * @param id2 AlgorithIdentifier two
     * @return True if are the same, False if not
     */
    public static boolean isAlgIdEqual(final AlgorithmIdentifier id1,
                                       final AlgorithmIdentifier id2) {
        if (!id1.getAlgorithm().equals(id2.getAlgorithm())) {
            return false;
        }
        if (id1.getParameters() == null) {
            if (id2.getParameters() != null && !id2.getParameters().equals(DERNull.INSTANCE)) {
                return false;
            }
            return true;
        }
        if (id2.getParameters() == null) {
            if (id1.getParameters() != null && !id1.getParameters().equals(DERNull.INSTANCE)) {
                return false;
            }
            return true;
        }
        return id1.getParameters().equals(id2.getParameters());
    }

     /**
     * Get the PolicyQualifier from the Certificate Policies Extension.
     *
     * @param certificate Attribute Certificate information
     * @return Policy Qualifier from the Certificate Policies Extension
     */
    public static Map<String, String> getPolicyQualifier(
                final AttributeCertificateInfo certificate) {
        Preconditions.checkArgument(certificate.getExtensions() != null,
                "Platform certificate should have extensions.");

        CertificatePolicies certPolicies
                = CertificatePolicies.fromExtensions(certificate.getExtensions());
        Map<String, String> policyQualifiers = new HashMap<>();
        String userNoticeQualifier = "";
        String cpsURI = "";

        if (certPolicies != null) {
            // Must contain at least one Policy
            for (PolicyInformation policy : certPolicies.getPolicyInformation()) {
                for (ASN1Encodable pQualifierInfo: policy.getPolicyQualifiers().toArray()) {
                    PolicyQualifierInfo info = PolicyQualifierInfo.getInstance(pQualifierInfo);
                    // Subtract the data based on the OID
                    switch (info.getPolicyQualifierId().getId()) {
                        case POLICY_QUALIFIER_CPSURI:
                            cpsURI = DERIA5String.getInstance(info.getQualifier()).getString();
                            break;
                        case POLICY_QUALIFIER_USER_NOTICE:
                            UserNotice userNotice = UserNotice.getInstance(info.getQualifier());
                            userNoticeQualifier = userNotice.getExplicitText().getString();
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        // Add to map
        policyQualifiers.put("userNotice", userNoticeQualifier);
        policyQualifiers.put("cpsURI", cpsURI);

        return policyQualifiers;
    }
}
