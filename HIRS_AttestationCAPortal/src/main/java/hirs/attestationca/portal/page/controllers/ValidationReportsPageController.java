package hirs.attestationca.portal.page.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import hirs.attestationca.persist.entity.manager.SupplyChainValidationSummaryRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller for the Validation Reports page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/validation-reports")
public class ValidationReportsPageController extends PageController<NoPageParams> {

    private final SupplyChainValidationSummaryRepository supplyChainValidatorSummaryRepository;
    private final CertificateRepository certificateRepository;
    private final DeviceRepository deviceRepository;
    private final PlatformCertificateRepository platformCertificateRepository;
    @Autowired(required = false)
    private EntityManager entityManager;

    private static String systemColumnHeaders = "Verified Manufacturer,"
            + "Model,SN,Verification Date,Device Status";
    private static String componentColumnHeaders = "Component name,Component manufacturer,"
            + "Component model,Component SN,Issuer,Component status";
    private static final String DEFAULT_COMPANY = "AllDevices";
    private static final String UNDEFINED = "undefined";
    private static final String TRUE = "true";

    /**
     * Constructor providing the Page's display and routing specification.
     * @param supplyChainValidatorSummaryRepository the manager
     * @param certificateRepository the certificate manager
     * @param deviceRepository the device manager
     * @param platformCertificateRepository the platform certificate manager
     */
    @Autowired
    public ValidationReportsPageController(
            final SupplyChainValidationSummaryRepository supplyChainValidatorSummaryRepository,
            final CertificateRepository certificateRepository,
            final DeviceRepository deviceRepository,
            final PlatformCertificateRepository platformCertificateRepository) {
        super(Page.VALIDATION_REPORTS);
        this.supplyChainValidatorSummaryRepository = supplyChainValidatorSummaryRepository;
        this.certificateRepository = certificateRepository;
        this.deviceRepository = deviceRepository;
        this.platformCertificateRepository = platformCertificateRepository;
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
    @RequestMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<SupplyChainValidationSummary> getTableData(
            final DataTableInput input) {

        log.debug("Handling request for summary list: " + input);
        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();
        log.debug("Ordering on column: " + orderColumnName);

        FilteredRecordsList<SupplyChainValidationSummary> records = new FilteredRecordsList<>();
        int currentPage = input.getStart() / input.getLength();
        Pageable paging = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));
        org.springframework.data.domain.Page<SupplyChainValidationSummary> pagedResult = supplyChainValidatorSummaryRepository.findByArchiveFlagFalse(paging);

        if (pagedResult.hasContent()) {
            records.addAll(pagedResult.getContent());
            records.setRecordsTotal(pagedResult.getContent().size());
        } else {
            records.setRecordsTotal(input.getLength());
        }

        records.setRecordsFiltered(supplyChainValidatorSummaryRepository.count());

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

        log.info("Downloading validation report");
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
            log.info(parameter + ": " + parameterValue);
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
                        endDate = LocalDate.now(ZoneId.of("America/New_York"));
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
                        if (!columnHeaders.isEmpty()) {
                            columnHeaders = "," + columnHeaders;
                        }
                        columnHeaders = systemColumnHeaders + columnHeaders;
                    }
                    break;
                case "component":
                    if (parameterValue.equals(TRUE)) {
                        componentOnly = true;
                        if (!columnHeaders.isEmpty()) {
                            columnHeaders += ",";
                        }
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
                Device device = deviceRepository.findByName(deviceNames[i]);
                PlatformCredential pc = platformCertificateRepository.findByDeviceId(device.getId()).get(0);
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
                                    + device.getSupplyChainValidationStatus() + ",");
                        }
                        if (!systemOnly) {
                            ArrayList<ArrayList<String>> parsedComponents = parseComponents(pc);
                            for (ArrayList<String> component : parsedComponents) {
                                for (String data : component) {
                                    reportData.append(data + ",");
                                }
                                reportData.deleteCharAt(reportData.length() - 1);
                                reportData.append(System.lineSeparator());
                                if (!componentOnly) {
                                    reportData.append(",,,,,");
                                }
                            }
                            reportData = reportData.delete(
                                    reportData.lastIndexOf(System.lineSeparator()) + 1,
                                    reportData.length());
                        }
                    }
                }
            }
        }
        if (!jsonVersion) {
            if (columnHeaders.isEmpty()) {
                columnHeaders = systemColumnHeaders + "," + componentColumnHeaders;
            }
            bufferedWriter.append(columnHeaders + System.lineSeparator());
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
        String deviceName = deviceRepository.findById((pc)
                .getDeviceId()).get().getName();

        systemData.addProperty("Company", company);
        systemData.addProperty("Contract number", contractNumber);
        systemData.addProperty("Verified Manufacturer", pc.getManufacturer());
        systemData.addProperty("Model", pc.getModel());
        systemData.addProperty("SN", pc.getPlatformSerial());
        systemData.addProperty("Verification Date", LocalDateTime.now().toString());
        systemData.addProperty("Device Status", deviceRepository.findByName(deviceName)
                .getSupplyChainValidationStatus().toString());

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
            List<PlatformCredential> chainCertificates = certificateRepository.byBoardSerialNumber(pc.getPlatformSerial());
            // combine all components in each certificate
            for (ComponentIdentifier ci : pc.getComponentIdentifiers()) {
                ArrayList<Object> issuerAndComponent = new ArrayList<Object>();
                issuerAndComponent.add(pc.getHolderIssuer());
                issuerAndComponent.add(ci);
                chainComponents.add(issuerAndComponent);
            }

            for (PlatformCredential cert : chainCertificates) {
                componentFailureString.append(cert.getComponentFailures());
                if (!cert.isPlatformBase()) {
                    for (ComponentIdentifier ci : cert.getComponentIdentifiers()) {
                        ArrayList<Object> issuerAndComponent = new ArrayList<Object>();
                        issuerAndComponent.add(cert.getHolderIssuer());
                        issuerAndComponent.add(ci);
                        chainComponents.add(issuerAndComponent);
                    }
                }
            }
            log.info("Component failures: " + componentFailureString.toString());
            for (ArrayList<Object> issuerAndComponent : chainComponents) {
                ArrayList<String> componentData = new ArrayList<String>();
                String issuer = (String) issuerAndComponent.get(0);
                issuer = issuer.replaceAll(",", " ");
                ComponentIdentifier ci = (ComponentIdentifier) issuerAndComponent.get(1);
                if (ci instanceof ComponentIdentifierV2) {
                    String componentClass =
                            ((ComponentIdentifierV2) ci).getComponentClass().toString();
                    String[] splitStrings = componentClass.split("\r\n|\n|\r");
                    StringBuilder sb = new StringBuilder();
                    for (String s : splitStrings) {
                        sb.append(s);
                        sb.append(" ");
                    }
                    sb = sb.deleteCharAt(sb.length() - 1);
                    componentData.add(sb.toString());
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
                log.info(String.join(",", componentData));
            }
        }

        return parsedComponents;
    }
}
