package hirs.attestationca.persist.provision.service;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.ComponentEvidenceResultRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentEvidenceCheck;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentEvidenceResult;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.PlatformConfigurationV2;
import hirs.attestationca.persist.provision.helper.EpscBootlogEvidence;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import hirs.attestationca.persist.validation.ComponentEvidenceValidator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Asks platform components to prove possession of their keys during provisioning.
 * <p>
 * The provisioner offers component keys as external {@link ProvisionerTpm2.KeyCandidate}s. A candidate qualifies when
 * a platform certificate lists a component whose componentPlatformCert identifies the same certificate. The ACA
 * sends a nonce for each one with the identity claim response, and verifies the evidence returned with the
 * certificate request.
 */
@Service
@Log4j2
public class ComponentEvidenceService {
    private static final ProvisionerTpm2.EvidenceFormat SUPPORTED_FORMAT =
            ProvisionerTpm2.EvidenceFormat.HP_EPSC_BOOTLOG;

    private final ComponentResultRepository componentResultRepository;
    private final ComponentEvidenceResultRepository componentEvidenceResultRepository;

    /**
     * Constructor.
     *
     * @param componentResultRepository         platform certificate components
     * @param componentEvidenceResultRepository component evidence results
     */
    @Autowired
    public ComponentEvidenceService(final ComponentResultRepository componentResultRepository,
                                    final ComponentEvidenceResultRepository componentEvidenceResultRepository) {
        this.componentResultRepository = componentResultRepository;
        this.componentEvidenceResultRepository = componentEvidenceResultRepository;
    }

    /**
     * Issues a nonce for each component key in the claim that a platform certificate identifies.
     *
     * @param identityClaim        the identity claim
     * @param platformCredentials  the device's platform certificates, already validated
     * @return challenges to send to the provisioner
     */
    public List<ProvisionerTpm2.VerificationChallenge> createChallenges(
            final ProvisionerTpm2.IdentityClaim identityClaim, final List<PlatformCredential> platformCredentials) {
        return componentCandidates(identityClaim)
                .filter(candidate -> isListed(candidate, platformCredentials))
                .map(candidate -> ProvisionerTpm2.VerificationChallenge.newBuilder()
                        .setKeyId(candidate.getKeyId())
                        .setExternalEvidence(ProvisionerTpm2.ExternalEvidenceChallenge.newBuilder()
                                .setNonce(ByteString.copyFrom(
                                        ProvisionUtils.generateRandomBytes(EpscBootlogEvidence.NONCE_SIZE)))
                                .setEvidenceFormat(SUPPORTED_FORMAT))
                        .build())
                .toList();
    }

    /**
     * Serializes challenges so they can be kept with the provisioner state until the certificate request.
     *
     * @param challenges challenges sent to the provisioner
     * @return serialized challenges
     */
    public static byte[] serializeChallenges(final List<ProvisionerTpm2.VerificationChallenge> challenges) {
        return ProvisionerTpm2.IdentityClaimResponse.newBuilder().addAllChallenge(challenges).build().toByteArray();
    }

    /**
     * Verifies the provisioner's answers to the challenges and saves a result for each component.
     *
     * @param serializedChallenges challenges kept from the identity claim, may be null
     * @param identityClaim        the identity claim
     * @param certificateRequest   the certificate request with the answers
     * @param platformCredentials  the device's platform certificates
     * @param deviceName           the device's name
     * @return a result for each challenge
     */
    public List<ComponentEvidenceResult> verify(final byte[] serializedChallenges,
                                                final ProvisionerTpm2.IdentityClaim identityClaim,
                                                final ProvisionerTpm2.CertificateRequest certificateRequest,
                                                final List<PlatformCredential> platformCredentials,
                                                final String deviceName) {
        byte[] platformUuid = identityClaim.getDv().getHw().getPlatformUuid().toByteArray();
        return parseChallenges(serializedChallenges).stream()
                .map(challenge -> verify(challenge, identityClaim, certificateRequest, platformCredentials,
                        deviceName, platformUuid))
                .map(componentEvidenceResultRepository::save)
                .toList();
    }

    /**
     * Summarizes results for the provisioner.
     *
     * @param results component evidence results
     * @return one line per component
     */
    public static String summarize(final List<ComponentEvidenceResult> results) {
        return results.stream()
                .map(result -> "Component evidence (" + result.getEvidenceFormat() + "): "
                        + (result.isPassed() ? "PASS" : "FAIL (" + failedChecks(result) + ")"))
                .collect(Collectors.joining("; "));
    }

