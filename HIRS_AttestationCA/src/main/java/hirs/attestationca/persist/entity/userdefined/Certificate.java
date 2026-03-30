package hirs.attestationca.persist.entity.userdefined;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateVariables;
import hirs.attestationca.persist.util.CredentialHelper;
import hirs.utils.HexUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Lob;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLTaggedObject;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AttCertIssuer;
import org.bouncycastle.asn1.x509.AttributeCertificate;
import org.bouncycastle.asn1.x509.AttributeCertificateInfo;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.V2Form;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * This class enables the persistence of a single X509 certificates or X509 attribute certificate.
 * It stores certain attributes separately from the serialized certificate to enable querying on
 * those attributes.
 */
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Log4j2
@Entity
public abstract class Certificate extends ArchivableEntity {

    /**
     * Decimal digit representation of base 16.
     */
    public static final int HEX_BASE = 16;

    /**
     * Min length representing the attribute certificate.
     */
    public static final int MIN_ATTR_CERT_LENGTH = 8;

    /**
     * Holds the name of the entity 'ID' field.
     */
    public static final String ID_FIELD = "id";

    /**
     * Holds the name of the entity 'Archived' field.
     */
    public static final String ARCHIVE_FIELD = "archivedTime";

    /**
     * Holds the name of the 'serialNumber' field.
     */
    public static final String SERIAL_NUMBER_FIELD = "serialNumber";

    /**
     * Holds the name of the 'issuer' field.
     */
    public static final String ISSUER_FIELD = "issuer";

    /**
     * Holds the name of the 'issuerSorted' field.
     */
    public static final String ISSUER_SORTED_FIELD = "issuerSorted";

    /**
     * Holds the name of the 'subject' field.
     */
    public static final String SUBJECT_FIELD = "subject";

    /**
     * Holds the name of the 'subjectSorted' field.
     */
    public static final String SUBJECT_SORTED_FIELD = "subjectSorted";

    /**
     * Holds the name of the 'encodedPublicKey' field.
     */
    public static final String ENCODED_PUBLIC_KEY_FIELD = "encodedPublicKey";

    /**
     * Holds the name of the 'encodedPublicKey' field.
     */
    public static final String PUBLIC_KEY_MODULUS_FIELD = "publicKeyModulusHexValue";

    /**
     * Holds the name of the 'certificateHash' field.
     */
    public static final String CERTIFICATE_HASH_FIELD = "certificateHash";

    /**
     * Holds the name of the 'holderSerialNumber' field.
     */
    public static final String HOLDER_SERIAL_NUMBER_FIELD = "holderSerialNumber";

    /**
     * Holds the name of the 'authorityKeyIdentifier' field.
     */
    public static final String AUTHORITY_KEY_ID_FIELD = "authorityKeyIdentifier";

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP") // this is not an IP address; PMD thinks it is
    private static final String POLICY_CONSTRAINTS = "2.5.29.36";

    @Getter
    @Column(nullable = false, precision = CertificateVariables.MAX_NUMERIC_PRECISION, scale = 0)
    private final BigInteger serialNumber;

    @Getter
    @Column(nullable = false)
    private final String issuer;

    @Getter
    @Column
    private final String issuerSorted;

    @Getter
    @Column
    private final String subject;

    @Getter
    @Column
    private final String subjectSorted;

    @Column(length = CertificateVariables.MAX_CERT_LENGTH_BYTES)
    private final byte[] encodedPublicKey;

    // We're currently seeing 2048-bit keys, which is 512 hex digits.
    // Using a max length of 1024 for future-proofing.
    @Getter
    @Column(length = CertificateVariables.MAX_PUB_KEY_MODULUS_HEX_LENGTH)
    private final String publicKeyModulusHexValue;

    @Column(length = CertificateVariables.MAX_CERT_LENGTH_BYTES, nullable = false)
    private final byte[] signature;

    @Column(nullable = false)
    private final Date beginValidity;

