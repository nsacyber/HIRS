package hirs.utils.crypto;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.encoders.Base64;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import hirs.utils.signature.cose.CoseAlgorithm;
import lombok.Getter;
import lombok.Setter;

/**
 * Class that provides a default implementation of the {@link CryptoEngine}
 * interface, offering support for cryptographic operations such as key loading,
 * signature generation, and signature verification. Can handle cryptographic
 * algorithms aligned with COSE standards and translate them into Java-supported
 * cryptographic algorithms for interoperability.
 */
public class DefaultCrypto implements CryptoEngine {
    private static final String X509 = "X.509";
    private static final String PKCS1_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PKCS1_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";
    private static final String CERTIFICATE_HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String CERTIFICATE_FOOTER = "-----END CERTIFICATE-----";
    private static final String ECC_KEY_HEADER = "-----BEGIN EC PRIVATE KEY-----";
    private static final String ECC_KEY_FOOTER = "-----END EC PRIVATE KEY-----";
    private static final String ECC_PARAM_HEADER = "-----BEGIN EC PARAMETERS-----";
    private static final String ECC_PARAM_FOOTER = "-----END EC PARAMETERS-----";
    private static final String[] JSON_WEB_KEY_HEADER = {"keys", ":", "{", "}"};

    private PrivateKey privateKey = null;
    private KeyPair keyPair = null;
    @Setter @Getter
    private PublicKey publicKey = null;
    @Setter @Getter
    private String algorithm = "";
    @Setter @Getter
    private String kid = "";

    /**
     * Default Crypto Constructor. Used as a default mechanism for providing
     * cryptographic operations.
     */
    public DefaultCrypto() {

    }

    /**
     * This method extracts the private key from a  file.
     * Supports PEM and JSon Web Keys.
     * Json Web Keys are used to validate external test patterns.
     * Json Web Keys are expected to have parameters for KID and Algorithm.
     * Will load in a public key from a JSON Web key if present.
     * Both PKCS1 and PSS formats are handled for RSA.
     * ECDSA keys are supported.
     * Algorithm argument is present to allow handling of multiple encryption algorithms,
     * but for now it is always RSA.
     * @param pathTokey full file path to the private key to be loaded
     * @param cert a properly formatted x509 certificate
     * @param sigHashAlg a string representing the signature hash algorithm to use if the certificate is null
     * @return true if the private key was successfully loaded
     */
    @Override
    public boolean loadPrivateKey(final String pathTokey, final X509Certificate cert,
                                  final String sigHashAlg) throws Exception {
        String alg = "";
        String signAlg = "";
        PrivateKeyInfo pkInfo = null;
        if (cert != null) {
            alg = getAlgorithmFromCert(cert);
            algorithm = alg;
            signAlg = coseAlgToJavaAlg(alg);
        } else {
            if (!sigHashAlg.isEmpty()) {
                algorithm = sigHashAlg;
                signAlg = coseAlgToJavaAlg(sigHashAlg);
            }
        }
        try {
            final File file = new File(pathTokey);
            final byte[] keyBytes = Files.readAllBytes(file.toPath());
            String privateKeyStr = new String(keyBytes, StandardCharsets.UTF_8);
            if (privateKeyStr.contains(PKCS1_HEADER)) {
                try (PEMParser pemParser = new PEMParser(
                        new InputStreamReader(new FileInputStream(pathTokey), StandardCharsets.UTF_8))) {
                    final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                    final KeyPair parsedKeyPair = converter.getKeyPair((PEMKeyPair) pemParser.readObject());
                    privateKey = parsedKeyPair.getPrivate();
                }
            } else if (privateKeyStr.contains(PKCS8_HEADER)) {
                privateKeyStr = privateKeyStr.replace(PKCS8_HEADER, "");
                privateKeyStr = privateKeyStr.replace(PKCS8_FOOTER, "");
                byte[] decodedKey = Base64.decode(privateKeyStr);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
                KeyFactory keyFactory = KeyFactory.getInstance(signAlg);
                privateKey = keyFactory.generatePrivate(spec);
            } else if (privateKeyStr.contains(ECC_KEY_HEADER)) {
                try (
                        ByteArrayInputStream kbs = new ByteArrayInputStream(keyBytes);
                        InputStreamReader isr = new InputStreamReader(kbs, StandardCharsets.UTF_8);
                        PEMParser p = new PEMParser(isr)
                ) {
                    Object readObject1 = p.readObject();   // Can contain key or parameters
                    Object readObject2 = p.readObject();   // null if no parameters are in private key file
                    if (readObject2 == null) {
                        pkInfo = ((PEMKeyPair) readObject1).getPrivateKeyInfo();
                    } else {
                        pkInfo = ((PEMKeyPair) readObject2).getPrivateKeyInfo();
                    }
                    privateKey = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                            .getPrivateKey(pkInfo);
                }
            } else if (containsAll(privateKeyStr, JSON_WEB_KEY_HEADER)) {  // process Json Web Key
                JWKSet jwkSet = JWKSet.load(new File(pathTokey));
                JWK jwKey = jwkSet.getKeys().get(0);
                com.nimbusds.jose.Algorithm  joseAlg = jwKey.getAlgorithm();
                if (AlgorithmsIds.isEcc(AlgorithmsIds.SPEC_COSE_ALG, joseAlg.getName())) {
                    privateKey = jwKey.toECKey().toPrivateKey();
                    keyPair = jwKey.toECKey().toKeyPair();
                    publicKey = jwKey.toECKey().toPublicKey();
                    algorithm = joseAlg.getName();
                    kid = jwKey.getKeyID();
                } else if (AlgorithmsIds.isRsa(AlgorithmsIds.SPEC_COSE_ALG, joseAlg.getName())) {
                    privateKey = jwKey.toRSAKey().toPrivateKey();
                    keyPair = jwKey.toRSAKey().toKeyPair();
                    publicKey = jwKey.toRSAKey().toPublicKey();
                    algorithm = joseAlg.getName();
                    kid = jwKey.getKeyID();
                } else {
                    throw new RuntimeException("Unknown JSON Web Key algorithm: + joseAlg");
                }
            } else {
                throw new RuntimeException("Private Key Type not supported");
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Default Crypto: Unable to locate private key file: " + pathTokey);

        } catch (IOException e) {
            throw new RuntimeException("Default Crypto Error: " + e.getMessage());
        }
        return true;
    }


    /**
     * Helper method to lookup a COSE algoritm used by this class from an x509
     * certificate specified algorithm.
     *
     * @param cert a properly formatted x509 certificate
     * @return A IANA published COSE algorithm description
     * @throws NoSuchAlgorithmException if the algorithm is not found in the
     * x509 certificate
     */
    private String getAlgorithmFromCert(final X509Certificate cert) throws NoSuchAlgorithmException {
        final String x509Alg = cert.getSigAlgName();
        final String alg = AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_SIG,
                AlgorithmsIds.SPEC_X509_ALG, x509Alg, AlgorithmsIds.SPEC_COSE_ALG);
        return alg;
    }

