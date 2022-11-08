package hirs.attestationca.servicemanager;

import hirs.data.persist.ReferenceManifest;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.ReferenceManifestSelector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to persist and retrieve {@link ReferenceManifest}s into
 * and from the database.
 */
@Service
public class DBReferenceManifestManager extends DBManager<ReferenceManifest>
        implements ReferenceManifestManager {

    private static final Logger LOGGER = LogManager.getLogger(DBReferenceManifestManager.class);

    /**
     * Default Constructor.
     *
     * @param em entity manager used to access database connections
     */
    public DBReferenceManifestManager(final EntityManager em) {
        super(ReferenceManifest.class, em);
    }

    /**
     * This method does not need to be used directly as it is used by
     * {@link hirs.persist.ReferenceManifestSelector}'s get* methods. Regardless, it may be
     * used to retrieve ReferenceManifest by other code in this package, given a
     * configured ReferenceManifestSelector.
     *
     * @param referenceManifestSelector a configured
     * {@link hirs.persist.ReferenceManifestSelector} to use for querying
     * @return the resulting set of ReferenceManifest, possibly empty
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends  ReferenceManifest> Set<T> get(
            final ReferenceManifestSelector referenceManifestSelector) {
        LOGGER.info("Getting the full set of Reference Manifest files.");
//        CriteriaBuilder builder = this.getFactory().getCriteriaBuilder();
        return new HashSet<>(0
//                new HashSet<>((List<T>) getWithCriteria(
//                referenceManifestSelector.getReferenceManifestClass(),
//                referenceManifestSelector.getCriterion(builder))
        );
    }

    /**
     * Remove a ReferenceManifest from the database.
     *
     * @param referenceManifest the referenceManifest to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteReferenceManifest(final ReferenceManifest referenceManifest) {
        LOGGER.info(String.format("Deleting reference to %s", referenceManifest.getTagId()));
        return deleteById(referenceManifest.getId());
    }
}
