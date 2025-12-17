package hirs.attestationca.persist.service;


import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import hirs.attestationca.persist.entity.manager.SupplyChainValidationSummaryRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A service layer class responsible for encapsulating all business logic related to the Validation Summary
 * Page.
 */
@Service
@Log4j2
public class ValidationSummaryPageService {
    private static final String DEFAULT_COMPANY = "AllDevices";
    private static final String UNDEFINED = "undefined";
    private static final String SYSTEM_COLUMN_HEADERS = "Verified Manufacturer,"
            + "Model,SN,Verification Date,Device Status";
    private static final String COMPONENT_COLUMN_HEADERS = "Component name,Component manufacturer,"
            + "Component model,Component SN,Issuer,Component status";

    private final SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository;
    private final PlatformCertificateRepository platformCertificateRepository;
    private final EntityManager entityManager;
    private final CertificateRepository certificateRepository;
    private final DeviceRepository deviceRepository;

    /**
     * Constructor for the Validation Summary Page Service.
     *
     * @param supplyChainValidationSummaryRepository supply chain validation summary repository
     * @param platformCertificateRepository          platform certificate repository
     * @param certificateRepository                  certificate repository
     * @param deviceRepository                       device repository
     * @param entityManager                          entity manager
     */
    @Autowired
    public ValidationSummaryPageService(final SupplyChainValidationSummaryRepository
                                                supplyChainValidationSummaryRepository,
                                        final PlatformCertificateRepository platformCertificateRepository,
                                        final CertificateRepository certificateRepository,
                                        final DeviceRepository deviceRepository,
                                        final EntityManager entityManager) {
        this.supplyChainValidationSummaryRepository = supplyChainValidationSummaryRepository;
        this.platformCertificateRepository = platformCertificateRepository;
        this.certificateRepository = certificateRepository;
        this.deviceRepository = deviceRepository;
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
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
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
        query.orderBy(getSortingOrders(criteriaBuilder, supplyChainValidationSummaryRoot, pageable));

        // Apply pagination
        TypedQuery<SupplyChainValidationSummary> typedQuery = this.entityManager.createQuery(query);
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
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
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
        query.orderBy(getSortingOrders(criteriaBuilder, supplyChainValidationSummaryRoot, pageable));

        // Apply pagination
        TypedQuery<SupplyChainValidationSummary> typedQuery = this.entityManager.createQuery(query);
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
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
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
        query.orderBy(getSortingOrders(criteriaBuilder, supplyChainValidationSummaryRoot, pageable));

        // Apply pagination
        TypedQuery<SupplyChainValidationSummary> typedQuery = this.entityManager.createQuery(query);
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
    public Page<SupplyChainValidationSummary> findValidationSummaryReportsByPageable(
            final Pageable pageable) {
        return this.supplyChainValidationSummaryRepository.findByArchiveFlagFalse(pageable);
    }

    /**
     * Retrieves the total number of records in the supply chain validation summary repository.
     *
     * @return total number of records in the supply chain validation summary repository
     */
    public long findValidationSummaryRepositoryCount() {
        return this.supplyChainValidationSummaryRepository.count();
    }

    /**
     * Downloads the validation summary reports based on the provided request parameters.
     *
     * @param request  http request
     * @param response http response
     * @throws IOException if there are any issues while trying to download the summary reports
     */
    public void downloadValidationReports(final HttpServletRequest request,
                                          final HttpServletResponse response)
            throws IOException {
        String company = "";
        String contractNumber = "";
        Pattern pattern = Pattern.compile("^\\w*$");
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        DateTimeFormatter dateTimeFormat =
                DateTimeFormatter.ofPattern("EEE, dd MMM uuuu HH:mm:ss z");
        LocalDate startDate = null;
        LocalDate endDate = null;
        ArrayList<LocalDate> createTimes = new ArrayList<>();
        String[] deviceNames = new String[] {};

        final Enumeration<String> parameters = request.getParameterNames();

        while (parameters.hasMoreElements()) {
            String parameter = parameters.nextElement();
            String parameterValue = request.getParameter(parameter);
            log.debug("HTTP Servlet Request Param: {}: HTTP Servlet Request Param Value: {}", parameter,
                    parameterValue);
            switch (parameter) {
                case "company":
                    Matcher companyMatcher = pattern.matcher(parameterValue);
                    if (companyMatcher.matches()) {
                        company = parameterValue;
                    } else {
                        company = DEFAULT_COMPANY;
                    }
                    break;
                case "contract":
                    Matcher contractMatcher = pattern.matcher(parameterValue);
                    if (contractMatcher.matches()) {
                        contractNumber = parameterValue;
                    } else {
                        contractNumber = "none";
                    }
                    break;
                case "dateStart":
                    if (parameterValue != null && !parameterValue.isEmpty()) {
                        startDate = LocalDate.parse(parameterValue, dateFormat);
                    } else {
                        startDate = LocalDate.ofEpochDay(0);
                    }
                    break;
                case "dateEnd":
                    if (parameterValue != null && !parameterValue.isEmpty()) {
                        endDate = LocalDate.parse(parameterValue, dateFormat);
                    } else {
                        endDate = LocalDate.now(ZoneId.of("America/New_York"));
                    }
                    break;
                case "createTimes":
                    if (!parameterValue.equals(UNDEFINED)
                            && !parameterValue.isEmpty()) {
                        String[] timestamps = parameterValue.split(";");

                        for (String timestamp : timestamps) {
                            ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, dateTimeFormat);

                            // Convert to LocalDateTime (drops time zone info)
                            LocalDate localDate = zonedDateTime.toLocalDate();

                            createTimes.add(localDate);
                        }
                    }
                    break;
                case "deviceNames":
                    if (!parameterValue.equals(UNDEFINED)
                            && !parameterValue.isEmpty()) {
                        deviceNames = parameterValue.split(",");
                    }
                    break;
                default:
            }
        }

        response.setHeader("Content-Type", "text/csv");
        response.setHeader("Content-Disposition",
                "attachment;filename=validation_report.csv");

        BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));

        StringBuilder reportData = new StringBuilder();

        for (int i = 0; i < deviceNames.length; i++) {
            if ((createTimes.get(i).isAfter(startDate) || createTimes.get(i).isEqual(
                    Objects.requireNonNull(startDate)))
                    && (createTimes.get(i).isBefore(endDate)
                    || createTimes.get(i).isEqual(Objects.requireNonNull(endDate)))) {
                Device device = deviceRepository.findByName(deviceNames[i]);
                PlatformCredential pc = platformCertificateRepository.findByDeviceId(device.getId()).get(0);

                if (i == 0) {
                    bufferedWriter.append("Company: ").append(company).append("\n");
                    bufferedWriter.append("Contract number: ").append(contractNumber).append("\n");
                }

                StringBuilder systemInfo = new StringBuilder();
                systemInfo.append(pc.getManufacturer())
                        .append(",")
                        .append(pc.getModel())
                        .append(",")
                        .append(pc.getPlatformSerial())
                        .append(",")
                        .append(LocalDateTime.now())
                        .append(",")
                        .append(device.getSupplyChainValidationStatus())
                        .append(",");

                ArrayList<ArrayList<String>> parsedComponents = parsePlatformCredentialComponents(pc);

                for (ArrayList<String> component : parsedComponents) {
                    reportData.append(systemInfo);
                    for (String data : component) {
                        reportData.append(data).append(",");
                    }
                    reportData.deleteCharAt(reportData.length() - 1);
                    reportData.append(System.lineSeparator());
                }
            }
        }

        bufferedWriter.append(new StringBuilder(SYSTEM_COLUMN_HEADERS + "," + COMPONENT_COLUMN_HEADERS))
                .append(System.lineSeparator());
        bufferedWriter.append(reportData.toString());
        bufferedWriter.flush();
    }

    /**
     * Helper method that generates a list of sorting orders based on the provided {@link Pageable} object.
     * This method checks if sorting is enabled in the {@link Pageable} and applies the necessary sorting
     * to the query using the CriteriaBuilder and Supply Chain Validation Summary Root.
     *
     * @param criteriaBuilder                  the CriteriaBuilder used to create the sort expressions.
     * @param supplyChainValidationSummaryRoot the validation summary root to which the sorting should be applied.
     * @param pageable                         the {@link Pageable} object that contains the sort information.
     * @return a list of {@link Order} objects, which can be applied to a CriteriaQuery for sorting.
     */
    private List<Order> getSortingOrders(final CriteriaBuilder criteriaBuilder,
                                         final Root<SupplyChainValidationSummary> supplyChainValidationSummaryRoot,
                                         final Pageable pageable) {
        List<Order> orders = new ArrayList<>();

        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                Path<Object> path;

                if (order.getProperty().startsWith("device.")) {
                    path = null; //todo
                } else {
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

    /**
     * This method parses the provided platform credential's list of ComponentIdentifiers into an ArrayList
     * of ArrayLists.
     * - ComponentClass
     * - Manufacturer
     * - Model
     * - Serial number
     * - Pass/fail status (based on componentFailures string)
     *
     * @param pc the platform credential.
     * @return the ArrayList of ArrayLists containing the parsed component data.
     */
    private ArrayList<ArrayList<String>> parsePlatformCredentialComponents(final PlatformCredential pc)
            throws IOException {
        ArrayList<ArrayList<String>> parsedComponents = new ArrayList<>();
        ArrayList<ArrayList<Object>> chainComponents = new ArrayList<>();

        // get all the certificates associated with the platform serial
        final List<PlatformCredential> chainCertificates =
                certificateRepository.byBoardSerialNumber(pc.getPlatformSerial());

        StringBuilder componentFailureString = new StringBuilder();
        componentFailureString.append(pc.getComponentFailures());
        log.debug("Component failures: {}", componentFailureString);

        // if the platform credential's has a list of version 1 component identifiers
        if (pc.getPlatformConfigurationV1() != null && pc.getComponentIdentifiers() != null) {
            List<ComponentIdentifier> componentIdentifiers = pc.getComponentIdentifiers();

            // combine all components in each certificate
            for (ComponentIdentifier ci : componentIdentifiers) {
                ArrayList<Object> issuerAndComponent = new ArrayList<>();
                issuerAndComponent.add(pc.getHolderIssuer());
                issuerAndComponent.add(ci);
                chainComponents.add(issuerAndComponent);
            }

            for (PlatformCredential cert : chainCertificates) {
                componentFailureString.append(cert.getComponentFailures());
                if (!cert.isPlatformBase()) {
                    List<ComponentIdentifier> chainComponentIdentifiers = cert.getComponentIdentifiers();
                    for (ComponentIdentifier ci : chainComponentIdentifiers) {
                        ArrayList<Object> issuerAndComponent = new ArrayList<>();
                        issuerAndComponent.add(cert.getHolderIssuer());
                        issuerAndComponent.add(ci);
                        chainComponents.add(issuerAndComponent);
                    }
                }
            }

            for (ArrayList<Object> issuerAndComponent : chainComponents) {
                ArrayList<String> componentData = new ArrayList<>();
                String issuer = (String) issuerAndComponent.get(0);
                issuer = issuer.replaceAll(",", " ");
                ComponentIdentifier ci = (ComponentIdentifier) issuerAndComponent.get(1);

                componentData.add("Platform Component");
                componentData.add(ci.getComponentManufacturer().getString());
                componentData.add(ci.getComponentModel().getString());
                componentData.add(ci.getComponentSerial().getString());
                componentData.add(issuer);

                //Failing components are identified by hashcode
                if (componentFailureString.toString().contains(String.valueOf(ci.hashCode()))) {
                    componentData.add("Fail");
                } else {
                    componentData.add("Pass");
                }
                parsedComponents.add(componentData);
                log.debug("Parsed Component Identifiers V1: {}",
                        String.join(",", componentData));
            }
        } else if (pc.getPlatformConfigurationV2() != null && pc.getComponentIdentifiersV2() != null) {
            List<ComponentIdentifierV2> componentIdentifiersV2 = pc.getComponentIdentifiersV2();

            // combine all components in each certificate
            for (ComponentIdentifierV2 ci2 : componentIdentifiersV2) {
                ArrayList<Object> issuerAndComponent = new ArrayList<>();
                issuerAndComponent.add(pc.getHolderIssuer());
                issuerAndComponent.add(ci2);
                chainComponents.add(issuerAndComponent);
            }

            for (PlatformCredential cert : chainCertificates) {
                componentFailureString.append(cert.getComponentFailures());
                if (!cert.isPlatformBase()) {
                    List<ComponentIdentifierV2> chainComponentIdentifiersV2 =
                            cert.getComponentIdentifiersV2();
                    for (ComponentIdentifierV2 ci2 : chainComponentIdentifiersV2) {
                        ArrayList<Object> issuerAndComponent = new ArrayList<>();
                        issuerAndComponent.add(cert.getHolderIssuer());
                        issuerAndComponent.add(ci2);
                        chainComponents.add(issuerAndComponent);
                    }
                }
            }

            for (ArrayList<Object> issuerAndComponent : chainComponents) {
                ArrayList<String> componentData = new ArrayList<>();
                String issuer = (String) issuerAndComponent.get(0);
                issuer = issuer.replaceAll(",", " ");
                ComponentIdentifierV2 ci2 = (ComponentIdentifierV2) issuerAndComponent.get(1);

                String componentClass = ci2.getComponentClass().toString();
                String[] splitStrings = componentClass.split("\r\n|\n|\r");

                componentData.add(String.join(" ", splitStrings));
                componentData.add(ci2.getComponentManufacturer().getString());
                componentData.add(ci2.getComponentModel().getString());
                componentData.add(ci2.getComponentSerial().getString());
                componentData.add(issuer);
                //Failing components are identified by hashcode
                if (componentFailureString.toString().contains(String.valueOf(ci2.hashCode()))) {
                    componentData.add("Fail");
                } else {
                    componentData.add("Pass");
                }
                parsedComponents.add(componentData);
                log.debug("Parsed Component Identifiers V2: {}",
                        String.join(",", componentData));
            }
        }

        return parsedComponents;
    }
}
