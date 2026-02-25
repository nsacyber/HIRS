package hirs.attestationca.persist.provision.helper;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.enums.PublicKeyAlgorithm;
import hirs.attestationca.persist.exceptions.IdentityProcessingException;
import hirs.attestationca.persist.exceptions.UnexpectedServerException;
import hirs.utils.HexUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

@Log4j2
public final class ProvisionUtils {

    /**
     * The default size for IV blocks.
     */
    public static final int DEFAULT_IV_SIZE = 16;

    /**
     * HMAC Size Length in bytes.
     */
    public static final int HMAC_SIZE_LENGTH_BYTES = 2;

    /**
     * HMAC key Length in bytes.
     */
    public static final int HMAC_KEY_LENGTH_BYTES = 32;

    /**
     * Seed Length in bytes.
     */
    public static final int SEED_LENGTH = 32;

    /**
     * Max secret length.
     */
    public static final int MAX_SECRET_LENGTH = 32;

    /**
     * AES Key Length un bytes.
     */
    public static final int AES_KEY_LENGTH_BYTES = 16;

    /**
     * Defines the well known exponent.
     * https://en.wikipedia.org/wiki/65537_(number)#Applications
     */
    private static final BigInteger EXPONENT = new BigInteger("010001", DEFAULT_IV_SIZE);

    private static final int TPM2_CREDENTIAL_BLOB_SIZE = 392;

    private static final int DEFAULT_RSA_MODULUS_LENGTH_IN_BYTES = 256;

    private static final int DEFAULT_ECC_LENGTH = 0;

    // Constants used to parse out the ak name from the ak public data. Used in generateAkName
    private static final String AK_NAME_PREFIX = "000b";

    private static final String AK_NAME_HASH_PREFIX =
            "0001000b00050072000000100014000b0800000000000100";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * This private constructor was created to silence checkstyle error.
     */
    private ProvisionUtils() {
    }

    /**
     * Helper method to parse a byte array into an {@link ProvisionerTpm2.IdentityClaim}.
     *
     * @param identityClaim byte array that should be converted to a Protobuf IdentityClaim object
     * @return the Protobuf generated Identity Claim object
     */
    public static ProvisionerTpm2.IdentityClaim parseIdentityClaim(final byte[] identityClaim) {
        try {
            return ProvisionerTpm2.IdentityClaim.parseFrom(identityClaim);
        } catch (InvalidProtocolBufferException ipbe) {
            throw new IdentityProcessingException(
                    "Could not deserialize Protobuf Identity Claim object.", ipbe);
        }
    }

    /**
     * Helper method to extract a DER encoded ASN.1 certificate from an X509 certificate.
     *
     * @param certificate the X509 certificate to be converted to DER encoding
     * @return the byte array representing the DER encoded certificate
     */
    public static byte[] getDerEncodedCertificate(final X509Certificate certificate) {
        try {
            return certificate.getEncoded();
        } catch (CertificateEncodingException ceEx) {
            log.error("Error converting certificate to ASN.1 DER Encoding.", ceEx);
            throw new UnexpectedServerException(
                    "Encountered error while converting X509 Certificate to ASN.1 DER Encoding: "
                            + ceEx.getMessage(), ceEx);
        }
    }

    /**
     * Helper method to extract a PEM encoded certificate from an X509 certificate.
     *
     * @param certificate the X509 certificate to be converted to PEM encoding
     * @return the string representing the PEM encoded certificate
     */
    public static String getPemEncodedCertificate(final X509Certificate certificate) {
        try {
            final StringWriter stringWriter = new StringWriter();
            final JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
            pemWriter.writeObject(certificate);
            pemWriter.flush();
            pemWriter.close();
            return stringWriter.toString();
        } catch (IOException ioEx) {
            log.error("Error converting certificate to PEM Encoding.", ioEx);
            throw new UnexpectedServerException(
                    "Encountered error while converting X509 Certificate to PEM Encoding: "
                            + ioEx.getMessage(), ioEx);
        }
    }

