package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.service.util.DataTablesColumn;
import hirs.attestationca.persist.service.util.PredicateFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * @return A Page containing a list of reference digest values that match the global search term entered
     * in the global search textbox
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

        final Predicate combinedGlobalSearchPredicates =
                createPredicatesForGlobalSearch(searchableColumnNames, criteriaBuilder,
                        referenceDigestValueRoot,
                        globalSearchTerm);

        // Define the conditions (predicates) for the query's WHERE clause.
        query.where(criteriaBuilder.and(combinedGlobalSearchPredicates));

        // Apply sorting if present in the Pageable
        query.orderBy(getSortingOrders(criteriaBuilder, referenceDigestValueRoot, pageable.getSort()));

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
     * Takes the provided columns that come with a search criteria and attempts to find
     * reference digest values that match the column's specific search criteria's search value.
     *
     * @param columnsWithSearchCriteria columns that have a search criteria applied to them
     * @param pageable                  pageable
     * @return A Page containing a list of reference digest values that match the column specific search
     * criteria
     */
    public Page<ReferenceDigestValue> findReferenceDigestValuesByColumnSpecificSearchTerm(
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceDigestValue> query =
                criteriaBuilder.createQuery(ReferenceDigestValue.class);
        Root<ReferenceDigestValue> referenceDigestValueRoot =
                query.from(ReferenceDigestValue.class);

        final Predicate combinedColumnSearchPredicates =
                createPredicatesForColumnSpecificSearch(columnsWithSearchCriteria, criteriaBuilder,
                        referenceDigestValueRoot);

        // Define the conditions (predicates) for the query's WHERE clause.
        query.where(criteriaBuilder.and(combinedColumnSearchPredicates));

        // Apply sorting if present in the Pageable
        query.orderBy(getSortingOrders(criteriaBuilder, referenceDigestValueRoot, pageable.getSort()));

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
     * Finds reference digest values based on both global search and column-specific search criteria.
     * The method applies the provided global search term across all searchable columns
     * and also applies column-specific filters based on the individual column search criteria.
     * The results are returned with pagination support.
     * <p>
     * This method combines the logic of two search functionalities:
     * - Global search: Searches across all specified columns for a matching term.
     * - Column-specific search: Filters based on individual column search criteria, such as text
     * or date searches.
     * <p>
     *
     * @param searchableColumnNames     list of the searchable column names
     * @param globalSearchTerm          text that was input in the global search textbox
     * @param columnsWithSearchCriteria columns that have a search criteria applied to them
     * @param pageable                  pageable
     * @return A Page containing a list of reference digest values that match both the global search term and
     * the column-specific search criteria.
     */
    public Page<ReferenceDigestValue> findReferenceDigestValuesByGlobalAndColumnSpecificSearchTerm(
            final Set<String> searchableColumnNames,
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Pageable pageable) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceDigestValue> query =
                criteriaBuilder.createQuery(ReferenceDigestValue.class);
        Root<ReferenceDigestValue> referenceDigestValueRoot = query.from(ReferenceDigestValue.class);

        final Predicate globalSearchPartOfChainedPredicates =
                createPredicatesForGlobalSearch(searchableColumnNames, criteriaBuilder,
                        referenceDigestValueRoot,
                        globalSearchTerm);

        final Predicate columnSearchPartOfChainedPredicates =
                createPredicatesForColumnSpecificSearch(columnsWithSearchCriteria, criteriaBuilder,
                        referenceDigestValueRoot);

        // Define the conditions (predicates) for the query's WHERE clause.
        // Combine global and column-specific predicates using AND logic
        query.where(criteriaBuilder.and(globalSearchPartOfChainedPredicates,
                columnSearchPartOfChainedPredicates));

        // Apply sorting if present in the Pageable
        query.orderBy(getSortingOrders(criteriaBuilder, referenceDigestValueRoot, pageable.getSort()));

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

    /**
     * Helper method that generates a list of sorting orders based on the provided {@link Pageable} object.
     * This method checks if sorting is enabled in the {@link Pageable} and applies the necessary sorting
     * to the query using the CriteriaBuilder and Reference Digest Value Root.
     *
     * @param criteriaBuilder          the CriteriaBuilder used to create the sort expressions.
     * @param referenceDigestValueRoot the RDV Root to which the sorting should be applied.
     * @param pageableSort             the {@link Sort} object that contains the sort information.
     * @return a list of {@link Order} objects, which can be applied to a CriteriaQuery for sorting.
     */
    private List<Order> getSortingOrders(final CriteriaBuilder criteriaBuilder,
                                         final Root<ReferenceDigestValue> referenceDigestValueRoot,
                                         final Sort pageableSort) {
        List<Order> orders = new ArrayList<>();

        if (pageableSort.isSorted()) {
            pageableSort.forEach(order -> {
                Path<Object> path = referenceDigestValueRoot.get(order.getProperty());
                orders.add(order.isAscending() ? criteriaBuilder.asc(path) : criteriaBuilder.desc(path));
            });
        }
        return orders;
    }

    /**
     * Helper method that generates a combined predicate for global search across searchable columns.
     * For each column, if the field is of type `String`, a "contains" condition is created. If the field
     * is of type 'Integer', an "equals" condition is created.
     *
     * @param searchableColumnNames    the columns to be searched globally
     * @param criteriaBuilder          the criteria builder to construct the predicates
     * @param referenceDigestValueRoot the root entity representing the reference digest value
     * @param globalSearchTerm         the term to search for across columns
     * @return a combined `Predicate` representing the global search conditions
     */
    private Predicate createPredicatesForGlobalSearch(
            final Set<String> searchableColumnNames,
            final CriteriaBuilder criteriaBuilder,
            final Root<ReferenceDigestValue> referenceDigestValueRoot,
            final String globalSearchTerm) {
        List<Predicate> combinedGlobalSearchPredicates = new ArrayList<>();

        // Dynamically loop through columns and create LIKE conditions for each searchable column
        for (String columnName : searchableColumnNames) {
            if (String.class.equals(referenceDigestValueRoot.get(columnName).getJavaType())) {
                Path<String> stringFieldPath = referenceDigestValueRoot.get(columnName);

                Predicate predicate = PredicateFactory.createPredicateForStringFields(criteriaBuilder,
                        stringFieldPath, globalSearchTerm,
                        PredicateFactory.STRING_FIELD_GLOBAL_SEARCH_LOGIC);
                combinedGlobalSearchPredicates.add(predicate);
            } else if (Integer.class.equals(referenceDigestValueRoot.get(columnName).getJavaType())) {
                try {
                    Path<Integer> integerFieldPath = referenceDigestValueRoot.get(columnName);

                    Integer searchInteger = Integer.valueOf(globalSearchTerm); // Will throw if not a number

                    // For Integer fields, use EQUAL if the search term is numeric
                    Predicate predicate = PredicateFactory.createPredicateForIntegerFields(criteriaBuilder,
                            integerFieldPath, searchInteger,
                            PredicateFactory.INTEGER_FIELD_GLOBAL_SEARCH_LOGIC);

                    combinedGlobalSearchPredicates.add(predicate);
                } catch (NumberFormatException e) {
                    // If the globalSearchTerm is not a valid number, skip this field
                }
            }
        }

        return criteriaBuilder.or(combinedGlobalSearchPredicates.toArray(new Predicate[0]));
    }


    /**
     * Helper method that generates a combined predicate for column-specific search criteria.
     * It constructs conditions based on the field type (e.g., `String` or `Integer`)
     * and the provided search term and logic for each column.
     *
     * @param columnsWithSearchCriteria the columns and their associated search criteria
     * @param criteriaBuilder           the criteria builder to construct the predicates
     * @param referenceDigestValueRoot  the root entity representing the reference digest value
     * @return a combined `Predicate` representing the column-specific search conditions
     */
    private Predicate createPredicatesForColumnSpecificSearch(
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final CriteriaBuilder criteriaBuilder,
            final Root<ReferenceDigestValue> referenceDigestValueRoot) {
        List<Predicate> combinedColumnSearchPredicates = new ArrayList<>();

        // loop through all the datatable columns that have an applied search criteria
        for (DataTablesColumn columnWithSearchCriteria : columnsWithSearchCriteria) {
            final String columnName = columnWithSearchCriteria.getColumnName();
            final String columnSearchTerm = columnWithSearchCriteria.getColumnSearchTerm();
            final String columnSearchLogic = columnWithSearchCriteria.getColumnSearchLogic();

            if (String.class.equals(referenceDigestValueRoot.get(columnName).getJavaType())) {
                Path<String> stringFieldPath = referenceDigestValueRoot.get(columnName);

                Predicate predicate =
                        PredicateFactory.createPredicateForStringFields(criteriaBuilder, stringFieldPath,
                                columnSearchTerm,
                                columnSearchLogic);
                combinedColumnSearchPredicates.add(predicate);
            } else if (Integer.class.equals(referenceDigestValueRoot.get(columnName).getJavaType())) {
                try {
                    Integer searchInteger = Integer.parseInt(columnSearchTerm); // Will throw if not a number

                    Path<Integer> integerFieldPath = referenceDigestValueRoot.get(columnName);

                    Predicate predicate = PredicateFactory.createPredicateForIntegerFields(criteriaBuilder,
                            integerFieldPath, searchInteger, columnSearchLogic);
                    combinedColumnSearchPredicates.add(predicate);
                } catch (NumberFormatException e) {
                    // If the columnSearchTerm is not a valid number, skip this field
                }
            }
        }

        return criteriaBuilder.and(combinedColumnSearchPredicates.toArray(new Predicate[0]));
    }
}
