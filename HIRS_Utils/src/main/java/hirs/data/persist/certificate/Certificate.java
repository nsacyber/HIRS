package hirs.data.persist.certificate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hirs.data.persist.ArchivableEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x509.AttributeCertificate;
import org.bouncycastle.asn1.x509.AttributeCertificateInfo;
import org.bouncycastle.asn1.x509.AttCertIssuer;
import org.bouncycastle.asn1.x509.V2Form;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.DERTaggedObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
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
@Entity
public abstract class Certificate extends ArchivableEntity {
    private static final String PEM_HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_FOOTER = "-----END CERTIFICATE-----";
    private static final String PEM_ATTRIBUTE_HEADER = "-----BEGIN ATTRIBUTE CERTIFICATE-----";
    private static final String PEM_ATTRIBUTE_FOOTER = "-----END ATTRIBUTE CERTIFICATE-----";
    private static final int MAX_CERT_LENGTH_BYTES = 2048;
    private static final int MAX_NUMERIC_PRECISION = 49; // Can store up to 160 bit values
    private static final int MAX_PUB_KEY_MODULUS_HEX_LENGTH = 1024;

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
     * Holds the name of the 'subject' field.
     */
    public static final String SUBJECT_FIELD = "subject";
    @Column(nullable = true)
    private final String subject;

    /**
     * Holds the name of the 'issuerOrganization' field.
     */
    public static final String ISSUER_ORGANIZATION_FIELD = "issuerOrganization";
    @Column
    private String issuerOrganization = null;

    /**
     * Holds the name of the 'subjectOrganization' field.
     */
    public static final String SUBJECT_ORGANIZATION_FIELD = "subjectOrganization";
    @Column
    private String subjectOrganization = null;

    /**
     * Holds the name of the 'encodedPublicKey' field.
     */
    public static final String ENCODED_PUBLIC_KEY_FIELD = "encodedPublicKey";
    @Column(length = MAX_CERT_LENGTH_BYTES, nullable = true)
    private final byte[] encodedPublicKey;

