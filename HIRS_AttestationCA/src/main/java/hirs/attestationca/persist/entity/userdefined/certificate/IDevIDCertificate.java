package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    @Getter
    @Column
    private String hwSerialNum;

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
     * Get the PolicyQualifier from the Certificate Policies Extension.
     *
     * @return Policy Qualifier from the Certificate Policies Extension
     */
    public Map<String, Boolean> getPolicyQualifier(byte[] policyBytes) throws IOException {
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

    // Parse fields related to IDevID certificates.
    private void parseIDevIDCertificate() throws IOException {

        this.subjectAltName =
                getX509Certificate().getExtensionValue(SUBJECT_ALTERNATIVE_NAME_EXTENSION);

        if (this.subjectAltName != null) {

            ASN1InputStream input;
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(subjectAltName);
            input = new ASN1InputStream(byteArrayInputStream);

            try {
                ASN1OctetString obj = (ASN1OctetString) input.readObject();
                boolean tcgOid = false;

                // Parse the otherName structure. According to the specification "TPM 2.0 Keys for Device Identity and
                // Attestation", otherName can contain two elements: hardwareModuleName and permanentIdentifier.
                // Currently, this parser only supports hardwareModuleName (if present).

                if (obj != null) {
                    // Parse Hardware Module Name structure, comprised of a hwType and hwSerialNum, and associated OID
                    // See also RFC 4108
                    ASN1Sequence seq1 = ASN1Sequence.getInstance(obj.getOctets());

                    ASN1TaggedObject tagObj1 = ASN1TaggedObject.getInstance(seq1.getObjectAt(0));

                    seq1 = ASN1Sequence.getInstance(tagObj1, false);

                    ASN1ObjectIdentifier obj1 = ASN1ObjectIdentifier.getInstance(seq1.getObjectAt(0));
                    tagObj1 = ASN1TaggedObject.getInstance(seq1.getObjectAt(1));

                    if (!obj1.toString().equals(HARDWARE_MODULE_NAME_OID)) {
                        throw new IOException("Unsupported OID found for id-on-hardwareModuleName: " + obj1);
                    }

                    // HardwareModuleName sequence
                    seq1 = ASN1Sequence.getInstance(tagObj1, false);
                    seq1 = ASN1Sequence.getInstance(seq1.getObjectAt(0));

                    obj1 = ASN1ObjectIdentifier.getInstance(seq1.getObjectAt(0));
                    ASN1OctetString obj2 = ASN1OctetString.getInstance(seq1.getObjectAt(1));

                    hwSerialNum = new String(obj2.getOctets(), "US-ASCII");

                    // Currently, only an OID corresponding to TPM 2.0 for hwType is supported, according to the
                    // specification "TPM 2.0 Keys for Device Identity and Attestation". The contents of the
                    // hwSerialNum field will be parsed accordingly.
                    switch (obj1.toString()) {
                        case HWTYPE_TCG_TPM2_OID:
                            tcgOid = true;
                            break;
                        default:
                            throw new IOException("Unsupported hwType OID: " + obj1);
                    }
                }

                // Check for certificate policy qualifiers, which must be present for IDevIDs if in compliance with the
                // TCG specification.
                // For interoperability reasons, this will only cause an exception if a TCG OID is specified above.
                byte[] policyBytes = getX509Certificate().getExtensionValue(Extension.certificatePolicies.getId());
                Map<String, Boolean> policyQualifiers = null;

                if (policyBytes != null) {
                    policyQualifiers = this.getPolicyQualifier(policyBytes);
                }
                if (tcgOid) {
                    boolean failCondition;
                    if (policyQualifiers != null) {
                        failCondition = !(policyQualifiers.get("verifiedTPMResidency") &&
                                (policyQualifiers.get("verifiedTPMFixed") ||
                                        policyQualifiers.get("verifiedTPMRestricted")));
                    }
                    else {
                        failCondition = true;
                    }
                    if (failCondition) {
                        throw new IOException("Policy qualifiers not found. Certificate is not in compliance with " +
                                "TCG specification.");
                    }
                }

                // Log a warning if notAfter field has an expiry date that is not indefinite
                if (!this.getEndValidity().toInstant().equals(Instant.ofEpochSecond(UNDEFINED_EXPIRY_DATE))) {
                    log.warn("IDevID does not contain an indefinite expiry date. This may indicate an invalid " +
                            "certificate.");
                }
            }
            catch (IOException e) {
                log.error(e);
            }
            finally
            {
                input.close();
            }
        }
        else {
            throw new IOException("Invalid IDevID certificate: no subjectAltName extension found.");
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

        return Objects.equals(getHwSerialNum(), that.getHwSerialNum());
    }

    @Override
    @SuppressWarnings({"checkstyle:magicnumber", "checkstyle:avoidinlineconditionals"})
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getHwSerialNum() != null ? getHwSerialNum().hashCode() : 0);

        return result;
    }
}