    /**
     * Parses a public key from a byte array and returns it as a generic PublicKey.
     * Supports RSA and EC (Elliptic Curve) keys.
     *
     * @param publicKeyAlgorithm public key algorithm
     * @param keyBytes           the DER-encoded public key bytes
     * @return the parsed PublicKey instance
     * @throws GeneralSecurityException if the key cannot be parsed
     */
    public static PublicKey parsePublicKeyFromPublicDataSegment(final PublicKeyAlgorithm publicKeyAlgorithm,
                                                                final byte[] keyBytes) throws GeneralSecurityException {
        return switch (publicKeyAlgorithm) {
            case RSA -> parseRSAKeyFromPublicDataSegment(keyBytes);
            case ECC -> parseECCKeyFromPublicDataSegment(keyBytes);
            default -> throw new GeneralSecurityException("Unsupported or invalid public key");
        };
    }

    /**
     * Parses the RSA public key from public data segment generated by TPM 2.0.
     *
     * @param publicAreaSegment the public area segment to parse
     * @return the RSA public key of the supplied public data
     */
    public static RSAPublicKey parseRSAKeyFromPublicDataSegment(final byte[] publicAreaSegment) {
        final int publicAreaLen = publicAreaSegment.length;

        if (publicAreaLen < DEFAULT_RSA_MODULUS_LENGTH_IN_BYTES) {
            throw new IllegalArgumentException("EK or AK public data segment is not long enough");
        }

        // public data ends with 256 byte modulus
        byte[] modulus =
                HexUtils.subarray(publicAreaSegment, publicAreaLen - DEFAULT_RSA_MODULUS_LENGTH_IN_BYTES,
                        publicAreaLen - 1);
        return (RSAPublicKey) assembleRSAPublicKey(modulus);
    }

    /**
     * Constructs an RSA public key where the modulus is in raw form.
     *
     * @param modulus in byte array form
     * @return RSA public key using specific modulus and the well known exponent
     */
    public static PublicKey assembleRSAPublicKey(final byte[] modulus) {
        return assembleRSAPublicKey(Hex.encodeHexString(modulus));
    }

    /**
     * Constructs an RSA public key where the modulus is Hex encoded.
     *
     * @param modulus hex encoded modulus
     * @return RSA public key using specific modulus and the well known exponent
     */
    public static PublicKey assembleRSAPublicKey(final String modulus) {
        return assembleRSAPublicKey(new BigInteger(modulus, DEFAULT_IV_SIZE));
    }

    /**
     * Assembles an RSA public key using a defined big int modulus and the well known exponent.
     *
     * @param modulus modulus
     * @return RSA public key using the provided integer modulus
     */
    public static PublicKey assembleRSAPublicKey(final BigInteger modulus) {
        // generate an RSA key spec using mod and exp
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, EXPONENT);