    /**
     * Holds the name of the 'encodedPublicKey' field.
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

    // we don't need to persist this, but we don't want to unpack this cert multiple times
    @Transient
    private X509Certificate parsedX509Cert = null;


    /**
     * Default constructor necessary for Hibernate.
     */
    protected Certificate() {
        super();
        this.serialNumber = BigInteger.ZERO;
        this.issuer = null;
        this.subject = null;

        this.encodedPublicKey = null;
        this.publicKeyModulusHexValue = null;
        this.signature = null;
        this.beginValidity = null;
        this.endValidity = null;
        this.certificateBytes = null;
        this.certificateHash = 0;
        this.certAndTypeHash = 0;
        this.holderSerialNumber = BigInteger.ZERO;
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
        if (isPEM(possiblePem)) {
            possiblePem = possiblePem.replace(PEM_HEADER, "");
            possiblePem = possiblePem.replace(PEM_FOOTER, "");
            possiblePem = possiblePem.replace(PEM_ATTRIBUTE_HEADER, "");
            possiblePem = possiblePem.replace(PEM_ATTRIBUTE_FOOTER, "");
            this.certificateBytes = Base64.decode(possiblePem);
        }

        //Extract certificate data
        switch (getCertificateType()) {
            case X509_CERTIFICATE:

                X509Certificate x509Certificate = getX509Certificate();
                this.serialNumber = x509Certificate.getSerialNumber();
                this.issuer = x509Certificate.getIssuerX500Principal().getName();
                this.subject = x509Certificate.getSubjectX500Principal().getName();
                this.encodedPublicKey = x509Certificate.getPublicKey().getEncoded();
                this.publicKeyModulusHexValue = getPublicKeyModulus(x509Certificate)
                                                    .toString(HEX_BASE);
                this.signature = x509Certificate.getSignature();
                this.beginValidity = x509Certificate.getNotBefore();
                this.endValidity = x509Certificate.getNotAfter();
                this.holderSerialNumber = BigInteger.ZERO;
                this.issuerOrganization = getOrganization(this.issuer);
                this.subjectOrganization = getOrganization(this.subject);
                break;

            case ATTRIBUTE_CERTIFICATE:
                AttributeCertificate attCert = getAttributeCertificate();
                AttributeCertificateInfo attCertInfo = attCert.getAcinfo();

                //Set null values (Attribute certificates do not have this values)
                this.subject = null;
                this.subjectOrganization = null;
                this.encodedPublicKey = null;
                this.publicKeyModulusHexValue = null;

                //Get attribute certificate information
                this.serialNumber = attCertInfo.getSerialNumber().getValue();
                this.holderSerialNumber = attCertInfo
                                            .getHolder()
                                            .getBaseCertificateID()
                                            .getSerial()
                                            .getValue();
                this.signature = attCert.getSignatureValue().getBytes();

                this.issuer = getAttributeCertificateIssuerNames(
                                        attCertInfo.getIssuer())[0].toString();
                this.issuerOrganization = getOrganization(this.issuer);

                //Parse notBefore and notAfter dates
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

        this.certificateHash = Arrays.hashCode(this.certificateBytes);
        this.certAndTypeHash = Objects.hash(certificateHash, getClass().getSimpleName());
    }

    /**
     * @return the type of certificate.
     * @throws java.io.IOException if there is a problem extracting information from the certificate
     */
    protected CertificateType getCertificateType() throws IOException {
        //Parse the certificate into a sequence
        ASN1Sequence testCred1 = (ASN1Sequence) ASN1Primitive.fromByteArray(this.certificateBytes);
        ASN1Sequence testSeq = (ASN1Sequence) ((ASN1Object) testCred1.toArray()[0]);

        if (testSeq.toArray()[0] instanceof ASN1Integer) {
             if (testSeq.toArray().length >= MIN_ATTR_CERT_LENGTH) {
                 //Attribute Certificate
                 return CertificateType.ATTRIBUTE_CERTIFICATE;
             } else {
                 //V1 X509Certificate
                 return CertificateType.X509_CERTIFICATE;
             }
        } else if (testSeq.toArray()[0] instanceof DERTaggedObject) {
            //V2 or V3 X509Certificate
            return CertificateType.X509_CERTIFICATE;
        }

        return CertificateType.INVALID_CERTIFICATE;
    }

    /**
     * Extracts the organization field out of a distinguished name. Returns null if
     * no organization field exists.
     * @param distinguishedName distinguished name to extract the organization from
     * @return the value of the organization field
     */
    protected static String getOrganization(final String distinguishedName) {
        String organization = null;

        //Return null for empy strings
        if (distinguishedName.isEmpty()) {
            return null;
        }

        //Parse string to X500Name
        X500Name name = new X500Name(distinguishedName);
        if (name.getRDNs(RFC4519Style.o).length > 0) {
            RDN rdn = name.getRDNs(RFC4519Style.o)[0];
            //For multivalue check the RDNs Attributes
            if (rdn.isMultiValued()) {
                for (AttributeTypeAndValue att: rdn.getTypesAndValues()) {
                    if (RFC4519Style.o.equals(att.getType())) {
                        organization = att.getValue().toString();
                    }
                }
            } else {
                organization = rdn.getFirst().getValue().toString();
            }
        }

        return organization;
    }

    private boolean isPEM(final String possiblePEM) {
        return possiblePEM.contains(PEM_HEADER) || possiblePEM.contains(PEM_ATTRIBUTE_HEADER);
    }

    /**
     * Checks if another certificate is the issuer for this certificate.
     *
     * @param issuer the other certificate to check (must be an X509Certificate,
     * not an X509AttributeCertificateHolder)
     * @return whether or not the other certificate is the issuer for this certificate
     * @throws IOException if there is an issue deserializing either certificate
     */
    public boolean isIssuer(final Certificate issuer) throws IOException {
        CertificateType cType = issuer.getCertificateType();
        if (cType != CertificateType.X509_CERTIFICATE) {
            throw new IllegalArgumentException("issuer cert must be X509Certificate");
        }

        boolean isIssuer = false;
        X509Certificate issuerX509 = issuer.getX509Certificate();

        //Validate if it's the issuer
        switch (getCertificateType()) {
            case X509_CERTIFICATE:
                X509Certificate certX509 = getX509Certificate();
                try {
                    certX509.verify(issuerX509.getPublicKey());
                    isIssuer = true;
                } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException
                        | NoSuchProviderException | SignatureException e) {
                    LOGGER.error(e);
                }
                break;

            case ATTRIBUTE_CERTIFICATE:
                AttributeCertificate attCert = getAttributeCertificate();
                String algorith = "SHA256withRSA";
                try {
                    Signature sig = Signature.getInstance(algorith);
                    sig.initVerify(issuerX509.getPublicKey());
                    sig.update(attCert.getAcinfo().getEncoded());
                    isIssuer = sig.verify(attCert.getSignatureValue().getBytes());
                } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                    LOGGER.error(e);
                }
                break;
            default:
                break;
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
     * @return this certificate's serial number
     */
    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    /**
     * @return this certificate's holder serial number
     */
    public BigInteger getHolderSerialNumber() {
        return holderSerialNumber;
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
     * @return this certificate's associated issuer organization
     */
    public String getIssuerOrganization() {
        return issuerOrganization;
    }

    /**
     * @return this certificate's associated subject organization
     */
    public String getSubjectOrganization() {
        return subjectOrganization;
    }

    /**
     * Gets the raw bytes for the certificate.
     *
     * @return copy of the certificate bytes
     */
    @JsonIgnore
    public byte[] getRawBytes() {
        if (null != this.certificateBytes) {
            return this.certificateBytes.clone();
        }
        return null;
    }

    @Override
    public String toString() {
        return "Certificate{"
                + ", serialNumber=" + serialNumber
                + ", issuer='" + issuer + '\''
                + '}';
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
     * Retrieve an X509 certificate's public key modulus.
     *
     * @param certificate the certificate holding a public key
     * @return a BigInteger representing its public key's modulus
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
        return getPublicKeyModulus(
                certificateHolder.getSubjectPublicKeyInfo().parsePublicKey().toASN1Primitive()
        );

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
