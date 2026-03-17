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
import javax.crypto.KeyAgreement;
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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
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

    private static final int DEFAULT_ECC_KEY_LENGTH_IN_BYTES = 32;

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
     * Parses a public key from a byte array using one of the two supported public key
     * algorithm and returns it as a generic PublicKey.
     *
     * @param publicAreaSegment public area segment generated by TPM 2.0
     * @return the parsed PublicKey instance
     */
    public static PublicKey parsePublicKeyFromPublicDataSegment(final byte[] publicAreaSegment) {
        final PublicKeyAlgorithm publicKeyAlgorithm = extractPublicKeyAlgorithmFromByteArray(publicAreaSegment);

        return switch (publicKeyAlgorithm) {
            case RSA -> parseRSAKeyFromPublicDataSegment(publicAreaSegment);
            case ECC -> parseECCKeyFromPublicDataSegment(publicAreaSegment);
            default -> throw new UnsupportedOperationException("Unsupported or invalid public key algorithm");
        };
    }

    /**
     * Extracts the public key algorithm from the first two bytes of the provided byte array.
     *
     * @param byteArray byte array
     * @return public key algorithm
     */
    public static PublicKeyAlgorithm extractPublicKeyAlgorithmFromByteArray(byte[] byteArray) {

        // Return UNKNOWN if there are not enough bytes for comparison
        if (byteArray == null || byteArray.length < 2) {
            return PublicKeyAlgorithm.UNKNOWN;
        }

        final int unsignedByteMask = 0xFF;
        final int byteShiftVal = 8;

        // Extract the first two bytes from the byte array and combine them into a 16-bit integer (algorithm ID).
        // - `byteArray[0] & 0xFF` ensures the first byte is treated as unsigned (0-255 range).
        // - `<< 8` shifts the first byte 8 bits to the left, placing it in the higher byte of the 16-bit value.
        // - `byteArray[1] & 0xFF` ensures the second byte is treated as unsigned, then it occupies the lower byte.
        // - The bitwise OR (`|`) combines the two parts into a single 16-bit value (int).
        final int algorithmId = ((byteArray[0] & unsignedByteMask) << byteShiftVal) | (byteArray[1] & unsignedByteMask);

        // Compare the extracted ID with the enum's list of ids
        return PublicKeyAlgorithm.fromId(algorithmId);
    }

    /**
     * Parses the RSA public key from public data segment generated by TPM 2.0.
     *
     * @param publicDataSegment the public area segment to parse
     * @return the RSA public key of the supplied public data
     */
    public static RSAPublicKey parseRSAKeyFromPublicDataSegment(final byte[] publicDataSegment) {
        final int publicDataSegmentLen = publicDataSegment.length;

        if (publicDataSegmentLen < DEFAULT_RSA_MODULUS_LENGTH_IN_BYTES) {
            final String errorMsg = "Could not parse RSA Public Key due to public data segment not being long enough.";
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // public data ends with 256 byte modulus
        byte[] modulus =
                HexUtils.subarray(publicDataSegment, publicDataSegmentLen - DEFAULT_RSA_MODULUS_LENGTH_IN_BYTES,
                        publicDataSegmentLen - 1);
        return assembleRSAPublicKey(modulus);
    }

    /**
     * Constructs an RSA public key where the modulus is Hex encoded.
     *
     * @param modulus in byte array form
     * @return RSA public key using specific modulus and the well known exponent
     */
    public static RSAPublicKey assembleRSAPublicKey(final byte[] modulus) {
        final String modulusHexString = Hex.encodeHexString(modulus);
        return assembleRSAPublicKey(new BigInteger(modulusHexString, DEFAULT_IV_SIZE));
    }

    /**
     * Assembles an RSA public key using a defined big int modulus and the well known exponent.
     *
     * @param modulus modulus
     * @return RSA public key using the provided integer modulus
     */
    public static RSAPublicKey assembleRSAPublicKey(final BigInteger modulus) {
        // generate an RSA key spec using mod and exp
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, EXPONENT);

        // create the RSA public key
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new UnexpectedServerException(
                    "Encountered unexpected error creating RSA public key: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the ECC public key from public data segment generated by TPM 2.0.
     *
     * @param publicDataSegment the public area segment to parse
     * @return the ECC public key of the supplied public data
     */
    public static ECPublicKey parseECCKeyFromPublicDataSegment(final byte[] publicDataSegment) {
        final int publicDataSegmentLen = publicDataSegment.length;

        if (publicDataSegmentLen < DEFAULT_ECC_KEY_LENGTH_IN_BYTES) {
            final String errorMsg = "Could not parse ECC Public Key due to public data segment not being long enough.";
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        byte[] eccKeySection =
                HexUtils.subarray(publicDataSegment, publicDataSegmentLen - DEFAULT_ECC_KEY_LENGTH_IN_BYTES,
                        publicDataSegmentLen - 1);

        return assembleECCPublicKey(eccKeySection);
    }

    /**
     * Assembles the ECC public key using the provided byte array.
     *
     * @param eccKeySection ecc public key section of the byte array
     * @return the ECC public key
     */
    public static ECPublicKey assembleECCPublicKey(final byte[] eccKeySection) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");

            // Create an X509EncodedKeySpec object with the byte array
            X509EncodedKeySpec spec = new X509EncodedKeySpec(eccKeySection);

            // Generate the ECC public key from the byte array
            return (ECPublicKey) keyFactory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new UnexpectedServerException(
                    "Encountered unexpected error creating ECC public key: " + e.getMessage(), e);
        }
    }

    /**
     * Determines the public key algorithm of both the endorsement and attestation public key in order to
     * properly create a credential blob string.
     *
     * @param endorsementPublicKey endorsement public key in the identity claim
     * @param attestationPublicKey attestation public key in the identity claim
     * @param secret               a nonce
     * @return the encrypted blob forming the identity claim challenge
     */
    public static ByteString tpm20MakeCredential(final PublicKey endorsementPublicKey,
                                                 final PublicKey attestationPublicKey,
                                                 final byte[] secret) {
        // check size of the secret
        if (secret.length > MAX_SECRET_LENGTH) {
            throw new IllegalArgumentException("Secret must be " + MAX_SECRET_LENGTH + " bytes or smaller.");
        }

        if (endorsementPublicKey instanceof RSAPublicKey && attestationPublicKey instanceof RSAPublicKey) {
            return tpm20MakeCredentialUsingRSA(
                    (RSAPublicKey) endorsementPublicKey,
                    (RSAPublicKey) attestationPublicKey, secret);
        } else if (endorsementPublicKey instanceof ECPublicKey && attestationPublicKey instanceof ECPublicKey) {
            return tpm20MakeCredentialUsingECC(
                    (ECPublicKey) endorsementPublicKey,
                    (ECPublicKey) attestationPublicKey, secret);
        }

        throw new UnsupportedOperationException("Mismatched or unsupported public key algorithms: "
                + endorsementPublicKey.getAlgorithm() + " / " + attestationPublicKey.getAlgorithm());
    }

    /**
     * Performs the first step of the TPM 2.0 identity claim process using the RSA public key algorithm:
     * <ul>
     *      <li>Takes an ek, ak, and secret and generates a seed that is used to generate AES and HMAC keys.</li>
     *      <li>Parses the ak name.</li>
     *      <li>Encrypts the seed with the public ek.</li>
     *      <li>Uses the AES key to encrypt the secret. </li>
     *      <li>Uses the HMAC key to generate an HMAC to cover the encrypted secret and the ak name.</li>
     * </ul>
     *
     * <p>
     * The output is an encrypted blob that acts as the first part of a challenge-response authentication mechanism
     * to validate an identity claim.
     * <p>
     * Equivalent to calling tpm2_makecredential using tpm2_tools.
     *
     * @param endorsementRSAKey endorsement RSA key in the identity claim
     * @param attestationRSAKey attestation RSA key in the identity claim
     * @param secret            a nonce
     * @return the encrypted blob forming the identity claim challenge
     */
    public static ByteString tpm20MakeCredentialUsingRSA(final RSAPublicKey endorsementRSAKey,
                                                         final RSAPublicKey attestationRSAKey,
                                                         final byte[] secret) {
        // generate a random 32 byte seed
        byte[] seed = generateRandomBytes(SEED_LENGTH);

        try {
            // encrypt seed with endorsement RSA Public Key
            Cipher asymCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

            OAEPParameterSpec oaepSpec = new OAEPParameterSpec("SHA-256", "MGF1",
                    MGF1ParameterSpec.SHA256, new PSource.PSpecified("IDENTITY\0".getBytes(StandardCharsets.UTF_8)));

            asymCipher.init(Cipher.PUBLIC_KEY, endorsementRSAKey, oaepSpec);
            asymCipher.update(seed);
            byte[] encSeed = asymCipher.doFinal();

            // generate ak name from akMod
            byte[] akModTemp = attestationRSAKey.getModulus().toByteArray();
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
            ByteBuffer lengthBuffer = ByteBuffer.allocate(2);
            lengthBuffer.putShort((short) (secret.length));
            byte[] secretLength = lengthBuffer.array();
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
            lengthBuffer = ByteBuffer.allocate(2);
            lengthBuffer.putShort((short) (HMAC_SIZE_LENGTH_BYTES + HMAC_KEY_LENGTH_BYTES + encSecret.length));
            byte[] topSize = lengthBuffer.array();

            // return ordered blob of assembled credentials
            byte[] bytesToReturn = assembleCredential(topSize, integrity, encSecret, encSeed);
            return ByteString.copyFrom(bytesToReturn);

        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
                 | InvalidKeyException | InvalidAlgorithmParameterException
                 | NoSuchPaddingException e) {
            throw new IdentityProcessingException(
                    "Encountered error while making the identity claim challenge for the provided RSA public keys: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Performs the first step of the TPM 2.0 identity claim process using the ECC public key algorithm:
     * <ul>
     *      <li>Takes an ek, ak, and secret and generates a seed that is used to generate AES and HMAC keys.</li>
     *      <li>Parses the ak name.</li>
     *      <li>Encrypts the seed with the public ek.</li>
     *      <li>Uses the AES key to encrypt the secret. </li>
     *      <li>Uses the HMAC key to generate an HMAC to cover the encrypted secret and the ak name.</li>
     * </ul>
     *
     * <p>
     * The output is an encrypted blob that acts as the first part of a challenge-response authentication mechanism
     * to validate an identity claim.
     * <p>
     * Equivalent to calling tpm2_makecredential using tpm2_tools.
     *
     * @param endorsementECCKey endorsement ECC key in the identity claim
     * @param attestationECCKey attestation ECC key in the identity claim
     * @param secret            a nonce
     * @return the encrypted blob forming the identity claim challenge
     */
    public static ByteString tpm20MakeCredentialUsingECC(final ECPublicKey endorsementECCKey,
                                                         final ECPublicKey attestationECCKey,
                                                         final byte[] secret) {
        try {
            // --- Step 1: Generate ephemeral ECC key pair for encryption ---

            // This temporary key pair is used for ECC "encryption" via ECDH.
            // Only the TPM with the corresponding private EK can recover the shared secret.
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");

            // Initialize the key pair generator with the same curve parameters as the TPM's EK.
            // This ensures ECDH works correctly between the ephemeral key and the EK.
            kpg.initialize(endorsementECCKey.getParams());

            // Generate the ephemeral key pair (random, unique for this credential)
            KeyPair ephKeyPair = kpg.generateKeyPair();

            // Extract the private key from the ephemeral pair
            // This will stay secret and is used to compute the ECDH shared secret with the EK public key.
            ECPrivateKey ephemeralPrivateKey = (ECPrivateKey) ephKeyPair.getPrivate();

            // Extract the public key from the ephemeral pair
            // This will be sent to the TPM so it can also compute the shared secret using its private EK.
            ECPublicKey ephemeralPublicKey = (ECPublicKey) ephKeyPair.getPublic();

            // Encode ephemeral and endorsement keys as bytes for KDF context
            byte[] ephemeralPublicKeyBytes = convertECPublicKeyToBytes(ephemeralPublicKey);

            // --- Step 2: Compute ECDH shared secret Z ---

            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(ephemeralPrivateKey);

            // Combine your ephemeral private key with the TPM's Endorsement Key public key
            // This performs the elliptic curve math needed to derive the shared secret
            keyAgreement.doPhase(endorsementECCKey, true);

            // Generate the shared secret bytes (traditionally called 'Z' in the spec)
            byte[] sharedSecret = keyAgreement.generateSecret();

            // --- Step 3: Derive AES and HMAC keys using cryptKDFa ---

            byte[] aesKey = cryptKDFa(sharedSecret, "STORAGE", ephemeralPublicKeyBytes, AES_KEY_LENGTH_BYTES);
            byte[] hmacKey = cryptKDFa(sharedSecret, "INTEGRITY", null, HMAC_KEY_LENGTH_BYTES);

            // --- Step 4: Encrypt the secret using AES-GCM ---

            final int aesBlockSizeBytes = 16;
            final byte[] zeroIv = new byte[aesBlockSizeBytes];
            IvParameterSpec ivSpec = new IvParameterSpec(zeroIv);

            Cipher aesCipher = Cipher.getInstance("AES/CFB/NoPadding");
            SecretKeySpec aesKeySpec = new SecretKeySpec(aesKey, "AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec, ivSpec);

            // ---- ADD LENGTH PREFIX (REQUIRED) ----

            // Allocate 2 bytes to store secret length
            ByteBuffer lengthBuffer = ByteBuffer.allocate(2);
            lengthBuffer.putShort((short) secret.length);
            byte[] lengthBytes = lengthBuffer.array();

            // Create new array: [length (2 bytes) | secret]
            byte[] secretWithLength = new byte[2 + secret.length];
            System.arraycopy(lengthBytes, 0, secretWithLength, 0, 2);
            System.arraycopy(secret, 0, secretWithLength, 2, secret.length);

            // Encrypt the combined array
            byte[] encryptedSecret = aesCipher.doFinal(secretWithLength);

            // Encrypt the credential value

            //todo
            
            // --- Step 5: Compute HMAC over (encryptedSecret || akName) ---
            // Generate the AK name (this uniquely identifies the attestation key)
            // TPM uses this to ensure the credential is tied to the correct AK
            //byte[] akName = generateAkName(attestationECCKey);
//
            //// Create HMAC instance using SHA-256 (matches your KDF and TPM expectations)
            //Mac hmac = Mac.getInstance("HmacSHA256");
            //SecretKeySpec hmacKeySpec = new SecretKeySpec(hmacKey, "HmacSHA256");
            //hmac.init(hmacKeySpec);
            //hmac.update(encryptedSecret);
            //hmac.update(akName);
//
            //// Compute final HMAC value
            //byte[] hmacValue = hmac.doFinal();
//
//
            //byte[] bytesToReturn = assembleCredential(topSize, hmacValue, encryptedSecret, encSeed);
            //return ByteString.copyFrom(bytesToReturn);

            throw new UnsupportedOperationException("");

        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
                 | InvalidKeyException | InvalidAlgorithmParameterException
                 | NoSuchPaddingException e) {
            throw new IdentityProcessingException(
                    "Encountered error while making the identity claim challenge for the provided RSA public keys: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Converts an EC public key point into a fixed-length uncompressed byte array.
     * This format is required for TPM key derivation functions (KDFe).
     * <pre>
     * Format: 0x04 || X || Y
     * </pre>
     *
     * <p>
     * TPM ECC operations (like making a credential) require the public point to be
     * encoded as a deterministic byte array. Both sides (client and TPM) must
     * compute the same bytes for the ECDH key derivation to work.
     *
     * @param publicKey The EC public key to encode.
     * @return Byte array representing the uncompressed EC point.
     */
    public static byte[] convertECPublicKeyToBytes(final ECPublicKey publicKey) {
        // Get the raw EC point (X, Y coordinates)
        ECPoint ecPoint = publicKey.getW();

        // Constants for calculation
        // Example: 521 bits + 7 = 528 → 528 / 8 = 66 bytes
        final int fieldSizeInBytes = getFieldSizeInBytes(publicKey);

        // Convert X and Y coordinates to fixed-length byte arrays
        byte[] xBytes = convertBigIntegerToFixedLength(ecPoint.getAffineX(), fieldSizeInBytes);
        byte[] yBytes = convertBigIntegerToFixedLength(ecPoint.getAffineY(), fieldSizeInBytes);

        // Constants for encoding the EC point
        final byte uncompressedPointPrefix = 0x04;  // Indicates this is an uncompressed point (X and Y)
        final int numCoordinates = 2;               // X + Y

        // Allocate the byte array for the encoded point. Total length = 1 byte prefix + X bytes + Y bytes
        int encodedPointLength = 1 + (numCoordinates * fieldSizeInBytes);
        byte[] encodedPointByteArray = new byte[encodedPointLength];

        // Set the uncompressed point prefix at the first byte
        encodedPointByteArray[0] = uncompressedPointPrefix;

        // Copy X coordinate bytes into the array
        System.arraycopy(xBytes, 0, encodedPointByteArray, 1, fieldSizeInBytes);

        // Copy Y coordinate bytes immediately after X
        System.arraycopy(yBytes, 0, encodedPointByteArray, 1 + fieldSizeInBytes, fieldSizeInBytes);

        return encodedPointByteArray;
    }

    /**
     * Calculates the number of bytes required for each coordinate (X or Y).
     *
     * @param eccPublicKey {@link ECPublicKey}
     * @return {@link ECPublicKey}'s field size in bytes
     */
    public static int getFieldSizeInBytes(final ECPublicKey eccPublicKey) {
        // Standard: 8 bits in a byte
        final int bitsPerByte = 8;

        // Add 7 to bits before dividing by 8 to round up
        final int roundUpOffset = 7;

        // The field size of the curve is in bits (e.g., P-256 = 256 bits)
        // TPM expects each coordinate to be **fixed-length**, even if the number would normally take fewer bytes
        return (eccPublicKey.getParams().getCurve().getField().getFieldSize()
                + roundUpOffset) / bitsPerByte;
    }

    /**
     * Converts a BigInteger to a fixed-length byte array. Pads with leading zeros if necessary or trims extra bytes
     * if needed.
     *
     * @param value  The BigInteger to convert.
     * @param length Desired byte length of the output array.
     * @return Byte array of exact length representing the BigInteger.
     */
    public static byte[] convertBigIntegerToFixedLength(final BigInteger value, final int length) {
        byte[] rawBytes = value.toByteArray(); // default BigInteger encoding
        byte[] fixedLengthBytes = new byte[length];

        if (rawBytes.length == length) {
            // Already the correct size, just return
            return rawBytes;
        } else if (rawBytes.length > length) {
            // Trim leading byte(s) (sometimes BigInteger adds a sign byte)
            System.arraycopy(rawBytes, rawBytes.length - length, fixedLengthBytes, 0, length);
        } else {
            // Pad with leading zeros to reach the correct length
            System.arraycopy(rawBytes, 0, fixedLengthBytes, length - rawBytes.length, rawBytes.length);
        }

        return fixedLengthBytes;
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
    public static byte[] assembleCredential(final byte[] topSize,
                                            final byte[] integrityHmac,
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
    public static byte[] cryptKDFa(final byte[] seed, final String label, final byte[] context, final int sizeInBytes)
            throws NoSuchAlgorithmException, InvalidKeyException {
        // --- Step 1: Set up the counter (always 1 for this simplified version) ---
        final int capacity = 4;
        ByteBuffer b = ByteBuffer.allocate(capacity);
        b.putInt(1);
        byte[] counter = b.array();

        // --- Step 2: Ensure label ends with null byte (\0) as per TPM spec ---
        String labelWithEnding = label;
        if (label.charAt(label.length() - 1) != '\u0000') {
            labelWithEnding = label + "\0";
        }
        byte[] labelBytes = labelWithEnding.getBytes(StandardCharsets.UTF_8);

        // --- Step 3: Prepare size of desired output in bits ---
        final int byteOffset = 8;
        b = ByteBuffer.allocate(capacity);
        b.putInt(sizeInBytes * byteOffset);
        byte[] desiredSizeInBits = b.array();

        // --- Step 4: Compute total length of the message to HMAC ---
        int sizeOfMessage = byteOffset + labelBytes.length;
        if (context != null) {
            sizeOfMessage += context.length;
        }
        byte[] message = new byte[sizeOfMessage];
        int marker = 0;

        final int markerLength = 4;

        // --- Step 5: Copy counter into message ---
        System.arraycopy(counter, 0, message, marker, markerLength);
        marker += markerLength;

        // --- Step 6: Copy label into message ---
        System.arraycopy(labelBytes, 0, message, marker, labelBytes.length);
        marker += labelBytes.length;

        // --- Step 5: Copy counter into message ---
        if (context != null) {
            System.arraycopy(context, 0, message, marker, context.length);
            marker += context.length;
        }

        // --- Step 8: Copy desired size in bits at the end of message ---
        System.arraycopy(desiredSizeInBits, 0, message, marker, markerLength);

        // --- Step 9: Initialize HMAC-SHA256 with the seed as key ---
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec hmacKey = new SecretKeySpec(seed, hmac.getAlgorithm());
        hmac.init(hmacKey);

        // --- Step 10: Feed the constructed message into HMAC ---
        hmac.update(message);

        // --- Step 11: Compute HMAC output ---
        byte[] hmacResult = hmac.doFinal();

        // --- Step 12: Copy required number of bytes to return array ---
        byte[] toReturn = new byte[sizeInBytes];
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
    public static int calculateDaysBetweenDates(final Date date1, final Date date2) {
        final int hoursInADay = 24;
        final int secondsInAnHour = 3600;
        final int millisecondsInASecond = 1000;
        return (int) ((date2.getTime() - date1.getTime())
                / (millisecondsInASecond * secondsInAnHour * hoursInADay));
    }
}