    @Column(nullable = false)
    private final Date endValidity;

    @Column(nullable = false)
    @JsonIgnore
    @Getter
    private final int certificateHash;

    /**
     * This field exists to enforce a unique constraint on a hash over the certificate contents
     * and the certificate type.  This is to ensure the system only allows one copy of a
     * certificate per role in the system.
     */
    @Column(nullable = false, unique = true)
    @JsonIgnore
    private final int certAndTypeHash;

    @Getter
    @Column(nullable = false, precision = CertificateVariables.MAX_NUMERIC_PRECISION)
    private final BigInteger holderSerialNumber;

    @Getter
    @Column(precision = CertificateVariables.MAX_NUMERIC_PRECISION)
    private final BigInteger authoritySerialNumber;

    @Lob
    @Column(nullable = false, columnDefinition = "BLOB")
    @JsonIgnore
    private byte[] certificateBytes;

    @Getter
    private String holderIssuer;

    // we don't need to persist this, but we don't want to unpack this cert multiple times
    @Transient
    private X509Certificate parsedX509Cert = null;

    @Getter
    private String signatureAlgorithm;

    @Getter
    private String publicKeyAlgorithm;

    @Getter
    private String keyUsage;

    @Getter
    private String extendedKeyUsage;

    private byte[] policyConstraints;

    @Getter
    private String authorityKeyIdentifier;

    @Getter
    private String authorityInfoAccess;

    @Getter
    private String crlPoints;

    @Getter
    private int publicKeySize;

    /**
     * Default constructor necessary for Hibernate.
     */
    protected Certificate() {
        super();
        this.serialNumber = BigInteger.ZERO;
        this.issuer = null;
        this.subject = null;
        this.issuerSorted = null;
        this.subjectSorted = null;
        this.encodedPublicKey = null;
        this.publicKeyModulusHexValue = null;
        this.signature = null;
        this.beginValidity = null;
        this.endValidity = null;
        this.certificateBytes = null;
        this.certificateHash = 0;
        this.certAndTypeHash = 0;
        this.holderSerialNumber = BigInteger.ZERO;
        this.holderIssuer = null;
        this.publicKeyAlgorithm = null;
        this.signatureAlgorithm = null;
        this.keyUsage = null;
        this.extendedKeyUsage = null;
        this.policyConstraints = null;
        this.authorityKeyIdentifier = null;
        this.authorityInfoAccess = null;
        this.authoritySerialNumber = BigInteger.ZERO;
        this.crlPoints = null;
        this.publicKeySize = 0;
    }

    /**
     * Construct a new Certificate by parsing the file at the given path.  The given certificate
     * should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificatePath the path on disk to a certificate
     * @throws IOException if there is a problem reading the file
     */
    public Certificate(final Path certificatePath) throws IOException {
        this(readBytes(certificatePath));
    }

    /**
     * Construct a new Certificate given its binary contents.  The given certificate should
     * represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @throws IOException if there is a problem extracting information from the certificate
     */

