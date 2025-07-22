package hirs.attestationca.persist.service;

import lombok.Getter;

/**
 * Contains a set of request mapping paths associated with the Certificate Pages endpoints.
 * Each entry in this enum corresponds to a specific URL pattern handled by the certificate-related
 * controller.
 */
@Getter
public enum CertificateType {
    /**
     * Represents the request mapping path for the endpoints inside the Platform Credentials Page controller.
     */
    PLATFORM_CREDENTIALS("platform-credentials"),

    /**
     * Represents the request mapping path for the endpoints inside the
     * Endorsement Key Credentials Page controller.
     */
    ENDORSEMENT_CREDENTIALS("endorsement-key-credentials"),

    /**
     * Represents the request mapping path for the endpoints inside the IdevId Page controller.
     */
    IDEVID_CERTIFICATES("idevid-certificates"),

    /**
     * Represents the request mapping path for the endpoints inside the Issued Certificates Page controller.
     */
    ISSUED_CERTIFICATES("issued-certificates"),

    /**
     * Represents the request mapping path for the endpoints inside the Trust Chains Page controller.
     */
    TRUST_CHAIN("trust-chain");

    private final String certificateRequestPath;

    CertificateType(final String certificateRequestPath) {
        this.certificateRequestPath = certificateRequestPath;
    }
}
