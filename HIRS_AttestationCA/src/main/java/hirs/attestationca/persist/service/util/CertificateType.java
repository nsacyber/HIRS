package hirs.attestationca.persist.service.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Contains a set of request mapping paths associated with the Certificate Pages endpoints.
 * Each entry in this enum corresponds to a specific URL pattern handled by the certificate-related
 * controller.
 */
@AllArgsConstructor
@Getter
public enum CertificateType {
    /**
     * Represents the request mapping path for the endpoints inside the Platform Credentials Page controller.
     */
    PLATFORM_CREDENTIALS("platform-credentials", "PlatformCredential"),

    /**
     * Holds the request mapping path and certificate type for the endpoints inside the
     * Endorsement Key Credentials Page controller.
     */
    ENDORSEMENT_CREDENTIALS("endorsement-key-credentials",
            "EndorsementCredential"),

    /**
     * Holds the request mapping path and certificate type for the endpoints inside the
     * IdevId Page controller.
     */
    IDEVID_CERTIFICATES("idevid-certificates", "IDevIDCertificate"),

    /**
     * Holds the request mapping path and certificate type for the endpoints inside the
     * Issued Certificates Page controller.
     */
    ISSUED_CERTIFICATES("issued-certificates",
            "IssuedAttestationCertificate"),

    /**
     * Holds the request mapping path and certificate type for the endpoints
     * inside the Trust Chains Page controller.
     */
    TRUST_CHAIN("trust-chain", "CertificateAuthorityCredential");

    /**
     * The path that is used for that specific certificate's controller.
     */
    private final String certificateRequestPath;

    /**
     * The certificate type that is used in the database.
     */
    private final String certificateTypeName;
}