    public Certificate(final byte[] certificateBytes) throws IOException {
        Preconditions.checkArgument(
                certificateBytes != null,
                "Cannot construct a Certificate from a null byte array"
        );

        Preconditions.checkArgument(
                certificateBytes.length > 0,
                "Cannot construct a Certificate from an empty byte array"
        );

        this.certificateBytes = certificateBytes.clone();

        // check for and handle possible PEM base 64 encoding
        String possiblePem = new String(certificateBytes, StandardCharsets.UTF_8);
        if (CredentialHelper.isPEM(possiblePem)) {
            this.certificateBytes = CredentialHelper.stripPemHeaderFooter(possiblePem);
        }

        AuthorityKeyIdentifier authKeyIdentifier;
        this.certificateBytes = CredentialHelper.trimCertificate(this.certificateBytes);

        // Extract certificate data
        switch (getCertificateType()) {
            case X509_CERTIFICATE:
                X509Certificate x509Certificate = getX509Certificate();

                this.serialNumber = x509Certificate.getSerialNumber();
                this.issuer = Certificate.getIssuerDNString(x509Certificate);
                this.subject = Certificate.getSubjectDNString(x509Certificate);
                this.encodedPublicKey = x509Certificate.getPublicKey().getEncoded();
                BigInteger publicKeyModulus = getPublicKeyModulus(x509Certificate);

                if (publicKeyModulus != null) {
                    this.publicKeyModulusHexValue = publicKeyModulus.toString(HEX_BASE);
                    this.publicKeySize = publicKeyModulus.bitLength();
                } else {
                    this.publicKeyModulusHexValue = null;
                }
                this.publicKeyAlgorithm = x509Certificate.getPublicKey().getAlgorithm();
                this.signatureAlgorithm = x509Certificate.getSigAlgName();
                this.signature = x509Certificate.getSignature();
                this.beginValidity = x509Certificate.getNotBefore();
                this.endValidity = x509Certificate.getNotAfter();
                this.holderSerialNumber = BigInteger.ZERO;
                this.issuerSorted = CredentialHelper.parseSortDNs(this.issuer);
                this.subjectSorted = CredentialHelper.parseSortDNs(this.subject);
                this.policyConstraints = x509Certificate
                        .getExtensionValue(POLICY_CONSTRAINTS);
                authKeyIdentifier = AuthorityKeyIdentifier
                        .getInstance(getExtensionValue(
                                Extension.authorityKeyIdentifier.getId()));

                this.authorityInfoAccess = getAuthorityInfoAccess(x509Certificate
                        .getExtensionValue(Extension.authorityInfoAccess.getId()));
                this.keyUsage = parseKeyUsage(x509Certificate.getKeyUsage());
                this.crlPoints = getCRLDistributionPoint();

                try {
                    if (x509Certificate.getExtendedKeyUsage() != null) {
                        StringBuilder sb = new StringBuilder();
                        for (String s : x509Certificate.getExtendedKeyUsage()) {
                            sb.append(String.format("%s%n", s));
                        }
                        this.extendedKeyUsage = sb.toString();
                    }
                } catch (CertificateParsingException ex) {
                    // do nothing
                }
                break;

            case ATTRIBUTE_CERTIFICATE:
                AttributeCertificate attCert = getAttributeCertificate();
                AttributeCertificateInfo attCertInfo = attCert.getAcinfo();
                if (attCertInfo == null) {
                    throw new IllegalArgumentException("Required attribute certificate info"
                            + " field not found in provided attribute certificate.");
                }

                // Set null values (Attribute certificates do not have this values)
                this.subject = null;
                this.subjectSorted = null;
                this.encodedPublicKey = null;
                this.publicKeyModulusHexValue = null;
                this.publicKeySize = 0;

                authKeyIdentifier = null;
                Extensions attCertInfoExtensions = attCertInfo.getExtensions();
                if (attCertInfoExtensions != null) {
                    authKeyIdentifier = AuthorityKeyIdentifier
                            .fromExtensions(attCertInfoExtensions);
                    this.authorityInfoAccess = getAuthorityInfoAccess(
                            AuthorityInformationAccess.fromExtensions(
                                    attCertInfoExtensions));
                }

                switch (attCert.getSignatureAlgorithm().getAlgorithm().getId()) {
                    case CertificateVariables.RSA256_OID:
                        this.signatureAlgorithm = CertificateVariables.RSA256_STRING;
                        break;
                    case CertificateVariables.RSA384_OID:
                        this.signatureAlgorithm = CertificateVariables.RSA384_STRING;
                        break;
                    case CertificateVariables.RSA224_OID:
                        this.signatureAlgorithm = CertificateVariables.RSA224_STRING;
                        break;
                    case CertificateVariables.RSA512_OID:
                        this.signatureAlgorithm = CertificateVariables.RSA512_STRING;
                        break;
                    case CertificateVariables.RSA512_224_OID:
                        this.signatureAlgorithm = CertificateVariables.RSA512_224_STRING;
                        break;
                    case CertificateVariables.RSA512_256_OID:
                        this.signatureAlgorithm = CertificateVariables.RSA512_256_STRING;
                        break;
                    case CertificateVariables.ECDSA_OID:
                        this.signatureAlgorithm = CertificateVariables.ECDSA_STRING;
                        break;
                    case CertificateVariables.ECDSA_SHA224_OID:
                        this.signatureAlgorithm = CertificateVariables.ECDSA_SHA224_STRING;
                        break;
                    default:
                        break;
                }

                // Get attribute certificate information
                this.serialNumber = attCertInfo.getSerialNumber().getValue();
                this.holderSerialNumber = attCertInfo
                        .getHolder()
                        .getBaseCertificateID()
                        .getSerial()
                        .getValue();
                this.holderIssuer = attCertInfo.getHolder()
                        .getBaseCertificateID().getIssuer()
                        .getNames()[0].getName().toString();
                this.signature = attCert.getSignatureValue().getBytes();
                this.issuer = getAttributeCertificateIssuerNames(
                        attCertInfo.getIssuer())[0].toString();
                this.issuerSorted = CredentialHelper.parseSortDNs(this.issuer);

                // Parse notBefore and notAfter dates
                this.beginValidity = recoverDate(attCertInfo
                        .getAttrCertValidityPeriod()
                        .getNotBeforeTime());
                this.endValidity = recoverDate(attCertInfo
                        .getAttrCertValidityPeriod()
                        .getNotAfterTime());
                break;
            default:
                throw new IllegalArgumentException("Cannot recognize certificate type.");
        }

        BigInteger authSerialNumber = null;
        if (authKeyIdentifier != null) {
            this.authorityKeyIdentifier = authKeyIdentifierToString(authKeyIdentifier);
            authSerialNumber = authKeyIdentifier.getAuthorityCertSerialNumber();
        }

        if (authSerialNumber != null) {
            this.authoritySerialNumber = authSerialNumber;
        } else {
            this.authoritySerialNumber = BigInteger.ZERO;
        }

        this.certificateHash = Arrays.hashCode(this.certificateBytes);
        this.certAndTypeHash = Objects.hash(certificateHash, getClass().getSimpleName());
    }