    private ComponentEvidenceResult verify(final ProvisionerTpm2.VerificationChallenge challenge,
                                           final ProvisionerTpm2.IdentityClaim identityClaim,
                                           final ProvisionerTpm2.CertificateRequest certificateRequest,
                                           final List<PlatformCredential> platformCredentials,
                                           final String deviceName, final byte[] platformUuid) {
        byte[] certIdentifier = componentCandidates(identityClaim)
                .filter(candidate -> candidate.getKeyId().equals(challenge.getKeyId()))
                .map(candidate -> candidate.getExternal().getCertDigest().toByteArray())
                .findFirst()
                .orElse(new byte[0]);
        Optional<ListedComponent> listed = findComponent(platformCredentials, certIdentifier);
        ComponentEvidenceResult result = new ComponentEvidenceResult(
                listed.map(this::componentResultId).orElse(null),
                listed.map(component -> component.platformCredential().getId()).orElse(null),
                deviceName, SUPPORTED_FORMAT.name(), challenge.getKeyId().toByteArray(),
                challenge.getExternalEvidence().getNonce().toByteArray());

        result.addCheck("Platform certificate signed by a trusted CA", listed.isPresent(), listed
                .map(component -> "Validated during platform credential validation. Issuer "
                        + component.platformCredential().getIssuer() + ", serial "
                        + component.platformCredential().getSerialNumber().toString(Certificate.HEX_BASE))
                .orElse("No validated platform certificate lists this component"));
        Optional<ProvisionerTpm2.ExternalEvidenceResult> answer = certificateRequest.getVerificationResultList()
                .stream()
                .filter(candidate -> candidate.getKeyId().equals(challenge.getKeyId())
                        && candidate.hasExternalEvidence())
                .map(ProvisionerTpm2.VerificationResult::getExternalEvidence)
                .findFirst();
        if (result.addCheck("Provisioner returned evidence", answer.isPresent()
                && !answer.get().getEvidence().isEmpty(), answer.isPresent() ? "" : "No answer to the challenge")) {
            ComponentEvidenceValidator.validateEpscBootlog(result, certIdentifier, platformUuid, answer.get());
        }
        log.info("Component evidence for {} on {}: {}", SUPPORTED_FORMAT, deviceName,
                result.isPassed() ? "PASS" : "FAIL (" + failedChecks(result) + ")");
        return result;
    }

    private static Stream<ProvisionerTpm2.KeyCandidate> componentCandidates(
            final ProvisionerTpm2.IdentityClaim identityClaim) {
        return identityClaim.getKeyCandidatesList().stream()
                .filter(candidate -> candidate.hasExternal()
                        && candidate.getExternal().getEvidenceFormat() == SUPPORTED_FORMAT);
    }

    private static boolean isListed(final ProvisionerTpm2.KeyCandidate candidate,
                                    final List<PlatformCredential> platformCredentials) {
        boolean listed = findComponent(platformCredentials,
                candidate.getExternal().getCertDigest().toByteArray()).isPresent();
        if (!listed) {
            log.warn("The provisioner offered a {} component key that no platform certificate identifies. "
                    + "It will not be challenged.", SUPPORTED_FORMAT);
        }
        return listed;
    }

    private static Optional<ListedComponent> findComponent(final List<PlatformCredential> platformCredentials,
                                                           final byte[] certIdentifier) {
        if (certIdentifier.length == 0) {
            return Optional.empty();
        }
        return platformCredentials.stream()
                .flatMap(platformCredential -> components(platformCredential).stream()
                        .map(component -> new ListedComponent(platformCredential, component)))
                .filter(listed -> listed.component().getComponentPlatformCert() != null
                        && Arrays.equals(listed.component().getComponentPlatformCert().getHashOverSignatureValue(),
                        certIdentifier))
                .findFirst();
    }

    private static List<ComponentIdentifierV2> components(final PlatformCredential platformCredential) {
        try {
            PlatformConfigurationV2 configuration = platformCredential.getPlatformConfigurationV2();
            return configuration == null ? List.of() : configuration.getComponentIdentifiers();
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Could not read the components of platform certificate {}", platformCredential.getId());
            return List.of();
        }
    }

    private UUID componentResultId(final ListedComponent listed) {
        PlatformCredential platformCredential = listed.platformCredential();
        ComponentIdentifierV2 component = listed.component();
        return componentResultRepository.findByCertificateSerialNumberAndBoardSerialNumber(
                        platformCredential.getSerialNumber().toString(), platformCredential.getPlatformSerial())
                .stream()
                .filter(result -> isSameComponent(result, component))
                .map(ComponentResult::getId)
                .findFirst()
                .orElse(null);
    }

    private static boolean isSameComponent(final ComponentResult result, final ComponentIdentifierV2 component) {
        return Objects.equals(result.getManufacturer(), component.getComponentManufacturer().toString())
                && Objects.equals(result.getModel(), component.getComponentModel().toString())
                && Objects.equals(result.getSerialNumber(), component.getComponentSerial().toString());
    }

    private static List<ProvisionerTpm2.VerificationChallenge> parseChallenges(final byte[] serializedChallenges) {
        if (serializedChallenges == null || serializedChallenges.length == 0) {
            return List.of();
        }
        try {
            return ProvisionerTpm2.IdentityClaimResponse.parseFrom(serializedChallenges).getChallengeList();
        } catch (InvalidProtocolBufferException e) {
            log.error("Could not read the challenges kept with the provisioner state");
            return List.of();
        }
    }

    private static String failedChecks(final ComponentEvidenceResult result) {
        return result.getChecks().stream()
                .filter(check -> !check.isPassed())
                .map(ComponentEvidenceCheck::getName)
                .collect(Collectors.joining(", "));
    }

    private record ListedComponent(PlatformCredential platformCredential, ComponentIdentifierV2 component) {
    }
}
