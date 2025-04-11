package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
public class ReferenceManifestService {

    private final EntityManager entityManager;

    
    public ReferenceManifestService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Page<ReferenceManifest> findRIMBySearchableColumns(final List<String> searchableColumns,
                                                              final String searchText,
                                                              final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceManifest> query = criteriaBuilder.createQuery(ReferenceManifest.class);
        Root<ReferenceManifest> rimRoot = query.from(ReferenceManifest.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchText)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {

                // there is a possibility that one of the column names
                // that matches one of the class fields is nested (e.g. device.name) ,
                // and we will need to do further work to extract the
                // field name
                // todo
                if (columnName.contains(".")) {

                }

                Predicate predicate =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(rimRoot.get(columnName)),
                                "%" + searchText.toLowerCase() + "%");
                predicates.add(predicate);
            }
        }

        query.where(criteriaBuilder.or(predicates.toArray(new Predicate[0])));

        // Apply pagination
        TypedQuery<ReferenceManifest> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<ReferenceManifest> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }
}
