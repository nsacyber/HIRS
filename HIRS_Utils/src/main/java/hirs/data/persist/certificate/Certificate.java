package hirs.data.persist.certificate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hirs.data.persist.ArchivableEntity;
import hirs.data.persist.certificate.attributes.AttributeCertificatePkc;
import hirs.utils.HexUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLSequence;
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
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.x509.extension.X509ExtensionUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;


/**
 * This class enables the persistence of a single X509 certificates or X509 attribute certificate.
 * It stores certain attributes separately from the serialized certificate to enable querying on
 * those attributes.
 */
@Entity
public abstract class Certificate extends ArchivableEntity {
    private static final String PEM_HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_FOOTER = "-----END CERTIFICATE-----";
    private static final String PEM_ATTRIBUTE_HEADER = "-----BEGIN ATTRIBUTE CERTIFICATE-----";
    private static final String PEM_ATTRIBUTE_FOOTER = "-----END ATTRIBUTE CERTIFICATE-----";
    private static final String MALFORMED_CERT_MESSAGE = "Malformed certificate detected.";
    private static final int MAX_CERT_LENGTH_BYTES = 2048;
    private static final int MAX_NUMERIC_PRECISION = 49; // Can store up to 160 bit values
    private static final int MAX_PUB_KEY_MODULUS_HEX_LENGTH = 1024;
    private static final int KEY_USAGE_BIT0 = 0;
    private static final int KEY_USAGE_BIT1 = 1;
    private static final int KEY_USAGE_BIT2 = 2;
    private static final int KEY_USAGE_BIT3 = 3;
    private static final int KEY_USAGE_BIT4 = 4;
    private static final int KEY_USAGE_BIT5 = 5;
    private static final int KEY_USAGE_BIT6 = 6;
    private static final int KEY_USAGE_BIT7 = 7;
    private static final int KEY_USAGE_BIT8 = 8;
    private static final String KEY_USAGE_DS = "DIGITAL SIGNATURE";
    private static final String KEY_USAGE_NR = "NON-REPUDIATION";
    private static final String KEY_USAGE_KE = "KEY ENCIPHERMENT";
    private static final String KEY_USAGE_DE = "DATA ENCIPHERMENT";
    private static final String KEY_USAGE_KA = "KEY AGREEMENT";
    private static final String KEY_USAGE_KC = "KEY CERT SIGN";
    private static final String KEY_USAGE_CS = "CRL SIGN";
    private static final String KEY_USAGE_EO = "ENCIPHER ONLY";
    private static final String KEY_USAGE_DO = "DECIPHER ONLY";
    private static final String ECDSA_OID = "1.2.840.10045.4.3.2";
    private static final String ECDSA_SHA224_OID = "1.2.840.10045.4.1";
    private static final String RSA256_OID = "1.2.840.113549.1.1.11";
    private static final String RSA384_OID = "1.2.840.113549.1.1.12";
    private static final String RSA512_OID = "1.2.840.113549.1.1.13";
    private static final String RSA224_OID = "1.2.840.113549.1.1.14";
    private static final String RSA512_224_OID = "1.2.840.113549.1.1.15";
    private static final String RSA512_256_OID = "1.2.840.113549.1.1.16";
    private static final String RSA256_STRING = "SHA256WithRSA";
    private static final String RSA384_STRING = "SHA384WithRSA";
    private static final String RSA224_STRING = "SHA224WithRSA";
    private static final String RSA512_STRING = "SHA512WithRSA";
    private static final String RSA512_224_STRING = "SHA512-224WithRSA";
    private static final String RSA512_256_STRING = "SHA512-256WithRSA";
    private static final String ECDSA_STRING = "SHA256WithECDSA";
    private static final String ECDSA_SHA224_STRING = "SHA224WithECDSA";

    private static final Logger LOGGER = LogManager.getLogger(Certificate.class);

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
    @Column(nullable = false, precision = MAX_NUMERIC_PRECISION, scale = 0)
    private final BigInteger serialNumber;

