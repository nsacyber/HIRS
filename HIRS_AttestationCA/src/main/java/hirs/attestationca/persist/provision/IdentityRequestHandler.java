package hirs.attestationca.persist.provision;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.exceptions.CertificateProcessingException;
import hirs.attestationca.persist.exceptions.IdentityProcessingException;
import hirs.attestationca.persist.provision.helper.CredentialManagementHelper;
import hirs.attestationca.persist.service.SupplyChainValidationService;
import hirs.structs.converters.SimpleStructBuilder;
import hirs.structs.converters.StructConverter;
import hirs.structs.elements.aca.IdentityRequestEnvelope;
import hirs.structs.elements.aca.IdentityResponseEnvelope;
import hirs.structs.elements.aca.SymmetricAttestation;
import hirs.structs.elements.tpm.EncryptionScheme;
import hirs.structs.elements.tpm.IdentityProof;
import hirs.structs.elements.tpm.IdentityRequest;
import hirs.structs.elements.tpm.SymmetricKey;
import hirs.structs.elements.tpm.SymmetricKeyParams;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Log4j2
public class IdentityRequestHandler extends AbstractRequestHandler {

    /**
     * Container wired ACA private key.
     */
    private final PrivateKey privateKey;
    private int validDays;
    private StructConverter structConverter;
    private CertificateRepository certificateRepository;
    private DeviceRepository deviceRepository;
    private SupplyChainValidationService supplyChainValidationService;
    private X509Certificate acaCertificate;

    /**
     * Constructor.
     * @param structConverter the struct converter
     * @param certificateRepository
     * @param deviceRepository
     * @param supplyChainValidationService the supply chain service
     * @param privateKey
     * @param validDays int for the time in which a certificate is valid.
     * @param acaCertificate object holding the x509 certificate
     */
    public IdentityRequestHandler(final StructConverter structConverter,
                                  final CertificateRepository certificateRepository,
                                  final DeviceRepository deviceRepository,
                                  final SupplyChainValidationService supplyChainValidationService,
                                  final PrivateKey privateKey,
                                  final int validDays, final X509Certificate acaCertificate) {
        super(privateKey, validDays);
        this.structConverter = structConverter;
        this.certificateRepository = certificateRepository;
        this.deviceRepository = deviceRepository;
        this.supplyChainValidationService = supplyChainValidationService;
        this.privateKey = privateKey;
        this.acaCertificate = acaCertificate;
    }

    /**
     * Basic implementation of the ACA processIdentityRequest method.
     *
     * @param identityRequest cannot be null
     * @return an identity response for the specified request
     */
    public byte[] processIdentityRequest(final byte[] identityRequest) {
        log.info("Identity Request Received...");
        if (ArrayUtils.isEmpty(identityRequest)) {
            throw new IllegalArgumentException("The IdentityRequest sent by the client"
                    + " cannot be null or empty.");
        }

        log.debug("received request to process identity request");

        // translate the bytes into the challenge
        IdentityRequestEnvelope challenge =
                structConverter.convert(identityRequest, IdentityRequestEnvelope.class);

        byte[] identityProof = unwrapIdentityRequest(challenge.getRequest());
        // the decrypted symmetric blob should be in the format of an IdentityProof. Use the
        // struct converter to generate it.
        IdentityProof proof = structConverter.convert(identityProof, IdentityProof.class);

        // convert the credential into an actual key.
        log.debug("assembling public endorsement key");
        PublicKey ekPublicKey = null;

        // attempt to find an endorsement credential to validate
        EndorsementCredential endorsementCredential = null;

        // first check the identity request for the endorsement credential
        byte[] ecBytesFromIdentityRequest = proof.getEndorsementCredential();
        if (ArrayUtils.isNotEmpty(ecBytesFromIdentityRequest)) {
            endorsementCredential = CredentialManagementHelper.storeEndorsementCredential(
                    this.certificateRepository, ecBytesFromIdentityRequest);
            try {
                BigInteger publicKeyModulus = Certificate.getPublicKeyModulus(
                        endorsementCredential.getX509Certificate());
                if (publicKeyModulus != null) {
                    ekPublicKey = assemblePublicKey(publicKeyModulus.toByteArray());
                } else {
                    throw new IdentityProcessingException("TPM 1.2 Provisioning requires EK "
                            + "Credentials to be created with RSA");
                }
            } catch (IOException ioEx) {
                log.error("Could not retrieve the public key modulus from the EK cert");
            }
        } else if (ArrayUtils.isNotEmpty(challenge.getEndorsementCredentialModulus())) {
            log.warn("EKC was not in the identity proof from the client. Checking for uploads.");
            // Check if the EC was uploaded
            ekPublicKey =
                    assemblePublicKey(new String(challenge.getEndorsementCredentialModulus()));
            endorsementCredential = getEndorsementCredential(ekPublicKey);
        } else {
            log.warn("Zero-length endorsement credential received in identity request.");
        }

        // get platform credential from the identity request
        List<PlatformCredential> platformCredentials = new LinkedList<>();
        byte[] pcBytesFromIdentityRequest = proof.getPlatformCredential();
        if (ArrayUtils.isNotEmpty(pcBytesFromIdentityRequest)) {
            platformCredentials.add(CredentialManagementHelper.storePlatformCredential(
                    this.certificateRepository, pcBytesFromIdentityRequest));
        } else if (endorsementCredential != null) {
            // if none in the identity request, look for uploaded platform credentials
            log.warn("PC was not in the identity proof from the client. Checking for uploads.");
            platformCredentials.addAll(getPlatformCredentials(endorsementCredential));
        } else {
            // if none in the identity request, look for uploaded platform credentials
            log.warn("Zero-length platform credential received in identity request.");
        }

        log.debug("Processing serialized device info report structure of length {}",
                challenge.getDeviceInfoReportLength());

        DeviceInfoReport deviceInfoReport = (DeviceInfoReport)
                SerializationUtils.deserialize(challenge.getDeviceInfoReport());

        if (deviceInfoReport == null) {
            log.error("Failed to deserialize Device Info Report");
            throw new IdentityProcessingException("Device Info Report failed to deserialize "
                    + "from Identity Request");
        }

        log.info("Processing Device Info Report");
        // store device and device info report.
        String deviceName = deviceInfoReport.getNetworkInfo().getHostname();
        Device device = this.deviceRepository.findByName(deviceName);
        device.setDeviceInfo(deviceInfoReport);

        // perform supply chain validation. Note: It's possible that this should be done earlier
        // in this method.
        SupplyChainValidationSummary summary =
                supplyChainValidationService.validateSupplyChain(endorsementCredential,
                        platformCredentials, device);

        // update the validation result in the device
        device.setSupplyChainValidationStatus(summary.getOverallValidationResult());
        deviceRepository.save(device);
        // check if supply chain validation succeeded.
        // If it did not, do not provide the IdentityResponseEnvelope
        if (summary.getOverallValidationResult() == AppraisalStatus.Status.PASS) {
            IdentityResponseEnvelope identityResponse =
                    generateIdentityResponseEnvelopeAndStoreIssuedCert(challenge,
                            ekPublicKey, endorsementCredential, platformCredentials, device);

            return structConverter.convert(identityResponse);
        } else {
            log.error("Supply chain validation did not succeed. Result is: "
                    + summary.getOverallValidationResult());
            return new byte[]{};
        }
    }

