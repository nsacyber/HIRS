package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentEvidenceResultRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.DownloadFile;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentEvidenceResult;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.PlatformConfigurationV2;
import hirs.attestationca.persist.provision.helper.EpscBootlogEvidence;
import hirs.attestationca.persist.validation.ComponentEvidenceValidator;
import hirs.attestationca.persist.validation.ValidationService;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the Component Evidence page: what the ACA verified when a platform component proved possession of its key,
 * how the component's certificate links to the platform certificate and its issuer, and the files behind each step.
 */
@Service
@Log4j2
public class ComponentEvidencePageService {
    private static final String CERTIFICATE_DETAILS = "/HIRS_AttestationCAPortal/portal/certificate-details";
    private static final String PEM_HEADER = "-----BEGIN CERTIFICATE-----\n";
    private static final String PEM_FOOTER = "\n-----END CERTIFICATE-----\n";
    private static final int PEM_LINE_LENGTH = 64;

    private static final List<Download> DOWNLOADS = List.of(
            new Download("evidence", "Evidence", "evidence.bin", files -> files.result().getEvidence()),
            new Download("deviceid", "Device ID certificate", "deviceid.cer",
                    files -> files.result().getDeviceIdCertificate()),
            new Download("attestation", "Attestation certificate", "attestation.cer",
                    files -> files.result().getAttestationCertificate()),
            new Download("nonce", "Nonce issued by the ACA", "nonce.hex", files -> Hex.toHexString(
                    files.result().getNonce()).toUpperCase().getBytes(StandardCharsets.US_ASCII)),
            new Download("platform", "Platform certificate", "platform_cert.cer",
                    files -> files.platformCredential().map(Certificate::getRawBytes).orElse(new byte[0])),
            new Download("chain", "Platform certificate trust chain, issuer first", "trust_chain.pem",
                    files -> pem(files.trustChain())));

    private final ComponentEvidenceResultRepository componentEvidenceResultRepository;
    private final ComponentResultRepository componentResultRepository;
    private final CertificateRepository certificateRepository;
    private final CACredentialRepository caCredentialRepository;

    /**
     * Constructor.
     *
     * @param componentEvidenceResultRepository component evidence results
     * @param componentResultRepository         platform certificate components
     * @param certificateRepository             certificates
     * @param caCredentialRepository            trusted CA certificates
     */
    @Autowired
    public ComponentEvidencePageService(final ComponentEvidenceResultRepository componentEvidenceResultRepository,
                                        final ComponentResultRepository componentResultRepository,
                                        final CertificateRepository certificateRepository,
                                        final CACredentialRepository caCredentialRepository) {
        this.componentEvidenceResultRepository = componentEvidenceResultRepository;
        this.componentResultRepository = componentResultRepository;
        this.certificateRepository = certificateRepository;
        this.caCredentialRepository = caCredentialRepository;
    }

    /**
     * Finds a component evidence result.
     *
     * @param id result id
     * @return the result, or empty if the id is missing, malformed or unknown
     */
    public Optional<ComponentEvidenceResult> findResult(final String id) {
        try {
            return id == null ? Optional.empty() : componentEvidenceResultRepository.findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid component evidence id: {}", id);
            return Optional.empty();
        }
    }

    /**
     * Collects everything the page shows for a result.
     *
     * @param result component evidence result
     * @return page data
     */
    public HashMap<String, Object> describe(final ComponentEvidenceResult result) {
        EvidenceFiles files = files(result);
        Optional<ComponentResult> componentResult = Optional.ofNullable(result.getComponentId())
                .flatMap(componentResultRepository::findById);
        HashMap<String, Object> data = new HashMap<>();
        data.put("result", result);
        data.put("nonce", Hex.toHexString(result.getNonce()).toUpperCase());
        data.put("platformUuid", EpscBootlogEvidence.formatSmbiosUuid(result.getPlatformUuid()));
        files.platformCredential().ifPresent(pc -> data.put("platformCredentialId", pc.getId().toString()));
        Map<String, Map<String, String>> certificates = new LinkedHashMap<>();
        certificates.put("Device ID certificate", describeCertificate(result.getDeviceIdCertificate()));
        certificates.put("Attestation certificate", describeCertificate(result.getAttestationCertificate()));
        data.put("certificates", certificates);
        data.put("downloads", DOWNLOADS.stream()
                .filter(download -> download.content().apply(files).length > 0)
                .collect(Collectors.toMap(Download::key, download -> download.label() + " (" + download.fileName()
                        + ")", (first, second) -> first, LinkedHashMap::new)));
        data.put("linkGraph", linkGraph(result, componentResult, files));
        EpscBootlogEvidence.parse(result.getEvidence()).ifPresent(evidence -> {
            data.put("evidenceUuid", EpscBootlogEvidence.formatSmbiosUuid(evidence.uuid()));
            data.put("measurements", evidence.measurements());
        });
        return data;
    }