    /**
     * Holds the name of the 'issuer' field.
     */
    public static final String ISSUER_FIELD = "issuer";
    @Column(nullable = false)
    private final String issuer;
    /**
     * Holds the name of the 'issuerSorted' field.
     */
    public static final String ISSUER_SORTED_FIELD = "issuerSorted";
    @Column
    private final String issuerSorted;

    /**
     * Holds the name of the 'subject' field.
     */
    public static final String SUBJECT_FIELD = "subject";
    @Column(nullable = true)
    private final String subject;
    /**
     * Holds the name of the 'subjectSorted' field.
     */
    public static final String SUBJECT_SORTED_FIELD = "subjectSorted";
    @Column
    private final String subjectSorted;

    /**
     * Holds the name of the 'encodedPublicKey' field.
     */
    public static final String ENCODED_PUBLIC_KEY_FIELD = "encodedPublicKey";
    @Column(length = MAX_CERT_LENGTH_BYTES, nullable = true)
    private final byte[] encodedPublicKey;

    /**
     * Holds the name of the 'publicKeyModulusHexValue' field.
     */
    public static final String PUBLIC_KEY_MODULUS_FIELD = "publicKeyModulusHexValue";

    // We're currently seeing 2048-bit keys, which is 512 hex digits.
    // Using a max length of 1024 for future-proofing.
    @Column(length = MAX_PUB_KEY_MODULUS_HEX_LENGTH, nullable = true)
    private final String publicKeyModulusHexValue;

    @Column(length = MAX_CERT_LENGTH_BYTES, nullable = false)
    private final byte[] signature;

    @Column(nullable = false)
    private final Date beginValidity;

    @Column(nullable = false)
    private final Date endValidity;

    @Column(length = MAX_CERT_LENGTH_BYTES, nullable = false)
    @JsonIgnore
    private byte[] certificateBytes;

    /**
     * Holds the name of the 'certificateHash' field.
     */
    public static final String CERTIFICATE_HASH_FIELD = "certificateHash";
    @Column(nullable = false)
    @JsonIgnore
    private final int certificateHash;

    /**
     * This field exists to enforce a unique constraint on a hash over the certificate contents
     * and the certificate type.  This is to ensure the system only allows one copy of a
     * certificate per role in the system.
     */
    @Column(nullable = false, unique = true)
    @JsonIgnore
    private final int certAndTypeHash;

    /**
     * Holds the name of the 'holderSerialNumber' field.
     */
    public static final String HOLDER_SERIAL_NUMBER_FIELD = "holderSerialNumber";

    @Column(nullable = false, precision = MAX_NUMERIC_PRECISION, scale = 0)
    private final BigInteger holderSerialNumber;
    private String holderIssuer;
    @Column(nullable = true, precision = MAX_NUMERIC_PRECISION, scale = 0)
    private final BigInteger authoritySerialNumber;

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP") // this is not an IP address; PMD thinks it is
    private static final String POLICY_CONSTRAINTS = "2.5.29.36";

    // we don't need to persist this, but we don't want to unpack this cert multiple times
    @Transient
    private X509Certificate parsedX509Cert = null;

    private String signatureAlgorithm;
    private String publicKeyAlgorithm;
    private String keyUsage;
    private String extendedKeyUsage;
    private byte[] policyConstraints;
    /**
     * Holds the name of the 'authorityKeyIdentifier' field.
     */
    public static final String AUTHORITY_KEY_ID_FIELD = "authorityKeyIdentifier";
    private String authorityKeyIdentifier;
    private String authorityInfoAccess;
    private String crlPoints;
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
    @SuppressWarnings("methodlength")
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
        if (isPEM(possiblePem)) {
            possiblePem = possiblePem.replace(PEM_HEADER, "");
            possiblePem = possiblePem.replace(PEM_FOOTER, "");
            possiblePem = possiblePem.replace(PEM_ATTRIBUTE_HEADER, "");
            possiblePem = possiblePem.replace(PEM_ATTRIBUTE_FOOTER, "");
            this.certificateBytes = Base64.decode(possiblePem);
        }

