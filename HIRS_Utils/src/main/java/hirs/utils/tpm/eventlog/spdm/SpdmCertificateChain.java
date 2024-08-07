package hirs.utils.tpm.eventlog.spdm;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiX509Cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

/**
 * Class to process the SpdmCertificateChain.
 * <p>
 * Certificate chain format, defined by SPDM v1.03, Sect 10.6.1, Table 33:
 * Certificate chain format {
 *      Length                          2 bytes;
 *      Reserved                        2 bytes;
 *      RootHash                        <H> bytes;
 *      Certificates                    <Length> - (4 + <H>) bytes;
 * }
 * <p>
 * Length: total length of cert chain including all fields in this block
 * H: the output size of the hash algorithm selected by the most recent ALGORITHMS response
 *    this field shall be in hash byte order
 *    hash algorithm is included in the DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_CERT_CHAIN
 *    structure as the member "SpdmBaseHashAlg"
 * RootHash: the digest of the Root Certificate.
 *    size is determined by hash algorithm selected by the most recent SPDM ALGORITHMS response;
 *    the hash algorithm is the DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_CERT_CHAIN SpdmBaseHashAlgo
 * Certificates: Complete cert chain consisting of 1 or more ASN.1 DER-encoded X.509 v3 certs
 *    this field shall be in Encoded ASN.1 byte order
 */
public class SpdmCertificateChain {

    /**
     * Length of the certificate chain to include all fields in this structure.
     */
    //private int length = 0;
    /**
     * Root hash.
     */
    private byte[] rootHash = null;
    /**
     * Number of certs in the SPDM cert chain.
     */
    private int numberOfCerts = 0;
    /**
     * Array List of certs found in the chain.
     */
    private ArrayList<UefiX509Cert> certList = new ArrayList<UefiX509Cert>();
    /**
     * Human-readable description of any error associated with SPDM base hash alg.
     */
    String spdmBaseHashAlgoError = "";
    /**
     * Human-readable description of any error associated with parsing the X509 certs.
     */
    String certProcessingError = "";

    /**
     * SpdmCertificateChain Constructor.
     *
     * @param spdmCertChainBytes byte array holding the SPDM Cert Chain bytes.
     * @param rootHashLength length of RootHash.
     */
    public SpdmCertificateChain(final byte[] spdmCertChainBytes, final int rootHashLength) {

        if(rootHashLength <= 0) {
            spdmBaseHashAlgoError = "SPDM base hash algorithm size is not >0";
        }
        else {
            byte[] lengthBytes = new byte[2];
            System.arraycopy(spdmCertChainBytes, 0, lengthBytes, 0, 2);
            //length = HexUtils.leReverseInt(lengthBytes);

            // Reserved: 2 bytes

            rootHash = new byte[rootHashLength];
            System.arraycopy(spdmCertChainBytes, 4, rootHash, 0, rootHashLength);

            int certChainStartPos = 4 + rootHashLength;
            int certChainLength = spdmCertChainBytes.length - certChainStartPos;
            byte[] certChainBytes = new byte[certChainLength];
            System.arraycopy(spdmCertChainBytes, certChainStartPos, certChainBytes, 0, certChainLength);

            processCertChain(certChainBytes);
        }
    }

    /**
     * Method for processing the data in an EFI SignatureList (ex. can be one or more X509 certs)
     *
     * @param certChainData Byte array holding the cert chain data
     */
        private void processCertChain(final byte[] certChainData) {

        UefiX509Cert cert = null;

        ByteArrayInputStream certChainDataIS = new ByteArrayInputStream(certChainData);
        while (certChainDataIS.available() > 0) {

            // java.io.IOException                     If there's a problem parsing the cert chain data.
            // java.security.cert.CertificateException if there's a problem parsing the X509 certificate.
            // java.security.NoSuchAlgorithmException  if there's a problem hashing the certificate.
            try {
                byte[] certType = new byte[2];
                certChainDataIS.read(certType);
                byte[] certLength = new byte[2];
                certChainDataIS.read(certLength);
                //int cLength = new BigInteger(certLength).intValue() + UefiConstants.SIZE_4;
                int cLength = new BigInteger(certLength).intValue();
                byte[] certData = new byte[cLength];
                certChainDataIS.read(certData);
                // put the cert back together
                byte[] certBlob = new byte[cLength + 4];
                System.arraycopy(certType, 0, certBlob, 0, 2);
                System.arraycopy(certLength, 0, certBlob, 2, 2);
                System.arraycopy(certData, 0, certBlob, 4, cLength);
                cert = new UefiX509Cert(certBlob);
                //cert = new X509Certificate(certBlob);
                certList.add(cert);
                numberOfCerts++;
            } catch (IOException e) {
                certProcessingError += "Error with Cert # " + (numberOfCerts+1)
                        + ": IOException (error reading cert data)";
                break;
            } catch (CertificateException e) {
                certProcessingError += "Error with Cert # " + (numberOfCerts+1)
                        + ": CertificateException";
                break;
            } catch (NoSuchAlgorithmException e) {
                certProcessingError += "Error with Cert # " + numberOfCerts+1
                        + ": CNoSuchAlgorithmException";
                break;
            }
        }
    }

    /**
     * Returns a human-readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {

        String spdmCertChainInfo = "";

        if(!spdmBaseHashAlgoError.isEmpty()) {
            spdmCertChainInfo += "   *** ERROR with SPDM base hash algorithm size ***\n";
            spdmCertChainInfo += "      " + spdmBaseHashAlgoError + "\n";
            spdmCertChainInfo += "      Stopping processing of this cert chain\n";
        }
        else {
            spdmCertChainInfo += "   Root hash = " + HexUtils.byteArrayToHexString(rootHash) + "\n";
            spdmCertChainInfo += "   Number of certs in chain = " + numberOfCerts + "\n";

            int certCnt = 1;
            for (UefiX509Cert cert : certList) {
                spdmCertChainInfo += "   Cert # " + certCnt++ + " of " +
                        numberOfCerts + ": ------------------\n";
                spdmCertChainInfo += cert.toString();
            }

            if (!certProcessingError.isEmpty()) {
                spdmCertChainInfo += "   *** ERROR processing cert ***\n";
                spdmCertChainInfo += "      " + certProcessingError + "\n";
                spdmCertChainInfo += "      Stopping processing of this cert chain\n";
            }
        }
        return spdmCertChainInfo;
    }
}
