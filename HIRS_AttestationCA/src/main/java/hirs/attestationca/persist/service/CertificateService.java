package hirs.attestationca.persist.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CertificateService {

    private final EntityManager entityManager;

    @Autowired
    public CertificateService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * @param entityClass       generic entity class
     * @param searchableColumns list of the searchable column name
     * @param searchText        text that waas input in the search textbox
     * @param archiveFlag
     * @param pageable
     * @param <T>               generic entity class
     * @return
     */
    public <T> Page<T> findBySearchableColumnsAndArchiveFlag(Class<T> entityClass,
                                                             List<String> searchableColumns,
                                                             String searchText,
                                                             Boolean archiveFlag,
                                                             Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
        Root<T> certificate = query.from(entityClass);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchText)) {
            // Dynamically loop through columns and create LIKE conditions for each one
            for (String columnName : searchableColumns) {
                Predicate predicate =
                        criteriaBuilder.like(criteriaBuilder.lower(certificate.get(columnName)),
                                "%" + searchText.toLowerCase() + "%");
                predicates.add(predicate);
            }
        }

        Predicate likeConditions = criteriaBuilder.or(predicates.toArray(new Predicate[0]));

        // Add archiveFlag condition if specified
        query.where(criteriaBuilder.and(likeConditions,
                criteriaBuilder.equal(certificate.get("archiveFlag"), archiveFlag)));

        // Apply pagination
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<T> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }
}
