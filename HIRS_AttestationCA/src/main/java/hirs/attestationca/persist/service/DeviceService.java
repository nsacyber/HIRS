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
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Service layer class that handles the storage and retrieval of devices.
 */
@Service
@Log4j2
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final PlatformCertificateRepository platformCertificateRepository;
    private final EndorsementCredentialRepository endorsementCredentialRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;
    private final EntityManager entityManager;

    /**
     * Constructor for Device Service.
     *
     * @param deviceRepository                device repository
     * @param platformCertificateRepository   platform certificate repository
     * @param endorsementCredentialRepository endorsement credential repository
     * @param issuedCertificateRepository     issued certificate repository
     * @param entityManager                   entity manager
     */
    @Autowired
    public DeviceService(final DeviceRepository deviceRepository,
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
     * @param searchableColumns list of the searchable column name
     * @param searchTerm        text that was input in the search textbox
     * @param pageable          pageable
     * @return page full of devices
     */
    public Page<Device> findAllDevicesBySearchableColumns(
            final List<String> searchableColumns,
            final String searchTerm,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Device> query =
                criteriaBuilder.createQuery(Device.class);
        Root<Device> deviceRoot =
                query.from(Device.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchTerm)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {
                Predicate predicate =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(deviceRoot.get(columnName)),
                                "%" + searchTerm.toLowerCase() + "%");
                predicates.add(predicate);
            }
        }

        query.where(criteriaBuilder.or(predicates.toArray(new Predicate[0])));

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
     * @return page of devices
     */
    public Page<Device> findAllDevices(final Pageable pageable) {
        return deviceRepository.findAll(pageable);
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
     * Returns the list of devices combined with the certificates.
     *
     * @param deviceList list containing the devices
     * @return a record list after the device and certificate was mapped together.
     */
    public FilteredRecordsList<HashMap<String, Object>> retrieveDevicesAndAssociatedCertificates(
            final FilteredRecordsList<Device> deviceList) {
        FilteredRecordsList<HashMap<String, Object>> records = new FilteredRecordsList<>();
        // hashmap containing the device-certificate relationship
        HashMap<String, Object> deviceCertMap = new HashMap<>();
        List<UUID> deviceIdList = getDevicesId(deviceList);
        List<PlatformCredential> platformCredentialList = new ArrayList<>();
        List<EndorsementCredential> endorsementCredentialList = new ArrayList<>();
        List<IssuedAttestationCertificate> issuedCertificateList = new ArrayList<>();
        List<Object> certificateListFromMap = new LinkedList<>();

        // parse if there is a Device
        if (!deviceList.isEmpty()) {
            // get a list of Certificates that contains the device IDs from the list
            for (UUID id : deviceIdList) {
                platformCredentialList.addAll(platformCertificateRepository.findByDeviceId(id));
                endorsementCredentialList.addAll(endorsementCredentialRepository.findByDeviceId(id));
                issuedCertificateList.addAll(issuedCertificateRepository.findByDeviceId(id));
            }

            HashMap<String, List<Object>> certificatePropertyMap;
            // loop all the devices
            for (Device device : deviceList) {
                // hashmap containing the list of certificates based on the certificate type
                certificatePropertyMap = new HashMap<>();

                deviceCertMap.put("device", device);
                String deviceName;

                // loop all the certificates and combined the ones that match the ID
                for (PlatformCredential pc : platformCredentialList) {
                    deviceName = deviceRepository.findById(pc.getDeviceId()).get().getName();

                    // set the certificate if it's the same ID
                    if (device.getName().equals(deviceName)) {
                        String certificateId = PlatformCredential.class.getSimpleName();
                        // create a new list for the certificate type if does not exist
                        // else add it to the current certificate type list
                        certificateListFromMap
                                = certificatePropertyMap.get(certificateId);
                        if (certificateListFromMap != null) {
                            certificateListFromMap.add(pc);
                        } else {
                            certificatePropertyMap.put(certificateId,
                                    new ArrayList<>(Collections.singletonList(pc)));
                        }
                    }
                }

                for (EndorsementCredential ec : endorsementCredentialList) {
                    deviceName = deviceRepository.findById(ec.getDeviceId()).get().getName();

                    // set the certificate if it's the same ID
                    if (device.getName().equals(deviceName)) {
                        String certificateId = EndorsementCredential.class.getSimpleName();
                        // create a new list for the certificate type if does not exist
                        // else add it to the current certificate type list
                        certificateListFromMap
                                = certificatePropertyMap.get(certificateId);
                        if (certificateListFromMap != null) {
                            certificateListFromMap.add(ec);
                        } else {
                            certificatePropertyMap.put(certificateId,
                                    new ArrayList<>(Collections.singletonList(ec)));
                        }
                    }
                }

                for (IssuedAttestationCertificate ic : issuedCertificateList) {
                    deviceName = ic.getDeviceName();
                    // set the certificate if it's the same ID
                    if (device.getName().equals(deviceName)) {
                        String certificateId = IssuedAttestationCertificate.class.getSimpleName();
                        // create a new list for the certificate type if does not exist
                        // else add it to the current certificate type list
                        certificateListFromMap
                                = certificatePropertyMap.get(certificateId);
                        if (certificateListFromMap != null) {
                            certificateListFromMap.add(ic);
                        } else {
                            certificatePropertyMap.put(certificateId,
                                    new ArrayList<>(Collections.singletonList(ic)));
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
     * Returns the list of devices IDs.
     *
     * @param deviceList list containing the devices
     * @return a list of the devices IDs
     */
    private List<UUID> getDevicesId(final FilteredRecordsList<Device> deviceList) {
        return deviceList.stream().map(Device::getId).toList();
    }
}