        // create the RSA public key
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(PublicKeyAlgorithm.RSA.getAlgorithmName());
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new UnexpectedServerException(
                    "Encountered unexpected error creating RSA public key: " + e.getMessage(), e);
        }
    }

    /**
     * todo
     * Parses the ECC public key from public data segment generated by TPM 2.0.
     *
     * @param publicAreaSegment the public area segment to parse
     * @return the ECC public key of the supplied public data
     */
    public static ECPublicKey parseECCKeyFromPublicDataSegment(final byte[] publicAreaSegment) {
        //final int pubLen = publicArea.length;

        final BigInteger x = new BigInteger("0");
        final BigInteger y = new BigInteger("0");

        return assembleECCPublicKey(new ECPoint(x, y));
    }

    /**
     * todo
     *
     * @param ecPoint
     * @return
     */
    public static ECPublicKey assembleECCPublicKey(final ECPoint ecPoint) {
        BigInteger a = new BigInteger("0");
        BigInteger b = new BigInteger("0");

        EllipticCurve ellipticCurve = new EllipticCurve(null, a, b);
        ECParameterSpec ecParameterSpec = null;

        return (ECPublicKey) assembleECCPublicKey(ecPoint, ecParameterSpec);
    }

    /**
     * todo
     *
     * @param ecpoint
     * @param ecParameterSpec
     * @return
     */
    public static PublicKey assembleECCPublicKey(final ECPoint ecpoint, final ECParameterSpec ecParameterSpec) {
        final ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(ecpoint, ecParameterSpec);

        // create the ECC public key
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(PublicKeyAlgorithm.ECC.getAlgorithmName());
            return keyFactory.generatePublic(ecPublicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new UnexpectedServerException(
                    "Encountered unexpected error creating ECC public key: " + e.getMessage(), e);
        }
    }

    /**
     * Performs the first step of the TPM 2.0 identity claim process. Takes an ek, ak, and secret
     * and then generates a seed that is used to generate AES and HMAC keys. Parses the ak name.
     * Encrypts the seed with the public ek. Uses the AES key to encrypt the secret. Uses the HMAC
     * key to generate an HMAC to cover the encrypted secret and the ak name. The output is an
     * encrypted blob that acts as the first part of a challenge-response authentication mechanism
     * to validate an identity claim.
     * <p>
     * Equivalent to calling tpm2_makecredential using tpm2_tools.
     *
     * @param ek     endorsement key in the identity claim
     * @param ak     attestation key in the identity claim
     * @param secret a nonce
     * @return the encrypted blob forming the identity claim challenge
     */
    public static ByteString tpm20MakeCredential(final RSAPublicKey ek, final RSAPublicKey ak,
                                                 final byte[] secret) {
        // check size of the secret
        if (secret.length > MAX_SECRET_LENGTH) {
            throw new IllegalArgumentException("Secret must be " + MAX_SECRET_LENGTH
                    + " bytes or smaller.");
        }

        // generate a random 32 byte seed
        byte[] seed = generateRandomBytes(SEED_LENGTH);

        try {
            // encrypt seed with pubEk
            Cipher asymCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepSpec = new OAEPParameterSpec("SHA-256", "MGF1",
                    MGF1ParameterSpec.SHA256,
                    new PSource.PSpecified("IDENTITY\0".getBytes(StandardCharsets.UTF_8)));
            asymCipher.init(Cipher.PUBLIC_KEY, ek, oaepSpec);
            asymCipher.update(seed);
            byte[] encSeed = asymCipher.doFinal();

            // generate ak name from akMod
            byte[] akModTemp = ak.getModulus().toByteArray();
            byte[] akMod = new byte[DEFAULT_RSA_MODULUS_LENGTH_IN_BYTES];
            int startpos = 0;
            // BigIntegers are signed, so a modulus that has a first bit of 1
            // will be padded with a zero byte that must be removed
            if (akModTemp[0] == 0x00) {
                startpos = 1;
            }
            System.arraycopy(akModTemp, startpos, akMod, 0, DEFAULT_RSA_MODULUS_LENGTH_IN_BYTES);
            byte[] akName = generateAkName(akMod);

            // generate AES and HMAC keys from seed
            byte[] aesKey = cryptKDFa(seed, "STORAGE", akName, AES_KEY_LENGTH_BYTES);
            byte[] hmacKey = cryptKDFa(seed, "INTEGRITY", null, HMAC_KEY_LENGTH_BYTES);

            // use two bytes to add a size prefix on secret
            ByteBuffer b;
            b = ByteBuffer.allocate(2);
            b.putShort((short) (secret.length));
            byte[] secretLength = b.array();
            byte[] secretBytes = new byte[secret.length + 2];
            System.arraycopy(secretLength, 0, secretBytes, 0, 2);
            System.arraycopy(secret, 0, secretBytes, 2, secret.length);

            // encrypt size prefix + secret with AES key
            Cipher symCipher = Cipher.getInstance("AES/CFB/NoPadding");
            byte[] defaultIv = HexUtils.hexStringToByteArray("00000000000000000000000000000000");
            IvParameterSpec ivSpec = new IvParameterSpec(defaultIv);
            symCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), ivSpec);
            byte[] encSecret = symCipher.doFinal(secretBytes);

            // generate HMAC covering encrypted secret and ak name
            Mac integrityHmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec integrityKey = new SecretKeySpec(hmacKey, integrityHmac.getAlgorithm());
            integrityHmac.init(integrityKey);
            byte[] message = new byte[encSecret.length + akName.length];
            System.arraycopy(encSecret, 0, message, 0, encSecret.length);
            System.arraycopy(akName, 0, message, encSecret.length, akName.length);
            integrityHmac.update(message);
            byte[] integrity = integrityHmac.doFinal();
            b = ByteBuffer.allocate(2);
            b.putShort((short) (HMAC_SIZE_LENGTH_BYTES + HMAC_KEY_LENGTH_BYTES + encSecret.length));
            byte[] topSize = b.array();

            // return ordered blob of assembled credentials
            byte[] bytesToReturn = assembleCredential(topSize, integrity, encSecret, encSeed);
            return ByteString.copyFrom(bytesToReturn);

        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
                 | InvalidKeyException | InvalidAlgorithmParameterException
                 | NoSuchPaddingException e) {
            throw new IdentityProcessingException(
                    "Encountered error while making the identity claim challenge: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Assembles a credential blob.
     *
     * @param topSize         byte array representation of the top size
     * @param integrityHmac   byte array representation of the integrity HMAC
     * @param encryptedSecret byte array representation of the encrypted secret
     * @param encryptedSeed   byte array representation of the encrypted seed
     * @return byte array representation of a credential blob
     */
    public static byte[] assembleCredential(final byte[] topSize, final byte[] integrityHmac,
                                            final byte[] encryptedSecret,
                                            final byte[] encryptedSeed) {
        /*
         * Credential structure breakdown with endianness:
         * 0-1 topSize (2), LE
         * 2-3 hashsize (2), BE always 0x0020
         * 4-35 integrity HMac (32)
         * 36-133 (98 = 32*3 +2) of zeros, copy over from encSecret starting at [36]
         * 134-135 (2) LE size, always 0x0001
         * 136-391 (256) copy over with encSeed
         * */
        byte[] credentialBlob = new byte[TPM2_CREDENTIAL_BLOB_SIZE];
        credentialBlob[0] = topSize[1];
        credentialBlob[1] = topSize[0];
        credentialBlob[2] = 0x00;

        final int credBlobPosition4 = 3;
        final byte credBlobFourthPositionValue = 0x20;
        credentialBlob[credBlobPosition4] = credBlobFourthPositionValue;

        final int credBlobPosition5 = 4;
        final int credBlobSizeFromPosition5 = 32;
        System.arraycopy(integrityHmac, 0, credentialBlob, credBlobPosition5, credBlobSizeFromPosition5);

        final int credBlobPosition99 = 98;
        final int credBlobPosition37 = 36;

        for (int i = 0; i < credBlobPosition99; i++) {
            credentialBlob[credBlobPosition37 + i] = 0x00;
        }
        System.arraycopy(encryptedSecret, 0, credentialBlob, credBlobPosition37, encryptedSecret.length);

        final int credBlobPosition135 = 134;
        credentialBlob[credBlobPosition135] = 0x00;

        final int credBlobPosition136 = 135;
        credentialBlob[credBlobPosition136] = 0x01;

        final int credBlobPosition137 = 136;
        final int credBlobSizeFromPosition137 = 256;
        System.arraycopy(encryptedSeed, 0, credentialBlob, credBlobPosition137, credBlobSizeFromPosition137);
        // return the result
        return credentialBlob;
    }

    /**
     * Determines the AK name from the AK Modulus.
     *
     * @param akModulus modulus of an attestation key
     * @return the ak name byte array
     * @throws NoSuchAlgorithmException Underlying SHA256 method used a bad algorithm
     */
    public static byte[] generateAkName(final byte[] akModulus) throws NoSuchAlgorithmException {
        byte[] namePrefix = HexUtils.hexStringToByteArray(AK_NAME_PREFIX);
        byte[] hashPrefix = HexUtils.hexStringToByteArray(AK_NAME_HASH_PREFIX);
        byte[] toHash = new byte[hashPrefix.length + akModulus.length];
        System.arraycopy(hashPrefix, 0, toHash, 0, hashPrefix.length);
        System.arraycopy(akModulus, 0, toHash, hashPrefix.length, akModulus.length);
        byte[] nameHash = sha256hash(toHash);
        byte[] toReturn = new byte[namePrefix.length + nameHash.length];
        System.arraycopy(namePrefix, 0, toReturn, 0, namePrefix.length);
        System.arraycopy(nameHash, 0, toReturn, namePrefix.length, nameHash.length);
        return toReturn;
    }

    /**
     * This replicates the TPM 2.0 CryptKDFa function to an extent. It will only work for generation
     * that uses SHA-256, and will only generate values of 32 B or less. Counters above zero and
     * multiple contexts are not supported in this implementation. This should work for all uses of
     * the KDF for TPM2_MakeCredential.
     *
     * @param seed        random value used to generate the key
     * @param label       first portion of message used to generate key
     * @param context     second portion of message used to generate key
     * @param sizeInBytes size of key to generate in bytes
     * @return the derived key
     * @throws NoSuchAlgorithmException Wrong crypto algorithm selected
     * @throws InvalidKeyException      Invalid key used
     */

    public static byte[] cryptKDFa(final byte[] seed, final String label, final byte[] context,
                                   final int sizeInBytes)
            throws NoSuchAlgorithmException, InvalidKeyException {
        final int capacity = 4;
        ByteBuffer b = ByteBuffer.allocate(capacity);
        b.putInt(1);
        byte[] counter = b.array();
        // get the label
        String labelWithEnding = label;
        if (label.charAt(label.length() - 1) != '\u0000') {
            labelWithEnding = label + "\0";
        }
        byte[] labelBytes = labelWithEnding.getBytes(StandardCharsets.UTF_8);
        final int byteOffset = 8;
        b = ByteBuffer.allocate(capacity);
        b.putInt(sizeInBytes * byteOffset);
        byte[] desiredSizeInBits = b.array();
        int sizeOfMessage = byteOffset + labelBytes.length;
        if (context != null) {
            sizeOfMessage += context.length;
        }
        byte[] message = new byte[sizeOfMessage];
        int marker = 0;

        final int markerLength = 4;
        System.arraycopy(counter, 0, message, marker, markerLength);
        marker += markerLength;
        System.arraycopy(labelBytes, 0, message, marker, labelBytes.length);
        marker += labelBytes.length;
        if (context != null) {
            System.arraycopy(context, 0, message, marker, context.length);
            marker += context.length;
        }
        System.arraycopy(desiredSizeInBits, 0, message, marker, markerLength);
        Mac hmac;
        byte[] toReturn = new byte[sizeInBytes];

        hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec hmacKey = new SecretKeySpec(seed, hmac.getAlgorithm());
        hmac.init(hmacKey);
        hmac.update(message);
        byte[] hmacResult = hmac.doFinal();
        System.arraycopy(hmacResult, 0, toReturn, 0, sizeInBytes);
        return toReturn;
    }

    /**
     * This method takes the provided TPM Quote and splits it between the PCR
     * quote and the signature hash.
     *
     * @param tpmQuote contains hash values for the quote and the signature
     * @return parsed TPM Quote hash
     */
    public static String parseTPMQuoteHash(final String tpmQuote) {
        if (tpmQuote != null) {
            String[] lines = tpmQuote.split(":");
            if (lines[1].contains("signature")) {
                return lines[1].replace("signature", "").trim();
            } else {
                return lines[1].trim();
            }
        }

        return "null";
    }

    /**
     * This method takes the provided TPM Quote and splits it between the PCR
     * quote and the signature hash.
     *
     * @param tpmQuote contains hash values for the quote and the signature
     * @return parsed TPM Quote signature
     */
    public static String parseTPMQuoteSignature(final String tpmQuote) {
        if (tpmQuote != null) {
            String[] lines = tpmQuote.split(":");

            return lines[2].trim();
        }

        return "null";
    }

    /**
     * Computes the sha256 hash of the given blob.
     *
     * @param blob byte array to take the hash of
     * @return sha256 hash of blob
     * @throws NoSuchAlgorithmException improper algorithm selected
     */
    public static byte[] sha256hash(final byte[] blob) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(blob);
        return md.digest();
    }

    /**
     * Generates an array of random bytes.
     *
     * @param numberOfBytes to be generated
     * @return byte array filled with the specified number of bytes.
     */
    public static byte[] generateRandomBytes(final int numberOfBytes) {
        byte[] bytes = new byte[numberOfBytes];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Calculates the difference in days between the two provided dates.
     *
     * @param date1 first provided date
     * @param date2 second provided date
     * @return difference in days between two dates
     */
    public static int daysBetween(final Date date1, final Date date2) {
        final int hoursInADay = 24;
        final int secondsInAnHour = 3600;
        final int millisecondsInASecond = 1000;
        return (int) ((date2.getTime() - date1.getTime())
                / (millisecondsInASecond * secondsInAnHour * hoursInADay));
    }
}
