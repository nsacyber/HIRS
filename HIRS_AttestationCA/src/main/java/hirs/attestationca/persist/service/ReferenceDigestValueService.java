package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Service
@Log4j2
public class ReferenceDigestValueService {

    private final EntityManager entityManager;

    public ReferenceDigestValueService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * @param searchableColumns list of the searchable column name
     * @param searchText        text that was input in the search textbox
     * @param pageable          pageable
     * @return page full of reference digest values
     */
    public Page<ReferenceDigestValue> findReferenceDigestValuesBySearchableColumns(
            final List<String> searchableColumns,
            final String searchText,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceDigestValue> query =
                criteriaBuilder.createQuery(ReferenceDigestValue.class);
        Root<ReferenceDigestValue> referenceDigestValueRoot =
                query.from(ReferenceDigestValue.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchText)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {
                // Get the attribute type from entity root
                Path<?> fieldPath = referenceDigestValueRoot.get(columnName);

                //  if the field is a string type
                if (String.class.equals(fieldPath.getJavaType())) {
                    Predicate predicate =
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(referenceDigestValueRoot.get(columnName)),
                                    "%" + searchText.toLowerCase() + "%");
                    predicates.add(predicate);
                }
                // if the field is a non-string type
                else {
                    Expression<String> fieldAsString = criteriaBuilder
                            .literal(fieldPath).as(String.class);

                    Predicate predicate = criteriaBuilder.like(
                            criteriaBuilder.lower(fieldAsString),
                            "%" + searchText.toLowerCase() + "%"
                    );

                    predicates.add(predicate);
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
}
