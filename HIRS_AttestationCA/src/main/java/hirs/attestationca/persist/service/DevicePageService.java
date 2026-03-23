package hirs.attestationca.persist.service;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.EndorsementCertificateRepository;
import hirs.attestationca.persist.entity.manager.IssuedCertificateRepository;
import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service class responsible for encapsulating all business logic related to the Device Page.
 */
@Service
@Log4j2
public class DevicePageService {
    private final DeviceRepository deviceRepository;
    private final PlatformCertificateRepository platformCertificateRepository;
    private final EndorsementCertificateRepository endorsementCertificateRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;
    private final EntityManager entityManager;

    /**
     * Constructor for Device Page Service.
     *
     * @param deviceRepository                 device repository
     * @param platformCertificateRepository    platform certificate repository
     * @param endorsementCertificateRepository endorsement certificate repository
     * @param issuedCertificateRepository      issued certificate repository
     * @param entityManager                    entity manager
     */
    @Autowired
    public DevicePageService(final DeviceRepository deviceRepository,
                             final PlatformCertificateRepository platformCertificateRepository,
                             final EndorsementCertificateRepository endorsementCertificateRepository,
                             final IssuedCertificateRepository issuedCertificateRepository,
                             final EntityManager entityManager) {
        this.deviceRepository = deviceRepository;
        this.platformCertificateRepository = platformCertificateRepository;
        this.endorsementCertificateRepository = endorsementCertificateRepository;
        this.issuedCertificateRepository = issuedCertificateRepository;
        this.entityManager = entityManager;
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * {@link Device} objects whose field values matches the provided search term.
     *
     * @param searchableColumnNames list of the searchable column name
     * @param globalSearchTerm      text that was input in the global search textbox
     * @param pageable              pageable
     * @return page full of {@link Device} objects
     */
    public Page<Device> findDevicesByGlobalSearchTerm(
            final Set<String> searchableColumnNames,
            final String globalSearchTerm,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Device> query = criteriaBuilder.createQuery(Device.class);
        Root<Device> deviceRoot = query.from(Device.class);

        final Predicate combinedGlobalSearchPredicates =
                createPredicatesForGlobalSearch(searchableColumnNames, criteriaBuilder, deviceRoot,
                        globalSearchTerm);

        // Define the conditions (predicates) for the query's WHERE clause.
        query.where(criteriaBuilder.and(combinedGlobalSearchPredicates));

        // Apply sorting if present in the Pageable
        query.orderBy(getSortingOrders(criteriaBuilder, deviceRoot, pageable.getSort()));

        // Apply pagination
        TypedQuery<Device> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<Device> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }


    /**
     * Takes the provided columns that come with a search criteria and attempts to find
     * {@link Device} objects that match the column's specific search criteria search value.
     *
     * @param columnsWithSearchCriteria columns that have a search criteria applied to them
     * @param pageable                  pageable
     * @return page full of {@link Device} objects
     */
    public Page<Device> findDevicesByColumnSpecificSearchTerm(
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Device> query = criteriaBuilder.createQuery(Device.class);
        Root<Device> deviceRoot = query.from(Device.class);

        final Predicate combinedColumnSearchPredicates =
                createPredicatesForColumnSpecificSearch(columnsWithSearchCriteria, criteriaBuilder,
                        deviceRoot);

        // Define the conditions (predicates) for the query's WHERE clause.
        query.where(criteriaBuilder.and(combinedColumnSearchPredicates));

        // Apply sorting if present in the Pageable
        query.orderBy(getSortingOrders(criteriaBuilder, deviceRoot, pageable.getSort()));

        // Apply pagination
        TypedQuery<Device> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<Device> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }


    /**
     * Finds {@link Device} objects based on both global search and column-specific search criteria.
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
     * @param pageable                  pageable
     * @return A page of {@link Device} objects that match both the global search term and
     * the column-specific search criteria.
     */
    public Page<Device> findDevicesByGlobalAndColumnSpecificSearchTerm(
            final Set<String> searchableColumnNames,
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Device> query = criteriaBuilder.createQuery(Device.class);
        Root<Device> deviceRoot = query.from(Device.class);

        final Predicate globalSearchPartOfChainedPredicates =
                createPredicatesForGlobalSearch(searchableColumnNames, criteriaBuilder, deviceRoot,
                        globalSearchTerm);

        final Predicate columnSearchPartOfChainedPredicates =
                createPredicatesForColumnSpecificSearch(columnsWithSearchCriteria, criteriaBuilder,
                        deviceRoot);

        // Define the conditions (predicates) for the query's WHERE clause.
        // Combine global and column-specific predicates using AND logic
        query.where(criteriaBuilder.and(globalSearchPartOfChainedPredicates,
                columnSearchPartOfChainedPredicates));

        // Apply sorting if present in the Pageable
        query.orderBy(getSortingOrders(criteriaBuilder, deviceRoot, pageable.getSort()));

        // Apply pagination
        TypedQuery<Device> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<Device> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }


    /**
     * Retrieves all {@link Device} objects from the database.
     *
     * @param pageable pageable
     * @return a page of all {@link Device} objects
     */
    public Page<Device> findAllDevices(final Pageable pageable) {
        return deviceRepository.findAll(pageable);
    }

    /**
     * Retrieves the total number of records stored in the {@link DeviceRepository}.
     *
     * @return total number of records stored in the {@link DeviceRepository}.
     */
    public long findDeviceRepositoryCount() {
        return deviceRepository.count();
    }

    /**
     * Returns a map of {@link Device} objects with their associated {@link PlatformCredential} and
     * {@link EndorsementCredential} objects.
     *
     * @param deviceList a filtered list of {@link Device} objects
     * @return a filtered map of {@link Device} objects and associated certificates.
     */
    public FilteredRecordsList<HashMap<String, Object>> retrieveDevicesAndAssociatedCertificates(
            final FilteredRecordsList<Device> deviceList) {
        FilteredRecordsList<HashMap<String, Object>> records = new FilteredRecordsList<>();

        // hashmap containing the device-certificate relationship
        HashMap<String, Object> deviceCertMap = new HashMap<>();

        for (Device device : deviceList) {
            if (device == null) {
                log.error("Encountered a null device in the list. Skipping to the next device in list...");
                continue;
            }

            // hashmap that uses the certificate type as the key and the set of device associated certificate IDs
            // as the value
            HashMap<String, Set<UUID>> certificatePropertyMap = new HashMap<>();

            deviceCertMap.put("device", device);

            // create a new entry for platform certificates associated with the current device in the map
            addPlatformCertificateEntryToDeviceMap(device, certificatePropertyMap);

            // create a new entry for the endorsement certificate associated with the current device in the map
            addEndorsementCertificateEntryToDeviceMap(device, certificatePropertyMap);

            // create a new entry for issued certificates associated with the current device in the map
            addIssuedCertificateEntryToDeviceMap(device, certificatePropertyMap);

            // add the device-certificate map to the record
            deviceCertMap.putAll(certificatePropertyMap);
            records.add(new HashMap<>(deviceCertMap));
            deviceCertMap.clear();
        }

        // set pagination values
        records.setRecordsTotal(deviceList.getRecordsTotal());
        records.setRecordsFiltered(deviceList.getRecordsFiltered());
        return records;
    }

    /**
     * Helper method that attempts to find all the {@link PlatformCredential} objects that are associated with the
     * provided {@link Device} object and add a new entry to the device-certificate hash map for Platform Certificate
     * IDs.
     *
     * @param device                 {@link Device} object
     * @param certificatePropertyMap hash map of the certificate type and list of associated certificate IDs
     */
    private void addPlatformCertificateEntryToDeviceMap(final Device device,
                                                        final HashMap<String, Set<UUID>> certificatePropertyMap) {
        // find all platform certificates associated with this device id
        final List<PlatformCredential> platformCertificateList =
                platformCertificateRepository.findByDeviceId(device.getId());

        final String platformCertificateIdsKey = "PlatformCertificateIds";

        for (PlatformCredential pc : platformCertificateList) {
            // verify that the platform certificate is associated with this device
            if (device.getName().equals(pc.getDeviceName())) {
                // if there is not a platform certificate entry already in the map, create a new set
                certificatePropertyMap.computeIfAbsent(platformCertificateIdsKey, _ -> new HashSet<>())
                        .add(pc.getId());  // Add the new ID to the set
            }
        }
    }

    /**
     * Helper method that attempts to find all the {@link EndorsementCredential} objects that are associated with the
     * provided device and add a new entry to the device-certificate hash map for Endorsement Certificate IDs.
     *
     * @param device                 {@link Device} object
     * @param certificatePropertyMap hash map of the certificate type and list of associated certificate IDs
     */
    private void addEndorsementCertificateEntryToDeviceMap(final Device device,
                                                           final HashMap<String, Set<UUID>> certificatePropertyMap) {
        // find all endorsement certificates associated with this device id
        final List<EndorsementCredential> endorsementCertificateList =
                endorsementCertificateRepository.findByDeviceId(device.getId());

        final String endorsementCertificateIdsKey = "EndorsementCertificateIds";

        for (EndorsementCredential ec : endorsementCertificateList) {
            // verify that the endorsement certificate is associated with this device
            if (device.getName().equals(ec.getDeviceName())) {
                // if there is not an endorsement certificate entry already in the map, create a new set
                certificatePropertyMap.computeIfAbsent(endorsementCertificateIdsKey, _ -> new HashSet<>())
                        .add(ec.getId());  // Add the new ID to the set
            }
        }
    }

    /**
     * Helper method that attempts to find all the {@link IssuedAttestationCertificate} objects that are associated with
     * the provided {@link Device} object and add a new entry to the device-certificate hash map for
     * Issued Certificate IDs.
     *
     * @param device                 {@link Device} object
     * @param certificatePropertyMap hash map of the certificate type and list of associated certificate IDs
     */
    private void addIssuedCertificateEntryToDeviceMap(final Device device,
                                                      final HashMap<String, Set<UUID>> certificatePropertyMap) {
        // find all issued certificates associated with this device id
        final List<IssuedAttestationCertificate> issuedCertificateList =
                issuedCertificateRepository.findByDeviceId(device.getId());

        final String issuedCertificatesIdsKey = "IssuedCertificateIds";

        for (IssuedAttestationCertificate ic : issuedCertificateList) {
            // verify that the issued certificate is associated with this device
            if (device.getName().equals(ic.getDeviceName())) {
                // if there is not an issued certificate entry already in the map, create a new set
                certificatePropertyMap.computeIfAbsent(issuedCertificatesIdsKey, _ -> new HashSet<>())
                        .add(ic.getId());  // Add the new ID to the set
            }
        }
    }

    /**
     * Helper method that generates a list of sorting orders based on the provided {@link Pageable} object.
     * This method checks if sorting is enabled in the {@link Pageable} and applies the necessary sorting
     * to the query using the CriteriaBuilder and Device Root.
     *
     * @param criteriaBuilder the CriteriaBuilder used to create the sort expressions.
     * @param deviceRoot      the Device Root to which the sorting should be applied.
     * @param pageableSort    the {@link Sort} object that contains the sort information.
     * @return a list of {@link Order} objects, which can be applied to a CriteriaQuery for sorting.
     */
    private List<Order> getSortingOrders(final CriteriaBuilder criteriaBuilder,
                                         final Root<Device> deviceRoot,
                                         final Sort pageableSort) {
        List<Order> orders = new ArrayList<>();

        if (pageableSort.isSorted()) {
            pageableSort.forEach(order -> {
                Path<Object> path = deviceRoot.get(order.getProperty());
                orders.add(order.isAscending() ? criteriaBuilder.asc(path) : criteriaBuilder.desc(path));
            });
        }
        return orders;
    }

    /**
     * Helper method that generates a combined predicate for global search across searchable columns.
     * For each column, if the field is of type `String`, a "contains" condition is created.
     *
     * @param searchableColumnNames the columns to be searched globally
     * @param criteriaBuilder       the criteria builder to construct the predicates
     * @param deviceRoot            the root entity representing the device
     * @param globalSearchTerm      the term to search for across columns
     * @return a combined `Predicate` representing the global search conditions
     */
    private Predicate createPredicatesForGlobalSearch(
            final Set<String> searchableColumnNames,
            final CriteriaBuilder criteriaBuilder,
            final Root<Device> deviceRoot,
            final String globalSearchTerm) {
        List<Predicate> combinedGlobalSearchPredicates = new ArrayList<>();

        // Dynamically loop through columns and create LIKE conditions for each searchable column
        for (String columnName : searchableColumnNames) {
            // since datatables returns us a nested column name, in order to get the correct
            // the column name for devices, we need remove the device
            // part of the string (e.g., "device.name" becomes "name").
            if (columnName.startsWith("device.")) {
                columnName = columnName.split("device.")[1]; // Take the part after "device."
            }

            if (String.class.equals(deviceRoot.get(columnName).getJavaType())) {
                Path<String> stringFieldPath = deviceRoot.get(columnName);

                Predicate predicate = PredicateFactory.createPredicateForStringFields(criteriaBuilder,
                        stringFieldPath, globalSearchTerm, PredicateFactory.STRING_FIELD_GLOBAL_SEARCH_LOGIC);
                combinedGlobalSearchPredicates.add(predicate);
            }
        }

        return criteriaBuilder.or(combinedGlobalSearchPredicates.toArray(new Predicate[0]));
    }


    /**
     * Helper method that generates a combined predicate for column-specific search criteria.
     * It constructs conditions based on the field type (e.g., `String` or `Timestamp`)
     * and the provided search term and logic for each column.
     *
     * @param columnsWithSearchCriteria the columns and their associated search criteria
     * @param criteriaBuilder           the criteria builder to construct the predicates
     * @param deviceRoot                the root entity representing the device
     * @return a combined `Predicate` representing the column-specific search conditions
     */
    private Predicate createPredicatesForColumnSpecificSearch(
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final CriteriaBuilder criteriaBuilder,
            final Root<Device> deviceRoot) {
        List<Predicate> combinedColumnSearchPredicates = new ArrayList<>();

        // loop through all the datatable columns that have an applied search criteria
        for (DataTablesColumn columnWithSearchCriteria : columnsWithSearchCriteria) {
            String columnName = columnWithSearchCriteria.getColumnName();
            final String columnSearchTerm = columnWithSearchCriteria.getColumnSearchTerm();
            final String columnSearchLogic = columnWithSearchCriteria.getColumnSearchLogic();

            // since datatables returns us a nested column name, in order to get the correct
            // the column name for devices, we need remove the device
            // part of the string (e.g., "device.name" becomes "name").
            if (columnName.startsWith("device.")) {
                columnName = columnName.split("device.")[1]; // Take the part after "device."
            }

            if (String.class.equals(deviceRoot.get(columnName).getJavaType())) {
                Path<String> stringFieldPath = deviceRoot.get(columnName);

                Predicate predicate =
                        PredicateFactory.createPredicateForStringFields(criteriaBuilder, stringFieldPath,
                                columnSearchTerm,
                                columnSearchLogic);
                combinedColumnSearchPredicates.add(predicate);
            }
        }

        return criteriaBuilder.and(combinedColumnSearchPredicates.toArray(new Predicate[0]));
    }
}