    /**
     * Gets a file for download.
     *
     * @param result component evidence result
     * @param file   evidence, deviceid, attestation, nonce, platform or chain
     * @return the file, or empty if there is no such file for this result
     */
    public Optional<DownloadFile> download(final ComponentEvidenceResult result, final String file) {
        EvidenceFiles files = files(result);
        return DOWNLOADS.stream()
                .filter(download -> download.key().equals(file))
                .map(download -> new DownloadFile(download.fileName(), download.content().apply(files)))
                .filter(download -> download.getFileBytes().length > 0)
                .findFirst();
    }

    private EvidenceFiles files(final ComponentEvidenceResult result) {
        Optional<PlatformCredential> platformCredential = platformCredential(result);
        return new EvidenceFiles(result, platformCredential,
                platformCredential.map(this::trustChain).orElse(List.of()));
    }

    /**
     * The platform certificate that lists the component. Falls back to the component's certificate serial numbers
     * if the certificate recorded with the result is gone, for example after it was deleted and uploaded again.
     *
     * @param result component evidence result
     * @return the platform certificate, or empty if none lists the component
     */
    private Optional<PlatformCredential> platformCredential(final ComponentEvidenceResult result) {
        return Optional.ofNullable(result.getPlatformCredentialId())
                .map(certificateRepository::getCertificate)
                .filter(PlatformCredential.class::isInstance)
                .map(PlatformCredential.class::cast)
                .or(() -> Optional.ofNullable(result.getComponentId())
                        .flatMap(componentResultRepository::findById)
                        .flatMap(this::platformCredentialListing));
    }

    private Optional<PlatformCredential> platformCredentialListing(final ComponentResult component) {
        return certificateRepository.byBoardSerialNumber(component.getBoardSerialNumber()).stream()
                .filter(pc -> !pc.isArchived())
                .filter(pc -> pc.getSerialNumber().toString().equals(component.getCertificateSerialNumber()))
                .findFirst();
    }

    /**
     * The trusted CA certificates that issued the platform certificate, issuer first, each verified by signature.
     *
     * @param platformCredential platform certificate
     * @return the chain, issuer first, or empty if no trusted CA verifies the platform certificate
     */
    private List<CertificateAuthorityCredential> trustChain(final PlatformCredential platformCredential) {
        Set<CertificateAuthorityCredential> candidates =
                ValidationService.getCaChainRec(platformCredential, Set.of(), caCredentialRepository);
        List<CertificateAuthorityCredential> chain = new ArrayList<>();
        Certificate current = platformCredential;
        for (int i = 0; i < candidates.size(); i++) {
            Certificate subject = current;
            Optional<CertificateAuthorityCredential> issuer = candidates.stream()
                    .filter(ca -> !chain.contains(ca) && isIssuedBy(subject, ca))
                    .findFirst();
            if (issuer.isEmpty()) {
                break;
            }
            chain.add(issuer.get());
            current = issuer.get();
        }
        return chain;
    }

    private List<LinkStep> linkGraph(final ComponentEvidenceResult result,
                                     final Optional<ComponentResult> componentResult, final EvidenceFiles files) {
        Optional<X509Certificate> deviceId = parseCertificate(result.getDeviceIdCertificate());
        byte[] deviceIdHash = deviceId.map(cert -> ComponentEvidenceValidator.sha384(cert.getSignature()))
                .orElse(new byte[0]);
        Optional<ComponentIdentifierV2> component = files.platformCredential()
                .flatMap(pc -> listedComponent(pc, componentResult, deviceIdHash));
        byte[] componentHash = component.map(c -> c.getComponentPlatformCert().getHashOverSignatureValue())
                .orElse(new byte[0]);

        List<LinkStep> steps = new ArrayList<>();
        steps.add(new LinkStep(deviceIdNode(deviceId, deviceIdHash), new LinkEdge("identified by",
                deviceIdHash.length > 0 && Arrays.equals(deviceIdHash, componentHash),
                "The component's certificate identifier is SHA-384 over the Device ID signature value")));
        steps.add(new LinkStep(componentNode(componentResult, component, componentHash), new LinkEdge(
                "listed in", component.isPresent(), "The platform certificate lists this component identifier")));
        steps.addAll(issuerSteps(files));
        return steps;
    }

