package hirs.attestationca.portal.page.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hirs.FilteredRecordsList;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.data.persist.SupplyChainValidationSummary;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.data.persist.certificate.attributes.ComponentIdentifier;
import hirs.data.persist.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.persist.CriteriaModifier;
import hirs.persist.CrudManager;
import hirs.persist.DeviceManager;
import hirs.persist.service.CertificateService;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static hirs.attestationca.portal.page.Page.VALIDATION_REPORTS;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Controller for the Validation Reports page.
 */
@RestController
@RequestMapping(path = "/validation-reports")
public class ValidationReportsPageController extends PageController<NoPageParams> {

    @Autowired
    private final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager;
    @Autowired
    private final CertificateService certificateService;
    @Autowired
    private final DeviceManager deviceManager;

    private static String systemColumnHeaders = "Verified Manufacturer,"
            + "Model,SN,Verification Date,Device Status";
    private static String componentColumnHeaders = "Component name,Component manufacturer,"
            + "Component model,Component SN,Issuer,Component status";
    private static final String DEFAULT_COMPANY = "AllDevices";
    private static final String UNDEFINED = "undefined";
    private static final String TRUE = "true";
    private static final Logger LOGGER = getLogger(ValidationReportsPageController.class);

    /**
     * Constructor providing the Page's display and routing specification.
     * @param supplyChainValidatorSummaryManager the manager
     * @param certificateService the certificate service
     * @param deviceManager the device manager
     */
    @Autowired
    public ValidationReportsPageController(
            final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager,
            final CertificateService certificateService,
            final DeviceManager deviceManager) {
        super(VALIDATION_REPORTS);
        this.supplyChainValidatorSummaryManager = supplyChainValidatorSummaryManager;
        this.certificateService = certificateService;
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
    @GetMapping
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
     * This method handles downloading a validation report.
     * @param request object
     * @param response object
     * @throws IOException thrown by BufferedWriter object
     */
    @SuppressWarnings({"checkstyle:magicnumber", "checkstyle:methodlength" })
    @RequestMapping(value = "download", method = RequestMethod.POST)
    public void download(final HttpServletRequest request,
                         final HttpServletResponse response) throws IOException {

        LOGGER.info("Downloading validation report");
        String company = "";
        String contractNumber = "";
        Pattern pattern = Pattern.compile("^\\w*$");
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        LocalDate startDate = null;
        LocalDate endDate = null;
        ArrayList<LocalDate> createTimes = new ArrayList<LocalDate>();
        String[] deviceNames = new String[]{};
        String columnHeaders = "";
        boolean systemOnly = false;
        boolean componentOnly = false;
        String filterManufacturer = "";
        String filterSerial = "";
        boolean jsonVersion = false;

        Enumeration parameters = request.getParameterNames();
        while (parameters.hasMoreElements()) {
            String parameter = (String) parameters.nextElement();
            String parameterValue = request.getParameter(parameter);
            LOGGER.info(parameter + ": " + parameterValue);
            switch (parameter) {
                case "company":
                    Matcher companyMatcher = pattern.matcher(parameterValue);
                    if (companyMatcher.matches()) {
                        company = parameterValue;
                    } else {
                        company = DEFAULT_COMPANY;
                    }
                    break;
                case "contract":
                    Matcher contractMatcher = pattern.matcher(parameterValue);
                    if (contractMatcher.matches()) {
                        contractNumber = parameterValue;
                    } else {
                        contractNumber = "none";
                    }
                    break;
                case "dateStart":
                    if (parameterValue != null && !parameterValue.isEmpty()) {
                        startDate = LocalDate.parse(parameterValue, dateFormat);
                    } else {
                        startDate = LocalDate.ofEpochDay(0);
                    }
                    break;
                case "dateEnd":
                    if (parameterValue != null && !parameterValue.isEmpty()) {
                        endDate = LocalDate.parse(parameterValue, dateFormat);
                    } else {
                        endDate = LocalDate.now();
                    }
                    break;
                case "createTimes":
                    if (!parameterValue.equals(UNDEFINED)
                        && !parameterValue.isEmpty()) {
                        String[] timestamps = parameterValue.split(",");
                        for (String timestamp : timestamps) {
                            createTimes.add(LocalDateTime.parse(timestamp,
                                    dateTimeFormat).toLocalDate());
                        }
                    }
                    break;
                case "deviceNames":
                    if (!parameterValue.equals(UNDEFINED)
                        && !parameterValue.isEmpty()) {
                        deviceNames = parameterValue.split(",");
                    }
                    break;
                case "system":
                    if (parameterValue.equals(TRUE)) {
                        systemOnly = true;
                        columnHeaders = systemColumnHeaders + columnHeaders;
                    }
                    break;
                case "component":
                    if (parameterValue.equals(TRUE)) {
                        componentOnly = true;
                        columnHeaders += componentColumnHeaders;
                    }
                    break;
                case "manufacturer":
                    if (parameterValue != null && !parameterValue.isEmpty()) {
                        filterManufacturer = parameterValue;
                    }
                    break;
                case "serial":
                    if (parameterValue != null && !parameterValue.isEmpty()) {
                        filterSerial = parameterValue;
                    }
                    break;
                case "json":
                    response.setHeader("Content-Type", "application/json");
                    jsonVersion = true;
                    break;
                default:
            }
        }

        if (!jsonVersion) {
            response.setHeader("Content-Type", "text/csv");
            response.setHeader("Content-Disposition",
                    "attachment;filename=validation_report.csv");
        }
        BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        StringBuilder reportData = new StringBuilder();
        JsonArray jsonReportData = new JsonArray();
        for (int i = 0; i < deviceNames.length; i++) {
            if ((createTimes.get(i).isAfter(startDate) || createTimes.get(i).isEqual(startDate))
                    && (createTimes.get(i).isBefore(endDate)
                        || createTimes.get(i).isEqual(endDate))) {
                UUID deviceId = deviceManager.getDevice(deviceNames[i]).getId();
                PlatformCredential pc = PlatformCredential.select(certificateService)
                        .byDeviceId(deviceId).getCertificate();
                if (jsonVersion) {
                    jsonReportData.add(assembleJsonContent(pc, parseComponents(pc),
                            company, contractNumber));
                } else {
                    if (i == 0) {
                        bufferedWriter.append("Company: " + company + "\n");
                        bufferedWriter.append("Contract number: " + contractNumber + "\n");
                    }
                    if (systemOnly && componentOnly) {
                        systemOnly = false;
                        componentOnly = false;
                    }
                    if ((filterManufacturer.isEmpty() || filterManufacturer.equals(
                            pc.getManufacturer()))
                            && (filterSerial.isEmpty() || filterSerial.equals(
                            pc.getPlatformSerial()))) {
                        if (!componentOnly) {
                            reportData.append(pc.getManufacturer() + ","
                                    + pc.getModel() + ","
                                    + pc.getPlatformSerial() + ","
                                    + LocalDateTime.now().toString() + ","
                                    + pc.getDevice().getSupplyChainStatus() + ",");
                        }
                        if (!systemOnly) {
                            ArrayList<ArrayList<String>> parsedComponents = parseComponents(pc);
                            for (ArrayList<String> component : parsedComponents) {
                                for (String data : component) {
                                    reportData.append(data + ",");
                                }
                                reportData.deleteCharAt(reportData.length() - 1);
                                reportData.append("\n");
                                if (!componentOnly) {
                                    reportData.append(",,,,,");
                                }
                            }
                        }
                    }
                    reportData.append("\n");
                }
            }
        }
        if (!jsonVersion) {
            if (columnHeaders.isEmpty()) {
                columnHeaders = systemColumnHeaders + componentColumnHeaders;
            }
            bufferedWriter.append(columnHeaders + "\n");
            bufferedWriter.append(reportData.toString());
        } else {
            bufferedWriter.append(jsonReportData.toString());
        }
        bufferedWriter.flush();
    }

    /**
     * This method builds a JSON object from the system and component data in a
     * validation report.
     * @param pc the platform credential used to validate.
     * @param parsedComponents component data parsed from the platform credential.
     * @param company company name.
     * @param contractNumber contract number.
     * @return the JSON object in String format.
     */
    @SuppressWarnings({"checkstyle:magicnumber" })
    private JsonObject assembleJsonContent(final PlatformCredential pc,
                                       final ArrayList<ArrayList<String>> parsedComponents,
                                       final String company,
                                       final String contractNumber) {
        JsonObject systemData = new JsonObject();

        systemData.addProperty("Company", company);
        systemData.addProperty("Contract number", contractNumber);
        systemData.addProperty("Verified Manufacturer", pc.getManufacturer());
        systemData.addProperty("Model", pc.getModel());
        systemData.addProperty("SN", pc.getPlatformSerial());
        systemData.addProperty("Verification Date", LocalDateTime.now().toString());
        systemData.addProperty("Device Status", pc.getDevice().getSupplyChainStatus().toString());

        JsonArray components = new JsonArray();
        for (ArrayList<String> componentData : parsedComponents) {
            JsonObject component = new JsonObject();
            component.addProperty("Component name", componentData.get(0));
            component.addProperty("Component manufacturer", componentData.get(1));
            component.addProperty("Component model", componentData.get(2));
            component.addProperty("Component SN", componentData.get(3));
            component.addProperty("Issuer", componentData.get(4));
            component.addProperty("Component status", componentData.get(5));
            components.add(component);
        }
        systemData.add("Components", components);

        return systemData;
    }

    /**
     * This method parses the following ComponentIdentifier fields into an ArrayList of ArrayLists.
     * - ComponentClass
     * - Manufacturer
     * - Model
     * - Serial number
     * - Pass/fail status (based on componentFailures string)
     * @param pc the platform credential.
     * @return the ArrayList of ArrayLists containing the parsed component data.
     */
    private ArrayList<ArrayList<String>> parseComponents(final PlatformCredential pc) {
        ArrayList<ArrayList<String>> parsedComponents = new ArrayList<ArrayList<String>>();
        ArrayList<ArrayList<Object>> chainComponents = new ArrayList<>();

        StringBuilder componentFailureString = new StringBuilder();
        if (pc.getComponentIdentifiers() != null
                && pc.getComponentIdentifiers().size() > 0) {
            componentFailureString.append(pc.getComponentFailures());
            // get all the certificates associated with the platform serial
            List<PlatformCredential> chainCertificates = PlatformCredential
                    .select(certificateService)
                    .byBoardSerialNumber(pc.getPlatformSerial())
                    .getCertificates().stream().collect(Collectors.toList());
            // combine all components in each certificate
            for (ComponentIdentifier ci : pc.getComponentIdentifiers()) {
                ArrayList<Object> issuerAndComponent = new ArrayList<Object>();
                issuerAndComponent.add(pc.getIssuer());
                issuerAndComponent.add(ci);
                chainComponents.add(issuerAndComponent);
            }

            for (PlatformCredential cert : chainCertificates) {
                componentFailureString.append(cert.getComponentFailures());
                if (!cert.isBase()) {
                    for (ComponentIdentifier ci : cert.getComponentIdentifiers()) {
                        ArrayList<Object> issuerAndComponent = new ArrayList<Object>();
                        issuerAndComponent.add(cert.getIssuer());
                        issuerAndComponent.add(ci);
                        chainComponents.add(issuerAndComponent);
                    }
                }
            }
            LOGGER.info("Component failures: " + componentFailureString.toString());
            for (ArrayList<Object> issuerAndComponent : chainComponents) {
                ArrayList<String> componentData = new ArrayList<String>();
                String issuer = (String) issuerAndComponent.get(0);
                issuer = issuer.replaceAll(",", " ");
                ComponentIdentifier ci = (ComponentIdentifier) issuerAndComponent.get(1);
                if (ci instanceof ComponentIdentifierV2) {
                    componentData.add(((ComponentIdentifierV2) ci).getComponentClass().toString());
                } else {
                    componentData.add("Platform Component");
                }
                componentData.add(ci.getComponentManufacturer().getString());
                componentData.add(ci.getComponentModel().getString());
                componentData.add(ci.getComponentSerial().getString());
                componentData.add(issuer);
                //Failing components are identified by hashcode
                if (componentFailureString.toString().contains(String.valueOf(ci.hashCode()))) {
                    componentData.add("Fail");
                } else {
                    componentData.add("Pass");
                }
                parsedComponents.add(componentData);
                LOGGER.info(String.join(",", componentData));
            }
        }

        return parsedComponents;
    }
}
