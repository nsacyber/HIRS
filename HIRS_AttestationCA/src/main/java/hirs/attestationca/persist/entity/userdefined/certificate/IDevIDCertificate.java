package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Entity
@Log4j2
public class IDevIDCertificate extends Certificate {

    // Undefined expiry date, as specified in 802.1AR
    private static final long UNDEFINED_EXPIRY_DATE = 253402300799L;

    // Supported OIDs
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final String SUBJECT_ALTERNATIVE_NAME_EXTENSION = "2.5.29.17";
    private static final String HARDWARE_MODULE_NAME_OID = "1.3.6.1.5.5.7.8.4";
    private static final String HWTYPE_TCG_TPM2_OID = "2.23.133.1.2";

    // Other OIDs (policy)
    private static final String POLICY_QUALIFIER_VERIFIED_TPM_RESIDENCY = "2.23.133.11.1.1";
    private static final String POLICY_QUALIFIER_VERIFIED_TPM_FIXED = "2.23.133.11.1.2";
    private static final String POLICY_QUALIFIER_VERIFIED_TPM_RESTRICTED = "2.23.133.11.1.3";

    @Getter
    @Transient
    private byte[] subjectAltName;

    /**
     * Corresponds to the serial number found in a Hardware Module Name (if present).
     */
    @Getter
    @Column
    private byte[] hwSerialNum;

    /**
     * TPM policy qualifiers (TCG only).
     */
    @Getter
    @Column
    private String tpmPolicies;

    /**
     * Construct a new IDevIDCertificate given its binary contents. The given
     * certificate should represent a valid X.509 certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public IDevIDCertificate(final byte[] certificateBytes)
            throws IOException {
        super(certificateBytes);

        this.parseIDevIDCertificate();
    }

    /**
     * Construct a new IDevIDCertificate by parsing the file at the given path.
     * The given certificate should represent a valid X.509 certificate.
     *
     * @param certificatePath the path on disk to a certificate
     * @throws IOException if there is a problem reading the file
     */
    public IDevIDCertificate(final Path certificatePath)
            throws IOException {
        super(certificatePath);

        this.parseIDevIDCertificate();
    }

    /**
     * Default constructor for Hibernate.
     */
    protected IDevIDCertificate() {
        subjectAltName = null;
    }

    /**
     * Obtains TPM policy qualifiers from the Certificate Policies extension, if present. These policy qualifiers are
     * specified in the TCG document "TPM 2.0 Keys for Device Identity and Attestation".
     *
     * @return A {@link java.util.Map} containing the policy qualifiers obtained.
     * @throws IOException if policy qualifiers cannot be parsed from extension value
     */
    public Map<String, Boolean> getTPMPolicyQualifiers(byte[] policyBytes) throws IOException {
        CertificatePolicies certPolicies =
                CertificatePolicies.getInstance(JcaX509ExtensionUtils.parseExtensionValue(policyBytes));
        Map<String, Boolean> policyQualifiers = new HashMap<>();
        boolean verifiedTPMResidency = false;
        boolean verifiedTPMFixed = false;
        boolean verifiedTPMRestricted = false;

        if (certPolicies != null) {
            // Must contain at least one Policy
            for (PolicyInformation policy : certPolicies.getPolicyInformation()) {
                // Add the data based on the OIDs
                switch (policy.getPolicyIdentifier().toString()) {
                    case POLICY_QUALIFIER_VERIFIED_TPM_RESIDENCY:
                        verifiedTPMResidency = true;
                        break;
                    case POLICY_QUALIFIER_VERIFIED_TPM_FIXED:
                        verifiedTPMFixed = true;
                        break;
                    case POLICY_QUALIFIER_VERIFIED_TPM_RESTRICTED:
                        verifiedTPMRestricted = true;
                        break;
                    default:
                        break;
                }
            }
        }

        // Add to map
        policyQualifiers.put("verifiedTPMResidency", Boolean.valueOf(verifiedTPMResidency));
        policyQualifiers.put("verifiedTPMFixed", Boolean.valueOf(verifiedTPMFixed));
        policyQualifiers.put("verifiedTPMRestricted", Boolean.valueOf(verifiedTPMRestricted));

        return policyQualifiers;
    }

