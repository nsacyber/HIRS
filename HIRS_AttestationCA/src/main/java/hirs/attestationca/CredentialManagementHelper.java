package hirs.attestationca;

import hirs.attestationca.entity.certificate.EndorsementCredential;
import hirs.attestationca.entity.certificate.PlatformCredential;
import hirs.persist.DBManagerException;
import hirs.attestationca.service.CertificateService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Utility class which includes credential management functions used by the ACA.
 */
public final class CredentialManagementHelper {
    private static final Logger LOG = LogManager.getLogger(CredentialManagementHelper.class);

    private CredentialManagementHelper() {

    }

    /**
     * Parses and stores the EK in the cert manager. If the cert is already present and archived,
     * it is unarchived.
     * @param certificateService the certificate service used for storage
     * @param endorsementBytes the raw EK bytes used for parsing
     * @return the parsed, valid EK
     * @throws IllegalArgumentException if the provided bytes are not a valid EK.
     */
    public static EndorsementCredential storeEndorsementCredential(
            final CertificateService certificateService,
            final byte[] endorsementBytes) throws IllegalArgumentException {

        if (certificateService == null) {
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

        LOG.info("Parsing Endorsement Credential of length " + endorsementBytes.length);

        EndorsementCredential endorsementCredential;
        try {
            endorsementCredential = EndorsementCredential
                    .parseWithPossibleHeader(endorsementBytes);
        } catch (IllegalArgumentException iae) {
            LOG.error(iae.getMessage());
            throw iae;
        }
        int certificateHash = endorsementCredential.getCertificateHash();
        EndorsementCredential existingCredential =
                EndorsementCredential.select(certificateService).includeArchived()
                        .byHashCode(certificateHash).getCertificate();
        if (existingCredential == null) {
            LOG.info("No Endorsement Credential found with hash: " + certificateHash);
            return (EndorsementCredential) certificateService
                    .saveCertificate(endorsementCredential);
        } else if (existingCredential.isArchived()) {
            // if the EK is stored in the DB and it's archived, unarchive.
            LOG.info("Unarchiving credential");
            existingCredential.restore();
            existingCredential.resetCreateTime();
            certificateService.updateCertificate(existingCredential,
                    existingCredential.getId());
        }
        return existingCredential;
    }

    /**
     * Parses and stores the PC in the cert manager. If the cert is already present and archived,
     * it is unarchived.
     * @param certificateService the certificate service used for storage
     * @param platformBytes the raw PC bytes used for parsing
     * @return the parsed, valid PC, or null if the provided bytes are not a valid EK.
     */
    public static PlatformCredential storePlatformCredential(
            final CertificateService certificateService,
            final byte[] platformBytes) {

        if (certificateService == null) {
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

        LOG.info("Parsing Platform Credential of length " + platformBytes.length);
        try {
            PlatformCredential platformCredential =
                    PlatformCredential.parseWithPossibleHeader(platformBytes);
            if (platformCredential == null) {
                return null;
            }
            PlatformCredential existingCredential =
                    PlatformCredential.select(certificateService)
                            .includeArchived()
                            .byHashCode(platformCredential
                                    .getCertificateHash())
                            .getCertificate();
            if (existingCredential == null) {
                if (platformCredential.getPlatformSerial() != null) {
                    List<PlatformCredential> certificates = PlatformCredential
                            .select(certificateService)
                            .byBoardSerialNumber(platformCredential.getPlatformSerial())
                            .getCertificates().stream().collect(Collectors.toList());
                    if (!certificates.isEmpty()) {
                        // found associated certificates
                        for (PlatformCredential pc : certificates) {
                            if (pc.isBase() && platformCredential.isBase()) {
                                // found a base in the database associated with
                                // parsed certificate
                                LOG.error(String.format("Base certificate stored"
                                                + " in database with same platform"
                                                + "serial number. (%s)",
                                        platformCredential.getPlatformSerial()));
                                return null;
                            }
                        }
                    }
                }
                return (PlatformCredential) certificateService
                        .saveCertificate(platformCredential);
            } else if (existingCredential.isArchived()) {
                // if the PC is stored in the DB and it's archived, unarchive.
                LOG.info("Unarchiving credential");
                existingCredential.restore();
                certificateService.updateCertificate(existingCredential,
                        existingCredential.getId());
                return existingCredential;
            }

            return existingCredential;
        } catch (DBManagerException dbe) {
            LOG.error("Error retrieving or saving platform credential", dbe);
        } catch (Exception e) {
            LOG.error("Error parsing platform credential", e);
        }
        return null;
    }
}
