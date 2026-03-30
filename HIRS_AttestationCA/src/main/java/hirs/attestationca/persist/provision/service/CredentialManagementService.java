package hirs.attestationca.persist.provision.service;

import com.google.protobuf.ByteString;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.exceptions.CertificateProcessingException;
import hirs.attestationca.persist.exceptions.DBManagerException;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Service class responsible for parsing {@link PlatformCredential} and {@link EndorsementCredential} objects,
 * along with related data such as device components from the Identity Claim. It also handles storing these objects
 * and assists in generating credentials, including the {@link IssuedAttestationCertificate}.
 */
@Service
@Log4j2
public class CredentialManagementService {
    private final PolicyRepository policyRepository;
    private final CertificateRepository certificateRepository;
    private final ComponentResultRepository componentResultRepository;

    /**
     * Constructor.
     *
     * @param policyRepository          policy repository
     * @param certificateRepository     certificate repository
     * @param componentResultRepository component result repository
     */
    @Autowired
    public CredentialManagementService(final PolicyRepository policyRepository,
                                       final CertificateRepository certificateRepository,
                                       final ComponentResultRepository componentResultRepository) {
        this.policyRepository = policyRepository;
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
    }

    /**
     * Parses an Endorsement Credential from a Protobuf generated
     * IdentityClaim. Will also check if the Endorsement Credential was already uploaded.
     * Persists the Endorsement Credential if it does not already exist.
     *
     * @param identityClaim a Protobuf generated Identity Claim object
     * @param ekPublicKey   the endorsement public key from the Identity Claim object
     * @return the Endorsement Credential, if one exists, null otherwise
     */
    public EndorsementCredential parseEcFromIdentityClaim(final ProvisionerTpm2.IdentityClaim identityClaim,
                                                          final PublicKey ekPublicKey) {
        EndorsementCredential endorsementCredential = null;

        if (identityClaim.hasEndorsementCredential()) {
            endorsementCredential = storeEndorsementCredential(identityClaim.getEndorsementCredential().toByteArray(),
                    identityClaim.getDv().getNw().getHostname());
        } else if (ekPublicKey != null) {
            log.warn("Endorsement Credential was not in the identity claim from the client.Checking for uploads.");
            endorsementCredential = getEndorsementCredential(ekPublicKey);
        } else {
            log.warn("No endorsement credential was received in identity claim and no EK Public"
                    + " Key was provided to check for uploaded certificates.");
        }

        return endorsementCredential;
    }

    /**
     * Helper method to parse a set of Platform Credentials from a Protobuf generated
     * IdentityClaim and Endorsement Credential. Persists the Platform Credentials if they
     * do not already exist.
     *
     * @param identityClaim         a Protobuf generated Identity Claim object
     * @param endorsementCredential an endorsement credential to check if platform credentials
     *                              exist
     * @return the List of Platform Credentials, if they exist, an empty set otherwise
     */
    public List<PlatformCredential> parsePcsFromIdentityClaim(final ProvisionerTpm2.IdentityClaim identityClaim,
                                                              final EndorsementCredential endorsementCredential) {
        List<PlatformCredential> platformCredentials = new LinkedList<>();

        if (identityClaim.getPlatformCredentialCount() > 0) {

            List<ByteString> platformCredentialList = identityClaim.getPlatformCredentialList();

            for (ByteString platformCredential : platformCredentialList) {
                if (!platformCredential.isEmpty()) {
                    PlatformCredential storedPlatformCredential =
                            storePlatformCredential(platformCredential.toByteArray(),
                                    identityClaim.getDv().getNw().getHostname());

                    if (storedPlatformCredential != null) {
                        platformCredentials.add(storedPlatformCredential);
                    }
                }
            }
        } else if (endorsementCredential != null) {
            // if none in the identity claim, look for uploaded platform credentials
            log.warn("PC was not in the identity claim from the client. Checking for uploads.");
            platformCredentials.addAll(getPlatformCredentials(endorsementCredential));
        } else {
            log.warn("No platform credential received in identity claim.");
        }

        return platformCredentials;
    }