    /**
     * Convert COSE defined algorithm Name to a Java.Security equivalent
     * algorithm string.
     *
     * @param coseAlg
     * @return Java.Security equivalent algorithm string
     * @throws RuntimeException
     */
    private String coseAlgToJavaAlg(final String coseAlg) throws RuntimeException {
        String signAlg = "";
        switch (coseAlg) {
            case CoseAlgorithm.RSA_SHA512_PKCS1:
                signAlg = "RSA";
                break;
            case CoseAlgorithm.RSA_SHA384_PKCS1:
                signAlg = "RSA";
                break;
            case CoseAlgorithm.RSA_SHA256_PKCS1:
                signAlg = "RSA";
                break;
            case CoseAlgorithm.RSA_SHA256_PSS:
                signAlg = "RSA";
                break;
            case CoseAlgorithm.RSA_SHA384_PSS:
                signAlg = "RSA";
                break;
            case CoseAlgorithm.RSA_SHA512_PSS:
                signAlg = "RSA";
                break;
            case CoseAlgorithm.ECDSA_SHA256:
                signAlg = "EC";
                break;
            case CoseAlgorithm.ECDSA_SHA384:
                signAlg = "EC";
                break;
            case CoseAlgorithm.ECDSA_SHA512:
                signAlg = "EC";
                break;
            default:
                throw new RuntimeException("algorithm combination not supported: " + signAlg);
        }
        return signAlg;
    }

    /**
     * This method reads a PKCS1 keypair from a PEM file.
     *
     * @param filename file holding the private key
     * @return a Java Key Pair based upon the file
     */
    private KeyPair getPKCS1KeyPair(final String filename) throws IOException {
        Security.addProvider(new BouncyCastleProvider());
        try (
                FileInputStream fis = new FileInputStream(filename);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                PEMParser pemParser = new PEMParser(isr)
        ) {
            final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            keyPair = converter.getKeyPair((PEMKeyPair) pemParser.readObject());
            return keyPair;
        }
    }

