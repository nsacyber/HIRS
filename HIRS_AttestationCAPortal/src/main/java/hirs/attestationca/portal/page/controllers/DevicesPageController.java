package hirs.attestationca.portal.page.controllers;

import hirs.FilteredRecordsList;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.servicemanager.DBManager;
import hirs.attestationca.entity.Device;
import hirs.attestationca.entity.certificate.Certificate;
import hirs.attestationca.entity.certificate.DeviceAssociatedCertificate;
import hirs.attestationca.service.DeviceService;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static hirs.attestationca.portal.page.Page.DEVICES;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Controller for the Device page.
 */
@RestController
@RequestMapping(path = "/devices")
public class DevicesPageController extends PageController<NoPageParams> {

    @Autowired
    private final DeviceService deviceService;
    // this may be what I need to do for all of them
    @Autowired
    private final DBManager<Certificate> certificateDBManager;
    private static final Logger LOGGER = getLogger(DevicesPageController.class);

    /**
     * Constructor providing the Page's display and routing specification.
     * @param deviceService the device manager
     * @param certificateDBManager the certificate DB manager
     */
    @Autowired
    public DevicesPageController(
            final DeviceService deviceService,
            final DBManager<Certificate> certificateDBManager) {
        super(DEVICES);
        this.deviceService = deviceService;
        this.certificateDBManager = certificateDBManager;
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView();
    }

    /**
     * Returns the list of devices using the datatable input for paging, ordering, and
     * filtering.
     * @param input the data tables input
     * @return the data tables response, including the result set and paging information
     */
    @ResponseBody
    @GetMapping
    @RequestMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<HashMap<String, Object>> getTableData(
            final DataTableInput input) {
        LOGGER.debug("Handling request for device list");
        String orderColumnName = input.getOrderColumnName();

        // get all the devices
        FilteredRecordsList<Device> deviceList =
                OrderedListQueryDataTableAdapter.getOrderedList(Device.class,
                        deviceService, input, orderColumnName);

        FilteredRecordsList<HashMap<String, Object>> record
                = retrieveDevicesAndAssociatedCertificates(deviceList);

        return new DataTableResponse<>(record, input);
    }

    /**
     * Returns the list of devices combined with the certificates.
     * @param deviceList list containing the devices
     * @return a record list after the device and certificate was mapped together.
     */
    private FilteredRecordsList<HashMap<String, Object>> retrieveDevicesAndAssociatedCertificates(
            final FilteredRecordsList<Device> deviceList) {
        FilteredRecordsList<HashMap<String, Object>> records = new FilteredRecordsList<>();
        // hashmap containing the device-certificate relationship
        HashMap<String, Object> deviceCertMap = new HashMap<>();
        Device device;
        Certificate certificate;

        // parse if there is a Device
        if (!deviceList.isEmpty()) {
            // get a list of Certificates that contains the device IDs from the list
            List<Certificate> certificateList = certificateDBManager.getList(
                    Certificate.class,
                    Restrictions.in("device.id", getDevicesIds(deviceList).toArray()));

            // loop all the devices
            for (int i = 0; i < deviceList.size(); i++) {
                // hashmap containing the list of certificates based on the certificate type
                HashMap<String, List<Object>> certificatePropertyMap = new HashMap<>();

                device = deviceList.get(i);
                deviceCertMap.put("device", device);

                // loop all the certificates and combined the ones that match the ID
                for (int j = 0; j < certificateList.size(); j++) {
                    certificate = certificateList.get(j);

                    // set the certificate if it's the same ID
                    if (device.getId().equals(
                                ((DeviceAssociatedCertificate) certificate).getDevice().getId())) {
                        String certificateId = certificate.getClass().getSimpleName();
                        // create a new list for the certificate type if does not exist
                        // else add it to the current certificate type list
                        List<Object> certificateListFromMap
                                = certificatePropertyMap.get(certificateId);
                        if (certificateListFromMap != null) {
                            certificateListFromMap.add(certificate);
                        } else {
                            certificatePropertyMap.put(certificateId,
                                    new ArrayList<>(Collections.singletonList(certificate)));
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
     * @param deviceList list containing the devices
     * @return a list of the devices IDs
     */
    private List<UUID> getDevicesIds(final FilteredRecordsList<Device> deviceList) {
        List<UUID> deviceIds =  new ArrayList<UUID>();

        // loop all the devices
        for (int i = 0; i < deviceList.size(); i++) {
            deviceIds.add(deviceList.get(i).getId());
        }

        return deviceIds;
    }
}
