package hirs.attestationca.persist;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentInfoRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.IssuedCertificateRepository;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.manager.TPM2ProvisionerStateRepository;
import hirs.attestationca.persist.provision.CertificateRequestProcessor;
import hirs.attestationca.persist.provision.IdentityClaimProcessor;
import hirs.attestationca.persist.service.SupplyChainValidationService;
import hirs.structs.converters.StructConverter;
import lombok.extern.log4j.Log4j2;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Provides base implementation of common tasks of an ACA that are required for attestation of an
 * Identity Request.
 */
@Log4j2
public abstract class AttestationCertificateAuthority {

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

    private final ComponentResultRepository componentResultRepository;
    private ComponentInfoRepository componentInfoRepository;
    private final CertificateRepository certificateRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;
    private final ReferenceManifestRepository referenceManifestRepository;
    private final DeviceRepository deviceRepository;
//    private final DBManager<TPM2ProvisionerState> tpm2ProvisionerStateDBManager;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final PolicyRepository policyRepository;
    private final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository;

    private CertificateRequestProcessor certificateRequestHandler;
    private IdentityClaimProcessor identityClaimHandler;

    /**
     * Constructor.
     * @param supplyChainValidationService the supply chain service
     * @param privateKey the ACA private key
     * @param acaCertificate the ACA certificate
     * @param structConverter the struct converter
     * @param componentResultRepository the component result manager
     * @param componentInfoRepository the component info manager
     * @param certificateRepository the certificate manager
     * @param referenceManifestRepository the Reference Manifest manager
     * @param validDays the number of days issued certs are valid
     * @param deviceRepository the device manager
     * @param referenceDigestValueRepository the reference event manager
     * @param policyRepository policy setting repository
     * @param tpm2ProvisionerStateRepository tpm2 provisioner state repository
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public AttestationCertificateAuthority(
            final SupplyChainValidationService supplyChainValidationService,
            final PrivateKey privateKey, final X509Certificate acaCertificate,
            final StructConverter structConverter,
            final ComponentResultRepository componentResultRepository,
            final ComponentInfoRepository componentInfoRepository,
            final CertificateRepository certificateRepository,
            final IssuedCertificateRepository issuedCertificateRepository,
            final ReferenceManifestRepository referenceManifestRepository,
            final int validDays,
            final DeviceRepository deviceRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository,
            final PolicyRepository policyRepository,
            final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository) {
        this.supplyChainValidationService = supplyChainValidationService;
        this.privateKey = privateKey;
        this.acaCertificate = acaCertificate;
        this.structConverter = structConverter;
        this.componentResultRepository = componentResultRepository;
        this.componentInfoRepository = componentInfoRepository;
        this.certificateRepository = certificateRepository;
        this.issuedCertificateRepository = issuedCertificateRepository;
        this.referenceManifestRepository = referenceManifestRepository;
        this.validDays = validDays;
        this.deviceRepository = deviceRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.policyRepository = policyRepository;
        this.tpm2ProvisionerStateRepository = tpm2ProvisionerStateRepository;

        this.certificateRequestHandler = new CertificateRequestProcessor(supplyChainValidationService,
                certificateRepository, deviceRepository,
                privateKey, acaCertificate, validDays, tpm2ProvisionerStateRepository);
        this.identityClaimHandler = new IdentityClaimProcessor(supplyChainValidationService,
                certificateRepository, componentResultRepository, componentInfoRepository,
                referenceManifestRepository, referenceDigestValueRepository,
                deviceRepository, tpm2ProvisionerStateRepository, policyRepository);
    }

    byte[] processIdentityClaimTpm2(final byte[] identityClaim) {
        return this.identityClaimHandler.processIdentityClaimTpm2(identityClaim);
    }

    byte[] processCertificateRequest(final byte[] certificateRequest) {
        return this.certificateRequestHandler.processCertificateRequest(certificateRequest);
    }

    public byte[] getPublicKey() {
        return acaCertificate.getPublicKey().getEncoded();
    }
}
