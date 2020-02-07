package hirs.persist;

import hirs.data.persist.ReferenceManifest;
import org.hibernate.SessionFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is used to persist and retrieve {@link ReferenceManifest}s into
 * and from the database.
 */
public class DBReferenceManifestManager extends DBManager<ReferenceManifest>
        implements ReferenceManifestManager {

    private static final Logger LOGGER = LogManager.getLogger(DBReferenceManifestManager.class);

    /**
     * Default Constructor.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBReferenceManifestManager(final SessionFactory sessionFactory) {
        super(ReferenceManifest.class, sessionFactory);
    }

    /**
     * This method does not need to be used directly as it is used by
     * {@link ReferenceManifestSelector}'s get* methods.  Regardless, it may
     * be used to retrieve ReferenceManifest by other code in this
     * package, given a configured ReferenceManifestSelector.
     *
     * @param referenceManifestSelector a configured
     * {@link ReferenceManifestSelector} to use for querying
     * @return the resulting set of ReferenceManifest, possibly empty
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<ReferenceManifest> get(final ReferenceManifestSelector referenceManifestSelector) {
        LOGGER.info("Getting the full set of Reference Manifest files.");
        return new HashSet<>(
                (List<ReferenceManifest>) getWithCriteria(
                        referenceManifestSelector.getReferenceManifestClass(),
                        Collections.singleton(referenceManifestSelector.getCriterion())
                )
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
        return delete(referenceManifest);
    }
}
