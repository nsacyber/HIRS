package hirs.persist;

import hirs.data.persist.ReferenceManifest;
import hirs.data.persist.certificate.Certificate;
import org.hibernate.SessionFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used to persist and retrieve {@link ReferenceManifest}s into
 * and from the database.
 */
public class DBReferenceManifestManager extends DBManager<ReferenceManifest>
        implements ReferenceManifestManager {

    /**
     * Default Constructor.
     * @param sessionFactory session factory used to access database connections
     */
    public DBReferenceManifestManager(final SessionFactory sessionFactory) {
        super(ReferenceManifest.class, sessionFactory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<ReferenceManifest> get(ReferenceManifestSelector referenceManifestSelector) {
        return new HashSet<>(
                (List<ReferenceManifest>) getWithCriteria(
                        referenceManifestSelector.getReferenceManifestClass(),
                        Collections.singleton(referenceManifestSelector.getCriterion())
                )
        );
    }

    /**
     * Remove a rim from the database.
     *
     * @param referenceManifest the referenceManifest to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteReferenceManifest(final ReferenceManifest referenceManifest) {
        return delete(referenceManifest);
    }
}
