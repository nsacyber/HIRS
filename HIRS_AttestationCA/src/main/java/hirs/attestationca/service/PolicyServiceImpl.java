package hirs.attestationca.service;

import hirs.FilteredRecordsList;
import hirs.appraiser.Appraiser;
import hirs.attestationca.repository.PolicyRepository;
import hirs.data.persist.ArchivableEntity;
import hirs.data.persist.policy.Policy;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.PolicyMapper;
import hirs.persist.service.DefaultService;
import hirs.persist.service.PolicyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A <code>PolicyServiceImpl</code> manages <code>Policy</code>s. A
 * <code>PolicyServiceImpl</code> is used to store and manage policies. It has
 * support for the basic create, read, update, and delete methods.
 */
@Service
public class PolicyServiceImpl extends DbServiceImpl<Policy>
        implements DefaultService<Policy>, PolicyService {

    private static final Logger LOGGER = LogManager.getLogger(PolicyServiceImpl.class);
    @Autowired
    private PolicyRepository policyRepository;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Default Constructor.
     * @param entityManager entity manager for jpa hibernate events
     */
    public PolicyServiceImpl(final EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
    }

    @Override
    public List<Policy> getList() {
        LOGGER.debug("Getting all policies...");

        return getRetryTemplate().execute(new RetryCallback<List<Policy>, DBManagerException>() {
            @Override
            public List<Policy> doWithRetry(final RetryContext context)
                    throws DBManagerException {
                policyRepository.findAll();
                return null;
            }
        });
    }

    @Override
    public void updateElements(final List<Policy> policies) {
        LOGGER.debug("Updating {} certificates...", policies.size());

        policies.stream().forEach((policy) -> {
            if (policy != null) {
                this.updatePolicy(policy, policy.getId());
            }
        });
        policyRepository.flush();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deleteObjectById(final UUID uuid) {
        LOGGER.debug("Deleting policy by id: {}", uuid);

        getRetryTemplate().execute(new RetryCallback<Void, DBManagerException>() {
            @Override
            public Void doWithRetry(final RetryContext context)
                    throws DBManagerException {
                policyRepository.deleteById(uuid);
                policyRepository.flush();
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Policy savePolicy(final Policy policy) {
        LOGGER.debug("Saving policy: {}", policy);

        return getRetryTemplate().execute(new RetryCallback<Policy, DBManagerException>() {
            @Override
            public Policy doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return policyRepository.save(policy);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Policy updatePolicy(final Policy policy, final UUID uuid) {
        LOGGER.debug("Updating policy: {}", policy);
        Policy dbPolicy;

        if (uuid == null) {
            LOGGER.debug("Policy not found: {}", policy);
            dbPolicy = policy;
        } else {
            // will not return null, throws and exception
            dbPolicy = (Policy) policyRepository.getReferenceById(uuid);

            // run through things that aren't equal and update
        }

        return savePolicy(dbPolicy);
    }

    @Override
    public final Policy getDefaultPolicy(final Appraiser appraiser) {
        if (appraiser == null) {
            LOGGER.error("cannot get default policy for null appraiser");
            return null;
        }

        Policy ret = null;
        Transaction tx = null;
        Session session =  getEm().unwrap(org.hibernate.Session.class);
        try {
            tx = session.beginTransaction();
            LOGGER.debug("retrieving policy mapper from db where appraiser = {}",
                    appraiser);
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<PolicyMapper> criteriaQuery = criteriaBuilder
                    .createQuery(PolicyMapper.class);
            Root<PolicyMapper> root = criteriaQuery.from(PolicyMapper.class);
            Predicate recordPredicate = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("appraiser"), appraiser));
            criteriaQuery.select(root).where(recordPredicate);
            Query<PolicyMapper> query = session.createQuery(criteriaQuery);
            List<PolicyMapper> results = query.getResultList();
            PolicyMapper mapper = null;
            if (results != null && !results.isEmpty()) {
                mapper = results.get(0);
            }

            if (mapper == null) {
                LOGGER.debug("no policy mapper found for appraiser {}",
                        appraiser);
            } else {
                ret = mapper.getPolicy();
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "unable to get default policy";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
        return ret;
    }

    @Override
    public void setPolicy(final Appraiser appraiser, final Policy policy) {

    }

    @Override
    public void setDefaultPolicy(final Appraiser appraiser, final Policy policy) {

    }

    @Override
    public FilteredRecordsList getOrderedList(
            final Class<Policy> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns)
            throws DBManagerException {
        return null;
    }

    @Override
    public FilteredRecordsList<Policy> getOrderedList(
            final Class<Policy> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns,
            final CriteriaModifier criteriaModifier)
            throws DBManagerException {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean archive(final UUID uuid) {
        LOGGER.debug("archiving object: {}", uuid);
        if (uuid == null) {
            LOGGER.debug("null name argument");
            return false;
        }
        Policy target = (Policy)
                this.policyRepository.getReferenceById(uuid);
        if (target == null) {
            return false;
        }
        if (!(target instanceof ArchivableEntity)) {
            throw new DBManagerException("unable to archive non-archivable object");
        }

        ((ArchivableEntity) target).archive();
        this.updatePolicy(target, uuid);
        return true;
    }
}
