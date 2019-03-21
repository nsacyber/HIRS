
package hirs.data.persist.certificate.attributes;

import hirs.data.persist.DeviceInfoReport;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.x509.GeneralName;

/**
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
 */
public class CertificateIdentifier {
    
    private static final int IDENTIFIER_NUMBER = 2;

    private String hashAlgorithm;
    private String hashSigValue;
    private GeneralName issuerDN;
    private BigInteger certificateSerialNumber;

    public CertificateIdentifier() {
        hashAlgorithm = DeviceInfoReport.NOT_SPECIFIED;
        hashSigValue = DeviceInfoReport.NOT_SPECIFIED;
        issuerDN = null;
        certificateSerialNumber = BigInteger.ZERO;
    }

    public CertificateIdentifier(final ASN1Sequence sequence) {
        this();
        //Check if it have a valid number of identifers
        if (sequence.size() < IDENTIFIER_NUMBER) {
            throw new IllegalArgumentException("Component identifier do not have required values.");
        }

        ASN1Encodable attributeId = sequence.getObjectAt(0);
        ASN1Encodable genCertId = sequence.getObjectAt(1);
        ASN1TaggedObject attribute = ASN1TaggedObject.getInstance(sequence.getObjectAt(0));
        ASN1TaggedObject genericCert = ASN1TaggedObject.getInstance(sequence.getObjectAt(1));
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public String getHashSigValue() {
        return hashSigValue;
    }

    public GeneralName getIssuerDN() {
        return issuerDN;
    }

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

        sb.append("hashAlgorithm=");
        sb.append(hashAlgorithm);
        sb.append(", hashSigValue");
        sb.append(hashSigValue);
        sb.append(", issuerDN=");
        if (issuerDN != null) {
            sb.append(issuerDN.toString());
        }
        sb.append(", certificateSerialNumber=");
        if (certificateSerialNumber != null) {
            sb.append(certificateSerialNumber.toString());
        }

        return sb.toString();
    }


}
