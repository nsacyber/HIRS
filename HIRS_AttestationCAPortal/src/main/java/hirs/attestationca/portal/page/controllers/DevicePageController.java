package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.service.DevicePageService;
import hirs.attestationca.persist.service.util.DataTablesColumn;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.Order;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
    private final DevicePageService devicePageService;

    /**
     * Constructor for the Device Page Controller.
     *
     * @param devicePageService device page service
     */
    @Autowired
    public DevicePageController(final DevicePageService devicePageService) {
        super(Page.DEVICES);
        this.devicePageService = devicePageService;
    }

    /**
     * Returns the path for the view and the data model for the device page.
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
     * Processes the request to retrieve a list of devices and device related information for display on the
     * devices page.
     *
     * @param dataTableInput data table input.
     * @return data table of devices
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<HashMap<String, Object>> getDevicesTableData(
            final DataTableInput dataTableInput) {
        log.info("Received request to display list of devices");
        log.debug("Request received a datatable input object for the device page: {}",
                dataTableInput);

        // grab the column to which ordering has been applied
        final Order orderColumn = dataTableInput.getOrderColumn();

        // grab the value that was entered in the global search textbox
        final String globalSearchTerm = dataTableInput.getSearch().getValue();

        // find all columns that have a value that's been entered in column search dropdown
        final Set<DataTablesColumn> columnsWithSearchCriteria =
                ControllerPagesUtils.findColumnsWithSearchCriteriaForColumnSpecificSearch(
                        dataTableInput.getColumns());

        // find all columns that are considered searchable
        final Set<String> searchableColumnNames =
                ControllerPagesUtils.findSearchableColumnNamesForGlobalSearch(Device.class,
                        dataTableInput.getColumns());

        // since the column names are typically pre-fixed with the word `device.`, we need to...
        if (orderColumn != null && orderColumn.getName().startsWith("device.")) {
            // Take the part after `device.`
            orderColumn.setName(orderColumn.getName().split("device.")[1]);
        }

        Pageable pageable = ControllerPagesUtils.createPageableObject(
                dataTableInput.getStart(),
                dataTableInput.getLength(),
                orderColumn);

        FilteredRecordsList<Device> deviceList = getFilteredDeviceList(
                globalSearchTerm,
                columnsWithSearchCriteria,
                searchableColumnNames,
                pageable);

        FilteredRecordsList<HashMap<String, Object>> devicesAndAssociatedCertificates
                = this.devicePageService.retrieveDevicesAndAssociatedCertificates(deviceList);

        log.info("Returning the size of the filtered list of devices: {}",
                devicesAndAssociatedCertificates.size());
        return new DataTableResponse<>(devicesAndAssociatedCertificates, dataTableInput);
    }


    /**
     * Helper method that retrieves a filtered and paginated list of devices based on the provided search criteria.
     * The method allows filtering based on a global search term and column-specific search criteria,
     * and returns the result in a paginated format.
     *
     * <p>
     * The method handles four cases:
     * <ol>
     *     <li>If no global search term and no column-specific search criteria are provided,
     *         all devices are returned.</li>
     *     <li>If both a global search term and column-specific search criteria are provided,
     *         it performs filtering on both.</li>
     *     <li>If only column-specific search criteria are provided, it filters based on the column-specific
     *         criteria.</li>
     *     <li>If only a global search term is provided, it filters based on the global search term.</li>
     * </ol>
     * </p>
     *
     * @param globalSearchTerm          A global search term that will be used to filter the devices by the
     *                                  searchable fields.
     * @param columnsWithSearchCriteria A set of columns with specific search criteria entered by the user.
     * @param searchableColumnNames     A set of searchable column names that are  for the global search term.
     * @param pageable                  pageable
     * @return A {@link FilteredRecordsList<Device>} containing the filtered and paginated list of devices,
     * along with the total number of records and the number of records matching the filter criteria.
     */
    private FilteredRecordsList<Device> getFilteredDeviceList(
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Set<String> searchableColumnNames,
            final Pageable pageable) {

        org.springframework.data.domain.Page<Device> pagedResult;

        // if no value has been entered in the global search textbox and in the column search dropdown
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult =
                    this.devicePageService.findAllDevices(pageable);
        } else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered in both the global search textbox and in the column search dropdown
            pagedResult =
                    this.devicePageService.findDevicesByGlobalAndColumnSpecificSearchTerm(
                            searchableColumnNames,
                            globalSearchTerm,
                            columnsWithSearchCriteria,
                            pageable);
        } else if (!columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered ONLY in the column search dropdown
            pagedResult =
                    this.devicePageService.findDevicesByColumnSpecificSearchTerm(columnsWithSearchCriteria,
                            pageable);
        } else {
            // if a value has been entered ONLY in the global search textbox
            pagedResult = this.devicePageService.findDevicesByGlobalSearchTerm(
                    searchableColumnNames,
                    globalSearchTerm,
                    pageable);
        }

        FilteredRecordsList<Device> deviceList = new FilteredRecordsList<>();
        if (pagedResult.hasContent()) {
            deviceList.addAll(pagedResult.getContent());
        }
        deviceList.setRecordsFiltered(pagedResult.getTotalElements());
        deviceList.setRecordsTotal(this.devicePageService.findDeviceRepositoryCount());

        return deviceList;
    }
}
