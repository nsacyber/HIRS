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
 *
 */
@Log4j2
@Service
public class ReferenceDigestValueService {
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final EntityManager entityManager;

    /**
     * @param referenceManifestRepository    reference manifest repository
     * @param referenceDigestValueRepository reference digest value repository
     */
    @Autowired
    public ReferenceDigestValueService(ReferenceManifestRepository referenceManifestRepository,
                                       ReferenceDigestValueRepository referenceDigestValueRepository,
                                       EntityManager entityManager) {
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.entityManager = entityManager;
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * reference digest values whose field values matches the provided search term.
     *
     * @param searchableColumns list of the searchable column name
     * @param searchTerm        text that was input in the search textbox
     * @param pageable          pageable
     * @return page full of reference digest values
     */
    public Page<ReferenceDigestValue> findReferenceDigestValuesBySearchableColumns(
            final Set<String> searchableColumns,
            final String searchTerm,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceDigestValue> query =
                criteriaBuilder.createQuery(ReferenceDigestValue.class);
        Root<ReferenceDigestValue> referenceDigestValueRoot =
                query.from(ReferenceDigestValue.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchTerm)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {
                // Get the attribute type from entity root
                Path<?> fieldPath = referenceDigestValueRoot.get(columnName);

                // if the field is a string type
                if (String.class.equals(fieldPath.getJavaType())) {
                    Predicate predicate =
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(referenceDigestValueRoot.get(columnName)),
                                    "%" + searchTerm.toLowerCase() + "%");
                    predicates.add(predicate);
                } else if (Integer.class.equals(fieldPath.getJavaType())) {
                    // For Integer fields, use EQUAL if the search term is numeric
                    try {
                        Integer searchInteger = Integer.valueOf(searchTerm); // Will throw if not a number
                        Predicate predicate = criteriaBuilder.equal(fieldPath, searchInteger);
                        predicates.add(predicate);
                    } catch (NumberFormatException e) {
                        // If the searchTerm is not a valid number, skip this field
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
     * Retrieves a page full of reference digest values using the provided pageable value.
     *
     * @param pageable pageable
     * @return page full of reference digest values
     */
    public Page<ReferenceDigestValue> findAllReferenceDigestValues(Pageable pageable) {
        return this.referenceDigestValueRepository.findAll(pageable);
    }

    /**
     * @param referenceDigestValue reference digest value
     */
    public void saveReferenceDigestValue(ReferenceDigestValue referenceDigestValue) {
        this.referenceDigestValueRepository.save(referenceDigestValue);
    }

    /**
     * Retrieves the total number of records in the platform credential repository.
     *
     * @return total number of records in the platform credential repository.
     */
    public long findReferenceDigestValueRepositoryCount() {
        return this.referenceDigestValueRepository.count();
    }

    /**
     * @param uuid
     * @return
     */
    public boolean doesRIMExist(UUID uuid) {
        return this.referenceManifestRepository.existsById(uuid);
    }

    /**
     * @param uuid
     * @return
     */
    public ReferenceManifest findRIMById(UUID uuid) {
        return this.referenceManifestRepository.getReferenceById(uuid);
    }
}