    /**
     * Parses and stores the EK in the cert manager. If the cert is already present and archived,
     * it is unarchived.
     *
     * @param endorsementBytes the raw EK bytes used for parsing
     * @param deviceName       the host name
     * @return the parsed, valid EK
     * @throws IllegalArgumentException if the provided bytes are not a valid EK.
     */
    public EndorsementCredential storeEndorsementCredential(final byte[] endorsementBytes, final String deviceName)
            throws IllegalArgumentException {

        if (endorsementBytes == null) {
            throw new IllegalArgumentException("null endorsement credential bytes");
        }

        if (endorsementBytes.length <= 1) {
            throw new IllegalArgumentException(String.format("%d-length byte array given for endorsement credential",
                    endorsementBytes.length)
            );
        }

        log.info("Parsing Endorsement Credential of length {}", endorsementBytes.length);

        EndorsementCredential endorsementCredential;
        try {
            endorsementCredential = EndorsementCredential.parseWithPossibleHeader(endorsementBytes);
        } catch (IllegalArgumentException iae) {
            log.error(iae.getMessage());
            throw iae;
        }

        int certificateHash = endorsementCredential.getCertificateHash();
        EndorsementCredential existingCredential = (EndorsementCredential) certificateRepository
                .findByCertificateHash(certificateHash);

        if (existingCredential == null) {
            log.info("No Endorsement Credential found with hash: {}", certificateHash);
            endorsementCredential.setDeviceName(deviceName);
            return certificateRepository.save(endorsementCredential);
        } else if (existingCredential.isArchived()) {
            // if the EK is stored in the DB and it's archived, un-archive it.
            log.info("Un-archiving endorsement credential");
            existingCredential.restore();
            existingCredential.resetCreateTime();
            certificateRepository.save(existingCredential);
        }
        return existingCredential;
    }

    /**
     * Parses and stores the PC in the cert manager. If the cert is already present and archived,
     * it is unarchived.
     *
     * @param platformBytes the raw PC bytes used for parsing
     * @param deviceName    the host name of the associated machine
     * @return the parsed, valid PC, or null if the provided bytes are not a valid EK.
     */
    public PlatformCredential storePlatformCredential(final byte[] platformBytes, final String deviceName) {

        if (platformBytes == null) {
            log.error("The provided platform credential byte array is null.");
            throw new IllegalArgumentException("null platform credential bytes");
        }

        if (platformBytes.length == 0) {
            log.error("The provided platform credential byte array is null.");
            throw new IllegalArgumentException("zero-length byte array given for platform credential");
        }

        log.info("Parsing Platform Credential of length {}", platformBytes.length);

        try {
            PlatformCredential platformCredential =
                    PlatformCredential.parseWithPossibleHeader(platformBytes);

            if (platformCredential == null) {
                log.error("The platform credential that was parsed with the provided byte array was null");
                return null;
            }

            PlatformCredential existingCredential = (PlatformCredential) certificateRepository
                    .findByCertificateHash(platformCredential.getCertificateHash());

            if (existingCredential == null) {
                if (platformCredential.getPlatformSerial() != null) {
                    List<PlatformCredential> certificates = certificateRepository
                            .byBoardSerialNumber(platformCredential.getPlatformSerial());
                    if (!certificates.isEmpty()) {
                        // found associated certificates
                        for (PlatformCredential pc : certificates) {
                            if (pc.isPlatformBase() && platformCredential.isPlatformBase()) {
                                // found a base in the database associated with
                                // parsed certificate
                                log.error("Base certificate stored"
                                                + " in database with same platform"
                                                + "serial number. ({})",
                                        platformCredential.getPlatformSerial());
                                return null;
                            }
                        }
                    }
                }
                platformCredential.setDeviceName(deviceName);
                return certificateRepository.save(platformCredential);
            } else if (existingCredential.isArchived()) {
                // if the PC is stored in the DB and it's archived, un-archive it.
                log.info("Un-archiving platform credential");
                existingCredential.restore();
                certificateRepository.save(existingCredential);
                return existingCredential;
            }

            return existingCredential;
        } catch (DBManagerException dbEx) {
            log.error("Error retrieving or saving platform credential to the database", dbEx);
        } catch (Exception e) {
            log.error("Error parsing platform credential", e);
        }

        log.error("Due to an exception being thrown while attempting to store platform certificate(s) "
                + "this method will return a null platform certificate.");
        return null;
    }