    /**
     * Parses fields related to IDevID certificates.
     * @throws IOException if a problem is encountered during parsing
     */
    private void parseIDevIDCertificate() throws IOException {

        this.subjectAltName =
                getX509Certificate().getExtensionValue(SUBJECT_ALTERNATIVE_NAME_EXTENSION);

        if (this.subjectAltName != null) {

            ASN1InputStream input;
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(subjectAltName);
            input = new ASN1InputStream(byteArrayInputStream);

            ASN1OctetString obj = (ASN1OctetString) input.readObject();
            boolean tcgOid = false;

            // Parse the otherName structure. According to the specification "TPM 2.0 Keys for Device Identity and
            // Attestation", otherName can contain up to two structures: HardwareModuleName and PermanentIdentifier.
            // Currently, this parser only supports HardwareModuleName (if present).

            if (obj != null) {
                // Parse Hardware Module Name structure, comprised of a hwType and hwSerialNum, and associated OID
                // See also RFC 4108
                ASN1Sequence seq1 = ASN1Sequence.getInstance(obj.getOctets());

                // Iterate over GeneralNames sequence until HardwareModuleName is found
                Iterator<ASN1Encodable> seqIterator = seq1.iterator();
                ASN1TaggedObject tagObj1;
                ASN1Encodable encodable;
                while (seqIterator.hasNext()) {
                    encodable = seqIterator.next();
                    if (encodable != null) {
                        tagObj1 = ASN1TaggedObject.getInstance(encodable.toASN1Primitive());

                        if (tagObj1 != null && tagObj1.hasContextTag(0)) { // Corresponds to otherName
                            seq1 = ASN1Sequence.getInstance(tagObj1, false);

                            ASN1ObjectIdentifier obj1 = ASN1ObjectIdentifier.getInstance(seq1.getObjectAt(0));
                            tagObj1 = ASN1TaggedObject.getInstance(seq1.getObjectAt(1));

                            if (obj1.toString().equals(HARDWARE_MODULE_NAME_OID)) {

                                // HardwareModuleName sequence
                                seq1 = ASN1Sequence.getInstance(tagObj1, false);
                                seq1 = ASN1Sequence.getInstance(seq1.getObjectAt(0));

                                obj1 = ASN1ObjectIdentifier.getInstance(seq1.getObjectAt(0));
                                ASN1OctetString obj2;
                                try {
                                    obj2 = ASN1OctetString.getInstance(seq1.getObjectAt(1));
                                } catch (IllegalArgumentException e) {
                                    // Some certs have been found to contain tagged objects for hwSerialNum.
                                    // Handle this as a special case.
                                    log.warn("Could not parse octet string for hwSerialNum. Attempting to parse tag.");
                                    try {
                                        tagObj1 = ASN1TaggedObject.getInstance(seq1.getObjectAt(1));
                                        obj2 = ASN1OctetString.getInstance(tagObj1, false);
                                    }
                                    catch (Exception i) {  // Invalid object found
                                        log.warn("Invalid object found for hwSerialNum.");
                                        break;
                                    }
                                }

                                // If an OID corresponding to TPM 2.0 for hwType is supported, according to the
                                // specification "TPM 2.0 Keys for Device Identity and Attestation", the contents of
                                // the hwSerialNum field will be parsed accordingly.
                                switch (obj1.toString()) {
                                    case HWTYPE_TCG_TPM2_OID:
                                        tcgOid = true;
                                        break;
                                }

                                // Convert octet string to byte array
                                hwSerialNum = obj2.getOctets();
                            }
                        }
                    }
                }
            }

            // Check for certificate policy qualifiers, which should be present for IDevIDs if in compliance with the
            // TCG specification.
            // For interoperability reasons, this will only log a warning if a TCG OID is specified above.
            byte[] policyBytes = getX509Certificate().getExtensionValue(Extension.certificatePolicies.getId());
            Map<String, Boolean> policyQualifiers = null;

            if (policyBytes != null) {
                policyQualifiers = this.getTPMPolicyQualifiers(policyBytes);
            }
            if (tcgOid) {
                boolean failCondition;
                if (policyQualifiers != null) {
                    StringBuilder qualifierSB = new StringBuilder();
                    policyQualifiers.forEach((key, value) -> {
                        if (value) {
                            if (qualifierSB.length() > 0) {
                                qualifierSB.append(" ");
                            }
                            qualifierSB.append(key);
                        }
                    });
                    tpmPolicies = qualifierSB.toString();

                    failCondition = !(policyQualifiers.get("verifiedTPMResidency") &&
                            (policyQualifiers.get("verifiedTPMFixed") ||
                                    policyQualifiers.get("verifiedTPMRestricted")));
                } else {
                    failCondition = true;
                }
                if (failCondition) {
                    log.warn("TPM policy qualifiers not found, or do not meet logical criteria. Certificate may not " +
                            "be in compliance with TCG specification.");
                }
            }

            // Log a warning if notAfter field has an expiry date that is not indefinite
            if (!this.getEndValidity().toInstant().equals(Instant.ofEpochSecond(UNDEFINED_EXPIRY_DATE))) {
                log.warn("IDevID does not contain an indefinite expiry date. This may indicate an invalid " +
                        "certificate.");
            }

            input.close();
        }
    }

    @Override
    @SuppressWarnings("checkstyle:avoidinlineconditionals")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        IDevIDCertificate that = (IDevIDCertificate) o;

        if (!Objects.equals(getTpmPolicies(), that.getTpmPolicies())) {
            return false;
        }

        return Objects.equals(getHwSerialNum(), that.getHwSerialNum());
    }

    @Override
    @SuppressWarnings({"checkstyle:magicnumber", "checkstyle:avoidinlineconditionals"})
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getTpmPolicies() != null ? getTpmPolicies().hashCode() : 0);
        result = 31 * result + (getHwSerialNum() != null ? getHwSerialNum().hashCode() : 0);

        return result;
    }
}