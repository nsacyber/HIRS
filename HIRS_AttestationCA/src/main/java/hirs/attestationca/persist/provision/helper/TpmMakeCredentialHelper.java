package hirs.attestationca.persist.provision.helper;

import com.google.protobuf.ByteString;
import hirs.attestationca.persist.enums.TcgAlgorithm;
import hirs.attestationca.persist.enums.TpmMlKemParameterSet;
import hirs.attestationca.persist.exceptions.IdentityProcessingException;
import hirs.utils.HexUtils;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.crypto.kems.MLKEMGenerator;
import org.bouncycastle.crypto.params.MLKEMParameters;
import org.bouncycastle.crypto.params.MLKEMPublicKeyParameters;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.MGF1ParameterSpec;

/**
 * Helper class to create a credential blob during the provisioning process. <div /> The resultant blob will
 * then be sent to the provisioner.
 */
public final class TpmMakeCredentialHelper {
    private static final String IDENTITY_LABEL = "IDENTITY";
    private static final String STORAGE_LABEL = "STORAGE";
    private static final String INTEGRITY_LABEL = "INTEGRITY";
    private static final int AES_128_KEY_BITS = 128;
    private static final int AES_192_KEY_BITS = 192;
    private static final int AES_256_KEY_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
        if (secret == null) {
            throw new IllegalStateException("Input credential value is null");
        }

        validateSymmetricDefinition(ekPub.symmetricDefinition().orElseThrow());

