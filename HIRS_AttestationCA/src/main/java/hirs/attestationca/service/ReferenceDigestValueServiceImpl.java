package hirs.attestationca.service;

import hirs.FilteredRecordsList;
import hirs.attestationca.repository.ReferenceDigestValueRepository;
import hirs.data.persist.ArchivableEntity;
import hirs.data.persist.ReferenceDigestValue;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.service.DefaultService;
import hirs.persist.service.ReferenceDigestValueService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A <code>ReferenceDigestValueServiceImpl</code> manages <code>Digest Value Event</code>s. A
 * <code>ReferenceDigestValueServiceImpl</code> is used to store and manage digest events. It has
 * support for the basic create, read, update, and delete methods.
 */
@Service
public class ReferenceDigestValueServiceImpl extends DbServiceImpl<ReferenceDigestValue>
        implements DefaultService<ReferenceDigestValue>, ReferenceDigestValueService {

    private static final Logger LOGGER = LogManager
            .getLogger(ReferenceDigestValueServiceImpl.class);
    @Autowired
    private ReferenceDigestValueRepository referenceDigestValueRepository;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Default Constructor.
     * @param entityManager entity manager for jpa hibernate events
     */
    public ReferenceDigestValueServiceImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<ReferenceDigestValue> getList() {
        LOGGER.debug("Getting all reference digest value...");
        return this.referenceDigestValueRepository.findAll();
    }

    @Override
    public void updateElements(final List<ReferenceDigestValue> referenceDigestValues) {
        LOGGER.debug("Updating {} reference digest values...", referenceDigestValues.size());

        referenceDigestValues.stream().forEach((values) -> {
            if (values != null) {
                this.updateDigestValue(values, values.getId());
            }
        });
        referenceDigestValueRepository.flush();
    }

    @Override
    public void deleteObjectById(final UUID uuid) {
        LOGGER.debug("Deleting reference digest values by id: {}", uuid);

        getRetryTemplate().execute(new RetryCallback<Void, DBManagerException>() {
            @Override
            public Void doWithRetry(final RetryContext context)
                    throws DBManagerException {
                referenceDigestValueRepository.deleteById(uuid);
                referenceDigestValueRepository.flush();
                return null;
            }
        });
    }

    @Override
    public ReferenceDigestValue saveDigestValue(final ReferenceDigestValue digestValue) {
        LOGGER.debug("Saving reference digest value: {}", digestValue);

        return getRetryTemplate().execute(new RetryCallback<ReferenceDigestValue,
                DBManagerException>() {
            @Override
            public ReferenceDigestValue doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return referenceDigestValueRepository.save(digestValue);
            }
        });
    }

    @Override
    public ReferenceDigestValue updateDigestValue(
            final ReferenceDigestValue digestValue, final UUID uuid) {
        LOGGER.debug("Updating reference digest value: {}", digestValue);
        ReferenceDigestValue dbDigestValue;

        if (uuid == null) {
            LOGGER.debug("Reference Digest Value not found: {}", digestValue);
            dbDigestValue = digestValue;
        } else {
            // will not return null, throws and exception
            dbDigestValue = this.referenceDigestValueRepository.getReferenceById(uuid);
            // run through things that aren't equal and update
            if (!dbDigestValue.getDigestValue().equals(digestValue.getDigestValue())) {
                dbDigestValue.setDigestValue(digestValue.getDigestValue());
            }

            if (!dbDigestValue.getEventType().equals(digestValue.getEventType())) {
                dbDigestValue.setEventType(digestValue.getEventType());
            }
        }

        return saveDigestValue(dbDigestValue);
    }

    @Override
    public List<ReferenceDigestValue> getValuesByBaseRimId(final UUID uuid) {
        LOGGER.debug("Find reference digest values base on: {}", uuid);

        return getRetryTemplate().execute(new RetryCallback<List<ReferenceDigestValue>,
                DBManagerException>() {
            @Override
            public List<ReferenceDigestValue> doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return referenceDigestValueRepository.findValuesByBaseRimId(uuid);
            }
        });
    }

    @Override
    public List<ReferenceDigestValue> getValuesBySupportRimId(final UUID uuid) {
        LOGGER.debug("Find reference digest values base on: {}", uuid);

        return getRetryTemplate().execute(new RetryCallback<List<ReferenceDigestValue>,
                DBManagerException>() {
            @Override
            public List<ReferenceDigestValue> doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return referenceDigestValueRepository.findValuesBySupportRimId(uuid);
            }
        });
    }

    @Override
    public FilteredRecordsList getOrderedList(
            final Class<ReferenceDigestValue> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns)
            throws DBManagerException {
        LOGGER.debug("Getting ordered object list");
        Class<ReferenceDigestValue> searchClass = clazz;
        if (clazz == null) {
            LOGGER.debug("clazz is null");
            searchClass = ReferenceDigestValue.class;
        }

        if (searchableColumns != null) {
            LOGGER.info(searchClass.getName() + " querying for "
                    + Arrays.toString(searchableColumns.entrySet().toArray())
                    + " with search strings \"" + search + "\"");
        }

        //Object that will store query values
        FilteredRecordsList<ReferenceDigestValue> rdvOrderedList = new FilteredRecordsList<>();

        // Search
//        Query keywordQuery = queryBuilder.

        // sort
        return rdvOrderedList;
    }

    @Override
    public FilteredRecordsList<ReferenceDigestValue> getOrderedList(
            final Class<ReferenceDigestValue> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns,
            final CriteriaModifier criteriaModifier)
            throws DBManagerException {
        return null;
    }

    @Override
    public boolean archive(UUID uuid) {
        LOGGER.debug("archiving object: {}", uuid);
        if (uuid == null) {
            LOGGER.debug("null name argument");
            return false;
        }
        ReferenceDigestValue target = (ReferenceDigestValue)
                this.referenceDigestValueRepository.getReferenceById(uuid);
        if (target == null) {
            return false;
        }
        if (!(target instanceof ArchivableEntity)) {
            throw new DBManagerException("unable to archive non-archivable object");
        }

        ((ArchivableEntity) target).archive();
        this.updateDigestValue(target, uuid);
        return true;
    }
}
