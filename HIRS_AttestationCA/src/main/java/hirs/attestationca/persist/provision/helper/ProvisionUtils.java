package hirs.attestationca.persist.provision.helper;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.userdefined.info.TPMInfo;
import hirs.attestationca.persist.exceptions.CertificateProcessingException;
import hirs.attestationca.persist.exceptions.IdentityProcessingException;
import hirs.attestationca.persist.exceptions.UnexpectedServerException;
import hirs.structs.converters.SimpleStructBuilder;
import hirs.structs.elements.aca.SymmetricAttestation;
import hirs.structs.elements.tpm.EncryptionScheme;
import hirs.structs.elements.tpm.IdentityRequest;
import hirs.structs.elements.tpm.SymmetricKey;
import hirs.structs.elements.tpm.SymmetricKeyParams;
import hirs.utils.HexUtils;
import hirs.utils.enums.DeviceInfoEnums;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

@Log4j2
public final class ProvisionUtils {

    /**
     * The default size for IV blocks.
     */
    public final static int DEFAULT_IV_SIZE = 16;
    /**
     * Defines the well known exponent.
     * https://en.wikipedia.org/wiki/65537_(number)#Applications
     */
    private final static BigInteger EXPONENT = new BigInteger("010001", DEFAULT_IV_SIZE);
    public static final int HMAC_SIZE_LENGTH_BYTES = 2;
    public static final int HMAC_KEY_LENGTH_BYTES = 32;
    public static final int SEED_LENGTH = 32;
    public static final int MAX_SECRET_LENGTH = 32;
    public static final int AES_KEY_LENGTH_BYTES = 16;
    private static final int TPM2_CREDENTIAL_BLOB_SIZE = 392;
    private static final int RSA_MODULUS_LENGTH = 256;
    // Constants used to parse out the ak name from the ak public data. Used in generateAkName
    private static final String AK_NAME_PREFIX = "000b";
    private static final String AK_NAME_HASH_PREFIX =
            "0001000b00050072000000100014000b0800000000000100";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Helper method to parse a byte array into an {@link hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2.IdentityClaim}.
     *
     * @param identityClaim byte array that should be converted to a Protobuf IdentityClaim
     *                      object
     * @throws {@link IdentityProcessingException} if byte array could not be parsed
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
     * @throws {@link UnexpectedServerException} if error occurs during encoding retrieval
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
     * @throws {@link UnexpectedServerException} if error occurs during encoding retrieval
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
     * Parse public key from public data segment generated by TPM 2.0.
     * @param publicArea the public area segment to parse
     * @return the RSA public key of the supplied public data
     */
    public static RSAPublicKey parsePublicKey(final byte[] publicArea) {
        int pubLen = publicArea.length;
        if (pubLen < RSA_MODULUS_LENGTH) {
            throw new IllegalArgumentException(
                    "EK or AK public data segment is not long enough");
        }
        // public data ends with 256 byte modulus
        byte[] modulus = HexUtils.subarray(publicArea,
                pubLen - RSA_MODULUS_LENGTH,
                pubLen - 1);
        return (RSAPublicKey) assemblePublicKey(modulus);
    }

    /**
     * Constructs a public key where the modulus is in raw form.
     *
     * @param modulus
     *            in byte array form
     * @return public key using specific modulus and the well known exponent
     */
    public static PublicKey assemblePublicKey(final byte[] modulus) {
        return assemblePublicKey(Hex.encodeHexString(modulus));
    }

    /**
     * Constructs a public key where the modulus is Hex encoded.
     *
     * @param modulus
     *            hex encoded modulus
     * @return public key using specific modulus and the well known exponent
     */
    public static PublicKey assemblePublicKey(final String modulus) {
        return assemblePublicKey(new BigInteger(modulus, DEFAULT_IV_SIZE));
    }

    /**
     * Assembles a public key using a defined big int modulus and the well known exponent.
     */
    public static PublicKey assemblePublicKey(final BigInteger modulus) {
        // generate a key spec using mod and exp
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, EXPONENT);

