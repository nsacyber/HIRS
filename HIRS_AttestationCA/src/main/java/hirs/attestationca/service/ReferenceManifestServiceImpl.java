package hirs.attestationca.service;

import hirs.FilteredRecordsList;
import hirs.attestationca.repository.ReferenceManifestRepository;
import hirs.data.persist.ArchivableEntity;
import hirs.data.persist.ReferenceManifest;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.ReferenceManifestSelector;
import hirs.persist.service.ReferenceManifestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A <code>ReferenceManifestServiceImpl</code> manages <code>ReferenceManifestService</code>s. A
 * <code>ReferenceManifestServiceImpl</code> is used to store and manage reference manifest. It has
 * support for the basic create, read, update, and delete methods.
 */
@Service
public class ReferenceManifestServiceImpl extends DbServiceImpl<ReferenceManifest>
        implements ReferenceManifestService {

    private static final Logger LOGGER = LogManager.getLogger(ReferenceManifestServiceImpl.class);
    @Autowired
    private ReferenceManifestRepository referenceManifestRepository;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Default constructor.
     * @param em entity manager for jpa hibernate events
     */
    public ReferenceManifestServiceImpl(final EntityManager em) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public ReferenceManifest saveRIM(final ReferenceManifest rim) {
        LOGGER.debug("Saving reference manifest: {}", rim);

        return getRetryTemplate().execute(new RetryCallback<ReferenceManifest,
                DBManagerException>() {
            @Override
            public ReferenceManifest doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return referenceManifestRepository.saveRIM(rim);
            }
        });
    }

    @Override
    public ReferenceManifest updateReferenceManifest(final ReferenceManifest rim) {
        return updateReferenceManifest(rim, rim.getId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ReferenceManifest updateReferenceManifest(final ReferenceManifest rim,
                                                     final UUID uuid) {
        LOGGER.debug("Updating reference manifest: {}", rim);
        ReferenceManifest dbRim;

        if (uuid == null) {
            LOGGER.debug("Reference Manifest not found: {}", rim);
            dbRim = rim;
        } else {
            // will not return null, throws and exception
            dbRim = (ReferenceManifest) this.referenceManifestRepository
                    .getReferenceById(uuid);

            // run through things that aren't equal and update

        }

        return saveRIM(dbRim);
    }

    @Override
    public void deleteRIM(final ReferenceManifest rim) {
        deleteObjectById(rim.getId());
    }

    @Override
    public <T extends ReferenceManifest> Set<T> getReferenceManifest(
            final ReferenceManifestSelector referenceManifestSelector) {
        return new HashSet<>(0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ReferenceManifest> getList() {
        LOGGER.debug("Getting all reference manifest...");

        return getRetryTemplate().execute(new RetryCallback<List<ReferenceManifest>,
                DBManagerException>() {
            @Override
            public List<ReferenceManifest> doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return referenceManifestRepository.findAll();
            }
        });
    }

    @Override
    public void updateElements(final List<ReferenceManifest> referenceManifests) {
        LOGGER.debug("Updating {} reference manifests...", referenceManifests.size());

        referenceManifests.stream().forEach((rim) -> {
            if (rim != null) {
                this.updateReferenceManifest(rim, rim.getId());
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deleteObjectById(final UUID uuid) {
        LOGGER.debug("Deleting reference manifest by id: {}", uuid);

        getRetryTemplate().execute(new RetryCallback<Void, DBManagerException>() {
            @Override
            public Void doWithRetry(final RetryContext context)
                    throws DBManagerException {
                referenceManifestRepository.deleteById(uuid);
                referenceManifestRepository.flush();
                return null;
            }
        });
    }

    @Override
    public FilteredRecordsList getOrderedList(
            final Class<ReferenceManifest> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns)
            throws DBManagerException {
        return null;
    }

    @Override
    public FilteredRecordsList<ReferenceManifest> getOrderedList(
            final Class<ReferenceManifest> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns,
            final CriteriaModifier criteriaModifier)
            throws DBManagerException {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean archive(final UUID uuid) throws DBManagerException {
        LOGGER.debug("archiving object: {}", uuid);
        if (uuid == null) {
            LOGGER.debug("null name argument");
            return false;
        }
        ReferenceManifest target = (ReferenceManifest)
                    this.referenceManifestRepository.getReferenceById(uuid);
        if (target == null) {
            return false;
        }
        if (!(target instanceof ArchivableEntity)) {
            throw new DBManagerException("unable to archive non-archivable object");
        }

        ((ArchivableEntity) target).archive();
        this.updateReferenceManifest(target, uuid);
        return true;
    }
}
