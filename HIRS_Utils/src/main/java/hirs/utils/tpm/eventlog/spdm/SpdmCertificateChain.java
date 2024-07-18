package hirs.utils.tpm.eventlog.spdm;

import hirs.utils.HexUtils;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Class to process the SpdmCertificateChain.
 * <p>
 * Certificate chain format, defined by SPDM v1.03, Sect 10.6.1, Table 33:
 * Certificate chain format {
 *      Length                          1 byte;
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
 * Certificates: Complete cert chain consisting of 1 or more ASN.1 DER-encoded X.509 v3 certs
 *    this field shall be in Encoded ASN.1 byte order
 */
public class SpdmCertificateChain {

    /**
     * Length of the certificate chain to include all fields in this structure.
     */
    @Getter
    private int length = 0;
    /**
     * Root hash.
     */
    private byte[] rootHash = null;
    /**
     * Certificates.
     */
//    private SpdmMeasurement spdmMeasurement;
    /**
     * Error reading SPDM Cert Chain.
     */
    private boolean spdmCertificateChainReadError = false;

    /**
     * SpdmMeasurementBlock Constructor.
     *
     * @param spdmMeasBlocks byte array holding the SPDM Measurement Block bytes.
     */
    public SpdmCertificateChain(final ByteArrayInputStream spdmMeasBlocks) {

        try {
            byte[] indexBytes = new byte[1];
            spdmMeasBlocks.read(indexBytes);
            index = HexUtils.leReverseInt(indexBytes);

            byte[] measurementSpecBytes = new byte[1];
            spdmMeasBlocks.read(measurementSpecBytes);
            measurementSpec = HexUtils.leReverseInt(measurementSpecBytes);

            // in future, can crosscheck this measurement size with the MeasurementSpec hash alg size
            byte[] measurementSizeBytes = new byte[2];
            spdmMeasBlocks.read(measurementSizeBytes);
            int measurementSize = HexUtils.leReverseInt(measurementSizeBytes);

            byte[] measurementBytes = new byte[measurementSize];
            spdmMeasBlocks.read(measurementBytes);
            spdmMeasurement = new SpdmMeasurement(measurementBytes);
        } catch (IOException ioEx) {
            spdmMeasurementBlockReadError = true;
        }
    }

    /**
     * Returns a human-readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {

//        String spdmMeasBlockInfo = "";
//
//        if(spdmMeasurementBlockReadError) {
//            spdmMeasBlockInfo += "\n      Error reading SPDM Measurement Block";
//        }
//        else {
//            spdmMeasBlockInfo += "\n      Index = " + index;
//            spdmMeasBlockInfo += "\n      MeasurementSpec = " +  measurementSpec;
//            spdmMeasBlockInfo += spdmMeasurement.toString();
//        }
//
//        return spdmMeasBlockInfo;
//    }
}
