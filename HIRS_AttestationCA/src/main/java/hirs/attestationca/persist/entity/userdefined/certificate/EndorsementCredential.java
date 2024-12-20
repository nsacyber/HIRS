package hirs.attestationca.persist.entity.userdefined.certificate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.TPMSecurityAssertions;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.TPMSpecification;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Null;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1UTCTime;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DERExternal;
import org.bouncycastle.asn1.DERGeneralString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERNumericString;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERT61String;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DERUniversalString;
import org.bouncycastle.asn1.DERVisibleString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class persists an Endorsement Credential by extending the base Certificate
 * class with fields unique to Endorsement credentials, as defined in the Trusted
 * Computing Group Credential Profiles, specification v.1.2.
 * <p>
 * trustedcomputinggroup.org/wp-content/uploads/Credential_Profiles_V1.2_Level2_Revision8.pdf
 */
@Log4j2
@SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
        justification = "property credentialType is guaranteed to always be non-null/initialized. Warning"
                + "stems from auto-generated lombok equals and hashcode method doing redundant null checks.")
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class EndorsementCredential extends DeviceAssociatedCertificate {

    // Indices for ASN1 OBJ items needed for parsing information
    private static final int ASN1_OBJ_ID = 0;
    private static final int ASN1_OBJ_PRIMITIVE = 1;
    private static final int ASN1_FAMILY_INDEX = 0;
    private static final int ASN1_LEVEL_INDEX = 1;
    private static final int ASN1_REV_INDEX = 2;
    private static final int ASN1_VER_INDEX = 0;
    private static final int ASN1_UPGRADEABLE_INDEX = 1;

    private static final int EK_LOC_VAL_MIN = 0;
    private static final int EK_LOC_VAL_MAX = 2;
    private static final int EK_TYPE_VAL_MIN = 0;
    private static final int EK_TYPE_VAL_MAX = 3;

    // EK Tag index values
    private static final int EK_TYPE_TAG = 0;
    private static final int EK_LOC_TAG = 1;
    private static final int EK_CERT_LOC_TAG = 2;

    private static final int ASN1_SEQ_UNKNOWN_SIZE = 2;
    private static final int ASN1_SEQ_KNOWN_SIZE = 3;

    private static final String TPM_MODEL = "2.23.133.2.2";

    private static final String TPM_VERSION = "2.23.133.2.3";

    private static final String TPM_MANUFACTURER = "2.23.133.2.1";

    private static final String TPM_SPECIFICATION = "2.23.133.2.16";

    private static final String TPM_SECURITY_ASSERTIONS = "2.23.133.2.18";

    private static final String CREDENTIAL_TYPE_LABEL = "1.3.6.1.5.5.7.2.2";

    // number of extra bytes potentially present in a cert header.
    private static final int EK_CERT_HEADER_BYTE_COUNT = 7;

    /**
     * this field is part of the TCG EC specification, but has not yet been found in
     * manufacturer-provided ECs, and is therefore not currently parsed.
     */
    @Getter
    @Column
    private final String credentialType = "TCPA Trusted Platform Module Endorsement";

    /**
     * this field is part of the TCG EC specification, but has not yet been found in
     * manufacturer-provided ECs, and is therefore not currently parsed.
     */
    @Getter
    @Column
    private final String policyReference = null; // optional

    /**
     * this field is part of the TCG EC specification, but has not yet been found in
     * manufacturer-provided ECs, and is therefore not currently parsed.
     */
    @Getter
    @Column
    private final String revocationLocator = null; // optional

    @Getter
    @Column
    private String manufacturer = null;

    @Getter
    @Column
    private String model = null;

    @Getter
    @Column
    private String version = null;

    @Getter
    @Embedded
    private TPMSpecification tpmSpecification = null;

    @Getter
    @Embedded
    private TPMSecurityAssertions tpmSecurityAssertions = null; //optional

    @Transient
    private Set<String> expectedOids;

    @Transient
    private Map<String, Object> parsedFields;

    /**
     * Construct a new EndorsementCredential given its binary contents.  The given
     * certificate should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public EndorsementCredential(final byte[] certificateBytes) throws IOException {
        super(certificateBytes);
        parseCertificate();
    }

    /**
     * Construct a new EndorsementCredential by parsing the file at the given path.  The given
     * certificate should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificatePath the path on disk to a certificate
     * @throws IOException if there is a problem reading the file
     */
    public EndorsementCredential(final Path certificatePath) throws IOException {
        this(readBytes(certificatePath));
    }

    /**
     * Parses the bytes as an EK. If parsing fails initially, the optionally present header
     * is removed and tried again. The cert header, if present, contains some certificate length
     * information which isn't needed for parsing.
     *
     * @param certificateBytes the bytes of the EC
     * @return the EC if a valid credential, null otherwise
     */
    public static EndorsementCredential parseWithPossibleHeader(final byte[] certificateBytes) {
        try {
            // first, attempt parsing as is
            return new EndorsementCredential(certificateBytes);
        } catch (Exception e) {
            // attempt parsing again after removing extra header bytes.
            if (certificateBytes.length <= EK_CERT_HEADER_BYTE_COUNT) {
                throw new IllegalArgumentException("EK parsing failed (only one attempt "
                        + "possible", e);
            }
        }

        log.debug("Attempting parse after removing extra header bytes");
        try {
            byte[] truncatedBytes = ArrayUtils.subarray(
                    certificateBytes, EK_CERT_HEADER_BYTE_COUNT,
                    certificateBytes.length);
            return new EndorsementCredential(truncatedBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse EK after multiple attempts", e);
        }
    }

    /**
     * Sets up the OID fields for the parser to search for and prepares a
     * hashmap field to hold the discovered values. Must be called once before
     * an ASN1Primitive can be parsed.
     */
    private void prepareParser() {
        expectedOids = new HashSet<>();
        expectedOids.add(TPM_MODEL);
        expectedOids.add(TPM_VERSION);
        expectedOids.add(TPM_MANUFACTURER);
        expectedOids.add(TPM_SPECIFICATION);
        expectedOids.add(TPM_SECURITY_ASSERTIONS);
        expectedOids.add(CREDENTIAL_TYPE_LABEL);
        parsedFields = new HashMap<>();
    }

    /**
     * Takes the bytes of an X509 certificate and parses them to extract the relevant fields of an
     * Endorsement Credential Certificate. This works by making a single pass through all of the
     * ASN1Primitives in the certificate and searches for matching OID keys of specific values. If
     * matching OID keys are found, their values are encoded in the fields of the current
     * EndorsementCredential object.
     *
     * @throws IOException the input certificate bytes were not readable into an X509
     *                     certificate format
     */
    private void parseCertificate() throws IOException {
        prepareParser();
        // although we start with a byte representation, we need to change the encoding to
        // make it parseable
        ASN1InputStream asn1In = null;
        try {
            X509Certificate ec = super.getX509Certificate();
            asn1In = new ASN1InputStream(ec.getEncoded());

            ASN1Primitive obj = asn1In.readObject();
            ASN1Sequence seq;

            while (obj != null) {
                seq = ASN1Sequence.getInstance(obj);
                parseSequence(seq, false, null);
                obj = asn1In.readObject();
            }
        } catch (CertificateException e) {
            throw new IOException("Couldn't read certificate bytes");
        } finally {
            if (asn1In != null) {
                asn1In.close();
            }
        }

        String oid;
        Object value;
        // unpack fields from parsedFields and set field values
        for (Map.Entry<String, Object> entry : parsedFields.entrySet()) {
            oid = entry.getKey();
            value = entry.getValue();
            if (oid.equals(TPM_MODEL)) {
                model = value.toString();
                log.debug("Found TPM Model: {}", model);
            } else if (oid.equals(TPM_VERSION)) {
                version = value.toString();
                log.debug("Found TPM Version: {}", version);
            } else if (oid.equals(TPM_MANUFACTURER)) {
                manufacturer = value.toString();
                log.debug("Found TPM Manufacturer: {}", manufacturer);
            }
        }
    }

    /**
     * Parses the ASN1Sequence type by iteratively unpacking each successive element. If,
     * however, the method is set to add the sequence to the OID mapping, it may search for
     * patterns that correspond to the TPM Security Assertions and TPM Specification and set
     * those fields appropriately.
     *
     * @param seq          the sequence to parse
     * @param addToMapping whether or not to store the sequence value as an OID key/value value
     * @param key          the associated OID key with this value necessary if addToMapping is true
     * @throws IOException parsing individual subcomponents failed
     */
    private void parseSequence(final ASN1Sequence seq, final boolean addToMapping,
                               final String key) throws IOException {
        // need to check if an OID/Value pair
        // it is possible these pairs could be in a larger sequence of size != 2
        // but it appears that all expected TPM related fields are of size 2.
        // The other larger ones are only used for generic X509 fields, which we
        // don't need to extract here.
        if (seq.size() == ASN1_SEQ_UNKNOWN_SIZE) {
            ASN1Encodable obj1 = seq.getObjectAt(ASN1_OBJ_ID);
            ASN1Encodable obj2 = seq.getObjectAt(ASN1_OBJ_PRIMITIVE);
            if (obj1 instanceof ASN1ObjectIdentifier) {
                String oid = ((ASN1ObjectIdentifier) obj1).getId();
                if (expectedOids.contains(oid)) {
                    // parse and put object 2
                    parseSingle((ASN1Primitive) obj2, true, oid);
                } else {
                    // there may be subfields that are expected, so continue parsing
                    parseSingle((ASN1Primitive) obj2, false, null);
                }
            }

            // The next two are special sequences that have already been matched with an OID.
        } else if (addToMapping && key.equals(TPM_SPECIFICATION)
                && seq.size() == ASN1_SEQ_KNOWN_SIZE) {
            // Parse TPM Specification
            DERUTF8String family = (DERUTF8String) seq.getObjectAt(ASN1_FAMILY_INDEX);
            ASN1Integer level = (ASN1Integer) seq.getObjectAt(ASN1_LEVEL_INDEX);
            ASN1Integer revision = (ASN1Integer) seq.getObjectAt(ASN1_REV_INDEX);
            tpmSpecification = new TPMSpecification(family.getString(), level.getValue(),
                    revision.getValue());
            log.debug("Found TPM Spec:{}", tpmSpecification);
        } else if (addToMapping && key.equals(TPM_SECURITY_ASSERTIONS)) {
            // Parse TPM Security Assertions
            int seqPosition = 0;

            ASN1Integer ver;
            // Parse Security Assertions Version
            if (seq.getObjectAt(seqPosition) instanceof ASN1Integer) {
                ver = (ASN1Integer) seq.getObjectAt(seqPosition++);
            } else {
                // Default value of 1 if field not found
                ver = new ASN1Integer(BigInteger.ONE);
            }

            ASN1Boolean fieldUpgradeable;
            // Parse Security Assertions Field Upgradeable
            if (seq.getObjectAt(seqPosition) instanceof ASN1Boolean) {
                fieldUpgradeable = (ASN1Boolean) seq.getObjectAt(seqPosition++);
            } else {
                // Default value of false if field not found
                fieldUpgradeable = ASN1Boolean.getInstance(false);
            }

            tpmSecurityAssertions = new TPMSecurityAssertions(ver.getValue(),
                    fieldUpgradeable.isTrue());

            log.debug("Found TPM Assertions: {}", tpmSecurityAssertions);
            // Iterate through remaining fields to set optional attributes
            int tag;
            ASN1TaggedObject obj;
            for (int i = seqPosition; i < seq.size(); i++) {
                if (seq.getObjectAt(i) instanceof ASN1TaggedObject) {
                    obj = (ASN1TaggedObject) seq.getObjectAt(i);
                    tag = obj.getTagNo();
                    if (tag == EK_TYPE_TAG) {
                        int ekGenTypeVal = ((ASN1Enumerated) obj.getBaseObject()).getValue().intValue();
                        if (ekGenTypeVal >= EK_TYPE_VAL_MIN && ekGenTypeVal <= EK_TYPE_VAL_MAX) {
                            TPMSecurityAssertions.EkGenerationType ekGenType
                                    = TPMSecurityAssertions.EkGenerationType.values()[ekGenTypeVal];
                            tpmSecurityAssertions.setEkGenType(ekGenType);
                        }
                    } else if (tag == EK_LOC_TAG) {
                        int ekGenLocVal = ((ASN1Enumerated) obj.getBaseObject()).getValue().intValue();
                        if (ekGenLocVal >= EK_LOC_VAL_MIN && ekGenLocVal <= EK_LOC_VAL_MAX) {
                            TPMSecurityAssertions.EkGenerationLocation ekGenLocation
                                    = TPMSecurityAssertions.EkGenerationLocation.values()[ekGenLocVal];
                            tpmSecurityAssertions.setEkGenerationLocation(ekGenLocation);
                        }
                    } else if (tag == EK_CERT_LOC_TAG) {
                        int ekCertGenLocVal = ((ASN1Enumerated) obj.getBaseObject())
                                .getValue().intValue();
                        if (ekCertGenLocVal >= EK_LOC_VAL_MIN
                                && ekCertGenLocVal <= EK_LOC_VAL_MAX) {
                            TPMSecurityAssertions.EkGenerationLocation ekCertGenLoc
                                    = TPMSecurityAssertions.EkGenerationLocation.
                                    values()[ekCertGenLocVal];
                            tpmSecurityAssertions.setEkCertificateGenerationLocation(ekCertGenLoc);
                        }
                    }
                    // ccInfo, fipsLevel, iso9000Certified, and iso9000Uri still to be implemented
                }
                // Will need additional else if case in the future for instanceof ASN1Boolean when
                // supporting TPMSecurityAssertions iso9000Certified field, which could be either
                // DERTaggedObject or ASN1Boolean
            }
        } else {
            //parse the elements of the sequence individually
            for (ASN1Encodable component : seq) {
                parseSingle((ASN1Primitive) component, false, null);
            }
        }
    }

    /**
     * Parses the many different types of ASN1Primitives and searches for specific OID
     * key/value pairs. Works by traversing the entire ASN1Primitive tree with a single
     * pass and populates relevant fields in the EndorsementCredential object.
     *
     * @param component    the ASN1Primitive to parse
     * @param addToMapping whether or not the current component has been matched as the
     *                     value in an expected TPM OID key/value pair
     * @param key          if addToMapping is true, the key in the OID key/value pair
     * @throws IOException parsing of subcomponents in the tree failed.
     */
    private void parseSingle(final ASN1Primitive component, final boolean addToMapping,
                             final String key) throws IOException {
        // null check the key if addToMapping is true
        if (addToMapping && StringUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Key cannot be empty if adding to field mapping");
        }

        if (component instanceof ASN1Sequence) {
            parseSequence((ASN1Sequence) component, addToMapping, key);

        } else if (component instanceof DERUTF8String) {
            if (addToMapping) {
                DERUTF8String nameData = (DERUTF8String) component;
                parsedFields.put(key, nameData.getString());
            }

        } else if (component instanceof ASN1ObjectIdentifier) {
            if (addToMapping) {
                // shouldn't ever be reached, but just in case
                parsedFields.put(key, ((ASN1ObjectIdentifier) component).getId());
            }

        } else if (component instanceof ASN1TaggedObject taggedObj) {
            parseSingle(taggedObj.getBaseObject().toASN1Primitive(), addToMapping, key);

        } else if (component instanceof ASN1OctetString octStr) {
            // this may contain parseable data or may just be a OID key-pair value
            byte[] bytes = octStr.getOctets();
            ByteArrayInputStream inStream = new ByteArrayInputStream(bytes);
            ASN1InputStream octIn = new ASN1InputStream(inStream);
            try {
                ASN1Encodable newComp = octIn.readObject();
                parseSingle((ASN1Primitive) newComp, false, null);
            } catch (IOException e) {
                // this means octet string didn't contain parsable data, so store the
                // value as is
                if (addToMapping) {
                    parsedFields.put(key, bytes);
                }
            } finally {
                if (octIn != null) {
                    octIn.close();
                }
            }

        } else if (component instanceof ASN1Set set) {
            // all ECs seen to this point use sets differently than sequences and their sets
            // don't contain top level OIDs, so we can parse everything term by term, if that
            // ceases to be the case, we need to switch to this parsing to be more like
            // parseSequences in the future
            Enumeration setContents = set.getObjects();
            ASN1Encodable subComp;
            while (setContents.hasMoreElements()) {
                subComp = (ASN1Encodable) setContents.nextElement();
                if (subComp instanceof ASN1ObjectIdentifier) {
                    log.warn("OID in top level of ASN1Set");
                }
                parseSingle((ASN1Primitive) subComp, addToMapping, key);
            }

        } else if (component instanceof ASN1Boolean) {
            if (addToMapping) {
                boolean fieldVal = ((ASN1Boolean) component).isTrue();
                parsedFields.put(key, fieldVal);
            }

        } else if (component instanceof ASN1BitString) {
            // I don't think this contains more fields and needs to be reparsed,
            // though not 100% sure
            if (addToMapping) {
                byte[] bytes = ((ASN1BitString) component).getBytes();
                parsedFields.put(key, bytes);
            }

        } else if (component instanceof ASN1Integer) {
            if (addToMapping) {
                BigInteger bigInt = ((ASN1Integer) component).getValue();
                parsedFields.put(key, bigInt);
            }

        } else if (component instanceof ASN1Null) {
            if (addToMapping) {
                parsedFields.put(key, null);
            }

        } else if (component instanceof ASN1UTCTime) {
            if (addToMapping) {
                try {
                    parsedFields.put(key, ((ASN1UTCTime) component).getDate());
                } catch (ParseException pe) {
                    pe.printStackTrace();
                }
            }

        } else if (component instanceof DERPrintableString) {
            if (addToMapping) {
                parsedFields.put(key, ((DERPrintableString) component).getString());
            }

        } else if (component instanceof ASN1Enumerated) {
            if (addToMapping) {
                BigInteger value = ((ASN1Enumerated) component).getValue();
                parsedFields.put(key, value);
            }
            // after about this point, I doubt we'll see any of the following field types, but
            // in the interest of completeness and robustness, they are still parsed
        } else if (component instanceof DERIA5String) {
            if (addToMapping) {
                String ia5Str = ((DERIA5String) component).getString();
                parsedFields.put(key, ia5Str);
            }

        } else if (component instanceof DERNumericString) {
            if (addToMapping) {
                String numStr = ((DERNumericString) component).getString();
                parsedFields.put(key, numStr);
            }

        } else if (component instanceof ASN1GeneralizedTime) {
            if (addToMapping) {
                try {
                    parsedFields.put(key, ((ASN1GeneralizedTime) component).getDate());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } else if (component instanceof DERBMPString) {
            if (addToMapping) {
                String bmpStr = ((DERBMPString) component).getString();
                parsedFields.put(key, bmpStr);
            }

        } else if (component instanceof DERExternal) {
            parseSingle(((DERExternal) component).getExternalContent(), addToMapping, key);

        } else if (component instanceof DERGeneralString) {
            if (addToMapping) {
                String generalStr = ((DERGeneralString) component).getString();
                parsedFields.put(key, generalStr);
            }

        } else if (component instanceof DERT61String) {
            if (addToMapping) {
                String t61Str = ((DERT61String) component).getString();
                parsedFields.put(key, t61Str);
            }

        } else if (component instanceof DERUniversalString) {
            if (addToMapping) {
                String univStr = ((DERUniversalString) component).getString();
                parsedFields.put(key, univStr);
            }

        } else if (component instanceof DERVisibleString) {
            if (addToMapping) {
                String visStr = ((DERVisibleString) component).getString();
                parsedFields.put(key, visStr);
            }

        } else {
            // there are some deprecated types that we don't parse
            log.error("Unparsed type: {}", component.getClass());
        }
    }
}
