package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.ArchivableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The outcome of asking a platform component to prove possession of its key, for example an HP EpSC signing a
 * nonce issued by the ACA. Keeps the evidence and certificates so they can be downloaded and checked again.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentEvidenceResult extends ArchivableEntity {
    private static final int MAX_BLOB_SIZE = 16777215;

    /**
     * The {@link ComponentResult} for the component in the platform certificate.
     */
    private UUID componentId;

    private UUID platformCredentialId;

    private String deviceName;

    private String evidenceFormat;

    private String evidenceVersion;

    private boolean passed;

    @Getter(AccessLevel.NONE)
    private byte[] keyId;

    @Getter(AccessLevel.NONE)
    private byte[] nonce;

    @Getter(AccessLevel.NONE)
    private byte[] platformUuid;

    @Lob
    @Column(length = MAX_BLOB_SIZE)
    @Getter(AccessLevel.NONE)
    private byte[] evidence;

    @Lob
    @Column(length = MAX_BLOB_SIZE)
    @Getter(AccessLevel.NONE)
    private byte[] deviceIdCertificate;

    @Lob
    @Column(length = MAX_BLOB_SIZE)
    @Getter(AccessLevel.NONE)
    private byte[] attestationCertificate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ComponentEvidenceCheck", joinColumns = @JoinColumn(name = "resultId"))
    @OrderColumn
    private List<ComponentEvidenceCheck> checks = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param componentId          id of the component's {@link ComponentResult}
     * @param platformCredentialId id of the platform certificate that lists the component
     * @param deviceName           device that was provisioned
     * @param evidenceFormat       evidence format name, such as HP_EPSC_BOOTLOG
     * @param keyId                key candidate id from the identity claim
     * @param nonce                nonce issued by the ACA
     */
    public ComponentEvidenceResult(final UUID componentId, final UUID platformCredentialId, final String deviceName,
                                   final String evidenceFormat, final byte[] keyId, final byte[] nonce) {
        this.componentId = componentId;
        this.platformCredentialId = platformCredentialId;
        this.deviceName = deviceName;
        this.evidenceFormat = evidenceFormat;
        this.keyId = keyId.clone();
        this.nonce = nonce.clone();
    }

    /**
     * Records what the component returned.
     *
     * @param version         evidence format version
     * @param rawEvidence     the evidence
     * @param deviceId        DER Device ID certificate, or empty
     * @param attestation     DER Attestation certificate, or empty
     * @param reportedUuid    platform UUID reported by the provisioner, SMBIOS byte order, or empty
     */
    public void setEvidence(final String version, final byte[] rawEvidence, final byte[] deviceId,
                            final byte[] attestation, final byte[] reportedUuid) {
        this.evidenceVersion = version;
        this.evidence = rawEvidence.clone();
        this.deviceIdCertificate = deviceId.clone();
        this.attestationCertificate = attestation.clone();
        this.platformUuid = reportedUuid.clone();
    }

    /**
     * Adds a check. The result passes only if every check passes.
     *
     * @param name   what was checked
     * @param passed whether it passed
     * @param detail values compared, or why it failed
     * @return whether it passed
     */
    public boolean addCheck(final String name, final boolean passed, final String detail) {
        checks.add(new ComponentEvidenceCheck(name, passed, detail));
        this.passed = checks.stream().allMatch(ComponentEvidenceCheck::isPassed);
        return passed;
    }

    /**
     * @return key candidate id
     */
    public byte[] getKeyId() {
        return copy(keyId);
    }

    /**
     * @return nonce issued by the ACA
     */
    public byte[] getNonce() {
        return copy(nonce);
    }

    /**
     * @return platform UUID reported by the provisioner, SMBIOS byte order
     */
    public byte[] getPlatformUuid() {
        return copy(platformUuid);
    }

    /**
     * @return the evidence
     */
    public byte[] getEvidence() {
        return copy(evidence);
    }

    /**
     * @return DER Device ID certificate
     */
    public byte[] getDeviceIdCertificate() {
        return copy(deviceIdCertificate);
    }

    /**
     * @return DER Attestation certificate
     */
    public byte[] getAttestationCertificate() {
        return copy(attestationCertificate);
    }

    private static byte[] copy(final byte[] value) {
        return value == null ? new byte[0] : value.clone();
    }
}
