package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.service.DeviceService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Set;

/**
 * Controller for the Devices page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/devices")
public class DevicePageController extends PageController<NoPageParams> {

    private final DeviceService deviceService;

    /**
     * Device Page Controller constructor.
     *
     * @param deviceService device service
     */
    @Autowired
    public DevicePageController(
            final DeviceService deviceService) {
        super(Page.DEVICES);
        this.deviceService = deviceService;
    }

    /**
     * Returns the path for the view and the data model for the validation reports page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model devices page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.DEVICES);
    }

    /**
     * Processes the request to retrieve a list of devices and device related
     * information for display on the devices page.
     *
     * @param input data table input.
     * @return data table of devices
     */
    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<HashMap<String, Object>> getDevicesTableData(
            final DataTableInput input) {
        log.info("Received request to display list of devices");
        log.debug("Request received a datatable input object for the device page: {}",
                input);

        String orderColumnName = input.getOrderColumnName();
        log.debug("Ordering on column: {}", orderColumnName);

        final String searchTerm = input.getSearch().getValue();
        final Set<String> searchableColumns =
                ControllerPagesUtils.findSearchableColumnsNamesWithClass(Device.class, input.getColumns());

        // get all the devices
        FilteredRecordsList<Device> deviceList = new FilteredRecordsList<>();

        final int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));
        org.springframework.data.domain.Page<Device> pagedResult;

        if (StringUtils.isBlank(searchTerm)) {
            pagedResult = this.deviceService.findAllDevices(pageable);
        } else {
            pagedResult = this.deviceService.findAllDevicesBySearchableColumns(searchableColumns, searchTerm,
                    pageable);
        }

        if (pagedResult.hasContent()) {
            deviceList.addAll(pagedResult.getContent());
        }

        deviceList.setRecordsFiltered(pagedResult.getTotalElements());
        deviceList.setRecordsTotal(this.deviceService.findDeviceRepositoryCount());

        FilteredRecordsList<HashMap<String, Object>> devicesAndAssociatedCertificates
                = this.deviceService.retrieveDevicesAndAssociatedCertificates(deviceList);

        log.info("Returning the size of the list of devices: {}", devicesAndAssociatedCertificates.size());
        return new DataTableResponse<>(devicesAndAssociatedCertificates, input);
    }
}
