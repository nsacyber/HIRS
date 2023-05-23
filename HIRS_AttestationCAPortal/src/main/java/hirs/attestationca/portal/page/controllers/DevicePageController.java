package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/devices")
public class DevicePageController extends PageController<NoPageParams> {
    /**
     * https://odrotbohm.de/2013/11/why-field-injection-is-evil/
     *
     * Autowiring property vs constructor
     */

    private final DeviceRepository deviceRepository;

    @Autowired
    public DevicePageController(final DeviceRepository deviceRepository) {
        super(Page.DEVICES);
        this.deviceRepository = deviceRepository;
    }

    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView();
    }

//    @RequestMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE,
//            method = RequestMethod.GET)
//    public DataTableResponse<HashMap<String, Object>> getTableData(
//            final DataTableInput input) {
//        String orderColumnName = input.getOrderColumnName();
//        FilteredRecordsList<HashMap<String, Object>> record
//                = retrieveDevicesAndAssociatedCertificates(deviceList);
//        modelMap.put("devices", deviceServiceImpl.retrieveDevices());
//        return new DataTableResponse<>(record, input);
//    }


    @GetMapping(path="/all")
    public @ResponseBody Iterable<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

}