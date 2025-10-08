package hirs.attestationca.persist.entity.userdefined.certificate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an issued attestation certificate to a HIRS Client.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class IssuedAttestationCertificate extends DeviceAssociatedCertificate {

    /**
     * AIC label that must be used.
     */
    public static final String AIC_TYPE_LABEL = "TCPA Trusted Platform Identity";

    @Column
    private boolean ldevID;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ek_id")
    private EndorsementCredential endorsementCredential;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "issued_attestation_platform_join_table",
            joinColumns = @JoinColumn(name = "issued_attestation_id"),         // foreign key to this certificate
            inverseJoinColumns = @JoinColumn(name = "platform_credential_id") // foreign key to related certificates
    )
    private List<PlatformCredential> platformCredentials;

    /**
     * Constructor.
     *
     * @param certificateBytes      the issued certificate bytes
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials   the platform credentials
     * @param ldevID                is LDevID
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public IssuedAttestationCertificate(final byte[] certificateBytes,
                                        final EndorsementCredential endorsementCredential,
                                        final List<PlatformCredential> platformCredentials,
                                        final boolean ldevID)
            throws IOException {
        super(certificateBytes);
        this.endorsementCredential = endorsementCredential;
        this.platformCredentials = new ArrayList<>(platformCredentials);
        this.ldevID = ldevID;
    }

    /**
     * Constructor.
     *
     * @param certificatePath       path to certificate
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials   the platform credentials
     * @param ldevID                is it an LDevID
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public IssuedAttestationCertificate(final Path certificatePath,
                                        final EndorsementCredential endorsementCredential,
                                        final List<PlatformCredential> platformCredentials,
                                        final boolean ldevID)
            throws IOException {
        this(readBytes(certificatePath), endorsementCredential, platformCredentials, ldevID);
    }
}
