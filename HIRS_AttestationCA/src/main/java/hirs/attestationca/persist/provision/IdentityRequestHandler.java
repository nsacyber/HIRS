package hirs.attestationca.persist.provision;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.utils.Certificate;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.exceptions.IdentityProcessingException;
import hirs.attestationca.persist.provision.helper.CredentialManagementHelper;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
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

import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

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

        byte[] identityProof = unwrapIdentityRequest(structConverter.convert(challenge.getRequest(),
                IdentityRequest.class));
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
                    ekPublicKey = ProvisionUtils.assemblePublicKey(publicKeyModulus.toByteArray());
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
                    ProvisionUtils.assemblePublicKey(new String(challenge.getEndorsementCredentialModulus()));
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
        byte[] identityProof = unwrapIdentityRequest(
                structConverter.convert(challenge.getRequest(), IdentityRequest.class));

        // the decrypted symmetric blob should be in the format of an IdentityProof. Use the
        // struct converter to generate it.
        IdentityProof proof = structConverter.convert(identityProof, IdentityProof.class);

        // generate a session key and convert to byte array
        log.debug("generating symmetric key for response");
        SymmetricKey sessionKey = ProvisionUtils.generateSymmetricKey();

        // generate the asymmetric contents for the identity response
        log.debug("generating asymmetric contents for response");
        byte[] asymmetricContents = ProvisionUtils.generateAsymmetricContents(
                structConverter.convert(proof.getIdentityKey()),
                structConverter.convert(sessionKey), ekPublicKey);

        // generate the identity credential
        log.debug("generating credential from identity proof");

        // transform the public key struct into a public key
        PublicKey publicKey = ProvisionUtils.assemblePublicKey(proof.getIdentityKey().getStorePubKey().getKey());
        X509Certificate credential = generateCredential(publicKey, endorsementCredential,
                platformCredentials, device.getDeviceInfo()
                        .getNetworkInfo()
                        .getIpAddress()
                        .getHostName(), acaCertificate);

        // generate the attestation using the credential and the key for this session
        log.debug("generating symmetric response");
        SymmetricAttestation attestation = ProvisionUtils.generateAttestation(credential, sessionKey);

        // construct the response with the both the asymmetric contents and the CA attestation
        IdentityResponseEnvelope identityResponse =
                new SimpleStructBuilder<>(IdentityResponseEnvelope.class)
                        .set("asymmetricContents", asymmetricContents)
                        .set("symmetricAttestation", attestation).build();

        // save new attestation certificate
        byte[] derEncodedAttestationCertificate = ProvisionUtils.getDerEncodedCertificate(credential);
        saveAttestationCertificate(this.certificateRepository, derEncodedAttestationCertificate,
                endorsementCredential, platformCredentials, device);

        return identityResponse;
    }

    /**
     * Unwraps a given identityRequest. That is to say, decrypt the asymmetric portion of a data
     * structure to determine the method to decrypt the symmetric portion.
     *
     * @param request
     *            to be decrypted
     * @return the decrypted symmetric portion of an identity request.
     */
    private byte[] unwrapIdentityRequest(final IdentityRequest request) {
        // in case the TPM did not specify the IV, it must be extracted from the symmetric blob.
        // the IV will then be the the first block of the cipher text.
        final byte[] iv;
        SymmetricKeyParams symmetricKeyParams = request.getSymmetricAlgorithm();
        if (symmetricKeyParams != null && symmetricKeyParams.getParams() != null) {
            iv = symmetricKeyParams.getParams().getIv();
        } else {
            iv = ProvisionUtils.extractInitialValue(request);
        }

        // determine the encryption scheme from the algorithm
        EncryptionScheme asymmetricScheme =
                EncryptionScheme.fromInt(request.getAsymmetricAlgorithm().getEncryptionScheme());

        // decrypt the asymmetric blob
        byte[] decryptedAsymmetricBlob =
                ProvisionUtils.decryptAsymmetricBlob(request.getAsymmetricBlob(), asymmetricScheme, getPrivateKey());

        // construct our symmetric key structure from the decrypted asymmetric blob
        SymmetricKey symmetricKey =
                structConverter.convert(decryptedAsymmetricBlob, SymmetricKey.class);

        byte[] decryptedSymmetricBlob =
                ProvisionUtils.decryptSymmetricBlob(request.getSymmetricBlob(), symmetricKey.getKey(), iv,
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

}
