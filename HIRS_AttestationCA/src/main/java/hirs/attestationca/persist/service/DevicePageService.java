package hirs.attestationca.persist.service;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.EndorsementCredentialRepository;
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
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A service layer class responsible for encapsulating all business logic related to the Device Page.
 */
@Service
@Log4j2
public class DevicePageService {
    private final DeviceRepository deviceRepository;
    private final PlatformCertificateRepository platformCertificateRepository;
    private final EndorsementCredentialRepository endorsementCredentialRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;
    private final EntityManager entityManager;

    /**
     * Constructor for Device Page Service.
     *
     * @param deviceRepository                device repository
     * @param platformCertificateRepository   platform certificate repository
     * @param endorsementCredentialRepository endorsement credential repository
     * @param issuedCertificateRepository     issued certificate repository
     * @param entityManager                   entity manager
     */
    @Autowired
    public DevicePageService(final DeviceRepository deviceRepository,
                             final PlatformCertificateRepository platformCertificateRepository,
                             final EndorsementCredentialRepository endorsementCredentialRepository,
                             final IssuedCertificateRepository issuedCertificateRepository,
                             final EntityManager entityManager) {
        this.deviceRepository = deviceRepository;
        this.platformCertificateRepository = platformCertificateRepository;
        this.endorsementCredentialRepository = endorsementCredentialRepository;
        this.issuedCertificateRepository = issuedCertificateRepository;
        this.entityManager = entityManager;
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * devices whose field values matches the provided search term.
     *
     * @param searchableColumnNames list of the searchable column name
     * @param globalSearchTerm      text that was input in the global search textbox
     * @param pageable              pageable
     * @return page full of devices
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
     * devices that match the column's specific search criteria's search value.
     *
     * @param columnsWithSearchCriteria columns that have a search criteria applied to them
     * @param pageable                  pageable
     * @return page full of devices
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
     * Finds devices based on both global search and column-specific search criteria.
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
     * @return A Page containing a list of devices that match both the global search term and
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
     * Retrieves all devices from the database.
     *
     * @param pageable pageable
     * @return a page of all devices
     */
    public Page<Device> findAllDevices(final Pageable pageable) {
        return this.deviceRepository.findAll(pageable);
    }

    /**
     * Retrieves the total number of records in the device repository.
     *
     * @return total number of records in the device repository.
     */
    public long findDeviceRepositoryCount() {
        return this.deviceRepository.count();
    }

    /**
     * Returns the list of devices associated with the platform and endorsement certificates.
     *
     * @param deviceList list containing the devices
     * @return a record list after the device and certificate was mapped together.
     */
    public FilteredRecordsList<HashMap<String, Object>> retrieveDevicesAndAssociatedCertificates(
            final FilteredRecordsList<Device> deviceList) {
        FilteredRecordsList<HashMap<String, Object>> records = new FilteredRecordsList<>();

        // hashmap containing the device-certificate relationship
        HashMap<String, Object> deviceCertMap = new HashMap<>();

        final List<UUID> deviceIdList = getDevicesId(deviceList);
        List<PlatformCredential> platformCredentialList = new ArrayList<>();
        List<EndorsementCredential> endorsementCredentialList = new ArrayList<>();
        List<IssuedAttestationCertificate> issuedCertificateList = new ArrayList<>();
        List<Object> certificateListFromMap;

        // parse if there is a Device
        if (!deviceList.isEmpty()) {
            // get a list of Certificates that contains the device IDs from the list
            for (UUID id : deviceIdList) {
                platformCredentialList.addAll(this.platformCertificateRepository.findByDeviceId(id));
                endorsementCredentialList.addAll(this.endorsementCredentialRepository.findByDeviceId(id));
                issuedCertificateList.addAll(this.issuedCertificateRepository.findByDeviceId(id));
            }

            HashMap<String, List<Object>> certificatePropertyMap;

            for (Device device : deviceList) {
                // hashmap containing the list of certificates based on the certificate type
                certificatePropertyMap = new HashMap<>();

                deviceCertMap.put("device", device);

                String deviceName;

                // loop all the certificates and combine the ones that match the ID
                for (PlatformCredential pc : platformCredentialList) {
                    deviceName = deviceRepository.findById(pc.getDeviceId()).get().getName();

                    // verify that the platform certificate is associated with this
                    // device
                    if (device.getName().equals(deviceName)) {
                        final String platformCredentialIdsKey =
                                PlatformCredential.class.getSimpleName() + "Ids";

                        // create a new list for the certificate type if it does not exist
                        // else add it to the current certificate type list
                        certificateListFromMap = certificatePropertyMap.get(platformCredentialIdsKey);

                        if (certificateListFromMap != null) {
                            certificateListFromMap.add(pc.getId());
                        } else {
                            certificatePropertyMap.put(platformCredentialIdsKey,
                                    Collections.singletonList(pc.getId()));
                        }
                    }
                }

                for (EndorsementCredential ec : endorsementCredentialList) {

                    deviceName = deviceRepository.findById(ec.getDeviceId()).get().getName();

                    // verify that the endorsement certificate is associated with this
                    // device
                    if (device.getName().equals(deviceName)) {
                        final String endorsementCredentialIdsKey =
                                EndorsementCredential.class.getSimpleName() + "Ids";

                        // create a new list for the certificate type if it does not exist
                        // else add it to the current certificate type list
                        certificateListFromMap = certificatePropertyMap.get(endorsementCredentialIdsKey);

                        if (certificateListFromMap != null) {
                            certificateListFromMap.add(ec.getId());
                        } else {
                            certificatePropertyMap.put(endorsementCredentialIdsKey,
                                    Collections.singletonList(ec.getId()));
                        }
                    }
                }

                for (IssuedAttestationCertificate ic : issuedCertificateList) {
                    // verify that the issued attestation certificate is associated with this
                    // device's id
                    if (device.getName().equals(ic.getDeviceName())) {
                        final String issuedCertificatesIdsKey =
                                IssuedAttestationCertificate.class.getSimpleName() + "Ids";

                        // create a new list for the certificate type if it does not exist
                        // else add it to the current certificate type list
                        certificateListFromMap = certificatePropertyMap.get(issuedCertificatesIdsKey);

                        if (certificateListFromMap != null) {
                            certificateListFromMap.add(ic.getId());
                        } else {
                            certificatePropertyMap.put(issuedCertificatesIdsKey,
                                    Collections.singletonList(ic.getId()));
                        }
                    }
                }

                // add the device-certificate map to the record
                deviceCertMap.putAll(certificatePropertyMap);
                records.add(new HashMap<>(deviceCertMap));
                deviceCertMap.clear();
            }
        }
        // set pagination values
        records.setRecordsTotal(deviceList.getRecordsTotal());
        records.setRecordsFiltered(deviceList.getRecordsFiltered());
        return records;
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

    /**
     * Returns the list of devices IDs.
     *
     * @param deviceList list containing the devices
     * @return a list of the devices IDs
     */
    private List<UUID> getDevicesId(final FilteredRecordsList<Device> deviceList) {
        return deviceList.stream().map(Device::getId).toList();
    }
}
