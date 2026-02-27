package hirs.attestationca.persist.provision.helper;

import com.google.protobuf.ByteString;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;


/**
 * Utility class which includes credential management functions used by the ACA.
 */
@Log4j2
public final class CredentialManagementHelper {

    private CredentialManagementHelper() {
    }

    /**
     * Parses and stores the EK in the cert manager. If the cert is already present and archived,
     * it is unarchived.
     *
     * @param certificateRepository the certificate manager used for storage
     * @param endorsementBytes      the raw EK bytes used for parsing
     * @param deviceName            the host name
     * @return the parsed, valid EK
     * @throws IllegalArgumentException if the provided bytes are not a valid EK.
     */
    public static EndorsementCredential storeEndorsementCredential(
            final CertificateRepository certificateRepository,
            final byte[] endorsementBytes, final String deviceName) throws IllegalArgumentException {

        if (certificateRepository == null) {
            throw new IllegalArgumentException("null certificate manager");
        }

        if (endorsementBytes == null) {
            throw new IllegalArgumentException("null endorsement credential bytes");
        }

        if (endorsementBytes.length <= 1) {
            throw new IllegalArgumentException(
                    String.format("%d-length byte array given for endorsement credential",
                            endorsementBytes.length)
            );
        }

        log.info("Parsing Endorsement Credential of length {}", endorsementBytes.length);

        EndorsementCredential endorsementCredential;
        try {
            endorsementCredential = EndorsementCredential
                    .parseWithPossibleHeader(endorsementBytes);
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
     * @param certificateRepository the certificate manager used for storage
     * @param platformBytes         the raw PC bytes used for parsing
     * @param deviceName            the host name of the associated machine
     * @return the parsed, valid PC, or null if the provided bytes are not a valid EK.
     */
    public static PlatformCredential storePlatformCredential(
            final CertificateRepository certificateRepository,
            final byte[] platformBytes, final String deviceName) {

        if (certificateRepository == null) {
            log.error("The provided certificate repository is null.");
            throw new IllegalArgumentException("null certificate manager");
        }

        if (platformBytes == null) {
            log.error("The provided platform credential byte array is null.");
            throw new IllegalArgumentException("null platform credential bytes");
        }

        if (platformBytes.length == 0) {
            log.error("The provided platform credential byte array is null.");
            throw new IllegalArgumentException(
                    "zero-length byte array given for platform credential"
            );
        }

        log.info("Parsing Platform Credential of length {}", platformBytes.length);

        try {
            PlatformCredential platformCredential =
                    PlatformCredential.parseWithPossibleHeader(platformBytes);

            if (platformCredential == null) {
                log.error("The platform credential that was parsed with the provided"
                        + "byte array was null");
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

        log.error("Due to an exception being thrown while "
                + " attempting to store platform certificate(s) "
                + "this method will return a null platform certificate.");
        return null;
    }

    /**
     * Helper method to parse an Endorsement Credential from a Protobuf generated
     * IdentityClaim. Will also check if the Endorsement Credential was already uploaded.
     * Persists the Endorsement Credential if it does not already exist.
     *
     * @param identityClaim         a Protobuf generated Identity Claim object
     * @param ekPub                 the endorsement public key from the Identity Claim object
     * @param certificateRepository db connector from certificates
     * @return the Endorsement Credential, if one exists, null otherwise
     */
    public static EndorsementCredential parseEcFromIdentityClaim(
            final ProvisionerTpm2.IdentityClaim identityClaim,
            final PublicKey ekPub, final CertificateRepository certificateRepository) {
        EndorsementCredential endorsementCredential = null;

        if (identityClaim.hasEndorsementCredential()) {
            endorsementCredential = CredentialManagementHelper.storeEndorsementCredential(
                    certificateRepository,
                    identityClaim.getEndorsementCredential().toByteArray(),
                    identityClaim.getDv().getNw().getHostname());
        } else if (ekPub != null) {
            log.warn("Endorsement Cred was not in the identity claim from the client."
                    + " Checking for uploads.");
            endorsementCredential = getEndorsementCredential(ekPub, certificateRepository);
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
     * @param certificateRepository db connector from certificates
     * @return the List of Platform Credentials, if they exist, an empty set otherwise
     */
    public static List<PlatformCredential> parsePcsFromIdentityClaim(
            final ProvisionerTpm2.IdentityClaim identityClaim,
            final EndorsementCredential endorsementCredential,
            final CertificateRepository certificateRepository) {
        List<PlatformCredential> platformCredentials = new LinkedList<>();

        if (identityClaim.getPlatformCredentialCount() > 0) {

            List<ByteString> platformCredentialList = identityClaim.getPlatformCredentialList();

            for (ByteString platformCredential : platformCredentialList) {
                if (!platformCredential.isEmpty()) {
                    PlatformCredential storedPlatformCredential =
                            CredentialManagementHelper.storePlatformCredential(
                                    certificateRepository, platformCredential.toByteArray(),
                                    identityClaim.getDv().getNw().getHostname());

                    if (storedPlatformCredential != null) {
                        platformCredentials.add(storedPlatformCredential);
                    }
                }
            }
        } else if (endorsementCredential != null) {
            // if none in the identity claim, look for uploaded platform credentials
            log.warn("PC was not in the identity claim from the client. Checking for uploads.");
            platformCredentials.addAll(getPlatformCredentials(certificateRepository, endorsementCredential));
        } else {
            log.warn("No platform credential received in identity claim.");
        }

        return platformCredentials;
    }

    /**
     * Gets the Endorsement Credential from the DB given the EK public key.
     *
     * @param ekPublicKey           the EK public key
     * @param certificateRepository db store manager for certificates
     * @return the Endorsement credential, if found, otherwise null
     */
    private static EndorsementCredential getEndorsementCredential(
            final PublicKey ekPublicKey,
            final CertificateRepository certificateRepository) {
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
     * @param certificateRepository certificateRepository
     * @param ec                    endorsement credential
     * @return list of platform credentials
     */
    private static List<PlatformCredential> getPlatformCredentials(final CertificateRepository certificateRepository,
                                                                   final EndorsementCredential ec) {
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
}
