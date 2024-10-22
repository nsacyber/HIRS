package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.EndorsementCredentialRepository;
import hirs.attestationca.persist.entity.manager.IssuedCertificateRepository;
import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Controller for the Device page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/devices")
public class DevicePageController extends PageController<NoPageParams> {

    private final DeviceRepository deviceRepository;
    private final CertificateRepository certificateRepository;
    private final PlatformCertificateRepository platformCertificateRepository;
    private final EndorsementCredentialRepository endorsementCredentialRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;

    @Autowired
    public DevicePageController(final DeviceRepository deviceRepository,
                                final CertificateRepository certificateRepository,
                                final PlatformCertificateRepository platformCertificateRepository,
                                final EndorsementCredentialRepository endorsementCredentialRepository,
                                final IssuedCertificateRepository issuedCertificateRepository) {
        super(Page.DEVICES);
        this.deviceRepository = deviceRepository;
        this.certificateRepository = certificateRepository;
        this.platformCertificateRepository = platformCertificateRepository;
        this.endorsementCredentialRepository = endorsementCredentialRepository;
        this.issuedCertificateRepository = issuedCertificateRepository;
    }

    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView();
    }

    @ResponseBody
    @RequestMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<HashMap<String, Object>> getTableData(
            final DataTableInput input) {
        log.debug("Handling request for device list");
        String orderColumnName = input.getOrderColumnName();
        log.info("Ordering on column: " + orderColumnName);

        // get all the devices
        FilteredRecordsList<Device> deviceList = new FilteredRecordsList<>();

        int currentPage = input.getStart() / input.getLength();
        Pageable paging = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));
        org.springframework.data.domain.Page<Device> pagedResult = deviceRepository.findAll(paging);

        if (pagedResult.hasContent()) {
            deviceList.addAll(pagedResult.getContent());
            deviceList.setRecordsTotal(pagedResult.getContent().size());
        } else {
            deviceList.setRecordsTotal(input.getLength());
        }
        deviceList.setRecordsFiltered(deviceRepository.count());

        FilteredRecordsList<HashMap<String, Object>> records
                = retrieveDevicesAndAssociatedCertificates(deviceList);

        return new DataTableResponse<>(records, input);
    }

    /**
     * Returns the list of devices combined with the certificates.
     *
     * @param deviceList list containing the devices
     * @return a record list after the device and certificate was mapped together.
     */
    private FilteredRecordsList<HashMap<String, Object>> retrieveDevicesAndAssociatedCertificates(
            final FilteredRecordsList<Device> deviceList) {
        FilteredRecordsList<HashMap<String, Object>> records = new FilteredRecordsList<>();
        // hashmap containing the device-certificate relationship
        HashMap<String, Object> deviceCertMap = new HashMap<>();
        PlatformCredential certificate;
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
        List<UUID> deviceIds = new ArrayList<>();

        // loop all the devices
        for (int i = 0; i < deviceList.size(); i++) {
            deviceIds.add(deviceList.get(i).getId());
        }

        return deviceIds;
    }

}