    /**
     * Stores the Platform Certificate's list of associated component results.
     *
     * @param platformCredentials list of platform credentials
     * @throws IOException if any issues arise from storing the platform credentials components
     */
    public void saveOrUpdatePlatformCertificateComponents(final List<PlatformCredential> platformCredentials)
            throws IOException {

        handleSpecialCaseForPlatformCertificates(platformCredentials);

        // store component results objects
        for (PlatformCredential platformCredential : platformCredentials) {
            List<ComponentResult> componentResults =
                    componentResultRepository.findByCertificateSerialNumberAndBoardSerialNumber(
                            platformCredential.getSerialNumber().toString(),
                            platformCredential.getPlatformSerial());

            if (componentResults.isEmpty()) {
                savePlatformCertificateComponents(platformCredential);
            } else {
                componentResults.forEach((componentResult) -> {
                    componentResult.restore();
                    componentResult.resetCreateTime();
                    componentResultRepository.save(componentResult);
                });
            }
        }
    }

    /**
     * Creates an {@link IssuedAttestationCertificate} object and set its corresponding device and persists it.
     *
     * @param derEncodedAttestationCertificate the byte array representing the Attestation
     *                                         certificate
     * @param endorsementCredential            the endorsement credential used to generate the AC
     * @param platformCredentials              the platform credentials used to generate the AC
     * @param device                           the device to which the attestation certificate is tied
     * @param ldevID                           whether the certificate is a ldevid
     * @return whether the certificate was saved successfully
     */
    public boolean saveAttestationCertificate(
            final byte[] derEncodedAttestationCertificate,
            final EndorsementCredential endorsementCredential,
            final List<PlatformCredential> platformCredentials,
            final Device device,
            final boolean ldevID) {
        List<IssuedAttestationCertificate> issuedAc;
        boolean generateCertificate;
        PolicySettings policySettings;
        Date currentDate = new Date();
        int days;
        try {
            // save issued certificate
            IssuedAttestationCertificate attCert = new IssuedAttestationCertificate(
                    derEncodedAttestationCertificate, endorsementCredential, platformCredentials, ldevID);

            policySettings = policyRepository.findByName("Default");

            Sort sortCriteria = Sort.by(Sort.Direction.DESC, "endValidity");
            issuedAc = certificateRepository.findByDeviceIdAndLdevID(device.getId(), ldevID,
                    sortCriteria);

            generateCertificate = ldevID ? policySettings.isIssueDevIdCertificateEnabled()
                    : policySettings.isIssueAttestationCertificateEnabled();

            if (issuedAc != null && !issuedAc.isEmpty()
                    && (ldevID ? policySettings.isGenerateDevIdCertificateOnExpiration()
                    : policySettings.isGenerateAttestationCertificateOnExpiration())) {
                if (issuedAc.getFirst().getEndValidity().after(currentDate)) {
                    // so the issued AC is not expired
                    // however are we within the threshold
                    days = ProvisionUtils.daysBetween(currentDate, issuedAc.getFirst().getEndValidity());
                    generateCertificate =
                            days < (ldevID ? policySettings.getDevIdReissueThreshold()
                                    : policySettings.getReissueThreshold());
                }
            }

            if (generateCertificate) {
                attCert.setDeviceId(device.getId());
                attCert.setDeviceName(device.getName());
                certificateRepository.save(attCert);
            }
        } catch (Exception e) {
            log.error("Error saving generated Attestation Certificate to database.", e);
            throw new CertificateProcessingException(
                    "Encountered error while storing Attestation Certificate: "
                            + e.getMessage(), e);
        }

        return generateCertificate;
    }

