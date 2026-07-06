package hirs.attestationca.persist.provision.helper;

import com.google.protobuf.ByteString;
import hirs.attestationca.persist.exceptions.IdentityProcessingException;
import hirs.utils.HexUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.MGF1ParameterSpec;

/**
 * Helper class to create a credential blob during the provisioning process. <div /> The resultant blob will
 * then be sent to the provisioner.
 */
public final class TpmMakeCredentialHelper {
    /** HMAC Size Length in bytes. */
    public static final int HMAC_SIZE_LENGTH_BYTES = 2;

    /** HMAC key Length in bytes. */
    public static final int HMAC_KEY_LENGTH_BYTES = 32;

    /** Seed Length in bytes. */
    public static final int SEED_LENGTH = 32;

    /** AES Key Length in bytes. */
    public static final int AES_KEY_LENGTH_BYTES = 16;

    /** Prevent instantiation. */
    private TpmMakeCredentialHelper() { }

    /**
     * Method to construct a credential blob given parsed EK and AK public areas, and a shared secret. The
     * credential assembly takes into account the algorithm types of the parsed objects.
     * @param ekPub the parsed EK public area
     * @param akPub the parsed AK public area
     * @param secret the shared secret
     * @return a {@link ByteString} containing the assembled credential blob
     */
    public static ByteString makeCredential(final ParsedTpmPublic ekPub, final ParsedTpmPublic akPub,
                                            final byte[] secret) {
        if (ekPub == null) {
            throw new IllegalStateException("Input EK public area is null");
        }
        if (akPub == null) {
            throw new IllegalStateException("Input AK public area is null");
        }

        return switch (ekPub.alg()) {
            case RSA, RSAPSS, RSASSA, RSAES -> makeCredentialRsa(ekPub, akPub, secret);
            case ECC, ECDH, ECDSA -> makeCredentialEcc(ekPub, akPub, secret);
            default -> throw new IllegalStateException("Unknown algorithm: " + ekPub.alg());
        };
    }

    private static ByteString makeCredentialRsa(final ParsedTpmPublic ekPub, final ParsedTpmPublic akPub,
                                                final byte[] secret) {
        // generate a random 32 byte seed
        byte[] seed = ProvisionUtils.generateRandomBytes(SEED_LENGTH);

        try {
            // encrypt seed with endorsement RSA Public Key
            Cipher asymCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

            OAEPParameterSpec oaepSpec = new OAEPParameterSpec("SHA-256", "MGF1",
                    MGF1ParameterSpec.SHA256, new PSource.PSpecified("IDENTITY\0".getBytes(StandardCharsets.UTF_8)));

            asymCipher.init(Cipher.PUBLIC_KEY, ekPub.publicKey(), oaepSpec);
            asymCipher.update(seed);
            byte[] encSeed = asymCipher.doFinal();

            // generate ak name
            byte[] akName = TpmNameHelper.computeName(akPub);

            // generate AES and HMAC keys from seed
            byte[] aesKey = ProvisionUtils.cryptKDFa(seed, "STORAGE", akName, AES_KEY_LENGTH_BYTES);
            byte[] hmacKey = ProvisionUtils.cryptKDFa(seed, "INTEGRITY", null, HMAC_KEY_LENGTH_BYTES);

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

            // return ordered blob of assembled credential
            byte[] bytesToReturn = ProvisionUtils.assembleCredential(topSize, integrity, encSecret, encSeed);
            return ByteString.copyFrom(bytesToReturn);

        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
                 | InvalidKeyException | InvalidAlgorithmParameterException
                 | NoSuchPaddingException e) {
            throw new IdentityProcessingException(
                    "Encountered error while making the identity claim challenge for the provided RSA public keys: "
                            + e.getMessage(), e);
        }
    }

    private static ByteString makeCredentialEcc(final ParsedTpmPublic ekPub, final ParsedTpmPublic akPub,
                                                final byte[] secret) {
        throw new RuntimeException("Not implemented yet");
    }
}