        // create the public key
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new UnexpectedServerException(
                    "Encountered unexpected error creating public key: " + e.getMessage(), e);
        }
    }

    /**
     * Will attempt to decrypt the asymmetric blob that originated from an
     * {@link hirs.structs.elements.tpm.IdentityRequest} using the cipher transformation.
     *
     * @param asymmetricBlob to be decrypted
     * @param scheme to decrypt with
     * @param privateKey cipher private key
     * @return decrypted blob
     */
    public static byte[] decryptAsymmetricBlob(final byte[] asymmetricBlob,
                                               final EncryptionScheme scheme,
                                               final PrivateKey privateKey) {
        try {
            // create a cipher from the specified transformation
            Cipher cipher = Cipher.getInstance(scheme.toString());

            switch (scheme) {
                case OAEP:
                    OAEPParameterSpec spec =
                            new OAEPParameterSpec("Sha1", "MGF1", MGF1ParameterSpec.SHA1,
                                    new PSource.PSpecified("".getBytes(StandardCharsets.UTF_8)));

                    cipher.init(Cipher.PRIVATE_KEY, privateKey, spec);
                    break;
                default:
                    // initialize the cipher to decrypt using the ACA private key.
                    cipher.init(Cipher.DECRYPT_MODE, privateKey);
            }

            cipher.update(asymmetricBlob);

            return cipher.doFinal();
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException
                | BadPaddingException | IllegalBlockSizeException
                | InvalidAlgorithmParameterException e) {
            throw new IdentityProcessingException(
                    "Encountered error while decrypting asymmetric blob of an identity request: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Will attempt to decrypt the symmetric blob that originated from an
     * {@link hirs.structs.elements.tpm.IdentityRequest} using the specified symmetric key
     * and cipher transformation.
     *
     * @param symmetricBlob to be decrypted
     * @param symmetricKey to use to decrypt
     * @param iv to use with decryption cipher
     * @param transformation of the cipher
     * @return decrypted symmetric blob
     */
    public static byte[] decryptSymmetricBlob(final byte[] symmetricBlob, final byte[] symmetricKey,
                                        final byte[] iv, final String transformation) {
        try {
            // create a cipher from the specified transformation
            Cipher cipher = Cipher.getInstance(transformation);

            // generate a key specification to initialize the cipher
            SecretKeySpec keySpec = new SecretKeySpec(symmetricKey, "AES");

            // initialize the cipher to decrypt using the symmetric key
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

            // decrypt the symmetric blob
            return cipher.doFinal(symmetricBlob);
        } catch (IllegalBlockSizeException | InvalidKeyException | NoSuchAlgorithmException
                | BadPaddingException | NoSuchPaddingException
                | InvalidAlgorithmParameterException exception) {
            log.error("Encountered error while decrypting symmetric blob of an identity request: "
                    + exception.getMessage(), exception);
        }

        return new byte[0];
    }

    public static SymmetricKey generateSymmetricKey() {
        // create a session key for the CA contents
        byte[] responseSymmetricKey =
                generateRandomBytes(DEFAULT_IV_SIZE);

        // create a symmetric key struct for the CA contents
        SymmetricKey sessionKey =
                new SimpleStructBuilder<>(SymmetricKey.class)
                        .set("algorithmId", SymmetricKey.ALGORITHM_AES)
                        .set("encryptionScheme", SymmetricKey.SCHEME_CBC)
                        .set("key", responseSymmetricKey).build();
        return sessionKey;
    }

    /**
     * Performs the first step of the TPM 2.0 identity claim process. Takes an ek, ak, and secret
     * and then generates a seed that is used to generate AES and HMAC keys. Parses the ak name.
     * Encrypts the seed with the public ek. Uses the AES key to encrypt the secret. Uses the HMAC
     * key to generate an HMAC to cover the encrypted secret and the ak name. The output is an
     * encrypted blob that acts as the first part of a challenge-response authentication mechanism
     * to validate an identity claim.
     *
     * Equivalent to calling tpm2_makecredential using tpm2_tools.
     *
     * @param ek endorsement key in the identity claim
     * @param ak attestation key in the identity claim
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
                    MGF1ParameterSpec.SHA256, new PSource.PSpecified("IDENTITY\0".getBytes(StandardCharsets.UTF_8)));
            asymCipher.init(Cipher.PUBLIC_KEY, ek, oaepSpec);
            asymCipher.update(seed);
            byte[] encSeed = asymCipher.doFinal();

            // generate ak name from akMod
            byte[] akModTemp = ak.getModulus().toByteArray();
            byte[] akMod = new byte[RSA_MODULUS_LENGTH];
            int startpos = 0;
            // BigIntegers are signed, so a modulus that has a first bit of 1
            // will be padded with a zero byte that must be removed
            if (akModTemp[0] == 0x00) {
                startpos = 1;
            }
            System.arraycopy(akModTemp, startpos, akMod, 0, RSA_MODULUS_LENGTH);
            byte[] akName = ProvisionUtils.generateAkName(akMod);

            // generate AES and HMAC keys from seed
            byte[] aesKey = ProvisionUtils.cryptKDFa(seed, "STORAGE", akName, AES_KEY_LENGTH_BYTES);
            byte[] hmacKey = ProvisionUtils.cryptKDFa(seed, "INTEGRITY", null, HMAC_KEY_LENGTH_BYTES);

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
            byte[] bytesToReturn = ProvisionUtils.assembleCredential(topSize, integrity, encSecret, encSeed);
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
     * Generate asymmetric contents part of the identity response.
     *
     * @param identityKey identity requests symmetric contents, otherwise, the identity proof
     * @param sessionKey identity response session key
     * @param publicKey of the EK certificate contained within the identity proof
     * @return encrypted asymmetric contents
     */
    public static byte[] generateAsymmetricContents(final byte[] identityKey,
                                      final byte[] sessionKey,
                                      final PublicKey publicKey) {
        try {
            // create a SHA1 digest of the identity key
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(identityKey);

            // generate the digest
            byte[] identityDigest = md.digest();

            // combine the session key with the digest of the identity key
            byte[] asymmetricContents = ArrayUtils.addAll(sessionKey, identityDigest);

            // encrypt the asymmetric contents and return
            OAEPParameterSpec oaepSpec =
                    new OAEPParameterSpec("Sha1", "MGF1", MGF1ParameterSpec.SHA1,
                            new PSource.PSpecified("TCPA".getBytes(StandardCharsets.UTF_8)));

            // initialize the asymmetric cipher using the default OAEP transformation
            Cipher cipher = Cipher.getInstance(EncryptionScheme.OAEP.toString());

            // initialize the cipher using the public spec with the additional OAEP specification
            cipher.init(Cipher.PUBLIC_KEY, publicKey, oaepSpec);

            return cipher.doFinal(asymmetricContents);
        } catch (NoSuchAlgorithmException | IllegalBlockSizeException | NoSuchPaddingException
                | InvalidKeyException | BadPaddingException
                | InvalidAlgorithmParameterException e) {
            throw new CertificateProcessingException(
                    "Encountered error while generating ACA session key: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the IV from the identity request. That is, take the first block of data from the
     * symmetric blob and treat that as the IV. This modifies the original symmetric block.
     *
     * @param identityRequest to extract the IV from
     * @return the IV from the identity request
     */
    public static byte[] extractInitialValue(final IdentityRequest identityRequest) {
        // make a reference to the symmetric blob
        byte[] symmetricBlob = identityRequest.getSymmetricBlob();

        // create the IV
        byte[] iv = new byte[DEFAULT_IV_SIZE];

        // initialize a new symmetric blob with the length of the original minus the IV
        byte[] updatedBlob = new byte[symmetricBlob.length - iv.length];

        // copy the IV out of the original symmetric blob
        System.arraycopy(symmetricBlob, 0, iv, 0, iv.length);

        // copy everything but the IV out of the original blob into the new blob
        System.arraycopy(symmetricBlob, iv.length, updatedBlob, 0, updatedBlob.length);

        // reassign the symmetric blob to the request.
        identityRequest.setSymmetricBlob(updatedBlob);

        return iv;
    }

    /**
     * Generate the Identity Response using the identity credential and the session key.
     *
     * @param credential the identity credential
     * @param symmetricKey generated session key for this request/response chain
     * @return identity response for an identity request
     */
    public static SymmetricAttestation generateAttestation(final X509Certificate credential,
                                             final SymmetricKey symmetricKey) {
        try {
            // initialize the symmetric cipher
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // generate a key specification to initialize the cipher
            SecretKeySpec keySpec = new SecretKeySpec(symmetricKey.getKey(), "AES");

            // fill IV with random bytes
            byte[] credentialIV = generateRandomBytes(DEFAULT_IV_SIZE);

            // create IV encryption parameter specification
            IvParameterSpec ivParameterSpec = new IvParameterSpec(credentialIV);

            // initialize the cipher to decrypt using the symmetric key
            aesCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);

            // encrypt the credential
            byte[] encryptedCredential = aesCipher.doFinal(credential.getEncoded());

            // prepend the IV to the encrypted credential
            byte[] credentialBytes = ArrayUtils.addAll(credentialIV, encryptedCredential);

            // create attestation for identity response that contains the credential
            SymmetricAttestation attestation =
                    new SimpleStructBuilder<>(SymmetricAttestation.class)
                            .set("credential", credentialBytes)
                            .set("algorithm",
                                    new SimpleStructBuilder<>(SymmetricKeyParams.class)
                                            .set("algorithmId", SymmetricKeyParams.ALGORITHM_AES)
                                            .set("encryptionScheme",
                                                    SymmetricKeyParams.SCHEME_CBC_PKCS5PADDING)
                                            .set("signatureScheme", 0).build()).build();

            return attestation;

        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
                | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException
                | CertificateEncodingException exception) {
            throw new CertificateProcessingException(
                    "Encountered error while generating Identity Response: "
                            + exception.getMessage(), exception);
        }
    }

    @SuppressWarnings("magicnumber")
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
        credentialBlob[3] = 0x20;
        System.arraycopy(integrityHmac, 0, credentialBlob, 4, 32);
        for (int i = 0; i < 98; i++) {
            credentialBlob[36 + i] = 0x00;
        }
        System.arraycopy(encryptedSecret, 0, credentialBlob, 36, encryptedSecret.length);
        credentialBlob[134] = 0x00;
        credentialBlob[135] = 0x01;
        System.arraycopy(encryptedSeed, 0, credentialBlob, 136, 256);
        // return the result
        return credentialBlob;
    }

    /**
     * Determines the AK name from the AK Modulus.
     * @param akModulus modulus of an attestation key
     * @return the ak name byte array
     * @throws java.security.NoSuchAlgorithmException Underlying SHA256 method used a bad algorithm
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
     * @param seed random value used to generate the key
     * @param label first portion of message used to generate key
     * @param context second portion of message used to generate key
     * @param sizeInBytes size of key to generate in bytes
     * @return the derived key
     * @throws NoSuchAlgorithmException Wrong crypto algorithm selected
     * @throws java.security.InvalidKeyException Invalid key used
     */
    @SuppressWarnings("magicnumber")
    public static byte[] cryptKDFa(final byte[] seed, final String label, final byte[] context,
                             final int sizeInBytes)
            throws NoSuchAlgorithmException, InvalidKeyException {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(1);
        byte[] counter = b.array();
        // get the label
        String labelWithEnding = label;
        if (label.charAt(label.length() - 1) != "\0".charAt(0)) {
            labelWithEnding = label + "\0";
        }
        byte[] labelBytes = labelWithEnding.getBytes(StandardCharsets.UTF_8);
        b = ByteBuffer.allocate(4);
        b.putInt(sizeInBytes * 8);
        byte[] desiredSizeInBits = b.array();
        int sizeOfMessage = 8 + labelBytes.length;
        if (context != null) {
            sizeOfMessage += context.length;
        }
        byte[] message = new byte[sizeOfMessage];
        int marker = 0;
        System.arraycopy(counter, 0, message, marker, 4);
        marker += 4;
        System.arraycopy(labelBytes, 0, message, marker, labelBytes.length);
        marker += labelBytes.length;
        if (context != null) {
            System.arraycopy(context, 0, message, marker, context.length);
            marker += context.length;
        }
        System.arraycopy(desiredSizeInBits, 0, message, marker, 4);
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
     * @param tpmQuote contains hash values for the quote and the signature
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
     * @param tpmQuote contains hash values for the quote and the signature
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
     * Generates a array of random bytes.
     *
     * @param numberOfBytes
     *            to be generated
     * @return byte array filled with the specified number of bytes.
     */
    public static byte[] generateRandomBytes(final int numberOfBytes) {
        byte[] bytes = new byte[numberOfBytes];
        random.nextBytes(bytes);
        return bytes;
    }

    @SuppressWarnings("magicnumber")
    public static int daysBetween(final Date date1, final Date date2) {
        return (int) ((date2.getTime() - date1.getTime()) / (1000 * 60 * 60 * 24));
    }
}
