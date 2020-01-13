package hirs.attestationca;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.persist.CertificateManager;
import hirs.persist.DBManagerException;

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
     * @param certificateManager the certificate manager used for storage
     * @param endorsementBytes the raw EK bytes used for parsing
     * @return the parsed, valid EK
     * @throws IllegalArgumentException if the provided bytes are not a valid EK.
     */
    public static EndorsementCredential storeEndorsementCredential(
            final CertificateManager certificateManager,
            final byte[] endorsementBytes) throws IllegalArgumentException {

        if (certificateManager == null) {
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
                EndorsementCredential.select(certificateManager).includeArchived()
                        .byHashCode(certificateHash).getCertificate();
        if (existingCredential == null) {
            LOG.info("No Endorsement Credential found with hash: " + certificateHash);
            return (EndorsementCredential) certificateManager.save(endorsementCredential);
        } else if (existingCredential.isArchived()) {
            // if the EK is stored in the DB and it's archived, unarchive.
            LOG.info("Unarchiving credential");
            existingCredential.restore();
            existingCredential.resetCreateTime();
            certificateManager.update(existingCredential);
        }
        return existingCredential;
    }

    /**
     * Parses and stores the PC in the cert manager. If the cert is already present and archived,
     * it is unarchived.
     * @param certificateManager the certificate manager used for storage
     * @param platformBytes the raw PC bytes used for parsing
     * @return the parsed, valid PC, or null if the provided bytes are not a valid EK.
     */
    public static PlatformCredential storePlatformCredential(
            final CertificateManager certificateManager,
            final byte[] platformBytes) {

        if (certificateManager == null) {
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
                    PlatformCredential.select(certificateManager)
                            .includeArchived()
                            .byHashCode(platformCredential
                                    .getCertificateHash())
                            .getCertificate();
            if (existingCredential == null) {
                if (platformCredential.getPlatformSerial() != null) {
                    List<PlatformCredential> certificates = PlatformCredential
                            .select(certificateManager)
                            .byBoardSerialNumber(platformCredential.getPlatformSerial())
                            .getCertificates().stream().collect(Collectors.toList());
                    if (!certificates.isEmpty()) {
                        // found associated certificates
                        for (PlatformCredential pc : certificates) {
                            if (pc.isBase()) {
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
                return (PlatformCredential) certificateManager.save(platformCredential);
            } else if (existingCredential.isArchived()) {
                // if the PC is stored in the DB and it's archived, unarchive.
                LOG.info("Unarchiving credential");
                existingCredential.restore();
                certificateManager.update(existingCredential);
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
