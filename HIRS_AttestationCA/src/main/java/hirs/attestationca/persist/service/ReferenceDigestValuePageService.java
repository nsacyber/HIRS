package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A service layer class responsible for encapsulating all business logic related to the RIM Database Page.
 */
@Log4j2
@Service
public class ReferenceDigestValuePageService {
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final EntityManager entityManager;

    /**
     * Constructor for the Reference Digest Value Page Service.
     *
     * @param referenceManifestRepository    reference manifest repository
     * @param referenceDigestValueRepository reference digest value repository
     * @param entityManager                  entity manager
     */
    @Autowired
    public ReferenceDigestValuePageService(final ReferenceManifestRepository referenceManifestRepository,
                                           final ReferenceDigestValueRepository referenceDigestValueRepository,
                                           final EntityManager entityManager) {
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.entityManager = entityManager;
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * reference digest values whose field values matches the provided search term.
     *
     * @param searchableColumnNames list of the searchable column names
     * @param globalSearchTerm      text that was input in the global search textbox
     * @param pageable              pageable
     * @return page full of reference digest values
     */
    public Page<ReferenceDigestValue> findReferenceDigestValuesByGlobalSearchTerm(
            final Set<String> searchableColumnNames,
            final String globalSearchTerm,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceDigestValue> query =
                criteriaBuilder.createQuery(ReferenceDigestValue.class);
        Root<ReferenceDigestValue> referenceDigestValueRoot =
                query.from(ReferenceDigestValue.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(globalSearchTerm)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumnNames) {
                // Get the attribute type from entity root
                Path<?> fieldPath = referenceDigestValueRoot.get(columnName);

                // if the field is a string type
                if (String.class.equals(fieldPath.getJavaType())) {
                    Predicate predicate =
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(referenceDigestValueRoot.get(columnName)),
                                    "%" + globalSearchTerm.toLowerCase() + "%");
                    predicates.add(predicate);
                } else if (Integer.class.equals(fieldPath.getJavaType())) {
                    // For Integer fields, use EQUAL if the search term is numeric
                    try {
                        Integer searchInteger =
                                Integer.valueOf(globalSearchTerm); // Will throw if not a number
                        Predicate predicate = criteriaBuilder.equal(fieldPath, searchInteger);
                        predicates.add(predicate);
                    } catch (NumberFormatException e) {
                        // If the globalSearchTerm is not a valid number, skip this field
                    }
                }
            }
        }

        query.where(criteriaBuilder.or(predicates.toArray(new Predicate[0])));

        // Apply pagination
        TypedQuery<ReferenceDigestValue> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<ReferenceDigestValue> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * @param columnsWithSearchCriteria
     * @param pageable
     * @return
     */
    public Page<ReferenceDigestValue> findReferenceDigestValuesByColumnSpecificSearchTerm(
            final Set<DataTablesColumnSearchCriteria> columnsWithSearchCriteria,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceDigestValue> query =
                criteriaBuilder.createQuery(ReferenceDigestValue.class);
        Root<ReferenceDigestValue> referenceDigestValueRoot =
                query.from(ReferenceDigestValue.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically loop through columns ... todo... for each searchable column
        for (DataTablesColumnSearchCriteria dataTablesColumnSearchCriteria : columnsWithSearchCriteria) {

            final String columnName = dataTablesColumnSearchCriteria.getColumnName();
            final String columnSearchTerm = dataTablesColumnSearchCriteria.getColumnSearchTerm();
            final String columnSearchType = dataTablesColumnSearchCriteria.getColumnSearchType();

            // Get the attribute type from entity root
            Path<?> fieldPath = referenceDigestValueRoot.get(columnName);

            // if the field is a string type
            if (String.class.equals(fieldPath.getJavaType())) {
                Predicate predicate =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(referenceDigestValueRoot.get(columnName)),
                                "%" + columnSearchTerm.toLowerCase() + "%");
                predicates.add(predicate);
            } else if (Integer.class.equals(fieldPath.getJavaType())) {
                // For Integer fields, use EQUAL if the search term is numeric
                try {
                    Integer searchInteger =
                            Integer.valueOf(columnSearchTerm); // Will throw if not a number
                    Predicate predicate = criteriaBuilder.equal(fieldPath, searchInteger);
                    predicates.add(predicate);
                } catch (NumberFormatException e) {
                    // If the columnSearchTerm is not a valid number, skip this field
                }
            }
        }

        // Apply pagination
        TypedQuery<ReferenceDigestValue> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<ReferenceDigestValue> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * Retrieves a page full of reference digest values using the provided pageable value.
     *
     * @param pageable pageable
     * @return page full of reference digest values
     */
    public Page<ReferenceDigestValue> findAllReferenceDigestValues(final Pageable pageable) {
        return this.referenceDigestValueRepository.findAll(pageable);
    }

    /**
     * Saves the provided reference digest value in the reference digest value repository.
     *
     * @param referenceDigestValue reference digest value
     */
    public void saveReferenceDigestValue(final ReferenceDigestValue referenceDigestValue) {
        this.referenceDigestValueRepository.save(referenceDigestValue);
    }

    /**
     * Retrieves the total number of records in the reference digest value repository.
     *
     * @return total number of records in the reference digest value repository.
     */
    public long findReferenceDigestValueRepositoryCount() {
        return this.referenceDigestValueRepository.count();
    }

    /**
     * Determines if the RIM, using the provided id, exists in the repository.
     *
     * @param uuid uuid representation of the reference manifest id
     * @return true if the provided RIM exists in the database,
     * otherwise it returns false if it doesn't exist
     */
    public boolean doesRIMExist(final UUID uuid) {
        return this.referenceManifestRepository.existsById(uuid);
    }

    /**
     * Retrieves the Reference Manifest in the repository using the provided id.
     *
     * @param uuid uuid representation of the RIM
     * @return the found Reference Manifest
     */
    public ReferenceManifest findRIMById(final UUID uuid) {
        return this.referenceManifestRepository.getReferenceById(uuid);
    }
}