        AuthorityKeyIdentifier authKeyIdentifier;
        this.certificateBytes = trimCertificate(this.certificateBytes);

        // Extract certificate data
        switch (getCertificateType(this.certificateBytes)) {
            case X509_CERTIFICATE:
                X509Certificate x509Certificate = getX509Certificate();
                this.serialNumber = x509Certificate.getSerialNumber();
                this.issuer = x509Certificate.getIssuerX500Principal().getName();
                this.subject = x509Certificate.getSubjectX500Principal().getName();
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
                this.issuerSorted = parseSortDNs(this.issuer);
                this.subjectSorted = parseSortDNs(this.subject);
                this.policyConstraints = x509Certificate
                        .getExtensionValue(POLICY_CONSTRAINTS);
                authKeyIdentifier = AuthorityKeyIdentifier
                        .getInstance((DLSequence) getExtensionValue(
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
                    case RSA256_OID:
                        this.signatureAlgorithm = RSA256_STRING;
                        break;
                    case RSA384_OID:
                        this.signatureAlgorithm = RSA384_STRING;
                        break;
                    case RSA224_OID:
                        this.signatureAlgorithm = RSA224_STRING;
                        break;
                    case RSA512_OID:
                        this.signatureAlgorithm = RSA512_STRING;
                        break;
                    case RSA512_224_OID:
                        this.signatureAlgorithm = RSA512_224_STRING;
                        break;
                    case RSA512_256_OID:
                        this.signatureAlgorithm = RSA512_256_STRING;
                        break;
                    case ECDSA_OID:
                        this.signatureAlgorithm = ECDSA_STRING;
                        break;
                    case ECDSA_SHA224_OID:
                        this.signatureAlgorithm = ECDSA_SHA224_STRING;
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
                this.issuerSorted = parseSortDNs(this.issuer);

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

    @SuppressWarnings("magicnumber")
    private byte[] trimCertificate(final byte[] certificateBytes) {
        int certificateStart = 0;
        int certificateLength = 0;
        ByteBuffer certificateByteBuffer = ByteBuffer.wrap(certificateBytes);

        StringBuilder malformedCertStringBuilder = new StringBuilder(MALFORMED_CERT_MESSAGE);
        while (certificateByteBuffer.hasRemaining()) {
            // Check if there isn't an ASN.1 structure in the provided bytes
            if (certificateByteBuffer.remaining() <= 2) {
                throw new IllegalArgumentException(malformedCertStringBuilder
                        .append(" No certificate length field could be found.").toString());
            }

            // Look for first ASN.1 Sequence marked by the two bytes (0x30) and (0x82)
            // The check advances our position in the ByteBuffer by one byte
            int currentPosition = certificateByteBuffer.position();
            if (certificateByteBuffer.get() == (byte) 0x30
                    && certificateByteBuffer.get(currentPosition + 1) == (byte) 0x82) {
                // Check if we have anything more in the buffer than an ASN.1 Sequence header
                if (certificateByteBuffer.remaining() <= 3) {
                    throw new IllegalArgumentException(malformedCertStringBuilder
                            .append(" Certificate is nothing more than ASN.1 Sequence.")
                            .toString());
                }
                // Mark the start of the first ASN.1 Sequence / Certificate Body
                certificateStart = currentPosition;

                // Parse the length as the 2-bytes following the start of the ASN.1 Sequence
                certificateLength = Short.toUnsignedInt(
                        certificateByteBuffer.getShort(currentPosition + 2));
                // Add the 4 bytes that comprise the start of the ASN.1 Sequence and the length
                certificateLength += 4;
                break;
            }
        }

        if (certificateStart + certificateLength > certificateBytes.length) {
            throw new IllegalArgumentException(malformedCertStringBuilder
                    .append(" Value of certificate length field extends beyond length")
                    .append(" of provided certificate.").toString());
        }
        // Return bytes representing the main certificate body
        return Arrays.copyOfRange(certificateBytes, certificateStart,
                certificateStart + certificateLength);
    }

    /**
     * Tests the bytes given to determine the type of credential.
     * @param certificateBytes byte array to test
     * @return the type of certificate.
     * @throws java.io.IOException if there is a problem extracting information from the certificate
     */
    public CertificateType getCertificateType(final byte[] certificateBytes)
            throws IOException {
        //Parse the certificate into a sequence
        ASN1Sequence testCred1 = (ASN1Sequence) ASN1Primitive
                .fromByteArray(trimCertificate(certificateBytes));
        ASN1Sequence testSeq = (ASN1Sequence) ((ASN1Object) testCred1.toArray()[0]);

        if (testSeq.toArray()[0] instanceof ASN1Integer) {
             if (testSeq.toArray().length >= MIN_ATTR_CERT_LENGTH) {
                 LOGGER.error("This is an Attribute. Should it?");
                 // Attribute Certificate
                 return CertificateType.ATTRIBUTE_CERTIFICATE;
             } else {
                 LOGGER.error("This is X509 V1. Should it?");
                 // V1 X509Certificate
                 return CertificateType.X509_CERTIFICATE;
             }
        } else if (testSeq.toArray()[0] instanceof DERTaggedObject) {
            LOGGER.error("This is X509 V2+. Should it?");
            // V2 or V3 X509Certificate
            return CertificateType.X509_CERTIFICATE;
        }

        return CertificateType.INVALID_CERTIFICATE;
    }

    private boolean isPEM(final String possiblePEM) {
        return possiblePEM.contains(PEM_HEADER) || possiblePEM.contains(PEM_ATTRIBUTE_HEADER);
    }

    private String parseKeyUsage(final boolean[] bools) {
        StringBuilder sb = new StringBuilder();

        if (bools != null) {
            for (int i = 0; i < bools.length; i++) {
                if (bools[i]) {
                    sb.append(getKeyUsageString(i));
                }
            }
        }

        return sb.toString();
    }

    /**
     * Return the string associated with the boolean slot.
     * @param bit associated with the location in the array.
     * @return string value of the bit set.
     */
    private String getKeyUsageString(final int bit) {
        String tempStr = "";

        switch (bit) {
            case KEY_USAGE_BIT0:
                tempStr = String.format("%s%n", KEY_USAGE_DS);
                break;
            case KEY_USAGE_BIT1:
                tempStr = String.format("%s%n", KEY_USAGE_NR);
                break;
            case KEY_USAGE_BIT2:
                tempStr = String.format("%s%n", KEY_USAGE_KE);
                break;
            case KEY_USAGE_BIT3:
                tempStr = String.format("%s%n", KEY_USAGE_DE);
                break;
            case KEY_USAGE_BIT4:
                tempStr = String.format("%s%n", KEY_USAGE_KA);
                break;
            case KEY_USAGE_BIT5:
                tempStr = String.format("%s%n", KEY_USAGE_KC);
                break;
            case KEY_USAGE_BIT6:
                tempStr = String.format("%s%n", KEY_USAGE_CS);
                break;
            case KEY_USAGE_BIT7:
                tempStr = String.format("%s%n", KEY_USAGE_EO);
                break;
            case KEY_USAGE_BIT8:
                tempStr = String.format("%s%n", KEY_USAGE_DO);
                break;
            default:
                break;
        }

        return tempStr;
    }

    /**
     * Getter for the authorityKeyIdentifier.
     * @return the ID's byte representation
     */
    private String authKeyIdentifierToString(final AuthorityKeyIdentifier aki) {
        String retValue = "";
        if (aki != null) {
            byte[] keyArray = aki.getKeyIdentifier();
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
     * @throws java.io.IOException
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
                LOGGER.error(ioEx);
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
     * @return List Authority info access list
     */
    private String getAuthorityInfoAccess(final byte[] authoInfoAccess) {
        StringBuilder sb = new StringBuilder();

        try {
            if (authoInfoAccess != null && authoInfoAccess.length > 0) {
                sb.append(getAuthorityInfoAccess(AuthorityInformationAccess
                        .getInstance(X509ExtensionUtil.fromExtensionValue(authoInfoAccess))));
            }
        } catch (IOException ioEx) {
            LOGGER.error(ioEx);
        }

        return sb.toString();
    }

    /**
     * Getter for the AuthorityInfoAccess extension value on list format.
     *
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
     * Getter for the CRL Distribution that is reference by the Revocation Locator
     * on the portal.
     *
     * @return A list of URLs that inform the location of the certificate revocation lists
     * @throws java.io.IOException
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
     * @return a big integer representing the certificate version. If there
     * is an error, return the max value to visible show error.
     */
    public int getX509CredentialVersion() {
        try {
            return getX509Certificate().getVersion() - 1;
        } catch (IOException ex) {
            LOGGER.warn("X509 Credential Version not found.");
            LOGGER.error(ex);
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Checks if another certificate is the issuer for this certificate.
     *
     * @param issuer the other certificate to check (must be an X509Certificate,
     * not an X509AttributeCertificateHolder)
     * @return whether or not the other certificate is the issuer for this certificate
     * @throws IOException if there is an issue deserializing either certificate
     */
    public String isIssuer(final Certificate issuer) throws IOException {
        String isIssuer = "Certificate signature failed to verify";
        // only run if of the correct type, otherwise false
        if (issuer.getCertificateType(this.certificateBytes) == CertificateType.X509_CERTIFICATE) {
            X509Certificate issuerX509 = issuer.getX509Certificate();
            // Validate if it's the issuer
            switch (getCertificateType(this.certificateBytes)) {
                case X509_CERTIFICATE:
                    X509Certificate certX509 = getX509Certificate();
                    try {
                        certX509.verify(issuerX509.getPublicKey());
                        isIssuer = "";
                    } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException
                            | NoSuchProviderException | SignatureException e) {
                        LOGGER.error(e);
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
                        LOGGER.error(sigEx);
                    }
                    break;
                default:
                    break;
            }
        }

        return isIssuer;
    }

    /**
     * Return whether or not this certificate is valid on a particular date.
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
        } catch (CertificateException e) {
            throw new IOException("Cannot construct X509Certificate from the input stream", e);
        }
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
        AttributeCertificate attCertificate = AttributeCertificate
                .getInstance(ASN1Primitive.fromByteArray(certificateBytes));
        return attCertificate;
    }

    /**
     * Retrieve the original Attribute Certificate.
     *
     * @return the original Attribute Certificate
     * @throws IOException if there is a problem deserializing the certificate as an X509
     *                     attribute cert
     */
    @JsonIgnore
    public AttributeCertificatePkc getAttributeCertificatePkc() throws IOException {
        AttributeCertificatePkc certificatePkc = AttributeCertificatePkc
                .getInstance(ASN1Primitive.fromByteArray(getRawBytes()));
        return certificatePkc;
    }

    /**
     * Getter for the Authority Info Access List.
     * @return return a list of Info Access
     */
    public String getAuthInfoAccess() {
        return authorityInfoAccess;
    }

    /**
     * Getter for the authorityKeyIdentifier.
     * @return the ID String
     */
    public String getAuthKeyId() {
        return authorityKeyIdentifier;
    }

    /**
     * Getter for the CRL Distribution Points.
     * @return CRLs
     */
    public String getCrlPoints() {
        return crlPoints;
    }

    /**
     * Getter for the policy statement.
     * @return cloned bit representation of constraints
     */
    public byte[] getPolicyConstraints() {
        if (policyConstraints != null) {
            return policyConstraints.clone();
        }

        return null;
    }

    /**
     * Getter for the keyUsage info.
     * @return key usages
     */
    public String getKeyUsage() {
        return keyUsage;
    }

    /**
     * Getter for the extended key info.
     * @return extended key usages
     */
    public String getExtendedKeyUsage() {
        return extendedKeyUsage;
    }

    /**
     * Getter for the algorithm.
     * @return algorithm for the signature
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * Getter for the public key algorithm.
     * @return  public key algorithm
     */
    public String getPublicKeyAlgorithm() {
        return publicKeyAlgorithm;
    }

    /**
     * Getter the Certificate's Serial Number.
     *
     * @return this certificate's serial number
     */
    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    /**
     * Getter for the Authority's Serial Number.
     *
     * @return this Authority's Key ID serial number.
     */
    public BigInteger getAuthoritySerialNumber() {
        return authoritySerialNumber;
    }

    /**
     * Getter for the Holder's Serial Number.
     *
     * @return this certificate's holder serial number
     */
    public BigInteger getHolderSerialNumber() {
        return holderSerialNumber;
    }

    /**
     * Getter for the Certificate's Holder Common Name information.
     * @return this certificate's holder issuer as a string.
     */
    public String getHolderIssuer() {
        return this.holderIssuer;
    }

    /**
     * @return this certificate's issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * @return this certificate's subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Getter for the Public Key Size.
     * @return bit count.
     */
    public int getPublicKeySize() {
        return publicKeySize;
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
     * Getter for the hex value.
     * @return a string for the public key
     */
    public String getPublicKeyModulusHexValue() {
        return publicKeyModulusHexValue;
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
     * @return the hash code of this certificate's bytes
     */
    public int getCertificateHash() {
        return certificateHash;
    }

    /**
     * @return this certificate's associated issuer sorted
     */
    public String getIssuerSorted() {
        return issuerSorted;
    }

    /**
     * @return this certificate's associated subject sorted
     */
    public String getSubjectSorted() {
        return subjectSorted;
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

    @Override
    public String toString() {
        return String.format("Certificate{%s, AuthID=%s, serialNumber=%s, "
                + "issuer=%s, AuthSerialNumber=%s, publicKeySize=%d, "
                + "signatureAlg=%s, Hash=%d}", super.toString(),
                authorityKeyIdentifier, serialNumber.toString(),
                issuer, authoritySerialNumber.toString(), publicKeySize,
                signatureAlgorithm, certificateHash);
    }

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

    @Override
    public int hashCode() {
        return Arrays.hashCode(certificateBytes);
    }

    /**
     *
     * Gets the raw bytes for the certificate.
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
                    certificateHolder.getSubjectPublicKeyInfo()
                            .parsePublicKey().toASN1Primitive());
        } catch (IOException e) {
            LOGGER.info("No RSA Key Detected in certificate");
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
        if (publicKeyASN1 instanceof ASN1Sequence) {
            ASN1Sequence publicKeyASN1Sequence = (ASN1Sequence) publicKeyASN1;
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
        if (publicKey instanceof ASN1Sequence) {
            ASN1Sequence pubKeySeq = (ASN1Sequence) publicKey;
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
     * This method is to take the DNs from certificates and sort them in an order
     * that will be used to lookup issuer certificates.  This will not be stored in
     * the certificate, just the DB for lookup.
     * @param distinguishedName the original DN string.
     * @return a modified string of sorted DNs
     */
    public static String parseSortDNs(final String distinguishedName) {
        StringBuilder sb = new StringBuilder();
        String dnsString;

        if (distinguishedName == null || distinguishedName.isEmpty()) {
            sb.append("BLANK");
        } else {
            dnsString = distinguishedName.trim();
            dnsString = dnsString.toLowerCase();
            List<String> dnValArray = Arrays.asList(dnsString.split(","));
            Collections.sort(dnValArray);
            ListIterator<String> dnListIter = dnValArray.listIterator();
            while (dnListIter.hasNext()) {
                sb.append(dnListIter.next());
                if (dnListIter.hasNext()) {
                    sb.append(",");
                }
            }
        }

        return sb.toString();
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

        return (X500Name[]) l.toArray(new X500Name[l.size()]);

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
}
