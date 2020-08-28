package hirs.attestationca;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.exceptions.CertificateProcessingException;
import hirs.attestationca.exceptions.IdentityProcessingException;
import hirs.attestationca.exceptions.UnexpectedServerException;
import hirs.attestationca.service.SupplyChainValidationService;
import hirs.data.persist.AppraisalStatus;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.info.FirmwareInfo;
import hirs.data.persist.info.HardwareInfo;
import hirs.data.persist.info.NetworkInfo;
import hirs.data.persist.info.OSInfo;
import hirs.data.persist.SupplyChainValidationSummary;
import hirs.data.persist.info.TPMInfo;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.IssuedAttestationCertificate;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.data.service.DeviceRegister;
import hirs.persist.CertificateManager;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.DBManager;
import hirs.persist.DeviceManager;
import hirs.persist.TPM2ProvisionerState;
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
import hirs.utils.HexUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.util.SerializationUtils;

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
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides base implementation of common tasks of an ACA that are required for attestation of an
 * Identity Request.
 */
public abstract class AbstractAttestationCertificateAuthority
                                                    implements AttestationCertificateAuthority {

    /**
     * Logger instance for for subclass instances.
     */
    protected static final Logger LOG = LogManager.getLogger(AttestationCertificateAuthority.class);

    /**
     * Defines the well known exponent. https://en.wikipedia.org/wiki/65537_(number)#Applications
     */
    private static final BigInteger EXPONENT = new BigInteger("010001",
            AttestationCertificateAuthority.DEFAULT_IV_SIZE);
    private static final String CATALINA_HOME = System.getProperty("catalina.base");
    private static final String TOMCAT_UPLOAD_DIRECTORY
            = "/webapps/HIRS_AttestationCA/upload/";
    private static final String PCR_UPLOAD_FOLDER
            = CATALINA_HOME + TOMCAT_UPLOAD_DIRECTORY;

    /**
     * Number of bytes to include in the TPM2.0 nonce.
     */
    public static final int NONCE_LENGTH = 20;

    private static final int SEED_LENGTH = 32;
    private static final int MAX_SECRET_LENGTH = 32;
    private static final int RSA_MODULUS_LENGTH = 256;
    private static final int AES_KEY_LENGTH_BYTES = 16;
    private static final int HMAC_KEY_LENGTH_BYTES = 32;
    private static final int HMAC_SIZE_LENGTH_BYTES = 2;
    private static final int TPM2_CREDENTIAL_BLOB_SIZE = 392;

    // Constants used to parse out the ak name from the ak public data. Used in generateAkName
    private static final String AK_NAME_PREFIX = "000b";
    private static final String AK_NAME_HASH_PREFIX =
            "0001000b00050072000000100014000b0800000000000100";
    private static final String TPM_SIGNATURE_ALG = "sha";

    private static final int MAC_BYTES = 6;

    /**
     * Container wired ACA private key.
     */
    private final PrivateKey privateKey;

    /**
     * Container wired ACA certificate.
     */
    private final X509Certificate acaCertificate;

    /**
     * Container wired {@link StructConverter} to be used in serialization / deserialization of TPM
     * data structures.
     */
    private final StructConverter structConverter;

    /**
     * A handle to the service used to validate the supply chain.
     */
    private final SupplyChainValidationService supplyChainValidationService;

    /**
     * Container wired application configuration property identifying the number of days that
     * certificates issued by this ACA are valid for.
     */
    private final Integer validDays;

    private final CertificateManager certificateManager;
    private final ReferenceManifestManager referenceManifestManager;
    private final DeviceRegister deviceRegister;
    private final DeviceManager deviceManager;
    private final DBManager<TPM2ProvisionerState> tpm2ProvisionerStateDBManager;
    private String tpmQuoteHash = "";
    private String tpmQuoteSignature = "";
    private String pcrValues;

    /**
     * Constructor.
     * @param supplyChainValidationService the supply chain service
     * @param privateKey the ACA private key
     * @param acaCertificate the ACA certificate
     * @param structConverter the struct converter
     * @param certificateManager the certificate manager
     * @param referenceManifestManager the Reference Manifest manager
     * @param deviceRegister the device register
     * @param validDays the number of days issued certs are valid
     * @param deviceManager the device manager
     * @param tpm2ProvisionerStateDBManager the DBManager for persisting provisioner state
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public AbstractAttestationCertificateAuthority(
            final SupplyChainValidationService supplyChainValidationService,
            final PrivateKey privateKey, final X509Certificate acaCertificate,
            final StructConverter structConverter,
            final CertificateManager certificateManager,
            final ReferenceManifestManager referenceManifestManager,
            final DeviceRegister deviceRegister, final int validDays,
            final DeviceManager deviceManager,
            final DBManager<TPM2ProvisionerState> tpm2ProvisionerStateDBManager) {
        this.supplyChainValidationService = supplyChainValidationService;
        this.privateKey = privateKey;
        this.acaCertificate = acaCertificate;
        this.structConverter = structConverter;
        this.certificateManager = certificateManager;
        this.referenceManifestManager = referenceManifestManager;
        this.deviceRegister = deviceRegister;
        this.validDays = validDays;
        this.deviceManager = deviceManager;
        this.tpm2ProvisionerStateDBManager = tpm2ProvisionerStateDBManager;
    }

    /**
     * Basic implementation of the ACA processIdentityRequest method.
     *
     * @param identityRequest cannot be null
     * @return an identity response for the specified request
     */
    @Override
    public byte[] processIdentityRequest(final byte[] identityRequest) {
        if (ArrayUtils.isEmpty(identityRequest)) {
            throw new IllegalArgumentException("The IdentityRequest sent by the client"
                    + " cannot be null or empty.");
        }

        LOG.debug("received request to process identity request");

        // translate the bytes into the challenge
        IdentityRequestEnvelope challenge =
                structConverter.convert(identityRequest, IdentityRequestEnvelope.class);

        byte[] identityProof = unwrapIdentityRequest(challenge.getRequest());
        // the decrypted symmetric blob should be in the format of an IdentityProof. Use the
        // struct converter to generate it.
        IdentityProof proof = structConverter.convert(identityProof, IdentityProof.class);

        // convert the credential into an actual key.
        LOG.debug("assembling public endorsement key");
        PublicKey ekPublicKey = null;

        // attempt to find an endorsement credential to validate
        EndorsementCredential endorsementCredential = null;

        // first check the identity request for the endorsement credential
        byte[] ecBytesFromIdentityRequest = proof.getEndorsementCredential();
        if (ArrayUtils.isNotEmpty(ecBytesFromIdentityRequest)) {
            endorsementCredential = CredentialManagementHelper.storeEndorsementCredential(
                    this.certificateManager, ecBytesFromIdentityRequest
            );
            try {
                BigInteger publicKeyModulus = Certificate.getPublicKeyModulus(
                        endorsementCredential.getX509Certificate());
                if (publicKeyModulus != null) {
                    ekPublicKey = assemblePublicKey(publicKeyModulus.toByteArray());
                } else {
                    throw new IdentityProcessingException("TPM 1.2 Provisioning requires EK "
                            + "Credentials to be created with RSA");
                }
            } catch (IOException e) {
                LOG.error("Could not retrieve the public key modulus from the EK cert");
            }
        } else if (ArrayUtils.isNotEmpty(challenge.getEndorsementCredentialModulus())) {
            LOG.warn("EKC was not in the identity proof from the client. Checking for uploads.");
            // Check if the EC was uploaded
            ekPublicKey =
                    assemblePublicKey(new String(challenge.getEndorsementCredentialModulus()));
            endorsementCredential = getEndorsementCredential(ekPublicKey);
        } else {
            LOG.warn("Zero-length endorsement credential received in identity request.");
        }

        // get platform credential from the identity request
        HashSet<PlatformCredential> platformCredentials = new HashSet<>();
        byte[] pcBytesFromIdentityRequest = proof.getPlatformCredential();
        if (ArrayUtils.isNotEmpty(pcBytesFromIdentityRequest)) {
            platformCredentials.add(CredentialManagementHelper.storePlatformCredential(
                    this.certificateManager, pcBytesFromIdentityRequest
            ));
        } else if (endorsementCredential != null) {
            // if none in the identity request, look for uploaded platform credentials
            LOG.warn("PC was not in the identity proof from the client. Checking for uploads.");
            platformCredentials.addAll(getPlatformCredentials(endorsementCredential));
        } else {
            // if none in the identity request, look for uploaded platform credentials
            LOG.warn("Zero-length platform credential received in identity request.");
        }

        LOG.debug("Processing serialized device info report structure of length {}",
                challenge.getDeviceInfoReportLength());

        DeviceInfoReport deviceInfoReport = (DeviceInfoReport)
                SerializationUtils.deserialize(challenge.getDeviceInfoReport());

        if (deviceInfoReport == null) {
            LOG.error("Failed to deserialize Device Info Report");
            throw new IdentityProcessingException("Device Info Report failed to deserialize "
                    + "from Identity Request");
        }

        LOG.info("Processing Device Info Report");
        // store device and device info report.
        Device device = this.deviceRegister.saveOrUpdateDevice(deviceInfoReport);

        // perform supply chain validation. Note: It's possible that this should be done earlier
        // in this method.
        SupplyChainValidationSummary summary =
                supplyChainValidationService.validateSupplyChain(endorsementCredential,
                        platformCredentials, device);

        // update the validation result in the device
        device.setSupplyChainStatus(summary.getOverallValidationResult());
        deviceManager.updateDevice(device);
        LOG.error("This is the device id? {} ", device.getId());
        // check if supply chain validation succeeded.
        // If it did not, do not provide the IdentityResponseEnvelope
        if (summary.getOverallValidationResult() == AppraisalStatus.Status.PASS) {
            IdentityResponseEnvelope identityResponse =
                generateIdentityResponseEnvelopeAndStoreIssuedCert(challenge,
                ekPublicKey, endorsementCredential, platformCredentials, device);

            return structConverter.convert(identityResponse);
        } else {
            LOG.error("Supply chain validation did not succeed. Result is: "
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
            final Set<PlatformCredential> platformCredentials, final Device device) {
        // decrypt the asymmetric / symmetric blobs
        LOG.debug("unwrapping identity request");
        byte[] identityProof = unwrapIdentityRequest(challenge.getRequest());

        // the decrypted symmetric blob should be in the format of an IdentityProof. Use the
        // struct converter to generate it.
        IdentityProof proof = structConverter.convert(identityProof, IdentityProof.class);

        // generate a session key and convert to byte array
        LOG.debug("generating symmetric key for response");
        SymmetricKey sessionKey = generateSymmetricKey();

        // generate the asymmetric contents for the identity response
        LOG.debug("generating asymmetric contents for response");
        byte[] asymmetricContents = generateAsymmetricContents(proof, sessionKey, ekPublicKey);

        // generate the identity credential
        LOG.debug("generating credential from identity proof");
        // transform the public key struct into a public key
        PublicKey publicKey = assemblePublicKey(proof.getIdentityKey().getStorePubKey().getKey());
        X509Certificate credential = generateCredential(publicKey, endorsementCredential,
                platformCredentials, device.getDeviceInfo()
                        .getNetworkInfo()
                        .getIpAddress()
                        .getHostName());

        // generate the attestation using the credential and the key for this session
        LOG.debug("generating symmetric response");
        SymmetricAttestation attestation = generateAttestation(credential, sessionKey);

        // construct the response with the both the asymmetric contents and the CA attestation
        IdentityResponseEnvelope identityResponse =
                new SimpleStructBuilder<>(IdentityResponseEnvelope.class)
                        .set("asymmetricContents", asymmetricContents)
                        .set("symmetricAttestation", attestation).build();

        // save new attestation certificate
        byte[] derEncodedAttestationCertificate = getDerEncodedCertificate(credential);
        saveAttestationCertificate(derEncodedAttestationCertificate, endorsementCredential,
                platformCredentials, device);

        return identityResponse;
    }

    /**
     * Basic implementation of the ACA processIdentityClaimTpm2 method. Parses the claim,
     * stores the device info, performs supply chain validation, generates a nonce,
     * and wraps that nonce with the make credential process before returning it to the client.
     *            attCert.setPcrValues(pcrValues);

     * @param identityClaim the request to process, cannot be null
     * @return an identity claim response for the specified request containing a wrapped blob
     */
    @Override
    public byte[] processIdentityClaimTpm2(final byte[] identityClaim) {

        LOG.debug("Got identity claim");

        if (ArrayUtils.isEmpty(identityClaim)) {
            LOG.error("Identity claim empty throwing exception.");
            throw new IllegalArgumentException("The IdentityClaim sent by the client"
                    + " cannot be null or empty.");
        }

        // attempt to deserialize Protobuf IdentityClaim
        ProvisionerTpm2.IdentityClaim claim = parseIdentityClaim(identityClaim);

        // parse the EK Public key from the IdentityClaim once for use in supply chain validation
        // and later tpm20MakeCredential function
        RSAPublicKey ekPub = parsePublicKey(claim.getEkPublicArea().toByteArray());

        AppraisalStatus.Status validationResult = doSupplyChainValidation(claim, ekPub);
        if (validationResult == AppraisalStatus.Status.PASS) {

            RSAPublicKey akPub = parsePublicKey(claim.getAkPublicArea().toByteArray());
            byte[] nonce = generateRandomBytes(NONCE_LENGTH);
            ByteString blobStr = tpm20MakeCredential(ekPub, akPub, nonce);

            String strNonce = HexUtils.byteArrayToHexString(nonce);
            LOG.info("Sending nonce: " + strNonce);
            LOG.info("Persisting claim of length: " + identityClaim.length);

            tpm2ProvisionerStateDBManager.save(new TPM2ProvisionerState(nonce, identityClaim));

            // Package response
            ProvisionerTpm2.IdentityClaimResponse response
                    = ProvisionerTpm2.IdentityClaimResponse.newBuilder()
                    .setCredentialBlob(blobStr).build();

            return response.toByteArray();
        } else {
            LOG.error("Supply chain validation did not succeed. Result is: "
                    + validationResult);
            return new byte[]{};
        }
    }

    /**
     * Performs supply chain validation.
     *
     * @param claim the identity claim
     * @param ekPub the public endorsement key
     * @return the {@link AppraisalStatus} of the supply chain validation
     */
    private AppraisalStatus.Status doSupplyChainValidation(
            final ProvisionerTpm2.IdentityClaim claim, final PublicKey ekPub) {
        // attempt to find an endorsement credential to validate
        EndorsementCredential endorsementCredential = parseEcFromIdentityClaim(claim, ekPub);

        // attempt to find platform credentials to validate
        Set<PlatformCredential> platformCredentials = parsePcsFromIdentityClaim(claim,
                endorsementCredential);

        // Parse and save device info
        Device device = processDeviceInfo(claim);

        // perform supply chain validation
        SupplyChainValidationSummary summary = supplyChainValidationService.validateSupplyChain(
                endorsementCredential, platformCredentials, device);
        device.setSummaryId(summary.getId().toString());
        // update the validation result in the device
        AppraisalStatus.Status validationResult = summary.getOverallValidationResult();
        device.setSupplyChainStatus(validationResult);
        deviceManager.updateDevice(device);
        return validationResult;
    }

    /**
     * Performs supply chain validation for just the quote under Firmware validation.
     * Performed after main supply chain validation and a certificate request.
     *
     * @param device associated device to validate.
     * @return the {@link AppraisalStatus} of the supply chain validation
     */
    private AppraisalStatus.Status doQuoteValidation(final Device device) {
        // perform supply chain validation
        SupplyChainValidationSummary scvs = supplyChainValidationService.validateQuote(
                device);
        AppraisalStatus.Status validationResult;

        // either validation wasn't enabled or device already failed
        if (scvs == null) {
            // this will just allow for the certificate to be saved.
            validationResult = AppraisalStatus.Status.PASS;
        } else {
            // update the validation result in the device
            validationResult = scvs.getOverallValidationResult();
            device.setSupplyChainStatus(validationResult);
            deviceManager.updateDevice(device);
        }

        return validationResult;
    }

    /**
     * Basic implementation of the ACA processCertificateRequest method.
     * Parses the nonce, validates its correctness, generates the signed,
     * public attestation certificate, stores it, and returns it to the client.
     *
     * @param certificateRequest request containing nonce from earlier identity
     *                           claim handshake
     * @return a certificateResponse containing the signed certificate
     */
    @Override
    public byte[] processCertificateRequest(final byte[] certificateRequest) {
        LOG.info("Got certificate request");

        if (ArrayUtils.isEmpty(certificateRequest)) {
            throw new IllegalArgumentException("The CertificateRequest sent by the client"
                    + " cannot be null or empty.");
        }

        // attempt to deserialize Protobuf CertificateRequest
        ProvisionerTpm2.CertificateRequest request;
        try {
            request = ProvisionerTpm2.CertificateRequest.parseFrom(certificateRequest);
        } catch (InvalidProtocolBufferException ipbe) {
            throw new CertificateProcessingException(
                    "Could not deserialize Protobuf Certificate Request object.", ipbe);
        }

        // attempt to retrieve provisioner state based on nonce in request
        TPM2ProvisionerState tpm2ProvisionerState = getTpm2ProvisionerState(request);
        if (tpm2ProvisionerState != null) {
            // Reparse Identity Claim to gather necessary components
            byte[] identityClaim = tpm2ProvisionerState.getIdentityClaim();
            ProvisionerTpm2.IdentityClaim claim = parseIdentityClaim(identityClaim);

            // Get endorsement public key
            RSAPublicKey ekPub = parsePublicKey(claim.getEkPublicArea().toByteArray());

            // Get attestation public key
            RSAPublicKey akPub = parsePublicKey(claim.getAkPublicArea().toByteArray());

            // Get Endorsement Credential if it exists or was uploaded
            EndorsementCredential endorsementCredential = parseEcFromIdentityClaim(claim, ekPub);

            // Get Platform Credentials if they exist or were uploaded
            Set<PlatformCredential> platformCredentials = parsePcsFromIdentityClaim(claim,
                    endorsementCredential);

            // Get device name and device
            String deviceName = claim.getDv().getNw().getHostname();
            Device device = deviceManager.getDevice(deviceName);

            // Parse through the Provisioner supplied TPM Quote and pcr values
            // these fields are optional
            if (request.getQuote() != null && !request.getQuote().isEmpty()) {
                parseTPMQuote(request.getQuote().toStringUtf8());
                TPMInfo savedInfo = device.getDeviceInfo().getTPMInfo();
                TPMInfo tpmInfo = null;
                try {
                    tpmInfo = new TPMInfo(savedInfo.getTPMMake(),
                        savedInfo.getTPMVersionMajor(),
                        savedInfo.getTPMVersionMinor(),
                        savedInfo.getTPMVersionRevMajor(),
                        savedInfo.getTPMVersionRevMinor(),
                        savedInfo.getPcrValues(),
                        this.tpmQuoteHash.getBytes("UTF-8"),
                        this.tpmQuoteSignature.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    LOG.error(e);
                }
                DeviceInfoReport dvReport = new DeviceInfoReport(
                        device.getDeviceInfo().getNetworkInfo(),
                        device.getDeviceInfo().getOSInfo(),
                        device.getDeviceInfo().getFirmwareInfo(),
                        device.getDeviceInfo().getHardwareInfo(), tpmInfo,
                        claim.getClientVersion());
                device = this.deviceRegister.saveOrUpdateDevice(dvReport);
            }

            AppraisalStatus.Status validationResult = doQuoteValidation(device);
            if (validationResult == AppraisalStatus.Status.PASS) {
                // Create signed, attestation certificate
                X509Certificate attestationCertificate = generateCredential(akPub,
                        endorsementCredential, platformCredentials, deviceName);
                byte[] derEncodedAttestationCertificate = getDerEncodedCertificate(
                        attestationCertificate);

                // We validated the nonce and made use of the identity claim so state can be deleted
                tpm2ProvisionerStateDBManager.delete(tpm2ProvisionerState);

                // Package the signed certificate into a response
                ByteString certificateBytes = ByteString.copyFrom(derEncodedAttestationCertificate);
                ProvisionerTpm2.CertificateResponse response = ProvisionerTpm2.CertificateResponse
                        .newBuilder().setCertificate(certificateBytes).build();

                saveAttestationCertificate(derEncodedAttestationCertificate, endorsementCredential,
                        platformCredentials, device);

                return response.toByteArray();
            } else {
                LOG.error("Supply chain validation did not succeed. "
                        + "Firmware Quote Validation failed. Result is: "
                        + validationResult);
                return new byte[]{};
            }
        } else {
            LOG.error("Could not process credential request. Invalid nonce provided: "
                    + request.getNonce().toString());
            throw new CertificateProcessingException("Invalid nonce given in request by client.");
        }
    }

    /**
     * This method takes the provided TPM Quote and splits it between the PCR
     * quote and the signature hash.
     * @param tpmQuote contains hash values for the quote and the signature
     */
    private boolean parseTPMQuote(final String tpmQuote) {
        boolean success = false;
        if (tpmQuote != null) {
            String[] lines = tpmQuote.split(":");
            if (lines[1].contains("signature")) {
                this.tpmQuoteHash = lines[1].replace("signature", "").trim();
            } else {
                this.tpmQuoteHash = lines[1].trim();
            }
            this.tpmQuoteSignature = lines[2].trim();
            success = true;
        }

        return success;
    }

    /**
     * This method splits all hashed pcr values into an array.
     * @param pcrValues contains the full list of 24 pcr values
     */
    private String[] parsePCRValues(final String pcrValues) {
        String[] pcrs = null;

        if (pcrValues != null) {
            int counter = 0;
            String[] lines = pcrValues.split("\\r?\\n");
            pcrs = new String[lines.length - 1];

            for (String line : lines) {
                if (!line.isEmpty()
                        && !line.contains(TPM_SIGNATURE_ALG)) {
                    LOG.error(line);
                    pcrs[counter++] = line.split(":")[1].trim();
                }
            }
        }

        return pcrs;
    }

    /**
     * Parse public key from public data segment generated by TPM 2.0.
     * @param publicArea the public area segment to parse
     * @return the RSA public key of the supplied public data
     */
    RSAPublicKey parsePublicKey(final byte[] publicArea) {
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
     * Converts a protobuf DeviceInfo object to a HIRS Utils DeviceInfoReport object.
     * @param claim the protobuf serialized identity claim containing the device info
     * @return a HIRS Utils DeviceInfoReport representation of device info
     */
    private DeviceInfoReport parseDeviceInfo(final ProvisionerTpm2.IdentityClaim claim) {
        ProvisionerTpm2.DeviceInfo dv = claim.getDv();

        // Get network info
        ProvisionerTpm2.NetworkInfo nwProto = dv.getNw();

        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(nwProto.getIpAddress());
        } catch (UnknownHostException e) {
            LOG.error("Unable to parse IP address: ", e);
        }
        String[] macAddressParts = nwProto.getMacAddress().split(":");

        // convert mac hex string to byte values
        byte[] macAddressBytes = new byte[MAC_BYTES];
        Integer hex;
        if (macAddressParts.length == MAC_BYTES) {
            for (int i = 0; i < MAC_BYTES; i++) {
                hex = HexUtils.hexToInt(macAddressParts[i]);
                macAddressBytes[i] = hex.byteValue();
            }
        }
        NetworkInfo nw = new NetworkInfo(nwProto.getHostname(), ip, macAddressBytes);

        // Get firmware info
        ProvisionerTpm2.FirmwareInfo fwProto = dv.getFw();
        FirmwareInfo fw = new FirmwareInfo(fwProto.getBiosVendor(), fwProto.getBiosVersion(),
                fwProto.getBiosReleaseDate());

        // Get OS info
        ProvisionerTpm2.OsInfo osProto = dv.getOs();
        OSInfo os = new OSInfo(osProto.getOsName(), osProto.getOsVersion(), osProto.getOsArch(),
                osProto.getDistribution(), osProto.getDistributionRelease());

        // Get hardware info
        ProvisionerTpm2.HardwareInfo hwProto = dv.getHw();
        // Make sure chassis info has at least one chassis
        String firstChassisSerialNumber = DeviceInfoReport.NOT_SPECIFIED;
        if (hwProto.getChassisInfoCount() > 0) {
            firstChassisSerialNumber = hwProto.getChassisInfo(0).getSerialNumber();
        }
        // Make sure baseboard info has at least one baseboard
        String firstBaseboardSerialNumber = DeviceInfoReport.NOT_SPECIFIED;
        if (hwProto.getBaseboardInfoCount() > 0) {
            firstBaseboardSerialNumber = hwProto.getBaseboardInfo(0).getSerialNumber();
        }
        HardwareInfo hw = new HardwareInfo(hwProto.getManufacturer(), hwProto.getProductName(),
                hwProto.getProductVersion(), hwProto.getSystemSerialNumber(),
                firstChassisSerialNumber, firstBaseboardSerialNumber);

        if (dv.getPcrslist() != null && !dv.getPcrslist().isEmpty()) {
            this.pcrValues = dv.getPcrslist().toStringUtf8();
        }

        // Get TPM info, currently unimplemented
        TPMInfo tpm = new TPMInfo();
        try {
            tpm = new TPMInfo(DeviceInfoReport.NOT_SPECIFIED,
                    (short) 0,
                    (short) 0,
                    (short) 0,
                    (short) 0,
                    this.pcrValues.getBytes("UTF-8"),
                    this.tpmQuoteHash.getBytes("UTF-8"),
                    this.tpmQuoteSignature.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            tpm = new TPMInfo();
        }

        // Create final report
        DeviceInfoReport dvReport = new DeviceInfoReport(nw, os, fw, hw, tpm,
                claim.getClientVersion());
        dvReport.setPaccorOutputString(claim.getPaccorOutput());

        return dvReport;
    }

    private Device processDeviceInfo(final ProvisionerTpm2.IdentityClaim claim) {
        DeviceInfoReport deviceInfoReport = parseDeviceInfo(claim);

        if (deviceInfoReport == null) {
            LOG.error("Failed to deserialize Device Info Report");
            throw new IdentityProcessingException("Device Info Report failed to deserialize "
                    + "from Identity Claim");
        }

        LOG.info("Processing Device Info Report");
        // store device and device info report.
        return this.deviceRegister.saveOrUpdateDevice(deviceInfoReport);
    }

    /**
     * Gets the Endorsement Credential from the DB given the EK public key.
     * @param ekPublicKey the EK public key
     * @return the Endorsement credential, if found, otherwise null
     */
    private EndorsementCredential getEndorsementCredential(final PublicKey ekPublicKey) {
        LOG.debug("Searching for endorsement credential based on public key: " + ekPublicKey);

        if (ekPublicKey == null) {
            throw new IllegalArgumentException("Cannot look up an EC given a null public key");
        }

        EndorsementCredential credential = null;

        try {
            credential = EndorsementCredential.select(this.certificateManager)
                    .byPublicKeyModulus(Certificate.getPublicKeyModulus(ekPublicKey))
                    .getCertificate();
        } catch (IOException e) {
            LOG.error("Could not extract public key modulus", e);
        }

        if (credential == null) {
            LOG.warn("Unable to find endorsement credential for public key.");
        } else {
            LOG.debug("Endorsement credential found.");
        }

        return credential;
    }

    private Set<PlatformCredential> getPlatformCredentials(final EndorsementCredential ec) {
        Set<PlatformCredential> credentials = null;

        if (ec == null) {
            LOG.warn("Cannot look for platform credential(s).  Endorsement credential was null.");
        } else {
            LOG.debug("Searching for platform credential(s) based on holder serial number: "
                        + ec.getSerialNumber());
            credentials = PlatformCredential.select(this.certificateManager)
                                            .byHolderSerialNumber(ec.getSerialNumber())
                                            .getCertificates();
            if (credentials == null || credentials.isEmpty()) {
                LOG.warn("No platform credential(s) found");
            } else {
                LOG.debug("Platform Credential(s) found: " + credentials.size());
            }
        }

        return credentials;
    }

    @Override
    public byte[] getPublicKey() {
        return acaCertificate.getPublicKey().getEncoded();
    }

    /**
     * Unwraps a given identityRequest. That is to say, decrypt the asymmetric portion of a data
     * structure to determine the method to decrypt the symmetric portion.
     *
     * @param identityRequest
     *            to be decrypted
     * @return the decrypted symmetric portion of an identity request.
     */
    byte[] unwrapIdentityRequest(final byte[] identityRequest) {
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
     * Will attempt to decrypt the asymmetric blob that originated from an
     * {@link hirs.structs.elements.tpm.IdentityRequest} using the cipher transformation.
     *
     * @param asymmetricBlob
     *            to be decrypted
     * @param scheme
     *            to decrypt with
     * @return decrypted blob
     */
    byte[] decryptAsymmetricBlob(final byte[] asymmetricBlob, final EncryptionScheme scheme) {
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
     * @param symmetricBlob
     *            to be decrypted
     * @param symmetricKey
     *            to use to decrypt
     * @param iv
     *            to use with decryption cipher
     * @param transformation
     *            of the cipher
     * @return decrypted symmetric blob
     */
    byte[] decryptSymmetricBlob(final byte[] symmetricBlob, final byte[] symmetricKey,
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
                | InvalidAlgorithmParameterException e) {
            throw new IdentityProcessingException(
                    "Encountered error while decrypting symmetric blob of an identity request: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Constructs a public key where the modulus is in raw form.
     *
     * @param modulus
     *            in byte array form
     * @return public key using specific modulus and the well known exponent
     */
    PublicKey assemblePublicKey(final byte[] modulus) {
        return assemblePublicKey(Hex.encodeHexString(modulus));
    }

    /**
     * Constructs a public key where the modulus is Hex encoded.
     *
     * @param modulus
     *            hex encoded modulus
     * @return public key using specific modulus and the well known exponent
     */
    PublicKey assemblePublicKey(final String modulus) {
        return assemblePublicKey(new BigInteger(modulus,
                AttestationCertificateAuthority.DEFAULT_IV_SIZE));
    }

    /**
     * Assembles a public key using a defined big int modulus and the well known exponent.
     */
    private PublicKey assemblePublicKey(final BigInteger modulus) {
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
     * @return {@link SymmetricKey} using random bytes
     */
    SymmetricKey generateSymmetricKey() {

        // create a session key for the CA contents
        byte[] responseSymmetricKey =
                generateRandomBytes(AttestationCertificateAuthority.DEFAULT_IV_SIZE);

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
     * @param proof
     *            identity requests symmetric contents, otherwise, the identity proof
     * @param symmetricKey
     *            identity response session key
     * @param publicKey
     *            of the EK certificate contained within the identity proof
     * @return encrypted asymmetric contents
     */
    byte[] generateAsymmetricContents(final IdentityProof proof, final SymmetricKey symmetricKey,
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
     * Generate the Identity Response using the identity credential and the session key.
     *
     * @param credential
     *            the identity credential
     * @param symmetricKey
     *            generated session key for this request/response chain
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
            byte[] credentialIV = generateRandomBytes(
                                                AttestationCertificateAuthority.DEFAULT_IV_SIZE);

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
                | CertificateEncodingException e) {
            throw new CertificateProcessingException(
                    "Encountered error while generating Identity Response: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a credential using the specified public key.
     *
     * @param publicKey
     *            cannot be null
     * @param endorsementCredential
     *            the endorsement credential
     * @param platformCredentials
     *            the set of platform credentials
     * @param deviceName
     *            The host name used in the subject alternative name
     * @return identity credential
     */
    X509Certificate generateCredential(final PublicKey publicKey,
                                       final EndorsementCredential endorsementCredential,
                                       final Set<PlatformCredential> platformCredentials,
                                       final String deviceName) {
        try {
            // have the certificate expire in the configured number of days
            Calendar expiry = Calendar.getInstance();
            expiry.add(Calendar.DAY_OF_YEAR, validDays);

            X500Name issuer =
                    new X500Name(acaCertificate.getSubjectX500Principal().getName());
            Date notBefore = new Date();
            Date notAfter = expiry.getTime();
            BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

            SubjectPublicKeyInfo subjectPublicKeyInfo =
                    SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());

            // The subject should be left blank, per spec
            X509v3CertificateBuilder builder =
                    new X509v3CertificateBuilder(issuer, serialNumber,
                            notBefore, notAfter, null /* subjectName */, subjectPublicKeyInfo);

            Extension subjectAlternativeName =
                IssuedCertificateAttributeHelper.buildSubjectAlternativeNameFromCerts(
                endorsementCredential, platformCredentials, deviceName);

            builder.addExtension(subjectAlternativeName);
            // identify cert as an AIK with this extension
            if (IssuedCertificateAttributeHelper.EXTENDED_KEY_USAGE_EXTENSION != null) {
                builder.addExtension(IssuedCertificateAttributeHelper.EXTENDED_KEY_USAGE_EXTENSION);
            } else {
                LOG.warn("Failed to build extended key usage extension and add to AIK");
                throw new IllegalStateException("Extended Key Usage attribute unavailable. "
                        + "Unable to issue certificates");
            }

            ContentSigner signer = new JcaContentSignerBuilder("SHA1WithRSA")
                .setProvider("BC").build(privateKey);
            X509CertificateHolder holder = builder.build(signer);
            X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider("BC").getCertificate(holder);
            return certificate;
        } catch (IOException | OperatorCreationException | CertificateException e) {
            throw new CertificateProcessingException("Encountered error while generating "
                    + "identity credential: " + e.getMessage(), e);
        }
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
    protected ByteString tpm20MakeCredential(final RSAPublicKey ek, final RSAPublicKey ak,
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
                    MGF1ParameterSpec.SHA256, new PSource.PSpecified("IDENTITY\0".getBytes()));
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

    @SuppressWarnings("magicnumber")
    private byte[] assembleCredential(final byte[] topSize, final byte[] integrityHmac,
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
     * @throws NoSuchAlgorithmException Underlying SHA256 method used a bad algorithm
     */
    byte[] generateAkName(final byte[] akModulus) throws NoSuchAlgorithmException {
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
     * @throws InvalidKeyException Invalid key used
     */
    @SuppressWarnings("magicnumber")
    private byte[] cryptKDFa(final byte[] seed, final String label, final byte[] context,
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
        byte[] labelBytes = labelWithEnding.getBytes();
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
     * Computes the sha256 hash of the given blob.
     * @param blob byte array to take the hash of
     * @return sha256 hash of blob
     * @throws NoSuchAlgorithmException improper algorithm selected
     */
    private byte[] sha256hash(final byte[] blob) throws NoSuchAlgorithmException {
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
    private byte[] generateRandomBytes(final int numberOfBytes) {
        byte[] bytes = new byte[numberOfBytes];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * Extracts the IV from the identity request. That is, take the first block of data from the
     * symmetric blob and treat that as the IV. This modifies the original symmetric block.
     *
     * @param identityRequest
     *            to extract the IV from
     * @return the IV from the identity request
     */
    private byte[] extractInitialValue(final IdentityRequest identityRequest) {

        // make a reference to the symmetric blob
        byte[] symmetricBlob = identityRequest.getSymmetricBlob();

        // create the IV
        byte[] iv = new byte[AttestationCertificateAuthority.DEFAULT_IV_SIZE];

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
     * Helper method to unwrap the certificate request sent by the client and verify the
     * provided nonce.
     *
     * @param request Client Certificate Request containing nonce to complete identity claim
     * @return the {@link TPM2ProvisionerState} if valid nonce provided / null, otherwise
     */
    private TPM2ProvisionerState getTpm2ProvisionerState(
            final ProvisionerTpm2.CertificateRequest request) {
        if (request.hasNonce()) {
            byte[] nonce = request.getNonce().toByteArray();
            return TPM2ProvisionerState.getTPM2ProvisionerState(tpm2ProvisionerStateDBManager,
                    nonce);
        }
        return null;
    }

    /**
     * Helper method to parse a byte array into an {@link ProvisionerTpm2.IdentityClaim}.
     *
     * @param identityClaim byte array that should be converted to a Protobuf IdentityClaim
     *                      object
     * @throws {@link IdentityProcessingException} if byte array could not be parsed
     * @return the Protobuf generated Identity Claim object
     */
    private ProvisionerTpm2.IdentityClaim parseIdentityClaim(final byte[] identityClaim) {
        try {
            return ProvisionerTpm2.IdentityClaim.parseFrom(identityClaim);
        } catch (InvalidProtocolBufferException ipbe) {
            throw new IdentityProcessingException(
                    "Could not deserialize Protobuf Identity Claim object.", ipbe);
        }
    }

    /**
     * Helper method to parse an Endorsement Credential from a Protobuf generated
     * IdentityClaim. Will also check if the Endorsement Credential was already uploaded.
     * Persists the Endorsement Credential if it does not already exist.
     *
     * @param identityClaim a Protobuf generated Identity Claim object
     * @param ekPub the endorsement public key from the Identity Claim object
     * @return the Endorsement Credential, if one exists, null otherwise
     */
    private EndorsementCredential parseEcFromIdentityClaim(
            final ProvisionerTpm2.IdentityClaim identityClaim,
            final PublicKey ekPub) {
        EndorsementCredential endorsementCredential = null;
        if (identityClaim.hasEndorsementCredential()) {
            endorsementCredential = CredentialManagementHelper.storeEndorsementCredential(
                    this.certificateManager,
                    identityClaim.getEndorsementCredential().toByteArray());
        } else if (ekPub != null) {
            LOG.warn("Endorsement Cred was not in the identity claim from the client."
                    + " Checking for uploads.");
            endorsementCredential = getEndorsementCredential(ekPub);
        } else {
            LOG.warn("No endorsement credential was received in identity claim and no EK Public"
                    + " Key was provided to check for uploaded certificates.");
        }
        return endorsementCredential;
    }

    /**
     * Helper method to parse a set of Platform Credentials from a Protobuf generated
     * IdentityClaim and Endorsement Credential. Persists the Platform Credentials if they
     * do not already exist.
     *
     * @param identityClaim a Protobuf generated Identity Claim object
     * @param endorsementCredential an endorsement credential to check if platform credentials
     *                              exist
     * @return the Set of Platform Credentials, if they exist, an empty set otherwise
     */
    private Set<PlatformCredential> parsePcsFromIdentityClaim(
            final ProvisionerTpm2.IdentityClaim identityClaim,
            final EndorsementCredential endorsementCredential) {
        Set<PlatformCredential> platformCredentials = new HashSet<>();
        if (identityClaim.getPlatformCredentialCount() > 0) {
            for (ByteString platformCredential : identityClaim.getPlatformCredentialList()) {
                if (!platformCredential.isEmpty()) {
                    platformCredentials.add(CredentialManagementHelper.storePlatformCredential(
                            this.certificateManager, platformCredential.toByteArray()));
                }
            }
        } else if (endorsementCredential != null) {
            // if none in the identity claim, look for uploaded platform credentials
            LOG.warn("PC was not in the identity claim from the client. Checking for uploads.");
            platformCredentials.addAll(getPlatformCredentials(endorsementCredential));
        } else {
            LOG.warn("No platform credential received in identity claim.");
        }
        return platformCredentials;
    }

    /**
     * Helper method to extract a DER encoded ASN.1 certificate from an X509 certificate.
     *
     * @param certificate the X509 certificate to be converted to DER encoding
     * @throws {@link UnexpectedServerException} if error occurs during encoding retrieval
     * @return the byte array representing the DER encoded certificate
     */
    private byte[] getDerEncodedCertificate(final X509Certificate certificate) {
        try {
            return certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            LOG.error("Error converting certificate to ASN.1 DER Encoding.", e);
            throw new UnexpectedServerException(
                    "Encountered error while converting X509 Certificate to ASN.1 DER Encoding: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Helper method to create an {@link IssuedAttestationCertificate} object, set its
     * corresponding device and persist it.
     *
     * @param derEncodedAttestationCertificate the byte array representing the Attestation
     *                                         certificate
     * @param endorsementCredential the endorsement credential used to generate the AC
     * @param platformCredentials the platform credentials used to generate the AC
     * @param device the device to which the attestation certificate is tied
     * @throws {@link CertificateProcessingException} if error occurs in persisting the Attestation
     *                                             Certificate
     */
    private void saveAttestationCertificate(final byte[] derEncodedAttestationCertificate,
                                            final EndorsementCredential endorsementCredential,
                                            final Set<PlatformCredential> platformCredentials,
                                            final Device device) {
        try {
            // save issued certificate
            IssuedAttestationCertificate attCert = new IssuedAttestationCertificate(
                    derEncodedAttestationCertificate, endorsementCredential, platformCredentials);
            attCert.setDevice(device);
            certificateManager.save(attCert);
        } catch (Exception e) {
            LOG.error("Error saving generated Attestation Certificate to database.", e);
            throw new CertificateProcessingException(
                    "Encountered error while storing Attestation Certificate: "
                            + e.getMessage(), e);
        }
    }
}
