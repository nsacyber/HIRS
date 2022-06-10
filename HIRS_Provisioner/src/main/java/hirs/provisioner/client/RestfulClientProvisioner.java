package hirs.provisioner.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hirs.DeviceInfoReportRequest;
import hirs.client.collector.DeviceInfoCollector;
import hirs.collector.CollectorException;
import hirs.data.persist.DeviceInfoReport;
import hirs.provisioner.CommandLineArguments;
import hirs.structs.converters.SimpleStructBuilder;
import hirs.structs.converters.StructConverter;
import hirs.structs.elements.aca.IdentityRequestEnvelope;
import hirs.structs.elements.aca.IdentityResponseEnvelope;
import hirs.structs.elements.tpm.AsymmetricKeyParams;
import hirs.structs.elements.tpm.AsymmetricPublicKey;
import hirs.structs.elements.tpm.RsaSubParams;
import hirs.structs.elements.tpm.StorePubKey;
import hirs.tpm.tss.Tpm;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

/**
 * Client implementation that uses a RestTemplate to communicate
 * with the Attestation Certificate Authority.
 *
 */
@Component
public class RestfulClientProvisioner implements ClientProvisioner {

    private static final Logger LOG = LogManager.getLogger(RestfulClientProvisioner.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Tpm tpm;

    @Autowired
    private StructConverter structConverter;

    @Value("${provisioner.files.certs}")
    private String certificatesPath;

    @Value("${provisioner.aca.url.key}")
    private String acaPublicKeyURL;

    @Value("${provisioner.aca.url.identity}")
    private String acaIdentityURL;

    /**
     * When the application context is initialized, begin provisioning.
     *
     * @param event ignored
     */
    @EventListener
    @SuppressFBWarnings(value = "DM_EXIT",
            justification = "need to exit app from spring managed class here")
    public final void handleContextInitialized(final ContextRefreshedEvent event) {

        // attempt to create the required directory structures before provisioning.
        try {
            LOG.debug("Creating certificate directory if non existent: " + certificatesPath);
            Files.createDirectories(Paths.get(certificatesPath));
        } catch (IOException ex) {
            LOG.error("Error creating certificate directories: " + ex.getMessage(), ex);
            throw new ProvisioningException(ex.getMessage(), ex);
        }

        // initiate provisioning process. If it fails, exit with non-zero so that
        // the caller (hirs.sh) can exit and not configure the client due to failure.
        if (!this.provision()) {
            LOG.debug("Exiting with error due to provisioning failure");
            System.exit(-1);
        }
    }

    @Override
    public boolean provision() {

        // take ownership of the TPM
        takeOwnership();
        LOG.debug("took ownership of TPM");

        // get the public key from the ACA
        RSAPublicKey acaPublicKey = getACAPublicKey();
        LOG.debug("received public key from ACA");

        // create uuid for identity request and response
        String uuid = UUID.randomUUID().toString();

        // create Tpm Key from ACA Public key
        AsymmetricPublicKey tpmKey = new SimpleStructBuilder<>(AsymmetricPublicKey.class)
            .set("asymmetricKeyParams", new SimpleStructBuilder<>(AsymmetricKeyParams.class)
                .set("algorithmId", AsymmetricPublicKey.DEFAULT_RSA_ALG_ID)
                .set("encryptionScheme", AsymmetricPublicKey.DEFAULT_RSA_ENCRYPTION_SCHEME)
                .set("signatureScheme", AsymmetricPublicKey.DEFAULT_RSA_SIGNATURE_SCHEME)
                .set("params", new SimpleStructBuilder<>(RsaSubParams.class)
                    .set("totalPrimes", AsymmetricPublicKey.DEFAULT_PRIME_TOTAL)
                    .set("keyLength", AsymmetricPublicKey.DEFAULT_KEY_LENGTH)
                    .set("exponent", acaPublicKey.getPublicExponent().toByteArray())
                    .build())
                .build())
            .set("storePubKey", new SimpleStructBuilder<>(StorePubKey.class)
                .set("key", acaPublicKey.getModulus().toByteArray())
                .build())
            .build();

        // obtain device information required for identity request
        DeviceInfoReport deviceInfoReport = getDeviceInfoReport();

        // create the identity request using the ACA public key and a generated UUID
        IdentityRequestEnvelope identityRequest = createIdentityRequest(tpmKey, uuid,
                deviceInfoReport);
        LOG.debug(String.format("created TPM identity request using UUID %s", uuid));

        // attest the request with the ACA and obtain it's response
        System.out.println("----> Sending Attestation Identity Credential Request");
        IdentityResponseEnvelope response = attestIdentityRequest(identityRequest);
        if (null == response) {
            LOG.error("Provisioning failed.  Please refer to the Attestation CA for details.");
            return false;
        }

        System.out.println("----> Attestation Identity Provisioning succeeded");

        // activate the identity with the TPM
        byte[] credential = activateCredential(response, uuid);
        LOG.debug("activated credential with TPM");

        // create the AIK file and write the credential to the file.
        try {
            Path identityFile = Files.createFile(Paths.get(certificatesPath, uuid + ".cer"));
            Files.write(identityFile, credential);
            LOG.debug(String.format("created credential file: %s", identityFile.toString()));
            return true;
        } catch (IOException e) {
            LOG.error(String.format("Error outputting identity credential to %s: %s",
                    certificatesPath, e.getMessage()), e);
            return false;
        }
    }

    /**
     * Uses the TPM instance to take over the TPM ownership, if possible.
     */
    void takeOwnership() {
        tpm.takeOwnership();
    }

    /**
     * Communicates with the configured ACA to obtain it's public key information.
     *
     * @return the ACA public key
     */
    RSAPublicKey getACAPublicKey() {
        // request the public key from the ACA
        ResponseEntity<byte[]> response = restTemplate.getForEntity(acaPublicKeyURL, byte[].class);
        X509EncodedKeySpec keySpec;
        byte[] body = response.getBody();

        try {
            if (body == null) {
                throw new ProvisioningException("Encountered error: "
                        + "ResponseEntity body is null.", null);
            } else {
                // use the public key information to create encoded key specification. then create a
                keySpec = new X509EncodedKeySpec(body);
            }

            // create the public key from that specification
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new ProvisioningException("Encountered error while create Public Key from "
                    + "ACA response: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an identity request using the ACA public certificate and an UUID.
     *
     * @param asymmetricPublicKey      that contains the public key information of the ACA
     * @param uuid                     unique identifier
     * @param deviceInfoReport         the full device info report used by the ACA for provisioning
     * @return identity request that can be sent on ward to the ACA for attestation.
     */
    IdentityRequestEnvelope createIdentityRequest(final AsymmetricPublicKey asymmetricPublicKey,
              final String uuid, final DeviceInfoReport deviceInfoReport) {
        try {
            // serialize the tpm key
            byte[] asymmetricBlob = structConverter.convert(asymmetricPublicKey);

            // collate identity based upon ACA public key and unique id
            byte[] request = tpm.collateIdentityRequest(asymmetricBlob, uuid);

            // obtain the endorsement credential
            byte[] endorsementCredentialModulus = tpm.getEndorsementCredentialModulus();

            // obtain full endorsement credential
            byte[] endorsementCredential = getEndorsementCredential();

            // format device info report
            byte[] deviceInfoReportBytes = SerializationUtils.serialize(deviceInfoReport);

            LOG.debug("Sending EC of length: " + endorsementCredential.length);
            // build up the ACA request
            return new SimpleStructBuilder<>(IdentityRequestEnvelope.class)
                .set("endorsementCredentialModulus", endorsementCredentialModulus)
                .set("endorsementCredential", endorsementCredential)
                .set("request", request)
                .set("deviceInfoReport", deviceInfoReportBytes)
                .build();
        } catch (Exception e) {
            throw new ProvisioningException("Encountered exception while creating identity "
                    + "credential: " + e.getMessage(), e);
        }
    }

    /**
     * Gets endorsement credential from TPM.
     * @return the EK from the TPM, or null if there was an error
     */
    private byte[] getEndorsementCredential() {
        // initialize to a non-null array so struct builder doesn't consider this null.
        byte[] endorsementCredential = new byte[] {1};
        try {
            endorsementCredential = tpm.getEndorsementCredential();
            System.out.println("----> Got endorsement credential from TPM");
            LOG.debug("Endorsement credential size: " + endorsementCredential.length);
        } catch (Exception e) {
            LOG.warn("Failed to get endorsement credential from TPM");
            LOG.debug("EK retrieval error", e);
        }
        return endorsementCredential;
    }

    /**
     * Attests a given identity request with the configured Attestation Certificate Authority.
     *
     * @param identityRequest to be attested
     * @return the ACA response
     */
    IdentityResponseEnvelope attestIdentityRequest(final IdentityRequestEnvelope identityRequest) {
        // create HTTP entity wrapper for the identity request
        HttpEntity<byte[]> message = new HttpEntity<>(structConverter.convert(identityRequest));

        // send the identity request to the ACA and receive the identity response.
        ResponseEntity<byte[]> response =
                restTemplate.exchange(acaIdentityURL, HttpMethod.POST, message, byte[].class);

        byte[] responseBody = response.getBody();
        if (ArrayUtils.isEmpty(responseBody)) {
            return null;
        }

        // grab the response from the HTTP response
        return structConverter.convert(responseBody, IdentityResponseEnvelope.class);
    }

    /**
     * Activates a given attested identity with the TPM associated by UUID. This UUID must match up
     * with the original identity request.
     *
     * @param response to be activated
     * @param uuid     of the original identity request
     * @return the raw activated credential
     */
    byte[] activateCredential(final IdentityResponseEnvelope response, final String uuid) {
        // grab both the symmetric and asymmetric from the identity response
        byte[] asymmetricContents = response.getAsymmetricContents();
        byte[] symmetricContents = structConverter.convert(response.getSymmetricAttestation());

        // active the identity with the TPM
        return tpm.activateIdentity(asymmetricContents, symmetricContents, uuid);
    }

    /**
     * Collects the Device Info Report without TPM information, due to not having an identity
     * cert yet.
     * @return the Device Info Report
     */
    DeviceInfoReport getDeviceInfoReport() {
        DeviceInfoReportRequest request = new DeviceInfoReportRequest();
        DeviceInfoReport report = null;
        try {
            System.out.println("----> Collecting device information");
            DeviceInfoCollector deviceInfoCollector = new DeviceInfoCollector(null, false,
                    CommandLineArguments.getHostName());
            report = (DeviceInfoReport) deviceInfoCollector.collect(request);
        } catch (CollectorException e) {
            LOG.error("Error collecting device information", e);
        }
        return report;
    }
}
