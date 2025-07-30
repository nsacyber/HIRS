package hirs.utils.crypto;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * Interface to abstract a cryptographic implementation. Currently just supports
 * digital signing.
 */
public interface CryptoEngine {
    /**
     * Loads a Private Key from the specified path.
     *
     * @param pathToKey a URI to the key
     * @param cert X509Certificate certificate associated with the private key
     * @param algorithm name of the IANA registerd COSE algorithm used to sign
     * or verify a signature if not specified by the X509 certificate or public
     * key
     * @return true if the key was successfully loaded
     * @throws Exception if an error occurred when loading or processing the key
     */
    boolean loadPrivateKey(String pathToKey, X509Certificate cert, String algorithm) throws Exception;

    /**
     * Method to digitally sign data.
     *
     * @param dataToSign data the specification is to sign
     * @return byte array holing the signed data
     * @throws Exception if an error with the signing of the data
     */
    byte[] sign(byte[] dataToSign) throws Exception;

    /**
     * Method to verify the signature on signed data using an X.509 Certificate.
     * Either an X.509 or public key must be provided.
     *
     * @param signCert A valid x.509 certificate used to verify the signed data,
     * can be null is publicKey is provided
     * @param publicKey Optional public key used to verify the signed data, can
     * be null is cert is provided
     * @param algorithm optional string holding the COSE defined signature
     * algorithm.
     * @param data Message data to be verified (toBeSigned data from original
     * msg)
     * @param signatureData The signature data from the signed message
     * @return True if data is valid , false if not
     * @throws Exception if an error occurred when processing the with the
     * certificate or signed data
     */
    boolean verify(X509Certificate signCert, PublicKey publicKey, String algorithm, byte[] data,
                   byte[] signatureData) throws Exception;

//    /**
//     * Method to verify the signature on signed data using an X.509 Certificate.
//     * @param publicKey Public Key used to verify the signed data
//     * @param data message data to be verified (toBeSigned data from original msg)
//     * @param signatureData The signature data from the signed message
//     *
//     * @return True if data is valid , false if not
//     * @throws Exception if an error occurred when processing the with the certificate or signed data
//     */
//    public boolean verify(PublicKey publicKey, byte[] data, byte[] signatureData) throws Exception;

    /**
     * Retrieves Public Key from a KeyPair or Certificate. note that
     * loadPrivateKey may load a public key as well.
     *
     * @return Java.Security.PublicKey object.
     */
    PublicKey getPublicKey();
}
