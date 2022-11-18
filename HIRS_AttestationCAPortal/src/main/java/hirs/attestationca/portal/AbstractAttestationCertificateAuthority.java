package hirs.attestationca.portal;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hirs.attestationca.AttestationCertificateAuthority;
import hirs.attestationca.CredentialManagementHelper;
import hirs.attestationca.IssuedCertificateAttributeHelper;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.entity.BaseReferenceManifest;
import hirs.attestationca.entity.Device;
import hirs.data.persist.DeviceInfoReport;
import hirs.attestationca.entity.EventLogMeasurements;
import hirs.attestationca.entity.ReferenceDigestValue;
import hirs.attestationca.entity.ReferenceManifest;
import hirs.attestationca.entity.SupplyChainValidationSummary;
import hirs.attestationca.entity.SupportReferenceManifest;
import hirs.attestationca.entity.SwidResource;
import hirs.attestationca.entity.TPM2ProvisionerState;
import hirs.attestationca.entity.certificate.Certificate;
import hirs.attestationca.entity.certificate.EndorsementCredential;
import hirs.attestationca.entity.certificate.IssuedAttestationCertificate;
import hirs.attestationca.entity.certificate.PlatformCredential;
import hirs.attestationca.exceptions.CertificateProcessingException;
import hirs.attestationca.exceptions.IdentityProcessingException;
import hirs.attestationca.exceptions.UnexpectedServerException;
import hirs.attestationca.portal.validation.SupplyChainValidationService;
import hirs.attestationca.service.CertificateService;
import hirs.attestationca.service.DeviceRegister;
import hirs.attestationca.service.DeviceService;
import hirs.attestationca.service.ReferenceDigestValueService;
import hirs.attestationca.service.ReferenceManifestService;
import hirs.data.persist.AppraisalStatus;
import hirs.data.persist.info.FirmwareInfo;
import hirs.data.persist.info.HardwareInfo;
import hirs.data.persist.info.NetworkInfo;
import hirs.data.persist.info.OSInfo;
import hirs.data.persist.info.TPMInfo;
import hirs.attestationca.policy.SupplyChainPolicy;
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
import hirs.tpm.eventlog.TCGEventLog;
import hirs.tpm.eventlog.TpmPcrEvent;
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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Defines the well known exponent.
     * https://en.wikipedia.org/wiki/65537_(number)#Applications
     */
    private static final BigInteger EXPONENT = new BigInteger("010001",
            AttestationCertificateAuthority.DEFAULT_IV_SIZE);
    private static final String CATALINA_HOME = System.getProperty("catalina.base");
    private static final String TOMCAT_UPLOAD_DIRECTORY
            = "/webapps/HIRS_AttestationCA/upload/";
    private static final String PCR_UPLOAD_FOLDER
            = CATALINA_HOME + TOMCAT_UPLOAD_DIRECTORY;
    private static final String PCR_QUOTE_MASK = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,"
            + "14,15,16,17,18,19,20,21,22,23";

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
     * Container wired {@link StructConverter} to be used in
     * serialization / deserialization of TPM data structures.
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
    private Integer validDays = 1;

    private final CertificateService certificateService;
    private final ReferenceManifestService referenceManifestService;
    private final ReferenceDigestValueService referenceDigestValueService;
    private final DeviceRegister deviceRegister;
    private final DeviceService deviceService;
    private String tpmQuoteHash = "";
    private String tpmQuoteSignature = "";

    /**
     * Constructor.
     * @param supplyChainValidationService the supply chain service
     * @param privateKey the ACA private key
     * @param acaCertificate the ACA certificate
     * @param structConverter the struct converter
     * @param certificateService the certificate service
     * @param referenceManifestService the Reference Manifest service
     * @param deviceRegister the device register
     * @param validDays the number of days issued certs are valid
     * @param deviceService the device Service
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public AbstractAttestationCertificateAuthority(
            final SupplyChainValidationService supplyChainValidationService,
            final PrivateKey privateKey, final X509Certificate acaCertificate,
            final StructConverter structConverter,
            final CertificateService certificateService,
            final ReferenceManifestService referenceManifestService,
            final ReferenceDigestValueService referenceDigestValueService,
            final DeviceRegister deviceRegister, final int validDays,
            final DeviceService deviceService) {
        this.supplyChainValidationService = supplyChainValidationService;
        this.privateKey = privateKey;
        this.acaCertificate = acaCertificate;
        this.structConverter = structConverter;
        this.certificateService = certificateService;
        this.referenceManifestService = referenceManifestService;
        this.referenceDigestValueService = referenceDigestValueService;
        this.deviceRegister = deviceRegister;
        this.validDays = validDays;
        this.deviceService = deviceService;
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
                    this.certificateService, ecBytesFromIdentityRequest
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
                    this.certificateService, pcBytesFromIdentityRequest
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
        deviceService.updateDevice(device, device.getId());
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
        // check the policy set valid date
        SupplyChainPolicy scp = this.supplyChainValidationService.getPolicy();
        if (scp != null) {
            this.validDays = Integer.parseInt(scp.getValidityDays());
        }
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
        AppraisalStatus.Status validationResult = AppraisalStatus.Status.FAIL;

        try {
            validationResult = doSupplyChainValidation(claim, ekPub);
        } catch (Exception ex) {
            for (StackTraceElement ste : ex.getStackTrace()) {
                LOG.error(ste.toString());
            }
        }

        ByteString blobStr = ByteString.copyFrom(new byte[]{});
        if (validationResult == AppraisalStatus.Status.PASS) {
            RSAPublicKey akPub = parsePublicKey(claim.getAkPublicArea().toByteArray());
            byte[] nonce = generateRandomBytes(NONCE_LENGTH);
            blobStr = tpm20MakeCredential(ekPub, akPub, nonce);
            SupplyChainPolicy scp = this.supplyChainValidationService.getPolicy();
            String pcrQuoteMask = PCR_QUOTE_MASK;

            String strNonce = HexUtils.byteArrayToHexString(nonce);
            LOG.info("Sending nonce: " + strNonce);
            LOG.info("Persisting claim of length: " + identityClaim.length);

            if (scp != null && scp.isIgnoreImaEnabled()) {
                pcrQuoteMask = PCR_QUOTE_MASK.replace("10,", "");
            }
            // Package response
            ProvisionerTpm2.IdentityClaimResponse response
                    = ProvisionerTpm2.IdentityClaimResponse.newBuilder()
                    .setCredentialBlob(blobStr).setPcrMask(pcrQuoteMask)
                    .setStatus(ProvisionerTpm2.ResponseStatus.PASS)
                    .build();
            return response.toByteArray();
        } else {
            LOG.error("Supply chain validation did not succeed. Result is: "
                    + validationResult);
            // empty response
            ProvisionerTpm2.IdentityClaimResponse response
                    = ProvisionerTpm2.IdentityClaimResponse.newBuilder()
                    .setCredentialBlob(blobStr)
                    .setStatus(ProvisionerTpm2.ResponseStatus.FAIL)
                    .build();
            return response.toByteArray();
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

        // There are situations in which the claim is sent with no PCs
        // or a PC from the tpm which will be deprecated
        // this is to check what is in the platform object and pull
        // additional information from the DB if information exists
        if (platformCredentials.size() == 1) {
            for (PlatformCredential pc : platformCredentials) {
                if (pc != null && pc.getPlatformSerial() != null) {
                    platformCredentials.addAll(PlatformCredential.select(this.certificateService)
                            .byBoardSerialNumber(pc.getPlatformSerial()).getCertificates());
                }
            }
        }
        // perform supply chain validation
        SupplyChainValidationSummary summary = supplyChainValidationService.validateSupplyChain(
                endorsementCredential, platformCredentials, device);
        device.setSummaryId(summary.getId().toString());
        // update the validation result in the device
        AppraisalStatus.Status validationResult = summary.getOverallValidationResult();
        device.setSupplyChainStatus(validationResult);
        deviceService.updateDevice(device, device.getId());
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
            device.setSummaryId(scvs.getId().toString());
            // update the validation result in the device
            validationResult = scvs.getOverallValidationResult();
            device.setSupplyChainStatus(validationResult);
            deviceService.updateDevice(device, device.getId());
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
            Device device = deviceService.getByName(deviceName);
            // check the policy set valid date
            SupplyChainPolicy scp = this.supplyChainValidationService.getPolicy();
            if (scp != null) {
                this.validDays = Integer.parseInt(scp.getValidityDays());
            }

            // Parse through the Provisioner supplied TPM Quote and pcr values
            // these fields are optional
            if (request.getQuote() != null && !request.getQuote().isEmpty()) {
                parseTPMQuote(request.getQuote().toStringUtf8());
                TPMInfo savedInfo = device.getDeviceInfo().getTPMInfo();
                TPMInfo tpmInfo = new TPMInfo(savedInfo.getTPMMake(),
                        savedInfo.getTPMVersionMajor(),
                        savedInfo.getTPMVersionMinor(),
                        savedInfo.getTPMVersionRevMajor(),
                        savedInfo.getTPMVersionRevMinor(),
                        savedInfo.getPcrValues(),
                        this.tpmQuoteHash.getBytes(StandardCharsets.UTF_8),
                        this.tpmQuoteSignature.getBytes(StandardCharsets.UTF_8));

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

                // Package the signed certificate into a response
                ByteString certificateBytes = ByteString
                        .copyFrom(derEncodedAttestationCertificate);
                ProvisionerTpm2.CertificateResponse response = ProvisionerTpm2.CertificateResponse
                        .newBuilder().setCertificate(certificateBytes)
                        .setStatus(ProvisionerTpm2.ResponseStatus.PASS)
                        .build();

                saveAttestationCertificate(derEncodedAttestationCertificate, endorsementCredential,
                        platformCredentials, device);

                return response.toByteArray();
            } else {
                LOG.error("Supply chain validation did not succeed. "
                        + "Firmware Quote Validation failed. Result is: "
                        + validationResult);
                ProvisionerTpm2.CertificateResponse response = ProvisionerTpm2.CertificateResponse
                        .newBuilder()
                        .setStatus(ProvisionerTpm2.ResponseStatus.FAIL)
                        .build();
                return response.toByteArray();
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

    private static final int NUM_OF_VARIABLES = 5;

    /**
     * Converts a protobuf DeviceInfo object to a HIRS Utils DeviceInfoReport object.
     * @param claim the protobuf serialized identity claim containing the device info
     * @return a HIRS Utils DeviceInfoReport representation of device info
     */
    @SuppressWarnings("methodlength")
    private DeviceInfoReport parseDeviceInfo(final ProvisionerTpm2.IdentityClaim claim)
            throws NoSuchAlgorithmException {
        ProvisionerTpm2.DeviceInfo dv = claim.getDv();
        String pcrValues = "";

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

        if (dv.hasPcrslist()) {
            pcrValues = dv.getPcrslist().toStringUtf8();
        }

        // check for RIM Base and Support files, if they don't exists in the database, load them
        String defaultClientName = String.format("%s_%s",
                dv.getHw().getManufacturer(),
                dv.getHw().getProductName());
        BaseReferenceManifest dbBaseRim = null;
        SupportReferenceManifest support;
        EventLogMeasurements measurements;
        String tagId = "";
        String fileName = "";
        Pattern pattern = Pattern.compile("([^\\s]+(\\.(?i)(rimpcr|rimel|bin|log))$)");
        Matcher matcher;
        MessageDigest messageDigest =  MessageDigest.getInstance("SHA-256");
        List<ReferenceManifest> listOfSavedRims = new LinkedList<>();

        if (dv.getLogfileCount() > 0) {
            for (ByteString logFile : dv.getLogfileList()) {
                try {
                    support = SupportReferenceManifest.select(referenceManifestService)
                            .byHexDecHash(Hex.encodeHexString(messageDigest.digest(
                                    logFile.toByteArray()))).includeArchived()
                            .getRIM();
                    if (support == null) {
                        support = new SupportReferenceManifest(
                                String.format("%s.rimel",
                                        defaultClientName),
                                logFile.toByteArray());
                        // this is a validity check
                        new TCGEventLog(support.getRimBytes());
                        // no issues, continue
                        support.setPlatformManufacturer(dv.getHw().getManufacturer());
                        support.setPlatformModel(dv.getHw().getProductName());
                        support.setFileName(String.format("%s_[%s].rimel", defaultClientName,
                                support.getHexDecHash().substring(
                                        support.getHexDecHash().length() - NUM_OF_VARIABLES)));
                        support.setDeviceName(dv.getNw().getHostname());
                        this.referenceManifestService.saveRIM(support);
                    } else {
                        LOG.info("Client provided Support RIM already loaded in database.");
                        if (support.isArchived()) {
                            support.restore();
                            support.resetCreateTime();
                            this.referenceManifestService.updateReferenceManifest(support,
                                    support.getId());
                        }
                    }
                } catch (IOException ioEx) {
                    LOG.error(ioEx);
                } catch (Exception ex) {
                    LOG.error(String.format("Failed to load support rim: %s", messageDigest.digest(
                            logFile.toByteArray()).toString()));
                }
            }
        } else {
            LOG.warn(String.format("%s did not send support RIM file...",
                    dv.getNw().getHostname()));
        }

        if (dv.getSwidfileCount() > 0) {
            for (ByteString swidFile : dv.getSwidfileList()) {
                try {
                    dbBaseRim = BaseReferenceManifest.select(referenceManifestService)
                            .byBase64Hash(Base64.getEncoder()
                                    .encodeToString(messageDigest
                                            .digest(swidFile.toByteArray())))
                            .includeArchived()
                            .getRIM();
                    if (dbBaseRim == null) {
                        dbBaseRim = new BaseReferenceManifest(
                                String.format("%s.swidtag",
                                        defaultClientName),
                                swidFile.toByteArray());
                        dbBaseRim.setDeviceName(dv.getNw().getHostname());
                        this.referenceManifestService.saveRIM(dbBaseRim);
                    } else {
                        LOG.info("Client provided Base RIM already loaded in database.");
                        /**
                         * Leaving this as is for now, however can there be a condition
                         * in which the provisioner sends swidtags without support rims?
                         */
                        if (dbBaseRim.isArchived()) {
                            dbBaseRim.restore();
                            dbBaseRim.resetCreateTime();
                            this.referenceManifestService.updateReferenceManifest(dbBaseRim,
                                    dbBaseRim.getId());
                        }
                    }
                } catch (IOException ioEx) {
                    LOG.error(ioEx);
                }
            }
        } else {
            LOG.warn(String.format("%s did not send swid tag file...",
                    dv.getNw().getHostname()));
        }

        //update Support RIMs and Base RIMs.
        for (ByteString swidFile : dv.getSwidfileList()) {
            dbBaseRim = BaseReferenceManifest.select(referenceManifestService)
                    .byBase64Hash(Base64.getEncoder().encodeToString(messageDigest.digest(
                            swidFile.toByteArray()))).includeArchived()
                    .getRIM();

            if (dbBaseRim != null) {
                // get file name to use
                for (SwidResource swid : dbBaseRim.parseResource()) {
                    matcher = pattern.matcher(swid.getName());
                    if (matcher.matches()) {
                        //found the file name
                        int dotIndex = swid.getName().lastIndexOf(".");
                        fileName = swid.getName().substring(0, dotIndex);
                        dbBaseRim.setFileName(String.format("%s.swidtag",
                                fileName));
                    }

                    // now update support rim
                    SupportReferenceManifest dbSupport = SupportReferenceManifest
                            .select(referenceManifestService)
                            .byHexDecHash(swid.getHashValue()).getRIM();
                    if (dbSupport != null) {
                        dbSupport.setFileName(swid.getName());
                        dbSupport.setSwidTagVersion(dbBaseRim.getSwidTagVersion());
                        dbSupport.setTagId(dbBaseRim.getTagId());
                        dbSupport.setSwidTagVersion(dbBaseRim.getSwidTagVersion());
                        dbSupport.setSwidVersion(dbBaseRim.getSwidVersion());
                        dbSupport.setSwidPatch(dbBaseRim.isSwidPatch());
                        dbSupport.setSwidSupplemental(dbBaseRim.isSwidSupplemental());
                        dbBaseRim.setAssociatedRim(dbSupport.getId());
                        dbSupport.setUpdated(true);
                        dbSupport.setAssociatedRim(dbBaseRim.getId());
                        this.referenceManifestService.updateReferenceManifest(dbSupport,
                                dbSupport.getId());
                        listOfSavedRims.add(dbSupport);
                    }
                }
                this.referenceManifestService.updateReferenceManifest(dbBaseRim,
                        dbBaseRim.getId());
                listOfSavedRims.add(dbBaseRim);
            }
        }

        generateDigestRecords(hw.getManufacturer(), hw.getProductName());

        if (dv.hasLivelog()) {
            LOG.info("Device sent bios measurement log...");
            fileName = String.format("%s.measurement",
                    dv.getNw().getHostname());
            try {
                EventLogMeasurements temp = new EventLogMeasurements(fileName,
                        dv.getLivelog().toByteArray());
                // find previous version.
                measurements = EventLogMeasurements.select(referenceManifestService)
                        .byDeviceName(dv.getNw().getHostname())
                        .includeArchived()
                        .getRIM();

                if (measurements != null) {
                    // Find previous log and delete it
                    referenceManifestService.deleteRIM(measurements);
                }

                BaseReferenceManifest baseRim = BaseReferenceManifest
                        .select(referenceManifestService)
                        .byManufacturerModelBase(dv.getHw().getManufacturer(),
                                dv.getHw().getProductName())
                        .getRIM();
                measurements = temp;
                measurements.setPlatformManufacturer(dv.getHw().getManufacturer());
                measurements.setPlatformModel(dv.getHw().getProductName());
                measurements.setTagId(tagId);
                measurements.setDeviceName(dv.getNw().getHostname());
                if (baseRim != null) {
                    measurements.setAssociatedRim(baseRim.getAssociatedRim());
                }
                this.referenceManifestService.saveRIM(measurements);

                if (baseRim != null) {
                    // pull the base versions of the swidtag and rimel and set the
                    // event log hash for use during provision
                    SupportReferenceManifest sBaseRim = SupportReferenceManifest
                            .select(referenceManifestService)
                            .byEntityId(baseRim.getAssociatedRim())
                            .getRIM();
                    baseRim.setEventLogHash(temp.getHexDecHash());
                    sBaseRim.setEventLogHash(temp.getHexDecHash());
                    referenceManifestService.updateReferenceManifest(baseRim, baseRim.getId());
                    referenceManifestService.updateReferenceManifest(sBaseRim, baseRim.getId());
                }
            } catch (IOException ioEx) {
                LOG.error(ioEx);
            }
        } else {
            LOG.warn(String.format("%s did not send bios measurement log...",
                    dv.getNw().getHostname()));
        }

        // Get TPM info, currently unimplemented
        TPMInfo tpm;
        tpm = new TPMInfo(DeviceInfoReport.NOT_SPECIFIED,
                (short) 0,
                (short) 0,
                (short) 0,
                (short) 0,
                pcrValues.getBytes(StandardCharsets.UTF_8),
                this.tpmQuoteHash.getBytes(StandardCharsets.UTF_8),
                this.tpmQuoteSignature.getBytes(StandardCharsets.UTF_8));

        // Create final report
        DeviceInfoReport dvReport = new DeviceInfoReport(nw, os, fw, hw, tpm,
                claim.getClientVersion());
        dvReport.setPaccorOutputString(claim.getPaccorOutput());

        return dvReport;
    }

    private boolean generateDigestRecords(final String manufacturer, final String model) {
        List<ReferenceDigestValue> rdValues = new LinkedList<>();
        SupportReferenceManifest baseSupportRim = null;
        List<SupportReferenceManifest> supplementalRims = new ArrayList<>();
        List<SupportReferenceManifest> patchRims = new ArrayList<>();
        Set<SupportReferenceManifest> dbSupportRims = SupportReferenceManifest
                .select(referenceManifestService)
                .byManufacturerModel(manufacturer, model).getRIMs();
        List<ReferenceDigestValue> sourcedValues = referenceDigestValueService.
                getValueByManufacturerModel(manufacturer, model);

        Map<String, ReferenceDigestValue> digestValueMap = new HashMap<>();
        sourcedValues.stream().forEach((rdv) -> {
            digestValueMap.put(rdv.getDigestValue(), rdv);
        });

        for (SupportReferenceManifest dbSupport : dbSupportRims) {
            if (dbSupport.isSwidPatch()) {
                patchRims.add(dbSupport);
            } else if (dbSupport.isSwidSupplemental()) {
                supplementalRims.add(dbSupport);
            } else {
                // we have a base support rim (verify this is getting set)
                baseSupportRim = dbSupport;
            }
        }

        if (baseSupportRim != null
                && referenceDigestValueService.getValuesByBaseRimId(
                        baseSupportRim.getId()).isEmpty()) {
            try {
                TCGEventLog logProcessor = new TCGEventLog(baseSupportRim.getRimBytes());
                ReferenceDigestValue rdv;
                for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                    rdv = new ReferenceDigestValue(baseSupportRim.getAssociatedRim(),
                            baseSupportRim.getId(), manufacturer, model, tpe.getPcrIndex(),
                            tpe.getEventDigestStr(), tpe.getEventTypeStr(),
                            false, false, true, tpe.getEventContent());
                    rdValues.add(rdv);
                }

                // since I have the base already I don't have to care about the backward
                // linkage
                for (SupportReferenceManifest supplemental : supplementalRims) {
                    logProcessor = new TCGEventLog(supplemental.getRimBytes());
                    for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                        // all RDVs will have the same base rim
                        rdv = new ReferenceDigestValue(baseSupportRim.getAssociatedRim(),
                                supplemental.getId(), manufacturer, model, tpe.getPcrIndex(),
                                tpe.getEventDigestStr(), tpe.getEventTypeStr(),
                                false, false, true, tpe.getEventContent());
                        rdValues.add(rdv);
                    }
                }

                // Save all supplemental values
                ReferenceDigestValue tempRdv;
                for (ReferenceDigestValue subRdv : rdValues) {
                    // check if the value already exists
                    if (digestValueMap.containsKey(subRdv.getDigestValue())) {
                        tempRdv = digestValueMap.get(subRdv.getDigestValue());
                        if (tempRdv.getPcrIndex() != subRdv.getPcrIndex()
                                && !tempRdv.getEventType().equals(subRdv.getEventType())) {
                            referenceDigestValueService.saveDigestValue(subRdv);
                        } else {
                            // will this be a problem down the line?
                            referenceDigestValueService.updateDigestValue(subRdv, subRdv.getId());
                        }
                    } else {
                        referenceDigestValueService.saveDigestValue(subRdv);
                    }
                    digestValueMap.put(subRdv.getDigestValue(), subRdv);
                }

                // if a patch value doesn't exist, error?
                ReferenceDigestValue dbRdv;
                String patchedValue;
                for (SupportReferenceManifest patch : patchRims) {
                    logProcessor = new TCGEventLog(patch.getRimBytes());
                    for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                        patchedValue = tpe.getEventDigestStr();
                        dbRdv = digestValueMap.get(patchedValue);

                        if (dbRdv == null) {
                            LOG.error(String.format("Patching value does not exist (%s)",
                                    patchedValue));
                        } else {
                            /**
                             * Until we get patch examples, this is WIP
                             */
                            dbRdv.setPatched(true);
                        }
                    }
                }
            } catch (CertificateException cEx) {
                LOG.error(cEx);
            } catch (NoSuchAlgorithmException noSaEx) {
                LOG.error(noSaEx);
            } catch (IOException ioEx) {
                LOG.error(ioEx);
            }
        }

        return true;
    }

    private Device processDeviceInfo(final ProvisionerTpm2.IdentityClaim claim) {
        DeviceInfoReport deviceInfoReport = null;

        try {
            deviceInfoReport = parseDeviceInfo(claim);
        } catch (NoSuchAlgorithmException noSaEx) {
            LOG.error(noSaEx);
        }

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
            credential = EndorsementCredential.select(this.certificateService)
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
            credentials = PlatformCredential.select(this.certificateService)
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

            Extension authKeyIdentifier = IssuedCertificateAttributeHelper
                    .buildAuthorityKeyIdentifier(endorsementCredential);

            builder.addExtension(subjectAlternativeName);
            if (authKeyIdentifier != null) {
                builder.addExtension(authKeyIdentifier);
            }
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
            return new JcaX509CertificateConverter()
                    .setProvider("BC").getCertificate(holder);
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
//        if (request.hasNonce()) {
//            byte[] nonce = request.getNonce().toByteArray();
//            return TPM2ProvisionerState.getTPM2ProvisionerState(tpm2ProvisionerStateDBManager,
//                    nonce);
//        }
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
                    this.certificateService,
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
                            this.certificateService, platformCredential.toByteArray()));
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
        IssuedAttestationCertificate issuedAc;
        boolean generateCertificate = true;
        SupplyChainPolicy scp = this.supplyChainValidationService.getPolicy();
        Date currentDate = new Date();
        int days;
        try {
            // save issued certificate
            IssuedAttestationCertificate attCert = new IssuedAttestationCertificate(
                    derEncodedAttestationCertificate, endorsementCredential, platformCredentials);

            if (scp != null) {
                issuedAc = IssuedAttestationCertificate.select(certificateService)
                        .byDeviceId(device.getId()).getCertificate();

                generateCertificate = scp.isIssueAttestationCertificate();
                if (issuedAc != null && scp.isGenerateOnExpiration()) {
                    if (issuedAc.getEndValidity().after(currentDate)) {
                        // so the issued AC is not expired
                        // however are we within the threshold
                        days = daysBetween(currentDate, issuedAc.getEndValidity());
                        if (days < Integer.parseInt(scp.getReissueThreshold())) {
                            generateCertificate = true;
                        } else {
                            generateCertificate = false;
                        }
                    }
                }
            }
            if (generateCertificate) {
                attCert.setDevice(device);
                certificateService.saveCertificate(attCert);
            }
        } catch (Exception e) {
            LOG.error("Error saving generated Attestation Certificate to database.", e);
            throw new CertificateProcessingException(
                    "Encountered error while storing Attestation Certificate: "
                            + e.getMessage(), e);
        }
    }

    @SuppressWarnings("magicnumber")
    private int daysBetween(final Date date1, final Date date2) {
        return (int) ((date2.getTime() - date1.getTime()) / (1000 * 60 * 60 * 24));
    }
}