    /**
     * Given a successful supply chain validation, generate an Identity Response envelope and
     * the issued certificate. The issued cert is stored in the database. The identity response
     * envelope is returned, and sent back to the client using the struct converter.
     * @param challenge the identity request envelope
     * @param ekPublicKey the EK public key
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials the set of platform credentials
     * @param device the device associated
     * @return the identity response envelope
     */
    private IdentityResponseEnvelope generateIdentityResponseEnvelopeAndStoreIssuedCert(
            final IdentityRequestEnvelope challenge, final PublicKey ekPublicKey,
            final EndorsementCredential endorsementCredential,
            final List<PlatformCredential> platformCredentials, final Device device) {
        // decrypt the asymmetric / symmetric blobs
        log.debug("unwrapping identity request");
        byte[] identityProof = unwrapIdentityRequest(challenge.getRequest());

        // the decrypted symmetric blob should be in the format of an IdentityProof. Use the
        // struct converter to generate it.
        IdentityProof proof = structConverter.convert(identityProof, IdentityProof.class);

        // generate a session key and convert to byte array
        log.debug("generating symmetric key for response");
        SymmetricKey sessionKey = generateSymmetricKey();

        // generate the asymmetric contents for the identity response
        log.debug("generating asymmetric contents for response");
        byte[] asymmetricContents = generateAsymmetricContents(proof, sessionKey, ekPublicKey);

        // generate the identity credential
        log.debug("generating credential from identity proof");

        // transform the public key struct into a public key
        PublicKey publicKey = assemblePublicKey(proof.getIdentityKey().getStorePubKey().getKey());
        X509Certificate credential = generateCredential(publicKey, endorsementCredential,
                platformCredentials, device.getDeviceInfo()
                        .getNetworkInfo()
                        .getIpAddress()
                        .getHostName(), acaCertificate);

        // generate the attestation using the credential and the key for this session
        log.debug("generating symmetric response");
        SymmetricAttestation attestation = generateAttestation(credential, sessionKey);

        // construct the response with the both the asymmetric contents and the CA attestation
        IdentityResponseEnvelope identityResponse =
                new SimpleStructBuilder<>(IdentityResponseEnvelope.class)
                        .set("asymmetricContents", asymmetricContents)
                        .set("symmetricAttestation", attestation).build();

        // save new attestation certificate
        byte[] derEncodedAttestationCertificate = getDerEncodedCertificate(credential);
        saveAttestationCertificate(this.certificateRepository, derEncodedAttestationCertificate,
                endorsementCredential, platformCredentials, device);

        return identityResponse;
    }

