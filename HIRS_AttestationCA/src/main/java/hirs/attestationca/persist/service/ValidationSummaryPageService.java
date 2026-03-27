package hirs.attestationca.persist.service;


import hirs.attestationca.persist.entity.manager.SupplyChainValidationSummaryRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.service.util.DataTablesColumn;
import hirs.attestationca.persist.service.util.PageServiceUtils;
import hirs.attestationca.persist.service.util.PredicateFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service class responsible for encapsulating all business logic related to the Validation Summary Page.
 */
@Service
@Log4j2
public class ValidationSummaryPageService {
    private final SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository;
    private final EntityManager entityManager;

    /**
     * Constructor for the Validation Summary Page Service.
     *
     * @param supplyChainValidationSummaryRepository supply chain validation summary repository
     * @param entityManager                          entity manager
     */
    @Autowired
    public ValidationSummaryPageService(final SupplyChainValidationSummaryRepository
                                                supplyChainValidationSummaryRepository,
                                        final EntityManager entityManager) {
        this.supplyChainValidationSummaryRepository = supplyChainValidationSummaryRepository;
        this.entityManager = entityManager;
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * validation summaries whose field values matches the provided search term.
     *
     * @param searchableColumnNames list of the searchable column name
     * @param globalSearchTerm      text that was input in the global search textbox
     * @param archiveFlag           archive flag
     * @param pageable              pageable
     * @return page full of the validation summaries.
     */
    public Page<SupplyChainValidationSummary> findValidationReportsByGlobalSearchTermAndArchiveFlag(
            final Set<String> searchableColumnNames,
            final String globalSearchTerm,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SupplyChainValidationSummary> query =
                criteriaBuilder.createQuery(SupplyChainValidationSummary.class);
        Root<SupplyChainValidationSummary> supplyChainValidationSummaryRoot =
                query.from(SupplyChainValidationSummary.class);

        final Predicate combinedGlobalSearchPredicates =
                createPredicatesForGlobalSearch(searchableColumnNames, criteriaBuilder,
                        supplyChainValidationSummaryRoot,
                        globalSearchTerm);

        // Define the conditions (predicates) for the query's WHERE clause.
        query.where(criteriaBuilder.and(
                combinedGlobalSearchPredicates,
                criteriaBuilder.equal(supplyChainValidationSummaryRoot.get("archiveFlag"), archiveFlag)
        ));

        // Apply sorting if present in the Pageable
        query.orderBy(
                getSortingOrders(criteriaBuilder, supplyChainValidationSummaryRoot, pageable.getSort()));

        // Apply pagination
        TypedQuery<SupplyChainValidationSummary> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<SupplyChainValidationSummary> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * Takes the provided columns that come with a search criteria and attempts to find
     * supply chain validation summary reports that match the column's specific search criteria's search
     * value.
     *
     * @param columnsWithSearchCriteria columns that have a search criteria applied to them
     * @param archiveFlag               archive flag
     * @param pageable                  pageable
     * @return page full of the validation summaries.
     */
    public Page<SupplyChainValidationSummary>
    findValidationSummaryReportsByColumnSpecificSearchTermAndArchiveFlag(
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SupplyChainValidationSummary> query =
                criteriaBuilder.createQuery(SupplyChainValidationSummary.class);
        Root<SupplyChainValidationSummary> supplyChainValidationSummaryRoot =
                query.from(SupplyChainValidationSummary.class);

        final Predicate combinedColumnSearchPredicates =
                createPredicatesForColumnSpecificSearch(columnsWithSearchCriteria, criteriaBuilder,
                        supplyChainValidationSummaryRoot);

        query.where(criteriaBuilder.and(combinedColumnSearchPredicates,
                criteriaBuilder.equal(supplyChainValidationSummaryRoot.get("archiveFlag"), archiveFlag)));

        // Apply sorting if present in the Pageable
        query.orderBy(
                getSortingOrders(criteriaBuilder, supplyChainValidationSummaryRoot, pageable.getSort()));

        // Apply pagination
        TypedQuery<SupplyChainValidationSummary> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<SupplyChainValidationSummary> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }


    /**
     * Finds supply chain validation summaries based on both global search and
     * column-specific search criteria.
     * The method applies the provided global search term across all searchable columns
     * and also applies column-specific filters based on the individual column search criteria.
     * The results are returned with pagination support.
     * <p>
     * This method combines the logic of two search functionalities:
     * - Global search: Searches across all specified columns for a matching term.
     * - Column-specific search: Filters based on individual column search criteria, such as text or date searches.
     * <p>
     *
     * @param searchableColumnNames     list of the searchable column names
     * @param globalSearchTerm          The term that the user enters in the global search box.
     * @param columnsWithSearchCriteria columns that have a search criteria applied to them
     * @param archiveFlag               archive flag
     * @param pageable                  pageable
     * @return A Page containing a list of validation summaries that match both the global search term and
     * the column-specific search criteria.
     */
    public Page<SupplyChainValidationSummary> findValidationSummaryReportsByGlobalAndColumnSpecificSearchTerm(
            final Set<String> searchableColumnNames,
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SupplyChainValidationSummary> query =
                criteriaBuilder.createQuery(SupplyChainValidationSummary.class);
        Root<SupplyChainValidationSummary> supplyChainValidationSummaryRoot =
                query.from(SupplyChainValidationSummary.class);

        final Predicate globalSearchPartOfChainedPredicates =
                createPredicatesForGlobalSearch(searchableColumnNames, criteriaBuilder,
                        supplyChainValidationSummaryRoot,
                        globalSearchTerm);

        final Predicate columnSearchPartOfChainedPredicates =
                createPredicatesForColumnSpecificSearch(columnsWithSearchCriteria, criteriaBuilder,
                        supplyChainValidationSummaryRoot);

        // Define the conditions (predicates) for the query's WHERE clause.
        // Combine global and column-specific predicates using AND logic
        query.where(criteriaBuilder.and(
                globalSearchPartOfChainedPredicates,
                columnSearchPartOfChainedPredicates,
                criteriaBuilder.equal(supplyChainValidationSummaryRoot.get("archiveFlag"), archiveFlag)
        ));

        // Apply sorting if present in the Pageable
        query.orderBy(
                getSortingOrders(criteriaBuilder, supplyChainValidationSummaryRoot, pageable.getSort()));

        // Apply pagination
        TypedQuery<SupplyChainValidationSummary> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<SupplyChainValidationSummary> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * Retrieves a page of Supply Chain Validation Summaries using the provided pageable value.
     *
     * @param pageable pageable
     * @return page of supply chain validation summaries
     */
    public Page<SupplyChainValidationSummary> findValidationSummaryReportsByPageable(final Pageable pageable) {
        return supplyChainValidationSummaryRepository.findByArchiveFlagFalse(pageable);
    }

    /**
     * Retrieves the total number of records in the supply chain validation summary repository.
     *
     * @return total number of records in the supply chain validation summary repository
     */
    public long findValidationSummaryRepositoryCount() {
        return supplyChainValidationSummaryRepository.count();
    }

    /**
     * Downloads a CSV report of the validation summary reports.
     *
     * @param bufferedWriter buffered writer
     * @throws IOException if there are any issues while trying to download the validation summary reports
     */
    public void downloadValidationReports(final BufferedWriter bufferedWriter) throws IOException {
        List<SupplyChainValidationSummary> summaries =
                supplyChainValidationSummaryRepository.findByArchiveFlagFalseOrderByCreateTimeDesc();

        final String validationReportsCsvHeader =
                "Device Status, Device Name, Validation Timestamp, Endorsement Certificate Validation Status,"
                        + " Platform Certificate Validation Status, Firmware Validation Status ";

        bufferedWriter.append(new StringBuilder(validationReportsCsvHeader)).append(System.lineSeparator());

        for (SupplyChainValidationSummary supplyChainValidationSummary : summaries) {
            bufferedWriter.append(supplyChainValidationSummary.getOverallValidationResult().toString())
                    .append(",")
                    .append(supplyChainValidationSummary.getDevice().getName())
                    .append(",")
                    .append(supplyChainValidationSummary.getCreateTime().toString())
                    .append(",");

            Set<SupplyChainValidation> supplyChainValidations = supplyChainValidationSummary.getValidations();

            // Build a map of validation type → result
            Map<SupplyChainValidation.ValidationType, String> validationResults = new HashMap<>();
            for (SupplyChainValidation validation : supplyChainValidations) {
                validationResults.put(validation.getValidationType(), validation.getValidationResult().toString());
            }

            // Append CSV columns in the order of the CSV header
            bufferedWriter.append(validationResults.getOrDefault(
                    SupplyChainValidation.ValidationType.ENDORSEMENT_CERTIFICATE, "")).append(",");
            bufferedWriter.append(validationResults.getOrDefault(
                    SupplyChainValidation.ValidationType.PLATFORM_CERTIFICATE, "")).append(",");
            bufferedWriter.append(validationResults.getOrDefault(
                    SupplyChainValidation.ValidationType.FIRMWARE, ""));
            bufferedWriter.append(System.lineSeparator());
        }
    }

    /**
     * Helper method that generates a list of sorting orders based on the provided {@link Pageable} object.
     * This method checks if sorting is enabled in the {@link Pageable} and applies the necessary sorting
     * to the query using the CriteriaBuilder and Supply Chain Validation Summary Root.
     *
     * @param criteriaBuilder                  the CriteriaBuilder used to create the sort expressions.
     * @param supplyChainValidationSummaryRoot the validation summary root to which the sorting should be applied.
     * @param pageableSort                     the {@link Sort} object that contains the sort information.
     * @return a list of {@link Order} objects, which can be applied to a CriteriaQuery for sorting.
     */
    private List<Order> getSortingOrders(final CriteriaBuilder criteriaBuilder,
                                         final Root<SupplyChainValidationSummary> supplyChainValidationSummaryRoot,
                                         final Sort pageableSort) {
        List<Order> orders = new ArrayList<>();

        if (pageableSort.isSorted()) {
            pageableSort.forEach(order -> {
                Path<Object> path;
                String property = order.getProperty();

                // handle nested properties like "device.name"
                if (property.startsWith("device.")) {
                    final String[] nestedColumnName = property.split("\\.");
                    final String mainField = nestedColumnName[0]; // device prefix
                    final String nestedField = nestedColumnName[1]; // property associated with device

                    // Handle the case where the related entity is the "device" field
                    Join<SupplyChainValidationSummary, Device> join =
                            supplyChainValidationSummaryRoot.join(mainField, JoinType.LEFT);

                    // Now, resolve the nested property on the joined entity (Device)
                    path = join.get(nestedField);  // Access the nested field on the "device" entity
                } else {
                    // handle simple properties that exist in the validation summary entity
                    path = supplyChainValidationSummaryRoot.get(order.getProperty());
                }

                orders.add(order.isAscending() ? criteriaBuilder.asc(path) : criteriaBuilder.desc(path));
            });
        }
        return orders;
    }

    /**
     * Helper method that generates a combined predicate for global search across searchable columns.
     * For each column, if the field is of type `String`, a "contains" condition is created.
     *
     * @param searchableColumnNames            the columns to be searched globally
     * @param criteriaBuilder                  the criteria builder to construct the predicates
     * @param supplyChainValidationSummaryRoot the root entity representing the supply chain validation
     *                                         summary
     * @param globalSearchTerm                 the term to search for across columns
     * @return a combined `Predicate` representing the global search conditions
     */
    private Predicate createPredicatesForGlobalSearch(
            final Set<String> searchableColumnNames,
            final CriteriaBuilder criteriaBuilder,
            final Root<SupplyChainValidationSummary> supplyChainValidationSummaryRoot,
            final String globalSearchTerm) {
        List<Predicate> combinedGlobalSearchPredicates = new ArrayList<>();

        // Dynamically loop through columns and create LIKE conditions for each searchable column
        for (String columnName : searchableColumnNames) {
            // If there is no period, we are dealing with a simple field
            if (!columnName.contains(".")) {
                if (String.class.equals(supplyChainValidationSummaryRoot.get(columnName).getJavaType())) {
                    Path<String> stringFieldPath = supplyChainValidationSummaryRoot.get(columnName);

                    Predicate predicate =
                            PredicateFactory.createPredicateForStringFields(criteriaBuilder,
                                    stringFieldPath, globalSearchTerm,
                                    PredicateFactory.STRING_FIELD_GLOBAL_SEARCH_LOGIC);
                    combinedGlobalSearchPredicates.add(predicate);
                } else if (Timestamp.class.equals(
                        supplyChainValidationSummaryRoot.get(columnName).getJavaType())) {
                    try {
                        Path<Timestamp> dateFieldPath = supplyChainValidationSummaryRoot.get(columnName);

                        final Timestamp columnSearchTimestamp =
                                PageServiceUtils.convertColumnSearchTermIntoTimeStamp(globalSearchTerm,
                                        PredicateFactory.DATE_FIELD_GLOBAL_SEARCH_LOGIC);

                        Predicate predicate =
                                PredicateFactory.createPredicateForTimestampFields(criteriaBuilder,
                                        dateFieldPath, columnSearchTimestamp,
                                        PredicateFactory.DATE_FIELD_GLOBAL_SEARCH_LOGIC);
                        combinedGlobalSearchPredicates.add(predicate);
                    } catch (DateTimeParseException dateTimeParseException) {
                        // ignore the exception since the user most likely has not entered a complete date
                    }
                }
            } else { // If there is a period, we are dealing with a nested field (e.g., "device.id")
                Predicate predicateForNestedField =
                        createPredicateForNestedField(criteriaBuilder, supplyChainValidationSummaryRoot,
                                columnName, globalSearchTerm,
                                PredicateFactory.STRING_FIELD_GLOBAL_SEARCH_LOGIC);
                combinedGlobalSearchPredicates.add(predicateForNestedField);
            }
        }

        return criteriaBuilder.or(combinedGlobalSearchPredicates.toArray(new Predicate[0]));
    }


    /**
     * Helper method that generates a combined predicate for column-specific search criteria.
     * It constructs conditions based on the field type (e.g., `String` or `Timestamp`)
     * and the provided search term and logic for each column.
     *
     * @param columnsWithSearchCriteria        the columns and their associated search criteria
     * @param criteriaBuilder                  the criteria builder to construct the predicates
     * @param supplyChainValidationSummaryRoot the root entity representing the supply chain validation
     *                                         summary
     * @return a combined `Predicate` representing the column-specific search conditions
     */
    private Predicate createPredicatesForColumnSpecificSearch(
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final CriteriaBuilder criteriaBuilder,
            final Root<SupplyChainValidationSummary> supplyChainValidationSummaryRoot) {
        List<Predicate> combinedColumnSearchPredicates = new ArrayList<>();

        // loop through all the datatable columns that have an applied search criteria
        for (DataTablesColumn columnWithSearchCriteria : columnsWithSearchCriteria) {
            final String columnName = columnWithSearchCriteria.getColumnName();
            final String columnSearchTerm = columnWithSearchCriteria.getColumnSearchTerm();
            final String columnSearchLogic = columnWithSearchCriteria.getColumnSearchLogic();

            // If there is no period, we are dealing with a simple field
            if (!columnName.contains(".")) {
                if (String.class.equals(supplyChainValidationSummaryRoot.get(columnName).getJavaType())) {
                    Path<String> stringFieldPath = supplyChainValidationSummaryRoot.get(columnName);

                    Predicate predicate =
                            PredicateFactory.createPredicateForStringFields(criteriaBuilder, stringFieldPath,
                                    columnSearchTerm,
                                    columnSearchLogic);
                    combinedColumnSearchPredicates.add(predicate);
                } else if (Timestamp.class.equals(
                        supplyChainValidationSummaryRoot.get(columnName).getJavaType())) {
                    try {
                        Path<Timestamp> dateFieldPath = supplyChainValidationSummaryRoot.get(columnName);

                        final Timestamp columnSearchTimestamp =
                                PageServiceUtils.convertColumnSearchTermIntoTimeStamp(columnSearchTerm,
                                        columnSearchLogic);

                        Predicate predicate =
                                PredicateFactory.createPredicateForTimestampFields(criteriaBuilder,
                                        dateFieldPath, columnSearchTimestamp,
                                        columnSearchLogic);
                        combinedColumnSearchPredicates.add(predicate);
                    } catch (DateTimeParseException dateTimeParseException) {
                        // ignore the exception since the user most likely has not entered a complete date
                    }
                }
            } else { // If there is a period, we are dealing with a nested field (e.g., "device.id")
                Predicate predicateForNestedField =
                        createPredicateForNestedField(criteriaBuilder, supplyChainValidationSummaryRoot,
                                columnName, columnSearchTerm, columnSearchLogic);
                combinedColumnSearchPredicates.add(predicateForNestedField);
            }
        }

        return criteriaBuilder.and(combinedColumnSearchPredicates.toArray(new Predicate[0]));
    }

    /**
     * Helper method that creates a {@link Predicate} for a nested field in the
     * {@link SupplyChainValidationSummary} entity.
     * This method handles fields that are part of a nested entity (e.g., "device.name") and generates a
     * predicate for filtering based on the provided search term and search logic.
     *
     * @param criteriaBuilder                  The {@link CriteriaBuilder} used to construct the
     *                                         {@link Predicate}.
     * @param supplyChainValidationSummaryRoot The root of the {@link CriteriaQuery} representing the
     *                                         {@link SupplyChainValidationSummary} entity.
     * @param columnName                       The name of the column or field, which may refer to a nested
     *                                         field (e.g., "device.name").
     * @param searchTerm                       The search term used to filter the field's value.
     * @param searchLogic                      The search logic (e.g., "contains", "equals") to apply to the
     *                                         search term.
     * @return A {@link Predicate} that can be used in the query's WHERE clause
     * to filter the results based on the nested field and search criteria.
     */
    private Predicate createPredicateForNestedField(
            final CriteriaBuilder criteriaBuilder,
            final Root<SupplyChainValidationSummary> supplyChainValidationSummaryRoot,
            final String columnName,
            final String searchTerm,
            final String searchLogic) {
        final String[] nestedColumnName = columnName.split("\\.");
        final String mainField = nestedColumnName[0];
        final String nestedField = nestedColumnName[1];

        // Handle the case where the related entity is the "device" field
        if (mainField.equals("device")) {
            // Dynamically join the main entity
            Join<SupplyChainValidationSummary, Device> join =
                    supplyChainValidationSummaryRoot.join(mainField, JoinType.LEFT);

            // Check the field type and create the predicate if it's a String
            if (String.class.equals(join.get(nestedField).getJavaType())) {
                Path<String> stringFieldPath = join.get(nestedField);
                return PredicateFactory.createPredicateForStringFields(criteriaBuilder, stringFieldPath,
                        searchTerm, searchLogic);
            }
        }

        return null; // No predicate if the nested field is not a string type
    }
}