    private static List<LinkStep> issuerSteps(final EvidenceFiles files) {
        if (files.platformCredential().isEmpty()) {
            return List.of(new LinkStep(new LinkNode("Platform certificate",
                    fields("Status", "Not found in the ACA"), null), null));
        }
        PlatformCredential platformCredential = files.platformCredential().get();
        List<CertificateAuthorityCredential> chain = files.trustChain();
        List<LinkStep> steps = new ArrayList<>();
        steps.add(new LinkStep(platformNode(platformCredential), new LinkEdge("signed by", !chain.isEmpty(),
                chain.isEmpty() ? "No trusted CA certificate verifies the signature"
                        : "The issuer's key verifies the platform certificate signature")));
        for (int i = 0; i < chain.size(); i++) {
            CertificateAuthorityCredential ca = chain.get(i);
            boolean last = i == chain.size() - 1;
            steps.add(new LinkStep(caNode(ca, i == 0), last ? null : new LinkEdge("signed by", true,
                    "The issuer's key verifies the certificate signature")));
        }
        if (chain.isEmpty()) {
            steps.add(new LinkStep(new LinkNode("Platform certificate issuer",
                    fields("Distinguished name", Objects.toString(platformCredential.getIssuer(), ""),
                            "Status", "Not in the trust chain"), null), null));
        }
        return steps;
    }

    /**
     * The component in the platform certificate: the one recorded with the result if known, else the one whose
     * certificate identifier matches the Device ID certificate.
     *
     * @param platformCredential platform certificate
     * @param componentResult    the component recorded with the result, if known
     * @param deviceIdHash       SHA-384 over the Device ID certificate's signature value
     * @return the component identifier, or empty if the platform certificate does not list it
     */
    private static Optional<ComponentIdentifierV2> listedComponent(final PlatformCredential platformCredential,
                                                                   final Optional<ComponentResult> componentResult,
                                                                   final byte[] deviceIdHash) {
        List<ComponentIdentifierV2> components = components(platformCredential).stream()
                .filter(component -> component.getComponentPlatformCert() != null)
                .toList();
        return componentResult
                .flatMap(result -> components.stream().filter(component -> isSame(result, component)).findFirst())
                .or(() -> components.stream()
                        .filter(component -> Arrays.equals(deviceIdHash,
                                component.getComponentPlatformCert().getHashOverSignatureValue()))
                        .findFirst());
    }

    private static List<ComponentIdentifierV2> components(final PlatformCredential platformCredential) {
        try {
            PlatformConfigurationV2 configuration = platformCredential.getPlatformConfigurationV2();
            return configuration == null ? List.of() : configuration.getComponentIdentifiers();
        } catch (IOException | IllegalArgumentException e) {
            return List.of();
        }
    }

    private static boolean isSame(final ComponentResult result, final ComponentIdentifierV2 component) {
        return Objects.equals(result.getManufacturer(), component.getComponentManufacturer().toString())
                && Objects.equals(result.getModel(), component.getComponentModel().toString())
                && Objects.equals(result.getSerialNumber(), component.getComponentSerial().toString());
    }

