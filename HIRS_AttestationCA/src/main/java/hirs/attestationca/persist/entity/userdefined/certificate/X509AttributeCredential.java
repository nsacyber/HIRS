package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.certificate.attributes.PublicKeyAttributeConfiguration;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.PublicKeyAttributeCredential;
import jakarta.persistence.Entity;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * This class persists X509AttributeCredential by extending the base Certificate
 * class with fields unique to a Platform credentials, as defined in the Trusted
 * Computing Group Credential Profiles, specification v.1.2.
 */
@Log4j2
@Entity
public class X509AttributeCredential extends PlatformCredential {

    private static final String PLATFORM_PKC_CERT = "2.23.133.8.4";
    private static final int CONTENT_BODY = 0;
    private static final int ALGORITHM = 1;
    private static final int SIGNATURE = 2;

    /**
     * Construct a new X509AttributeCredential by parsing the file at the given path.  The given
     * certificate should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public X509AttributeCredential(final byte[] certificateBytes) throws IOException {
        this(certificateBytes, true);
    }

    /**
     * Construct a new X509AttributeCredential by parsing the file at the given path.  The given
     * certificate should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @param parseFields boolean True to parse fields
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public X509AttributeCredential(final byte[] certificateBytes,
                                   final boolean parseFields) throws IOException {
        super(certificateBytes, false);
        if (parseFields) {
            parseFields();
        }

        this.setPlatformBase(true);
    }

    /**
     * Construct a new X509AttributeCredential by parsing the file at the given path.  The given
     * certificate should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificatePath the path on disk to a certificate
     * @throws IOException if there is a problem reading the file
     */
    public X509AttributeCredential(final Path certificatePath) throws IOException {
        this(readBytes(certificatePath), true);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected X509AttributeCredential() {

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
        X509Certificate x509Certificate = getX509Certificate();

        // Check if the algorithm identifier is the same
        if (!isAlgIdEqual(AlgorithmIdentifier.getInstance(x509Certificate.getSignature()),
                AlgorithmIdentifier.getInstance(this.getSignatureAlgorithm()))) {
            throw new IOException("signature invalid - algorithm identifier mismatch");
        }

        ContentVerifier verifier;
        try {
            // Set ContentVerifier with the signature that will verify
            verifier = verifierProvider.get(AlgorithmIdentifier
                    .getInstance(x509Certificate.getSignature()));

        } catch (Exception e) {
            throw new IOException("unable to process signature: " + e.getMessage(), e);
        }

        return verifier.verify(ASN1BitString.getInstance(getBaseEncoded(SIGNATURE)).getOctets());
    }

    /**
     * Get the x509 Platform Certificate version.
     * @return a big integer representing the certificate version.
     */
    @Override
    public int getX509CredentialVersion() {
        try {
            PublicKeyAttributeCredential attriPkc = new PublicKeyAttributeCredential(getRawBytes());
            return attriPkc.getX509CredentialVersion().getValue().intValue();
        } catch (IOException ioEx) {
            log.error(ioEx);
        }
        return 0;
    }

    /**
     * Get the Platform Configuration Attribute from the Platform Certificate.
     * @return a map with all the attributes
     * @throws IllegalArgumentException when there is a parsing error
     * @throws IOException when reading the certificate.
     */
    public Map<String, Object> getAllAttributes() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        ASN1Sequence attributeSequence = getAttributeCertificatePkc()
                .getAttributes();

        attributes.put("platformConfiguration", new PublicKeyAttributeConfiguration(attributeSequence));

        return attributes;
    }


    private ASN1Object getBaseEncoded(final int index) {
        ASN1Sequence topLevelItems = null;
        try {
            topLevelItems = ASN1Sequence.getInstance(ASN1Primitive.fromByteArray(getRawBytes()));

            switch (index) {
                case SIGNATURE:
                    return ASN1BitString.getInstance(topLevelItems.getObjectAt(SIGNATURE));
                case CONTENT_BODY:
                case ALGORITHM:
                default:
                    return null;
            }
        } catch (IOException ioEx) {
            return null;
        }
    }

    /**
     * Parse a PKC formatted certificate.
     * @throws IOException failed to find PKC OID
     */
    protected void parseFields() throws IOException {
        PublicKeyAttributeCredential acPkc = getAttributeCertificatePkc();
        this.setCredentialType(CERTIFICATE_TYPE_2_0);
        parsePkcCert(acPkc.getExtension(Extension.subjectAlternativeName));
        String oid = acPkc.getCredentialType().getId();

        this.setPlatformBase(true);

        if (!oid.equals(PLATFORM_PKC_CERT)) {
            throw new IOException("Invalid Public Key Credential: " + oid);
        }
    }

    /**
     * Parse a 2.0 Platform Certificate (Attribute Certificate).
     * @param subjectAlternativeName values associated with the subject
     */
    private void parsePkcCert(final Extension subjectAlternativeName) throws IOException {
        if (subjectAlternativeName != null) {
            ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(subjectAlternativeName
                    .getParsedValue());
            String oid;
            for (int i = 0; i < asn1Sequence.size(); i++) {
                if (asn1Sequence.getObjectAt(i) instanceof ASN1ObjectIdentifier) {
                    oid = asn1Sequence.getObjectAt(i).toString();
                    switch (oid) {
                        case PLATFORM_MANUFACTURER_2_0:
                            this.setManufacturer(asn1Sequence.getObjectAt(i + 1).toString());
                            break;
                        case PLATFORM_MODEL_2_0:
                            this.setModel(asn1Sequence.getObjectAt(i + 1).toString());
                            break;
                        case PLATFORM_VERSION_2_0:
                            this.setVersion(asn1Sequence.getObjectAt(i + 1).toString());
                            break;
                        case PLATFORM_SERIAL_2_0:
                            this.setPlatformSerial(asn1Sequence.getObjectAt(i + 1).toString());
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        try {
            getAllAttributes();
        } catch (IllegalArgumentException | IOException ioEx) {
            throw new IOException(ioEx.getMessage());
        }
    }
}
