package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.entity.manager.DeviceRepository;
import hirs.attestationca.portal.entity.userdefined.Device;
import hirs.attestationca.portal.enums.AppraisalStatus;
import hirs.attestationca.portal.enums.HealthStatus;
import hirs.attestationca.portal.enums.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.service.DeviceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/devices")
public class DevicePageController extends PageController<NoPageParams> {
    /**
     * https://odrotbohm.de/2013/11/why-field-injection-is-evil/
     *
     * Autowiring property vs constructor
     */

    private final DeviceServiceImpl deviceServiceImpl;
    private final DeviceRepository deviceRepository;

    @Autowired
    public DevicePageController(DeviceServiceImpl deviceServiceImpl,
                                DeviceRepository deviceRepository) {
        super(Page.DEVICES);
        this.deviceServiceImpl = deviceServiceImpl;
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

    @GetMapping(value = "populateDevices")
    public @ResponseBody String addDevice () {
        deviceRepository.save(new Device("Dell-01", HealthStatus.TRUSTED,
                AppraisalStatus.Status.UNKNOWN,
                Timestamp.valueOf(LocalDateTime.now()), false, "", "This is a summary"));

        deviceRepository.save(new Device("Dell-02", HealthStatus.TRUSTED,
                AppraisalStatus.Status.UNKNOWN,
                Timestamp.valueOf(LocalDateTime.now()), false, "", "This is a summary"));

        deviceRepository.save(new Device("HP-01", HealthStatus.UNKNOWN,
                AppraisalStatus.Status.UNKNOWN,
                Timestamp.valueOf(LocalDateTime.now()), false, "", "This is a summary"));

        deviceRepository.save(new Device("HP-02", HealthStatus.UNTRUSTED,
                AppraisalStatus.Status.UNKNOWN,
                Timestamp.valueOf(LocalDateTime.now()), false, "", "This is a summary"));

        return "all";
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

}