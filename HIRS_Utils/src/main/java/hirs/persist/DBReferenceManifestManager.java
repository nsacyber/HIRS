package hirs.persist;

import hirs.data.persist.ReferenceManifest;
import org.hibernate.SessionFactory;

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

}
