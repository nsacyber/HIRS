package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.data.persist.certificate.attributes.ComponentIdentifier;
import hirs.data.persist.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.persist.CertificateManager;
import hirs.persist.DeviceManager;
import org.apache.logging.log4j.Logger;
import static org.apache.logging.log4j.LogManager.getLogger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import static hirs.attestationca.portal.page.Page.VALIDATION_REPORTS;
import hirs.FilteredRecordsList;
import hirs.data.persist.SupplyChainValidationSummary;
import hirs.persist.CriteriaModifier;
import hirs.persist.CrudManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Controller for the Validation Reports page.
 */
@Controller
@RequestMapping("/validation-reports")
public class ValidationReportsPageController extends PageController<NoPageParams> {

    private final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager;
    private final CertificateManager certificateManager;
    private final DeviceManager deviceManager;

    private static final Logger LOGGER = getLogger(ValidationReportsPageController.class);
    private static final String DEFAULT_COMPANY = "AllDevices";

    /**
     * Constructor providing the Page's display and routing specification.
     * @param supplyChainValidatorSummaryManager the manager
     * @param certificateManager the certificate manager
     * @param deviceManager the device manager
     */
    @Autowired
    public ValidationReportsPageController(
            final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager,
            final CertificateManager certificateManager,
            final DeviceManager deviceManager) {
        super(VALIDATION_REPORTS);
        this.supplyChainValidatorSummaryManager = supplyChainValidatorSummaryManager;
        this.certificateManager = certificateManager;
        this.deviceManager = deviceManager;
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
     * Gets the list of validation summaries per the data table input query.
     * @param input the data table query.
     * @return the data table response containing the supply chain summary records
     */
    @ResponseBody
    @RequestMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<SupplyChainValidationSummary> getTableData(
            final DataTableInput input) {

        LOGGER.debug("Handling request for summary list: " + input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();

        LOGGER.debug("Ordering on column: " + orderColumnName);

        // define an alias so the composite object, device, can be used by the
        // datatables / query. This is necessary so the device.name property can
        // be used.
        CriteriaModifier criteriaModifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                criteria.add(Restrictions.isNull(Certificate.ARCHIVE_FIELD));
                criteria.createAlias("device", "device");
            }
        };

        FilteredRecordsList<SupplyChainValidationSummary> records =
                OrderedListQueryDataTableAdapter.getOrderedList(
                        SupplyChainValidationSummary.class,
                        supplyChainValidatorSummaryManager, input, orderColumnName,
                        criteriaModifier);

        return new DataTableResponse<>(records, input);
    }

