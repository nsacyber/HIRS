package hirs.attestationca.service;

import hirs.FilteredRecordsList;
import hirs.attestationca.repository.ReferenceDigestValueRepository;
import hirs.data.persist.ReferenceDigestValue;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.OrderedQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        implements DefaultService<ReferenceDigestValue>,
        ReferenceDigestValueService, OrderedQuery<ReferenceDigestValue> {

    private static final Logger LOGGER = LogManager
            .getLogger(ReferenceDigestValueServiceImpl.class);
    @Autowired
    private ReferenceDigestValueRepository referenceDigestValueRepository;

    /**
     * Default Constructor.
     */
    public ReferenceDigestValueServiceImpl() {
        super();
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
    }

    @Override
    public void deleteObjectById(final UUID uuid) {
        LOGGER.debug("Deleting reference digest values by id: {}", uuid);
        this.referenceDigestValueRepository.deleteById(uuid);
    }

    @Override
    public ReferenceDigestValue saveDigestValue(final ReferenceDigestValue digestValue) {
        LOGGER.debug("Saving reference digest value: {}", digestValue);
        return this.referenceDigestValueRepository.save(digestValue);
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

        this.referenceDigestValueRepository.save(dbDigestValue);

        return dbDigestValue;
    }

    @Override
    public FilteredRecordsList getOrderedList(
            final Class<ReferenceDigestValue> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns)
            throws DBManagerException {
        return null;
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
}