        try {
            SeedEncapsulation seedEncapsulation = switch (ekPub.alg()) {
                case RSA -> encapsulateSeedRsa(ekPub);
                case ECC -> encapsulateSeedEcc(ekPub);
                case MLKEM -> encapsulateSeedMlKem(ekPub);
                default -> throw new IllegalStateException("Unknown EK algorithm: " + ekPub.alg());
            };
            return protectCredential(ekPub, akPub, secret, seedEncapsulation);
        } catch (GeneralSecurityException e) {
            throw new IdentityProcessingException(
                    "Encountered error while making a credential for " + ekPub.alg(), e);
        }
    }

    private static SeedEncapsulation encapsulateSeedRsa(final ParsedTpmPublic ekPub)
            throws GeneralSecurityException {
        int seedLength = digestLengthBytes(ekPub.nameAlg());
        byte[] seed = ProvisionUtils.generateRandomBytes(seedLength);
        String digestName = ekPub.nameAlg().getAlgorithmName();
        Cipher asymCipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        OAEPParameterSpec oaepSpec = new OAEPParameterSpec(digestName, "MGF1",
                mgf1ParameterSpec(ekPub.nameAlg()),
                new PSource.PSpecified((IDENTITY_LABEL + "\0").getBytes(StandardCharsets.UTF_8)));
        asymCipher.init(Cipher.PUBLIC_KEY, ekPub.publicKey(), oaepSpec);
        byte[] ciphertext = asymCipher.doFinal(seed);
        return new SeedEncapsulation(seed, ProvisionUtils.marshalTpm2bEncryptedSecret(ciphertext));
    }

    private static SeedEncapsulation encapsulateSeedEcc(final ParsedTpmPublic ekPub)
            throws GeneralSecurityException {
        ECPublicKey ek = (ECPublicKey) ekPub.publicKey();
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(ek.getParams());
        KeyPair ephemeral = keyPairGenerator.generateKeyPair();

        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(ephemeral.getPrivate());
        keyAgreement.doPhase(ek, true);
        byte[] sharedSecret = keyAgreement.generateSecret();

        byte[] partyU = ProvisionUtils.convertECPublicKeyToBytes((ECPublicKey) ephemeral.getPublic());
        byte[] partyV = ProvisionUtils.convertECPublicKeyToBytes(ek);
        int seedLengthBits = digestLengthBytes(ekPub.nameAlg()) * Byte.SIZE;
        byte[] seed = ProvisionUtils.cryptKDFe(
                ekPub.nameAlg(), sharedSecret, IDENTITY_LABEL, partyU, partyV, seedLengthBits);
        byte[] encryptedSecret = ProvisionUtils.marshalTpm2bEccPoint((ECPublicKey) ephemeral.getPublic());
        return new SeedEncapsulation(seed, encryptedSecret);
    }

    private static SeedEncapsulation encapsulateSeedMlKem(final ParsedTpmPublic ekPub)
            throws GeneralSecurityException {
        if (!(ekPub instanceof ParsedTpmPublic.MlKemParsedTpmPublic mlKemPublic)) {
            throw new IllegalArgumentException("TPM_ALG_MLKEM public area has unexpected parsed type");
        }

        TpmMlKemParameterSet parameterSet = mlKemPublic.params().parameterSet();
        byte[] publicKey = mlKemPublic.params().encodedPublicKey();
        MLKEMPublicKeyParameters publicKeyParameters = new MLKEMPublicKeyParameters(
                toBouncyCastleParameters(parameterSet), publicKey);
        SecretWithEncapsulation encapsulation = new MLKEMGenerator(SECURE_RANDOM)
                .generateEncapsulated(publicKeyParameters);
        try {
            byte[] ciphertext = encapsulation.getEncapsulation();
            if (ciphertext.length != parameterSet.getCiphertextSize()) {
                throw new GeneralSecurityException("Unexpected " + parameterSet.getAlgorithmName()
                        + " ciphertext length: " + ciphertext.length);
            }
            int seedLengthBits = digestLengthBytes(ekPub.nameAlg()) * Byte.SIZE;
            byte[] seed = ProvisionUtils.cryptKDFa(
                    ekPub.nameAlg(),
                    encapsulation.getSecret(),
                    IDENTITY_LABEL,
                    ciphertext,
                    publicKey,
                    seedLengthBits);
            return new SeedEncapsulation(seed, ProvisionUtils.marshalTpm2bEncryptedSecret(ciphertext));
        } finally {
            try {
                encapsulation.destroy();
            } catch (DestroyFailedException e) {
                throw new GeneralSecurityException("Unable to clear ML-KEM shared secret", e);
            }
        }
    }

    private static TpmCredential protectCredential(
            final ParsedTpmPublic ekPub,
            final ParsedTpmPublic akPub,
            final byte[] secret,
            final SeedEncapsulation seedEncapsulation) throws GeneralSecurityException {
        byte[] akName = TpmNameHelper.computeName(akPub);
        int symmetricKeyBits = ekPub.symmetricDefinition().orElseThrow().keyBits();
        int digestBits = digestLengthBytes(ekPub.nameAlg()) * Byte.SIZE;
        byte[] symmetricKey = ProvisionUtils.cryptKDFa(
                ekPub.nameAlg(), seedEncapsulation.seed(), STORAGE_LABEL, akName, null, symmetricKeyBits);
        byte[] hmacKey = ProvisionUtils.cryptKDFa(
                ekPub.nameAlg(), seedEncapsulation.seed(), INTEGRITY_LABEL, null, null, digestBits);

        ByteBuffer credentialValue = ByteBuffer.allocate(Short.BYTES + secret.length);
        credentialValue.putShort((short) secret.length);
        credentialValue.put(secret);

        Cipher symmetricCipher = Cipher.getInstance("AES/CFB/NoPadding");
        byte[] iv = HexUtils.hexStringToByteArray("00000000000000000000000000000000");
        symmetricCipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(symmetricKey, "AES"), new IvParameterSpec(iv));
        byte[] encryptedIdentity = symmetricCipher.doFinal(credentialValue.array());

        String hmacAlgorithm = hmacAlgorithm(ekPub.nameAlg());
        Mac integrityHmac = Mac.getInstance(hmacAlgorithm);
        integrityHmac.init(new SecretKeySpec(hmacKey, hmacAlgorithm));
        integrityHmac.update(encryptedIdentity);
        integrityHmac.update(akName);
        byte[] integrity = integrityHmac.doFinal();

        byte[] credentialBlob = assembleIdObject(integrity, encryptedIdentity);
        return new TpmCredential(
                ByteString.copyFrom(credentialBlob),
                ByteString.copyFrom(seedEncapsulation.encryptedSecret()));
    }

    private static void validateSymmetricDefinition(
            final ParsedTpmPublic.SymmetricDefinition symmetricDefinition) {
        if (symmetricDefinition.algorithm() != TcgAlgorithm.AES
                || symmetricDefinition.mode() != TcgAlgorithm.CFB) {
            throw new IdentityProcessingException("MakeCredential requires an AES-CFB restricted-decryption EK");
        }
        int keyBits = symmetricDefinition.keyBits();
        if (keyBits != AES_128_KEY_BITS
                && keyBits != AES_192_KEY_BITS
                && keyBits != AES_256_KEY_BITS) {
            throw new IdentityProcessingException("Unsupported AES key size for MakeCredential: " + keyBits);
        }
    }

    private static int digestLengthBytes(final TcgAlgorithm hashAlgorithm)
            throws GeneralSecurityException {
        return MessageDigest.getInstance(hashAlgorithm.getAlgorithmName()).getDigestLength();
    }

    private static String hmacAlgorithm(final TcgAlgorithm hashAlgorithm) {
        return switch (hashAlgorithm) {
            case SHA256 -> "HmacSHA256";
            case SHA384 -> "HmacSHA384";
            case SHA512 -> "HmacSHA512";
            default -> throw new IdentityProcessingException(
                    "Unsupported MakeCredential name algorithm: " + hashAlgorithm);
        };
    }

    private static MGF1ParameterSpec mgf1ParameterSpec(final TcgAlgorithm hashAlgorithm) {
        return switch (hashAlgorithm) {
            case SHA256 -> MGF1ParameterSpec.SHA256;
            case SHA384 -> MGF1ParameterSpec.SHA384;
            case SHA512 -> MGF1ParameterSpec.SHA512;
            default -> throw new IdentityProcessingException(
                    "Unsupported MakeCredential name algorithm: " + hashAlgorithm);
        };
    }

    private static MLKEMParameters toBouncyCastleParameters(
            final TpmMlKemParameterSet parameterSet) {
        return switch (parameterSet) {
            case ML_KEM_512 -> MLKEMParameters.ml_kem_512;
            case ML_KEM_768 -> MLKEMParameters.ml_kem_768;
            case ML_KEM_1024 -> MLKEMParameters.ml_kem_1024;
        };
    }

    private static byte[] assembleIdObject(
            final byte[] outerHmac,
            final byte[] encryptedIdentity) {
        ByteBuffer body = ByteBuffer.allocate(Short.BYTES + outerHmac.length + encryptedIdentity.length);
        body.putShort((short) outerHmac.length);
        body.put(outerHmac);
        body.put(encryptedIdentity);

        ByteBuffer result = ByteBuffer.allocate(Short.BYTES + body.capacity());
        result.putShort((short) body.capacity());
        result.put(body.array());
        return result.array();
    }

    private record SeedEncapsulation(byte[] seed, byte[] encryptedSecret) { }

    /**
     * TPM2_MakeCredential output parameters.
     *
     * @param credentialBlobTpm2b TPM2B_ID_OBJECT
     * @param secretTpm2b TPM2B_ENCRYPTED_SECRET
     */
    public record TpmCredential(ByteString credentialBlobTpm2b, ByteString secretTpm2b) { }
}
