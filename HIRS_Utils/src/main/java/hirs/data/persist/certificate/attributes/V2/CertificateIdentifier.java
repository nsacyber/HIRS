package hirs.data.persist.certificate.attributes.V2;

import hirs.data.persist.DeviceInfoReport;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
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

    private static final int SEQUENCE_NUMBER = 2;
    private static final int ATTRIBUTE_ID_INDEX = 0;
    private static final int GENERIC_ID_INDEX = 1;

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

        ASN1TaggedObject taggedSequence;
        for (int i = 0; i < sequence.size(); i++) {
            taggedSequence = ASN1TaggedObject.getInstance(sequence.getObjectAt(i));

            switch (taggedSequence.getTagNo()) {
                case ATTRIBUTE_ID_INDEX:
                    // attributecertificateidentifier
                    parseAttributeCertId(ASN1Sequence.getInstance(taggedSequence));
                    break;
                case GENERIC_ID_INDEX:
                    // issuerserial
                    parseGenericCertId(ASN1Sequence.getInstance(taggedSequence));
                    break;
                default:
                    break;
            }
        }
    }

    private void parseAttributeCertId(final ASN1Sequence attrCertSeq) {
        //Check if it have a valid number of identifers
        if (attrCertSeq.size() != SEQUENCE_NUMBER) {
            throw new IllegalArgumentException("CertificateIdentifer"
                    + ".AttributeCertificateIdentifer does not have required values.");
        }

        hashAlgorithm = attrCertSeq.getObjectAt(0).toString();
        hashSigValue = DERUTF8String.getInstance(attrCertSeq.getObjectAt(1));
    }

    private void parseGenericCertId(final ASN1Sequence issuerSerialSeq) {
        //Check if it have a valid number of identifers
        if (issuerSerialSeq.size() != SEQUENCE_NUMBER) {
            throw new IllegalArgumentException("CertificateIdentifier"
                    + ".GenericCertificateIdentifer does not have required values.");
        }

        issuerDN = GeneralName.getInstance(issuerSerialSeq.getObjectAt(0));
        certificateSerialNumber = ASN1Integer.getInstance(issuerSerialSeq
                .getObjectAt(1)).getValue();
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
