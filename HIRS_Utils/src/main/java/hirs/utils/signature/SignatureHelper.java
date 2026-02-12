package hirs.utils.signature;

import hirs.utils.crypto.AlgorithmsIds;
import hirs.utils.signature.cose.CoseAlgorithm;

import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Helper class to support digital signatures.
 */
public final class SignatureHelper {

    /**
     * Default constructor.
     */
    private SignatureHelper() {
    }

    /**
     * Extracts the SKID from an X.509 certificate.
     * The TCG PC Client RIM and TCG Component RIM defines the key identifier as the
     * Subject Key identifier (SKID) of the certificate to be used for verification.
     * SKID is usually  a hash of the public key.
     *
     * @param signCert x.509 certificate
     * @return byte array holding the certificates SKID
     */
    public static byte[] getKidFromCert(final X509Certificate signCert) {
        return signCert.getExtensionValue("2.5.29.14");
    }

    /**
     * Extracts the COSE defined algorithm identifier associated with a certificates signing algorithm.
     *
     * @param signCert X.509 certificate to extract the algorithm identifier from
     * @return a COSE defined algorithm identifier
     * @throws NoSuchAlgorithmException if the specified algorithm is not available
     */
    public static int getCoseAlgFromCert(final X509Certificate signCert)
            throws NoSuchAlgorithmException {
        String alg = AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_SIG, AlgorithmsIds.SPEC_X509_ALG,
                signCert.getSigAlgName(), AlgorithmsIds.SPEC_COSE_ALG);
        int algId = CoseAlgorithm.getAlgId(alg);
        if (algId == 0) {
            throw new RuntimeException(
                    "Algorithm ID from the certificate did not map to a COSE registered algorithm");
        }
        return algId;
    }
}
