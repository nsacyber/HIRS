package hirs.data.persist.certificate.attributes.V2;

import hirs.data.persist.DeviceInfoReport;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.GeneralName;

/**
 * Basic class that handles a the attribute associate with a Certificate
 * Identifier for the component.
 * <pre>
 * CertificateIdentifier::= SEQUENCE {
 *       attributeCertIdentifier [0] IMPLICIT AttributeCertificateIdentifier OPTIONAL
 *       genericCertIdentifier   [1] IMPLICIT IssuerSerial OPTIONAL }
 *
 * AttributeCertificateIdentifier ::= SEQUENCE {
 *       hashAlgorithm  AlgorithmIdentifier,
 *       hashOverSignatureValue OCTET STRING }
 *
 * IssuerSerial ::= SEQUENCE {
 *       issuer        GeneralNames,
 *       serial        CertificateSerialNumber }
 * </pre>
 */
public class CertificateIdentifier {

    private static final int IDENTIFIER_NUMBER = 2;

    private String hashAlgorithm;
    private DERUTF8String hashSigValue;
    private GeneralName issuerDN;
    private BigInteger certificateSerialNumber;

    /**
     * Default constructor.
     */
    public CertificateIdentifier() {
        hashAlgorithm = DeviceInfoReport.NOT_SPECIFIED;
        hashSigValue = new DERUTF8String(DeviceInfoReport.NOT_SPECIFIED);
        issuerDN = null;
        certificateSerialNumber = BigInteger.ZERO;
    }

    /**
     * Primary constructor for the parsing of the sequence.
     * @param sequence containing the name and value of the Certificate Identifier
     */
    public CertificateIdentifier(final ASN1Sequence sequence) {
        this();
        //Check if it have a valid number of identifers
        if (sequence.size() < IDENTIFIER_NUMBER) {
            throw new IllegalArgumentException("Component identifier do not have required values.");
        }

        // attributecertificateidentifier
        ASN1Sequence attrCertSeq = ASN1Sequence.getInstance(sequence.getObjectAt(0));
        hashAlgorithm = attrCertSeq.getObjectAt(0).toString();
        hashSigValue = DERUTF8String.getInstance(attrCertSeq.getObjectAt(1));

        // issuerserial
        ASN1Sequence issuerSerialSeq = ASN1Sequence.getInstance(sequence.getObjectAt(1));
        issuerDN = GeneralName.getInstance(issuerSerialSeq.getObjectAt(0));
    }

    /**
     * @return the algorithm type
     */
    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    /**
     * @return the string representation of hash signature
     */
    public DERUTF8String getHashSigValue() {
        return hashSigValue;
    }

    /**
     * @return the distinguished name for the issuer serial
     */
    public GeneralName getIssuerDN() {
        return issuerDN;
    }

    /**
     * @return The serial number of the certificate.
     */
    public BigInteger getCertificateSerialNumber() {
        return certificateSerialNumber;
    }

    /**
     * String for the internal data stored.
     * @return String representation of the data.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("CertificateIdentifier{");
        sb.append("hashAlgorithm=").append(hashAlgorithm);
        sb.append(", hashSigValue").append(hashSigValue.toString());
        sb.append(", issuerDN=");
        if (issuerDN != null) {
            sb.append(issuerDN.toString());
        }
        sb.append(", certificateSerialNumber=");
        if (certificateSerialNumber != null) {
            sb.append(certificateSerialNumber.toString());
        }

        sb.append("}");
        return sb.toString();
    }
}