    /**
     * Unwraps a given identityRequest. That is to say, decrypt the asymmetric portion of a data
     * structure to determine the method to decrypt the symmetric portion.
     *
     * @param identityRequest
     *            to be decrypted
     * @return the decrypted symmetric portion of an identity request.
     */
    private byte[] unwrapIdentityRequest(final byte[] identityRequest) {
        IdentityRequest request = structConverter.convert(identityRequest, IdentityRequest.class);

        // in case the TPM did not specify the IV, it must be extracted from the symmetric blob.
        // the IV will then be the the first block of the cipher text.
        final byte[] iv;
        SymmetricKeyParams symmetricKeyParams = request.getSymmetricAlgorithm();
        if (symmetricKeyParams != null && symmetricKeyParams.getParams() != null) {
            iv = symmetricKeyParams.getParams().getIv();
        } else {
            iv = extractInitialValue(request);
        }

        // determine the encryption scheme from the algorithm
        EncryptionScheme asymmetricScheme =
                EncryptionScheme.fromInt(request.getAsymmetricAlgorithm().getEncryptionScheme());

        // decrypt the asymmetric blob
        byte[] decryptedAsymmetricBlob =
                decryptAsymmetricBlob(request.getAsymmetricBlob(), asymmetricScheme);

        // construct our symmetric key structure from the decrypted asymmetric blob
        SymmetricKey symmetricKey =
                structConverter.convert(decryptedAsymmetricBlob, SymmetricKey.class);

        byte[] decryptedSymmetricBlob =
                decryptSymmetricBlob(request.getSymmetricBlob(), symmetricKey.getKey(), iv,
                        "AES/CBC/PKCS5Padding");

        // decrypt the symmetric blob
        return decryptedSymmetricBlob;
    }

    /**
     * Gets the Endorsement Credential from the DB given the EK public key.
     * @param ekPublicKey the EK public key
     * @return the Endorsement credential, if found, otherwise null
     */
    private EndorsementCredential getEndorsementCredential(final PublicKey ekPublicKey) {
        log.debug("Searching for endorsement credential based on public key: " + ekPublicKey);

        if (ekPublicKey == null) {
            throw new IllegalArgumentException("Cannot look up an EC given a null public key");
        }

        EndorsementCredential credential = null;

        try {
            credential = certificateRepository.findByPublicKeyModulusHexValue(Certificate
                    .getPublicKeyModulus(ekPublicKey)
                    .toString());
        } catch (IOException ioEx) {
            log.error("Could not extract public key modulus", ioEx);
        }

        if (credential == null) {
            log.warn("Unable to find endorsement credential for public key.");
        } else {
            log.debug("Endorsement credential found.");
        }

        return credential;
    }

    private List<PlatformCredential> getPlatformCredentials(final EndorsementCredential ec) {
        List<PlatformCredential> credentials = null;

        if (ec == null) {
            log.warn("Cannot look for platform credential(s).  Endorsement credential was null.");
        } else {
            log.debug("Searching for platform credential(s) based on holder serial number: "
                    + ec.getSerialNumber());
            credentials = this.certificateRepository.getByHolderSerialNumber(ec.getSerialNumber());
            if (credentials == null || credentials.isEmpty()) {
                log.warn("No platform credential(s) found");
            } else {
                log.debug("Platform Credential(s) found: " + credentials.size());
            }
        }

        return credentials;
    }

    /**
     * Will attempt to decrypt the asymmetric blob that originated from an
     * {@link hirs.structs.elements.tpm.IdentityRequest} using the cipher transformation.
     *
     * @param asymmetricBlob to be decrypted
     * @param scheme to decrypt with
     * @return decrypted blob
     */
    private byte[] decryptAsymmetricBlob(final byte[] asymmetricBlob, final EncryptionScheme scheme) {
        try {
            // create a cipher from the specified transformation
            Cipher cipher = Cipher.getInstance(scheme.toString());

            switch (scheme) {
                case OAEP:
                    OAEPParameterSpec spec =
                            new OAEPParameterSpec("Sha1", "MGF1", MGF1ParameterSpec.SHA1,
                                    new PSource.PSpecified("".getBytes()));

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
    private byte[] decryptSymmetricBlob(final byte[] symmetricBlob, final byte[] symmetricKey,
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

    private SymmetricKey generateSymmetricKey() {
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
     * Generate asymmetric contents part of the identity response.
     *
     * @param proof identity requests symmetric contents, otherwise, the identity proof
     * @param symmetricKey identity response session key
     * @param publicKey of the EK certificate contained within the identity proof
     * @return encrypted asymmetric contents
     */
    byte[] generateAsymmetricContents(final IdentityProof proof,
                                      final SymmetricKey symmetricKey,
                                      final PublicKey publicKey) {
        try {
            // obtain the identity key from the identity proof
            byte[] identityKey = structConverter.convert(proof.getIdentityKey());
            byte[] sessionKey = structConverter.convert(symmetricKey);

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
                            new PSource.PSpecified("TCPA".getBytes()));

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
    private byte[] extractInitialValue(final IdentityRequest identityRequest) {

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
    SymmetricAttestation generateAttestation(final X509Certificate credential,
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
}