    /**
     * Checks whether the given text contains all specified substrings.
     *
     * @param text the string to search within
     * @param substrings an array of substrings to check for presence in the
     * text
     * @return true if all substrings are found in the text; false otherwise
     */
    public static boolean containsAll(final String text, final String[] substrings) {
        for (final String substring : substrings) {
            if (!text.contains(substring)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Signs given data using the algorithm set in the constructor.
     *
     * @param dataToSign data to be hashed and signed.
     * @return raw signature as a byte[]
     */
    @Override
    public byte[] sign(final byte[] dataToSign) throws Exception {
        Signature signature = null;
        signature = loadSignatureInstance(algorithm);
        signature.initSign(privateKey);
        signature.update(dataToSign);
        final byte[] derEncodedSignature = signature.sign();
        return removeDerFromSignature(derEncodedSignature);
    }

    /**
     * Verifies a signature on a COSE object using a public key vs an X.509
     * certificate. Note that the signature is expected to be in raw (IEEE).
     *
     * @param cert a properly formatted x509 certificate
     * @param pk Public Key used to verify the signed data
     * @param sigAlgorithm optional signature algorithm name; may be empty
     * @param data message data to be verified (toBeSigned data from original
     * msg)
     * @param signatureData The signature data from the signed message
     * @return true if the signature is valid; false otherwise
     */
    @Override
    public boolean verify(final X509Certificate cert, final PublicKey pk, final String sigAlgorithm,
                          final byte[] data, final byte[] signatureData) throws Exception {
        Signature signature = null;
        String alg = algorithm;

        if (!sigAlgorithm.isEmpty()) {
            alg = sigAlgorithm;
        }
        if (alg.isEmpty()) {
            if (cert != null) {
                alg = cert.getSigAlgName();
                // AlgorithmsIds algIds = new AlgorithmsIds();
                alg = AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_SIG, AlgorithmsIds.SPEC_X509_ALG,
                        alg, AlgorithmsIds.SPEC_COSE_ALG);
            } else if (pk != null) {
                final String x509Alg = pk.getAlgorithm();
                // AlgorithmsIds algIds = new AlgorithmsIds();
                alg = AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_SIG, AlgorithmsIds.SPEC_X509_ALG,
                        x509Alg, AlgorithmsIds.SPEC_COSE_ALG);
            }
            if (alg.isEmpty()) {
                alg = sigAlgorithm;
            }
        }
        algorithm = alg;
        // signature = loadSignatureInstance(alg);
        signature = loadSignatureInstance(alg);
        final byte[] derEncodedSignature = derEncodeRawSignature(signatureData);
        signature.initVerify(pk);
        signature.update(data);
        return signature.verify(derEncodedSignature);
    }

    /**
     * Helper method to load Java signature instances. Supports RSA PKCS1, RSA
     * PSS, and ECDSA signatures.
     *
     * @param alg the algorithm name in COSE-compatible format
     * @return a configured {@link Signature} instance
     */
    @SuppressWarnings("MagicNumber")
    private Signature loadSignatureInstance(final String alg)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Signature signature = null;
        if (CoseAlgorithm.isRsaPssName(alg)) {
            PSSParameterSpec pssParams = null;
            if (alg.compareToIgnoreCase(CoseAlgorithm.RSA_SHA256_PSS) == 0) {
                pssParams = new PSSParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), 32,
                        1);
            } else if (alg.compareToIgnoreCase(CoseAlgorithm.RSA_SHA384_PSS) == 0) {
                pssParams = new PSSParameterSpec("SHA-384", "MGF1", new MGF1ParameterSpec("SHA-384"), 48,
                        1);
            } else if (alg.compareToIgnoreCase(CoseAlgorithm.RSA_SHA512_PSS) == 0) {
                pssParams = new PSSParameterSpec("SHA-512", "MGF1", new MGF1ParameterSpec("SHA-512"), 64,
                        1);
            }
            signature = Signature.getInstance("RSASSA-PSS");
            signature.setParameter(pssParams);
        } else {
            switch (alg) {
                case CoseAlgorithm.RSA_SHA512_PKCS1:
                    signature = Signature.getInstance("SHA512withRSA");
                    break;
                case CoseAlgorithm.RSA_SHA384_PKCS1:
                    signature = Signature.getInstance("SHA384withRSA");
                    break;
                case CoseAlgorithm.RSA_SHA256_PKCS1:
                    signature = Signature.getInstance("SHA256withRSA");
                    break;
                case CoseAlgorithm.ECDSA_SHA256:
                    signature = Signature.getInstance("SHA256withECDSA");
                    break;
                case CoseAlgorithm.ECDSA_SHA384:
                    signature = Signature.getInstance("SHA384withECDSA");
                    break;
                case CoseAlgorithm.ECDSA_SHA512:
                    signature = Signature.getInstance("SHA512withECDSA");
                    break;
                default:
                    throw new RuntimeException("Algorithm combination not supported: " + algorithm);
            }
        }
        return signature;
    }

    /**
     * Converts the signature block (from COSE) to a DER encoded format used by
     * Java.security.
     *
     * @param signature a COSE-compatible signature block
     * @return the given signature block in DER encoded format
     */
    @SuppressWarnings("MagicNumber")
    private byte[] derEncodeRawSignature(final byte[] signature) throws IOException {
        int n = 32;
        final int signum = 1;
        // System.out.println("Signature Size is "+ signature.length);
        if (CoseAlgorithm.isEcdsaName(algorithm)) {
            if (signature.length >= 94 && signature.length <= 98) {
                n = 48;
            } else if (signature.length >= 130 && signature.length <= 134) {
                n = 66;
            }

            final BigInteger r = new BigInteger(signum, Arrays.copyOfRange(signature, 0, n));
            final BigInteger s = new BigInteger(signum, Arrays.copyOfRange(signature, n, n * 2));

            final DERSequence sequence = new DERSequence(
                    new ASN1Encodable[] {new ASN1Integer(r), new ASN1Integer(s)});
            return sequence.getEncoded();
        } else if (CoseAlgorithm.isRsaName(algorithm)) {
            return signature;
        }
        throw new RuntimeException(
                "Unknown Algorithm provided for COSE signature processing. COSE Algorithm ID = "
                        + algorithm);
    }

    /**
     * Removes the Der Encoding from the signature data to produce a "Raw"
     * Signature. Result should be conformant with IEEE P1363.
     *
     * @param signatureBytes the DER-encoded signature bytes to process
     * @return raw signature as a concatenation of r and s, or the input bytes
     * if RSA
     */
    private byte[] removeDerFromSignature(final byte[] signatureBytes) throws IOException {
        byte[] rawSignature = null;
        int rOffset = 0;
        int sOffset = 0;
        // Strip off DER encoding left by Java.Security
        if (CoseAlgorithm.isEcdsaName(algorithm)) {
            try (ASN1InputStream asn1InputStream = new ASN1InputStream(signatureBytes)) {
                final ASN1Sequence sequence = (ASN1Sequence) asn1InputStream.readObject();
                final BigInteger r = ((ASN1Integer) sequence.getObjectAt(0)).getValue();
                final BigInteger s = ((ASN1Integer) sequence.getObjectAt(1)).getValue();
                byte[] rbytes = r.toByteArray();
                byte[] sbytes = s.toByteArray();
                int rLength = r.toByteArray().length;
                int sLength = s.toByteArray().length;
                // Copy individual components into the final array but account
                // for missing leading 0's
                if (((rLength % 2) != 0) && (rbytes[0] == 0)) {
                    rbytes = removeLeadingByte(rbytes);
                    rLength = rbytes.length;
                } else if ((rLength % 2) != 0) {
                    rOffset = 1; // make adjustments for odd length and non zero
                    // first byte
                }

                if (((sLength % 2) != 0) && (sbytes[0] == 0)) {
                    sbytes = removeLeadingByte(sbytes);
                    sLength = sbytes.length;
                } else if ((sLength % 2) != 0) {
                    sOffset = 1; // make adjustments for odd length and non zero
                    // first byte
                }

                rawSignature = new byte[rLength + rOffset + sLength + sOffset];
                // If there was a leading zero then set the specific byte to 0
                // in the output
                if (rOffset == 1) {
                    rawSignature[0] = 0;
                }
                if (sOffset == 1) {
                    rawSignature[rLength + rOffset] = 0;
                }

                System.arraycopy(rbytes, 0, rawSignature, rOffset, rLength);
                System.arraycopy(sbytes, 0, rawSignature, rLength + rOffset + sOffset, sLength);
            }
        } else if (CoseAlgorithm.isRsaName(algorithm)) {
            return signatureBytes;
        } else {
            throw new RuntimeException(
                    " Unknown or unsupported algorithm specified when processing signature by the "
                            + "default cryptographic device");
        }
        return rawSignature;
    }

    /**
     * Removes a leading byte from an array and resizes the array.
     *
     * @param byteArray the original byte array
     * @return new array without leading byte
     */
    private byte[] removeLeadingByte(final byte[] byteArray) {
        if (byteArray == null || byteArray.length == 0) {
            return byteArray;
        }
        return Arrays.copyOfRange(byteArray, 1, byteArray.length);
    }
}