    /**
     * This method handles downloading a validation report. The report will contain the
     * following data:
     * - Company devices where shipped from
     * - Contract#
     * - Report for Date range (default to current date)
     * -Verified Manufacturer is the Platform Vendor
     * - Model is the Platform Model
     * - SN is the Chassis SN
     * - Verification Data is the not before time on the Attestation Certificate
     * - Component Status column is 8 component classes names listed above
     * (Component Status data is taken from the pass/fail status of the report summary)
     * - Device Status is the overall pass/fail of the report summary
     * @param request object
     * @param response object
     * @throws IOException thrown by BufferedWriter object
     */
    @RequestMapping(value = "download", method = RequestMethod.POST)
    public void download(final HttpServletRequest request,
                         final HttpServletResponse response) throws IOException {

        LOGGER.info("Downloading validation report");
        String company = "";
        String contractNumber = "";
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/uuuu");
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        LocalDate startDate = null;
        LocalDate endDate = null;
        ArrayList<LocalDate> createTimes = new ArrayList<LocalDate>();
        String[] deviceNames = new String[]{};
        Enumeration parameters = request.getParameterNames();
        while (parameters.hasMoreElements()) {
            String parameter = (String) parameters.nextElement();
            String parameterValue = request.getParameter(parameter);
            switch (parameter) {
                case "company":
                    company = parameterValue;
                    break;
                case "contract":
                    contractNumber = parameterValue;
                    break;
                case "dateStart":
                    if (parameterValue != null && !parameterValue.isEmpty()) {
                        startDate = LocalDate.parse(parameterValue, dateFormat);
                    }
                    break;
                case "dateEnd":
                    if (parameterValue != null && !parameterValue.isEmpty()) {
                        endDate = LocalDate.parse(parameterValue, dateFormat);
                    }
                    break;
                case "createTimes":
                    String[] timestamps = parameterValue.split(",");
                    for (String timestamp : timestamps) {
                        createTimes.add(LocalDateTime.parse(timestamp,
                                dateTimeFormat).toLocalDate());
                        LOGGER.info("Create time added: "
                                + createTimes.get(createTimes.size() - 1));
                    }
                    break;
                case "deviceNames":
                    deviceNames = parameterValue.split(",");
                    break;
                default:
            }
            LOGGER.info(parameter + ": " + parameterValue);
        }
        if (company.equals("")) {
            company = DEFAULT_COMPANY;
        }
        if (contractNumber.equals("")) {
            contractNumber = "none";
        }
        if (startDate == null) {
            startDate = LocalDate.ofEpochDay(0);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        LOGGER.info("Start date: " + startDate.toString() + ", end date: " + endDate.toString());

        response.setHeader("Content-Type", "text/csv");
        response.setHeader("Content-Disposition",
                "attachment;filename=\"validation_report.csv\"");
        BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
        String columnHeaders = "Verified Manufacturer, "
                + "Model, SN, Verification Date, Component Statuses, Device Status";
        bufferedWriter.append("Contract number: " + contractNumber + "\n");
        bufferedWriter.append(columnHeaders + "\n");
        LOGGER.info(columnHeaders);
        for (int i = 0; i < deviceNames.length; i++) {
            if ((createTimes.get(i).isAfter(startDate) || createTimes.get(i).isEqual(startDate))
                    && (createTimes.get(i).isBefore(endDate)
                        || createTimes.get(i).isEqual(endDate))) {
                UUID deviceId = deviceManager.getDevice(deviceNames[i]).getId();
                LOGGER.info(deviceId);
                PlatformCredential pc = PlatformCredential.select(certificateManager)
                        .byDeviceId(deviceId).getCertificate();
                LOGGER.info("Found platform credential: " + pc.toString());
                bufferedWriter.append(pc.getManufacturer() + ","
                        + pc.getModel() + ","
                        + pc.getChassisSerialNumber() + ","
                        + pc.getBeginValidity() + ",");
                LOGGER.info("Verified manufacturer: " + pc.getManufacturer());
                LOGGER.info("Model: " + pc.getModel());
                LOGGER.info("SN: " + pc.getChassisSerialNumber());
                LOGGER.info("Verification date: " + pc.getBeginValidity());
                if (pc.getComponentIdentifiers() != null
                        && pc.getComponentIdentifiers().size() > 0) {
                    String attributeStatuses = "";
                    for (ComponentIdentifier ci : pc.getComponentIdentifiers()) {
                        if (ci instanceof ComponentIdentifierV2) {
                            attributeStatuses += ((ComponentIdentifierV2) ci)
                                    .getAttributeStatus() + ",";
                            LOGGER.info(((ComponentIdentifierV2) ci).getComponentClass()
                                    + "\nComponent status: "
                                    + ((ComponentIdentifierV2) ci).getAttributeStatus());
                        } else {
                            //("Platform Components" + "\n");
                            LOGGER.info("\nPlatform Components");
                        }
                        LOGGER.info("Component manufacturer : "
                                + ci.getComponentManufacturer().getString()
                                + "\nComponent model: " + ci.getComponentModel().getString()
                                + "\nComponent revision: " + ci.getComponentRevision().getString());
                    }
                    attributeStatuses = attributeStatuses.substring(0,
                            attributeStatuses.length() - 1);
                    bufferedWriter.append("(" + attributeStatuses + "),");
                }
                bufferedWriter.append("" + pc.getDevice().getSupplyChainStatus() + "\n");
            }
        }
        bufferedWriter.flush();
    }
}
