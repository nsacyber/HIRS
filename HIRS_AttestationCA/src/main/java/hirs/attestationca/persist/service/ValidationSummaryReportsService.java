package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
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

/**
 * Service layer class that handles the storage and retrieval of validation reports.
 */
@Service
@Log4j2
public class ValidationSummaryReportsService {

    private final EntityManager entityManager;

    @Autowired
    public ValidationSummaryReportsService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static Path<?> getNestedPath(Root<?> root, CriteriaBuilder cb, String... fieldNames) {
        Path<?> path = root;
        for (String fieldName : fieldNames) {
            path = path.get(fieldName);
        }
        return path;
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * validation summaries whose field values matches the provided search term.
     *
     * @param searchableColumns list of the searchable column name
     * @param searchText        text that was input in the search textbox
     * @param archiveFlag       archive flag
     * @param pageable          pageable
     * @return page full of the validation summaries.
     */
    public Page<SupplyChainValidationSummary> findValidationReportsBySearchableColumnsAndArchiveFlag(
            final List<String> searchableColumns,
            final String searchText,
            Boolean archiveFlag,
            Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SupplyChainValidationSummary> query =
                criteriaBuilder.createQuery(SupplyChainValidationSummary.class);
        Root<SupplyChainValidationSummary> supplyChainValidationSummaryRoot =
                query.from(SupplyChainValidationSummary.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchText)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {

                // todo
                if (columnName.contains(".")) {

                }

                Predicate predicate =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(supplyChainValidationSummaryRoot.get(columnName)),
                                "%" + searchText.toLowerCase() + "%");
                predicates.add(predicate);
            }
        }

        Predicate likeConditions = criteriaBuilder.or(predicates.toArray(new Predicate[0]));

        // Add archiveFlag condition if specified
        query.where(criteriaBuilder.and(likeConditions,
                criteriaBuilder.equal(supplyChainValidationSummaryRoot.get("archiveFlag"), archiveFlag)));

        // Apply pagination
        TypedQuery<SupplyChainValidationSummary> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<SupplyChainValidationSummary> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }
}