    /**
     * Gets the raw bytes for the certificate.
     *
     * @param certificatePath path to the certificate file
     * @return bytes from the certificate file
     * @throws IOException if there is a problem reading the file
     */
    public static byte[] readBytes(final Path certificatePath) throws IOException {
        Preconditions.checkArgument(
                certificatePath != null,
                "Cannot construct a Certificate from a null path"
        );

        return Files.readAllBytes(certificatePath);
    }

    /**
     * Retrieve a formatted subject DN string from a certificate. This allows for extended support of DNs
     * found in various RFCs.
     *
     * @param certificate the certificate holding subject DNs
     * @return subject distinguished name
     * @throws IOException if there is an issue decoding the subject DNs
     */
    public static String getSubjectDNString(final X509Certificate certificate)
            throws IOException {
        X509CertificateHolder certificateHolder = null;
        try {
            certificateHolder = new X509CertificateHolder(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IOException("Could not encode certificate", e);
        }

        X500Name x500Name = certificateHolder.getSubject();
        return x500Name.toString();
    }

    /**
     * Retrieve a formatted issuer DN string from a certificate. This allows for extended support of DNs found
     * in various RFCs.
     *
     * @param certificate the certificate holding issuer DNs
     * @return issuer distinguished name
     * @throws IOException if there is an issue decoding the issuer distinguished names
     */
    public static String getIssuerDNString(final X509Certificate certificate)
            throws IOException {
        X509CertificateHolder certificateHolder = null;
        try {
            certificateHolder = new X509CertificateHolder(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IOException("Could not encode certificate", e);
        }

        X500Name x500Name = certificateHolder.getIssuer();
        return x500Name.toString();
    }

    /**
     * Retrieve an RSA-based X509 certificate's public key modulus.
     *
     * @param certificate the certificate holding a public key
     * @return a BigInteger representing its public key's modulus or null if none found
     * @throws IOException if there is an issue decoding the encoded public key
     */
    public static BigInteger getPublicKeyModulus(final X509Certificate certificate)
            throws IOException {
        X509CertificateHolder certificateHolder = null;
        try {
            certificateHolder = new X509CertificateHolder(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IOException("Could not encode certificate", e);
        }
        try {
            return getPublicKeyModulus(
                    certificateHolder.getSubjectPublicKeyInfo().parsePublicKey().toASN1Primitive()
            );
        } catch (IOException e) {
            log.info("No RSA Key Detected in certificate");
            return null;
        }
    }

    /**
     * Retrieves the modulus of the given PublicKey.
     *
     * @param publicKey the public key
     * @return a BigInteger representing the public key's modulus
     * @throws IOException if there is an issue decoding the public key
     */
    public static BigInteger getPublicKeyModulus(final PublicKey publicKey) throws IOException {
        ASN1Primitive publicKeyASN1 = ASN1Primitive.fromByteArray(publicKey.getEncoded());
        if (publicKeyASN1 instanceof ASN1Sequence publicKeyASN1Sequence) {
            ASN1BitString encodedModulusAndExponent = (ASN1BitString)
                    publicKeyASN1Sequence.getObjectAt(1);
            byte[] modulusAndExponentBytes = encodedModulusAndExponent.getOctets();
            return getPublicKeyModulus(ASN1Primitive.fromByteArray(modulusAndExponentBytes));
        } else {
            throw new IOException("Could not read public key as ASN1Sequence");
        }
    }

    private static BigInteger getPublicKeyModulus(final ASN1Primitive publicKey)
            throws IOException {
        if (publicKey instanceof ASN1Sequence pubKeySeq) {
            ASN1Encodable modulus = pubKeySeq.getObjectAt(0);
            if (modulus instanceof ASN1Integer) {
                return ((ASN1Integer) modulus).getValue();
            } else {
                throw new IOException("Could not read modulus as an ASN1Integer");
            }
        } else {
            throw new IOException("Could not parse public key information as an ASN1Sequence");
        }
    }

    /**
     * Retrieve the X509 Name array from the issuer in an Attribute Certificate.
     *
     * @param issuer for the Attribute Certificate
     * @return a X500Name[] representing the names of the issuer
     */
    public static X500Name[] getAttributeCertificateIssuerNames(final AttCertIssuer issuer) {
        final ASN1Encodable form = issuer.getIssuer();
        GeneralNames name;
        if (form instanceof V2Form) {
            name = ((V2Form) form).getIssuerName();
        } else {
            name = (GeneralNames) form;
        }

        GeneralName[] names = name.getNames();
        List<X500Name> l = new ArrayList<>(names.length);

        for (int i = 0; i != names.length; i++) {
            if (names[i].getTagNo() == GeneralName.directoryName) {
                l.add(X500Name.getInstance(names[i].getName()));
            }
        }

        return l.toArray(new X500Name[l.size()]);
    }

    /**
     * Retrieve the Date from an ASN1GeneralizedTime.
     *
     * @param time (ASN1GeneralizedTime) of the certificate
     * @return the Date from a ASN1GeneralizedTime
     */
    public static Date recoverDate(final ASN1GeneralizedTime time) {
        try {
            return time.getDate();
        } catch (ParseException e) {
            throw new IllegalStateException("unable to recover date: " + e.getMessage());
        }
    }

    /**
     * Getter for the CRL Distribution that is reference by the Revocation Locator
     * on the portal.
     *
     * @return A list of URLs that inform the location of the certificate revocation lists
     * @throws IOException if there is an issue while retrieving the CRL Distribution point
     */
    private String getCRLDistributionPoint() throws IOException {
        List<String> crlUrls = new ArrayList<>();
        ASN1Primitive primitive = getExtensionValue(Extension.cRLDistributionPoints.getId());
        StringBuilder sb = new StringBuilder();

        if (primitive != null) {
            CRLDistPoint crlDistPoint = CRLDistPoint.getInstance(primitive);
            DistributionPoint[] distributionPoints = crlDistPoint.getDistributionPoints();

            for (DistributionPoint distributionPoint : distributionPoints) {
                DistributionPointName dpn = distributionPoint.getDistributionPoint();
                // Look for URIs in fullName
                if (dpn != null && dpn.getType() == DistributionPointName.FULL_NAME) {
                    GeneralName[] genNames = GeneralNames.getInstance(dpn.getName())
                            .getNames();
                    for (GeneralName genName : genNames) {
                        if (genName.getTagNo() == GeneralName.uniformResourceIdentifier) {
                            String url = DERIA5String.getInstance(genName.getName())
                                    .getString();
                            crlUrls.add(url);
                        }
                    }
                }
            }
        }

        for (String s : crlUrls) {
            sb.append(String.format("%s%n", s));
        }

        return sb.toString();
    }

    /**
     * Getter for the x509 Platform Certificate version.
     *
     * @return a big integer representing the certificate version. If there
     * is an error, return the max value to visible show error.
     */
    public int getX509CredentialVersion() {
        try {
            return getX509Certificate().getVersion() - 1;
        } catch (IOException ex) {
            log.warn("X509 Credential Version not found.");
            log.error(ex);
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Checks if another certificate is the issuer for this certificate.
     *
     * @param issuer the other certificate to check (must be an X509Certificate,
     *               not an X509AttributeCertificateHolder)
     * @return whether or not the other certificate is the issuer for this certificate
     * @throws IOException if there is an issue deserializing either certificate
     */
    public String isIssuer(final Certificate issuer) throws IOException {
        String isIssuer = "Certificate signature failed to verify";
        // only run if of the correct type, otherwise false
        if (issuer.getCertificateType() == CertificateType.X509_CERTIFICATE) {
            X509Certificate issuerX509 = issuer.getX509Certificate();
            // Validate if it's the issuer
            switch (getCertificateType()) {
                case X509_CERTIFICATE:
                    X509Certificate certX509 = getX509Certificate();
                    try {
                        certX509.verify(issuerX509.getPublicKey());
                        isIssuer = "";
                    } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException
                             | NoSuchProviderException | SignatureException e) {
                        log.error(e);
                    }
                    break;
                case ATTRIBUTE_CERTIFICATE:
                    AttributeCertificate attCert = getAttributeCertificate();
                    try {
                        Signature sig = Signature.getInstance(this.getSignatureAlgorithm());
                        sig.initVerify(issuerX509.getPublicKey());
                        sig.update(attCert.getAcinfo().getEncoded());
                        if (sig.verify(attCert.getSignatureValue().getBytes())) {
                            isIssuer = "";
                        }
                    } catch (NoSuchAlgorithmException
                             | InvalidKeyException
                             | SignatureException sigEx) {
                        log.error(sigEx);
                    }
                    break;
                default:
                    break;
            }
        }

        return isIssuer;
    }

    /**
     * Return whether this certificate is valid on a particular date.
     *
     * @param date the date of interest.
     * @return true if the attribute certificate is valid, false otherwise.
     */
    public boolean isValidOn(final Date date) {
        return !date.before(getBeginValidity()) && !date.after(getEndValidity());
    }

    /**
     * Retrieve the original X509 certificate.
     *
     * @return the original X509 certificate
     * @throws IOException if there is a problem deserializing the certificate as an X509 cert
     */
    @JsonIgnore
    public X509Certificate getX509Certificate() throws IOException {
        if (parsedX509Cert != null) {
            return parsedX509Cert;
        }

        try (ByteArrayInputStream certInputStream = new ByteArrayInputStream(certificateBytes)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            parsedX509Cert = (X509Certificate) cf.generateCertificate(certInputStream);
            return parsedX509Cert;
        } catch (CertificateException cEx) {
            throw new IOException("Cannot construct X509Certificate from the input stream", cEx);
        }
    }

    /**
     * @return the type of certificate.
     * @throws java.io.IOException if there is a problem extracting information from the certificate
     */
    protected CertificateType getCertificateType() throws IOException {
        //Parse the certificate into a sequence
        ASN1Sequence testCred1 = (ASN1Sequence) ASN1Primitive.fromByteArray(this.certificateBytes);
        ASN1Sequence testSeq = (ASN1Sequence) testCred1.toArray()[0];

        if (testSeq.toArray()[0] instanceof ASN1Integer) {
            if (testSeq.toArray().length >= MIN_ATTR_CERT_LENGTH) {
                // Attribute Certificate
                return CertificateType.ATTRIBUTE_CERTIFICATE;
            } else {
                // V1 X509Certificate
                return CertificateType.X509_CERTIFICATE;
            }
        } else if (testSeq.toArray()[0] instanceof DERTaggedObject
                || testSeq.toArray()[0] instanceof DLTaggedObject) {
            // V2 or V3 X509Certificate
            return CertificateType.X509_CERTIFICATE;
        }

        return CertificateType.INVALID_CERTIFICATE;
    }

    private String parseKeyUsage(final boolean[] bools) {
        StringBuilder sb = new StringBuilder();

        if (bools != null) {
            for (int i = 0; i < bools.length; i++) {
                if (bools[i]) {
                    sb.append(CredentialHelper.getKeyUsageString(i));
                }
            }
        }

        return sb.toString();
    }

    /**
     * Getter for the authorityKeyIdentifier.
     *
     * @param aki authority key identifier
     * @return the ID's byte representation
     */
    private String authKeyIdentifierToString(final AuthorityKeyIdentifier aki) {
        String retValue = "";
        if (aki != null) {
            byte[] keyArray = aki.getKeyIdentifierOctets();
            if (keyArray != null) {
                retValue = HexUtils.byteArrayToHexString(keyArray);
            }
        }

        return retValue;
    }

    /**
     * Gets the contents of requested OID.
     *
     * @param oid Object Identifier
     * @return ASN1Primitive Content related to the requested OID
     * @throws IOException io exception
     */
    private ASN1Primitive getExtensionValue(final String oid) throws IOException {
        byte[] extensionValue = getX509Certificate().getExtensionValue(oid);
        ASN1Primitive asn1Primitive = null;
        ASN1InputStream asn1InputStream = null;

        if (extensionValue != null) {
            try {
                asn1InputStream = new ASN1InputStream(extensionValue);
                DEROctetString oct = (DEROctetString) asn1InputStream.readObject();
                asn1InputStream.close();
                asn1InputStream = new ASN1InputStream(oct.getOctets());
                asn1Primitive = asn1InputStream.readObject();
            } catch (IOException ioEx) {
                log.error(ioEx);
            } finally {
                if (asn1InputStream != null) {
                    asn1InputStream.close();
                }
            }
        }

        return asn1Primitive;
    }

    /**
     * Getter for the AuthorityInfoAccess extension value on list format.
     *
     * @param authInfoAccess byte representation of the authority info access
     * @return List Authority info access list
     */
    private String getAuthorityInfoAccess(final byte[] authInfoAccess) {
        StringBuilder sb = new StringBuilder();

        try {
            if (authInfoAccess != null && authInfoAccess.length > 0) {
                sb.append(getAuthorityInfoAccess(AuthorityInformationAccess
                        .getInstance(JcaX509ExtensionUtils.parseExtensionValue(authInfoAccess))));
            }
        } catch (IOException ioEx) {
            log.error(ioEx);
        }

        return sb.toString();
    }

    /**
     * Getter for the AuthorityInfoAccess extension value on list format.
     *
     * @param authInfoAccess authority information access
     * @return List Authority info access list
     */
    private String getAuthorityInfoAccess(final AuthorityInformationAccess authInfoAccess) {
        StringBuilder sb = new StringBuilder();

        if (authInfoAccess != null) {
            for (AccessDescription desc : authInfoAccess.getAccessDescriptions()) {
                if (desc.getAccessLocation().getTagNo() == GeneralName
                        .uniformResourceIdentifier) {
                    sb.append(String.format("%s%n", ((DERIA5String) desc
                            .getAccessLocation()
                            .getName())
                            .getString()));
                }
            }
        }

        return sb.toString();
    }

    /**
     * Retrieve the original X509 attribute certificate.
     *
     * @return the original X509 attribute certificate
     * @throws IOException if there is a problem deserializing the certificate as an X509
     *                     attribute cert
     */
    @JsonIgnore
    public X509AttributeCertificateHolder getX509AttributeCertificateHolder() throws IOException {
        return new X509AttributeCertificateHolder(certificateBytes);
    }

    /**
     * Retrieve the original Attribute Certificate.
     *
     * @return the original Attribute Certificate
     * @throws IOException if there is a problem deserializing the certificate as an X509
     *                     attribute cert
     */
    @JsonIgnore
    public AttributeCertificate getAttributeCertificate() throws IOException {
        return AttributeCertificate
                .getInstance(ASN1Primitive.fromByteArray(certificateBytes));
    }

    /**
     * @return this certificate's signature
     */
    public byte[] getSignature() {
        return signature.clone();
    }

    /**
     * @return this certificate's validity start date
     */
    public Date getBeginValidity() {
        return new Date(beginValidity.getTime());
    }

    /**
     * @return this certificate's validity end date
     */
    public Date getEndValidity() {
        return new Date(endValidity.getTime());
    }

    /**
     * Getter for the policy statement.
     *
     * @return cloned bit representation of constraints
     */
    public byte[] getPolicyConstraints() {
        if (policyConstraints != null) {
            return policyConstraints.clone();
        }
        return null;
    }

    /**
     * @return this certificate's encoded public key
     */
    public byte[] getEncodedPublicKey() {
        if (encodedPublicKey == null) {
            return null;
        } else {
            return encodedPublicKey.clone();
        }
    }

    /**
     * Gets the raw bytes for the certificate.
     *
     * @return copy of the certificate bytes
     */
    @JsonIgnore
    public byte[] getRawBytes() {
        if (this.certificateBytes != null) {
            return this.certificateBytes.clone();
        }
        return null;
    }

    /**
     * Creates a string representation of the Certificate object.
     *
     * @return a string representation of the Certificate object.
     */
    @Override
    public String toString() {
        return String.format("Certificate{%s, AuthID=%s, serialNumber=%s, "
                        + "issuer=%s, AuthSerialNumber=%s, publicKeySize=%d, "
                        + "signatureAlg=%s, Hash=%d}", super.toString(),
                authorityKeyIdentifier, serialNumber.toString(),
                issuer, authoritySerialNumber.toString(), publicKeySize,
                signatureAlgorithm, certificateHash);
    }

    /**
     * Compares this certificate to the provided object to verify that both this and the provided certificate
     * objects are equal.
     *
     * @param o object to compare
     * @return true if both the provided certificate and this certificate are equal, false otherwise
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Certificate that = (Certificate) o;

        return Arrays.equals(certificateBytes, that.certificateBytes);
    }

    /**
     * Creates an integer hash code for this Certificate object.
     *
     * @return integer hash code
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(certificateBytes);
    }

    /**
     * Holds the different certificate types.
     */
    public enum CertificateType {
        /**
         * Basic X509 Certificate.
         */
        X509_CERTIFICATE,
        /**
         * Basic Attribute Certificate.
         */
        ATTRIBUTE_CERTIFICATE,
        /**
         * Invalid Certificate.
         */
        INVALID_CERTIFICATE
    }
}
