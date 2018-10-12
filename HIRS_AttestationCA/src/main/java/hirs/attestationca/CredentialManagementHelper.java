package hirs.attestationca;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.persist.CertificateManager;
import hirs.persist.DBManagerException;


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

        if (null == certificateManager) {
            throw new IllegalArgumentException("null certificate manager");
        }

        if (null == endorsementBytes) {
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
        if (null == existingCredential) {
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

        if (null == certificateManager) {
            throw new IllegalArgumentException("null certificate manager");
        }

        if (null == platformBytes) {
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
            if (null == platformCredential) {
                return null;
            }
            PlatformCredential existingCredential =
                    PlatformCredential.select(certificateManager)
                .byHashCode(platformCredential.getCertificateHash()).getCertificate();
            if (null == existingCredential) {
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