    private static LinkNode deviceIdNode(final Optional<X509Certificate> deviceId, final byte[] signatureHash) {
        Map<String, String> fields = new LinkedHashMap<>();
        deviceId.ifPresent(cert -> {
            // toString names serialNumber; getName() (RFC 2253) prints it as a hex-encoded OID
            fields.put("Subject", cert.getSubjectX500Principal().toString());
            fields.put("Issuer", cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal())
                    ? "Self-signed" : cert.getIssuerX500Principal().toString());
            fields.put("SHA-384 over signature value", Hex.toHexString(signatureHash));
        });
        return new LinkNode("EpSC Device ID certificate", fields, null);
    }

    private static LinkNode componentNode(final Optional<ComponentResult> componentResult,
                                          final Optional<ComponentIdentifierV2> component, final byte[] hash) {
        Map<String, String> fields = new LinkedHashMap<>();
        componentResult.ifPresent(result -> fields.put("Class", result.getComponentClassStr()));
        component.ifPresent(c -> {
            fields.put("Manufacturer", c.getComponentManufacturer().toString());
            fields.put("Model", c.getComponentModel().toString());
            fields.put("Serial", c.getComponentSerial().toString());
            fields.put("Revision", c.getComponentRevision().toString());
            fields.put("componentPlatformCert", Hex.toHexString(hash));
        });
        if (component.isEmpty()) {
            fields.put("Status", "Not found in the platform certificate");
        }
        return new LinkNode("Component identifier", fields, null);
    }

    private static LinkNode platformNode(final PlatformCredential platformCredential) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Platform", String.join(" ", Objects.toString(platformCredential.getManufacturer(), ""),
                Objects.toString(platformCredential.getModel(), "")).trim());
        fields.put("Platform serial", Objects.toString(platformCredential.getPlatformSerial(), ""));
        fields.put("Certificate serial", platformCredential.getSerialNumber().toString(Certificate.HEX_BASE));
        fields.put("Issuer", platformCredential.getIssuer());
        fields.put("Signature algorithm", Objects.toString(platformCredential.getSignatureAlgorithm(), ""));
        return new LinkNode("Platform certificate", fields,
                CERTIFICATE_DETAILS + "?id=" + platformCredential.getId() + "&type=platform");
    }

    private static LinkNode caNode(final CertificateAuthorityCredential ca, final boolean issuer) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Subject", ca.getSubject());
        fields.put("Serial", ca.getSerialNumber().toString(Certificate.HEX_BASE));
        fields.put("Key", Objects.toString(ca.getPublicKeyAlgorithm(), ""));
        if (ca.getSubject() != null && ca.getSubject().equals(ca.getIssuer())) {
            fields.put("Trust anchor", "Self-signed");
        }
        return new LinkNode(issuer ? "Platform certificate issuer" : "CA certificate", fields,
                CERTIFICATE_DETAILS + "?id=" + ca.getId() + "&type=certificateauthority");
    }

    private static Map<String, String> fields(final String... namesAndValues) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (int i = 0; i + 1 < namesAndValues.length; i += 2) {
            fields.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return fields;
    }

    private static boolean isIssuedBy(final Certificate certificate, final CertificateAuthorityCredential ca) {
        try {
            return certificate.isIssuer(ca).isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    private static Map<String, String> describeCertificate(final byte[] der) {
        Map<String, String> description = new LinkedHashMap<>();
        parseCertificate(der).ifPresent(certificate -> {
            description.put("Subject", certificate.getSubjectX500Principal().toString());
            description.put("Issuer", certificate.getIssuerX500Principal().toString());
            description.put("Serial number", certificate.getSerialNumber().toString(Certificate.HEX_BASE));
            description.put("Valid", certificate.getNotBefore() + " to " + certificate.getNotAfter());
            description.put("Signature algorithm", certificate.getSigAlgName());
        });
        return description;
    }

    private static Optional<X509Certificate> parseCertificate(final byte[] der) {
        try {
            return Optional.of((X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der)));
        } catch (CertificateException e) {
            return Optional.empty();
        }
    }

    private static byte[] pem(final List<CertificateAuthorityCredential> chain) {
        Base64.Encoder encoder = Base64.getMimeEncoder(PEM_LINE_LENGTH, "\n".getBytes(StandardCharsets.US_ASCII));
        return chain.stream()
                .map(ca -> PEM_HEADER + encoder.encodeToString(ca.getRawBytes()) + PEM_FOOTER)
                .collect(Collectors.joining())
                .getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * A certificate or identifier in the link graph.
     *
     * @param title  what it is
     * @param fields values to show
     * @param link   details page, or null
     */
    public record LinkNode(String title, Map<String, String> fields, String link) {
    }

    /**
     * How one node leads to the next.
     *
     * @param label    relationship, such as "signed by"
     * @param verified whether the ACA verified it when the page loaded
     * @param detail   what was verified
     */
    public record LinkEdge(String label, boolean verified, String detail) {
    }

    /**
     * A node and its link to the next node. The last step has no link.
     *
     * @param node the node
     * @param next link to the next node, or null
     */
    public record LinkStep(LinkNode node, LinkEdge next) {
    }

    private record EvidenceFiles(ComponentEvidenceResult result, Optional<PlatformCredential> platformCredential,
                                 List<CertificateAuthorityCredential> trustChain) {
    }

    private record Download(String key, String label, String fileName, Function<EvidenceFiles, byte[]> content) {
    }
}