    /**
     * Gets the Endorsement Credential from the DB given the EK public key.
     *
     * @param ekPublicKey the EK public key
     * @return the Endorsement credential, if found, otherwise null
     */
    private EndorsementCredential getEndorsementCredential(final PublicKey ekPublicKey) {
        log.debug("Searching for endorsement credential based on public key: {}", ekPublicKey);

        if (ekPublicKey == null) {
            throw new IllegalArgumentException("Cannot look up an EC given a null public key");
        }

        EndorsementCredential credential = null;

        try {
            credential = certificateRepository.findByPublicKeyModulusHexValue(
                    Certificate.getPublicKeyModulus(ekPublicKey).toString(Certificate.HEX_BASE));
        } catch (IOException e) {
            log.error("Could not extract public key modulus", e);
        }

        if (credential == null) {
            log.warn("Unable to find endorsement credential for public key.");
        } else {
            log.debug("Endorsement credential found.");
        }

        return credential;
    }

    /**
     * Helper method that retrieves all the platform credentials associated with the provided Endorsement Credential.
     *
     * @param ec endorsement credential
     * @return list of platform credentials
     */
    private List<PlatformCredential> getPlatformCredentials(final EndorsementCredential ec) {
        List<PlatformCredential> credentials = null;

        if (ec == null) {
            log.warn("Cannot look for platform credential(s). Endorsement credential was null.");
        } else {
            log.debug("Searching for platform credential(s) based on holder serial number: {}",
                    ec.getSerialNumber());
            credentials = certificateRepository.getByHolderSerialNumber(ec.getSerialNumber());

            if (credentials == null || credentials.isEmpty()) {
                log.warn("No platform credential(s) found");
            } else {
                log.debug("Platform Credential(s) found: {}", credentials.size());
            }
        }

        return credentials;
    }

    /**
     * There are situations in which the claim is sent with no platform certificates or a platform certificate
     * from the tpm which will be deprecated. This is to check what is in the platform object and pull
     * additional information from the DB if information exists.
     *
     * @param platformCredentials list of platform credentials
     */
    private void handleSpecialCaseForPlatformCertificates(final List<PlatformCredential> platformCredentials) {

        if (platformCredentials.size() == 1) {
            List<PlatformCredential> additionalPC = new LinkedList<>();
            PlatformCredential pc = platformCredentials.getFirst();
            if (pc != null && pc.getPlatformSerial() != null) {
                additionalPC.addAll(certificateRepository.byBoardSerialNumber(pc.getPlatformSerial()));
            }
            platformCredentials.addAll(additionalPC);
        }
    }

    /**
     * Helper method that saves the provided platform certificate's components in the database.
     *
     * @param certificate certificate
     */
    private void savePlatformCertificateComponents(final Certificate certificate) throws IOException {
        PlatformCredential platformCredential;

        if (certificate instanceof PlatformCredential) {
            platformCredential = (PlatformCredential) certificate;
            ComponentResult componentResult;

            if (platformCredential.getPlatformConfigurationV1() != null) {
                List<ComponentIdentifier> componentIdentifiers = platformCredential
                        .getComponentIdentifiers();

                for (ComponentIdentifier componentIdentifier : componentIdentifiers) {
                    componentResult = new ComponentResult(platformCredential.getPlatformSerial(),
                            platformCredential.getSerialNumber().toString(),
                            platformCredential.getPlatformChainType(),
                            componentIdentifier);
                    componentResult.setFailedValidation(false);
                    componentResult.setDelta(!platformCredential.isPlatformBase());
                    componentResultRepository.save(componentResult);
                }
            } else if (platformCredential.getPlatformConfigurationV2() != null) {
                List<ComponentIdentifierV2> componentIdentifiersV2 = platformCredential
                        .getComponentIdentifiersV2();

                for (ComponentIdentifierV2 componentIdentifierV2 : componentIdentifiersV2) {
                    componentResult = new ComponentResult(platformCredential.getPlatformSerial(),
                            platformCredential.getSerialNumber().toString(),
                            platformCredential.getPlatformChainType(),
                            componentIdentifierV2);
                    componentResult.setFailedValidation(false);
                    componentResult.setDelta(!platformCredential.isPlatformBase());
                    componentResultRepository.save(componentResult);
                }
            }
        }
    }
}
