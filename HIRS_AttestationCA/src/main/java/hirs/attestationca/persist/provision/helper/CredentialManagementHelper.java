package hirs.attestationca.persist.provision.helper;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;


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
     * @param certificateRepository the certificate manager used for storage
     * @param endorsementBytes the raw EK bytes used for parsing
     * @param deviceName the host name
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

        log.info("Parsing Endorsement Credential of length " + endorsementBytes.length);

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
            log.info("No Endorsement Credential found with hash: " + certificateHash);
            endorsementCredential.setDeviceName(deviceName);
            return (EndorsementCredential) certificateRepository.save(endorsementCredential);
        } else if (existingCredential.isArchived()) {
            // if the EK is stored in the DB and it's archived, unarchive.
            log.info("Unarchiving credential");
            existingCredential.restore();
            existingCredential.resetCreateTime();
            certificateRepository.save(existingCredential);
        }
        return existingCredential;
    }

    /**
     * Parses and stores the PC in the cert manager. If the cert is already present and archived,
     * it is unarchived.
     * @param certificateRepository the certificate manager used for storage
     * @param platformBytes the raw PC bytes used for parsing
     * @return the parsed, valid PC, or null if the provided bytes are not a valid EK.
     */
    public static PlatformCredential storePlatformCredential(
            final CertificateRepository certificateRepository,
            final byte[] platformBytes) {

        if (certificateRepository == null) {
            throw new IllegalArgumentException("null certificate manager");
        }

        if (platformBytes == null) {
            throw new IllegalArgumentException("null platform credential bytes");
        }

        if (platformBytes.length == 0) {
            throw new IllegalArgumentException(
                    "zero-length byte array given for platform credential"
            );
        }

        log.info("Parsing Platform Credential of length " + platformBytes.length);
        try {
            PlatformCredential platformCredential =
                    PlatformCredential.parseWithPossibleHeader(platformBytes);
            if (platformCredential == null) {
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
                                log.error(String.format("Base certificate stored"
                                                + " in database with same platform"
                                                + "serial number. (%s)",
                                        platformCredential.getPlatformSerial()));
                                return null;
                            }
                        }
                    }
                }
                return (PlatformCredential) certificateRepository.save(platformCredential);
            } else if (existingCredential.isArchived()) {
                // if the PC is stored in the DB and it's archived, unarchive.
                log.info("Unarchiving credential");
                existingCredential.restore();
                certificateRepository.save(existingCredential);
                return existingCredential;
            }

            return existingCredential;
        } catch (DBManagerException dbEx) {
            log.error("Error retrieving or saving platform credential", dbEx);
        } catch (Exception e) {
            log.error("Error parsing platform credential", e);
        }
        return null;
    }
}
