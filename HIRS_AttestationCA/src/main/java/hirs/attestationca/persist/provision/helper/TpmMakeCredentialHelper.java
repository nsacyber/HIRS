package hirs.attestationca.persist.provision.helper;

import com.google.protobuf.ByteString;
import hirs.attestationca.persist.exceptions.IdentityProcessingException;
import hirs.utils.HexUtils;

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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.MGF1ParameterSpec;

/**
 * Helper class to create a credential blob during the provisioning process. <div /> The resultant blob will
 * then be sent to the provisioner.
 */
public final class TpmMakeCredentialHelper {

    /** HMAC key Length in bytes. */
    public static final int HMAC_KEY_LENGTH_BITS = 256;

    /** Seed Length in bytes. */
    public static final int SEED_LENGTH = 32;

    /** AES Key Length in bytes. */
    public static final int AES_KEY_LENGTH_BITS = 128;

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
    public static TpmCredential makeCredential(final ParsedTpmPublic ekPub, final ParsedTpmPublic akPub,
                                            final byte[] secret) {
        if (ekPub == null) {
            throw new IllegalStateException("Input EK public area is null");
        }
        if (akPub == null) {
            throw new IllegalStateException("Input AK public area is null");
        }

        return switch (ekPub.alg()) {
            case RSA -> makeCredentialRsa(ekPub, akPub, secret);
            case ECC -> makeCredentialEcc(ekPub, akPub, secret);
            default -> throw new IllegalStateException("Unknown algorithm: " + ekPub.alg());
        };
    }

    private static TpmCredential makeCredentialRsa(final ParsedTpmPublic ekPub, final ParsedTpmPublic akPub,
                                                final byte[] secret) {
        try {
            // generate a random seed
            int seedLenBytes = MessageDigest.getInstance(ekPub.nameAlg().getAlgorithmName()).getDigestLength();
            byte[] seed = ProvisionUtils.generateRandomBytes(seedLenBytes);

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
            byte[] aesKey = ProvisionUtils.cryptKDFa(ekPub.nameAlg(), seed, "STORAGE",
                    akName, null, AES_KEY_LENGTH_BITS);
            byte[] hmacKey = ProvisionUtils.cryptKDFa(ekPub.nameAlg(), seed, "INTEGRITY",
                    null, null, HMAC_KEY_LENGTH_BITS);

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
            byte[] encIdentity = symCipher.doFinal(secretBytes);

            Mac integrityHmac = Mac.getInstance("HmacSHA256");
            integrityHmac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            integrityHmac.update(encIdentity);
            integrityHmac.update(akName);
            byte[] integrity = integrityHmac.doFinal();

            byte[] encSecret = ProvisionUtils.marshalTpm2bEncryptedSecret(encSeed);

            byte[] credentialBlob = assembleIdObject(integrity, encIdentity);
            return assembleVariableCredentialAndSecret(credentialBlob, encSecret);

        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
                 | InvalidKeyException | InvalidAlgorithmParameterException
                 | NoSuchPaddingException e) {
            throw new IdentityProcessingException(
                    "Encountered error while making the identity claim challenge for the provided RSA public keys: "
                            + e.getMessage(), e);
        }
    }

    private static TpmCredential makeCredentialEcc(final ParsedTpmPublic ekPub, final ParsedTpmPublic akPub,
                                                final byte[] secret) {
        try {
            ECPublicKey ek = (ECPublicKey) ekPub.publicKey();

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(ek.getParams());
            KeyPair ephemeral = kpg.generateKeyPair();

            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(ephemeral.getPrivate());
            ka.doPhase(ek, true);
            byte[] z = ka.generateSecret();

            byte[] partyU = ProvisionUtils.convertECPublicKeyToBytes((ECPublicKey) ephemeral.getPublic());
            byte[] partyV = ProvisionUtils.convertECPublicKeyToBytes(ek);

            int seedLenBytes = MessageDigest.getInstance(ekPub.nameAlg().getAlgorithmName()).getDigestLength();
            int seedLenBits = seedLenBytes * Byte.SIZE;
            byte[] seed = ProvisionUtils.cryptKDFe(ekPub.nameAlg(), z, "IDENTITY", partyU, partyV, seedLenBits);

            byte[] akName = TpmNameHelper.computeName(akPub);
            byte[] aesKey = ProvisionUtils.cryptKDFa(ekPub.nameAlg(), seed, "STORAGE",
                    akName, null, AES_KEY_LENGTH_BITS);
            byte[] hmacKey = ProvisionUtils.cryptKDFa(ekPub.nameAlg(), seed, "INTEGRITY",
                    null, null, HMAC_KEY_LENGTH_BITS);

            ByteBuffer lengthBuffer = ByteBuffer.allocate(2);
            lengthBuffer.putShort((short) secret.length);
            byte[] secretLen = lengthBuffer.array();

            byte[] secretBytes = new byte[2 + secret.length];
            System.arraycopy(secretLen, 0, secretBytes, 0, 2);
            System.arraycopy(secret, 0, secretBytes, 2, secret.length);

            Cipher symCipher = Cipher.getInstance("AES/CFB/NoPadding");
            byte[] iv = HexUtils.hexStringToByteArray("00000000000000000000000000000000");
            symCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
            byte[] encIdentity = symCipher.doFinal(secretBytes);

            Mac integrityHmac = Mac.getInstance("HmacSHA256");
            integrityHmac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            integrityHmac.update(encIdentity);
            integrityHmac.update(akName);
            byte[] integrity = integrityHmac.doFinal();

            byte[] encSecret = ProvisionUtils.marshalTpm2bEccPoint((ECPublicKey) ephemeral.getPublic());

            byte[] credentialBlob = assembleIdObject(integrity, encIdentity);
            return assembleVariableCredentialAndSecret(credentialBlob, encSecret);

        } catch (GeneralSecurityException e) {
            throw new IdentityProcessingException(
                    "Encountered error while making ECC credential: " + e.getMessage(), e);
        }
    }

    private static TpmCredential assembleVariableCredentialAndSecret(
            final byte[] credentialBlobTpm2b,
            final byte[] secretTpm2b) {

        if (credentialBlobTpm2b == null || secretTpm2b == null) {
            throw new IllegalArgumentException("credentialBlob and secret must not be null");
        }

        return new TpmCredential(ByteString.copyFrom(credentialBlobTpm2b), ByteString.copyFrom(secretTpm2b));
    }

    private static byte[] assembleIdObject(
            final byte[] outerHmac,
            final byte[] encIdentity) {

        if (outerHmac == null || encIdentity == null) {
            throw new IllegalArgumentException("outerHmac and encIdentity must not be null");
        }

        ByteBuffer body = ByteBuffer.allocate(2 + outerHmac.length + encIdentity.length);
        body.putShort((short) outerHmac.length);
        body.put(outerHmac);
        body.put(encIdentity);

        byte[] bodyBytes = body.array();

        ByteBuffer out = ByteBuffer.allocate(2 + bodyBytes.length);
        out.putShort((short) bodyBytes.length);
        out.put(bodyBytes);

        return out.array();
    }

    public record TpmCredential(ByteString credentialBlobTpm2b, ByteString secretTpm2b) { }
}
