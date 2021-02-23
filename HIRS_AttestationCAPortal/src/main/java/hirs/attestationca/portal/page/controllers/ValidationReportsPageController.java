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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Controller for the Validation Reports page.
 */
@Controller
@RequestMapping("/validation-reports")
public class ValidationReportsPageController extends PageController<NoPageParams> {

    private final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager;
    private final CertificateManager certificateManager;
    private final DeviceManager deviceManager;

    private static String columnHeaders = "Verified Manufacturer,"
            + "Model,SN,Verification Date,Device Status,"
            + "Component name,Component manufacturer,Component model,"
            + "Component SN,Issuer,Component status";
    private static final String DEFAULT_COMPANY = "AllDevices";
    private static final String UNDEFINED = "undefined";
    private static final Logger LOGGER = getLogger(ValidationReportsPageController.class);

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
     * This method handles downloading a validation report.
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
        Pattern pattern = Pattern.compile("^\\w*$");
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        LocalDate startDate = null;
        LocalDate endDate = null;
        ArrayList<LocalDate> createTimes = new ArrayList<LocalDate>();
        String[] deviceNames = new String[]{};

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
                    if (!parameterValue.equals(UNDEFINED)) {
                        String[] timestamps = parameterValue.split(",");
                        for (String timestamp : timestamps) {
                            createTimes.add(LocalDateTime.parse(timestamp,
                                    dateTimeFormat).toLocalDate());
                        }
                    }
                    break;
                case "deviceNames":
                    if (!parameterValue.equals(UNDEFINED)) {
                        deviceNames = parameterValue.split(",");
                    }
                    break;
                default:
            }
        }

        response.setHeader("Content-Type", "text/csv");
        response.setHeader("Content-Disposition",
                "attachment;filename=validation_report.csv");
        BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
        StringBuilder reportData = new StringBuilder();
        bufferedWriter.append("Company: " + company + "\n");
        bufferedWriter.append("Contract number: " + contractNumber + "\n");
        for (int i = 0; i < deviceNames.length; i++) {
            if ((createTimes.get(i).isAfter(startDate) || createTimes.get(i).isEqual(startDate))
                    && (createTimes.get(i).isBefore(endDate)
                        || createTimes.get(i).isEqual(endDate))) {
                UUID deviceId = deviceManager.getDevice(deviceNames[i]).getId();
                LOGGER.info(deviceId);
                PlatformCredential pc = PlatformCredential.select(certificateManager)
                        .byDeviceId(deviceId).getCertificate();
                LOGGER.info("Found platform credential: " + pc.toString());
                reportData.append(pc.getManufacturer() + ","
                        + pc.getModel() + ","
                        + pc.getPlatformSerial() + ","
                        + LocalDateTime.now().toString() + ","
                        + pc.getDevice().getSupplyChainStatus() + ",");
                ArrayList<ArrayList<String>> parsedComponents = parseComponents(pc);
                for (ArrayList<String> component : parsedComponents) {
                    for (String data : component) {
                        reportData.append(data + ",");
                    }
                    reportData.deleteCharAt(reportData.length() - 1);
                    reportData.append("\n,,,,,");
                }
                reportData.delete(reportData.lastIndexOf("\n"), reportData.length());
            }
        }
        bufferedWriter.append(columnHeaders + "\n");
        bufferedWriter.append(reportData.toString() + "\n");
        LOGGER.info(columnHeaders);
        LOGGER.info(reportData.toString());
        bufferedWriter.flush();
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
        String componentFailureString = "";
        if (pc.getComponentIdentifiers() != null
                && pc.getComponentIdentifiers().size() > 0) {
            componentFailureString += pc.getComponentFailures();
            // get all the certificates associated with the platform serial
            List<PlatformCredential> chainCertificates = PlatformCredential
                    .select(certificateManager)
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
                componentFailureString += cert.getComponentFailures();
                if (!cert.isBase()) {
                    for (ComponentIdentifier ci : cert.getComponentIdentifiers()) {
                        ArrayList<Object> issuerAndComponent = new ArrayList<Object>();
                        issuerAndComponent.add(cert.getIssuer());
                        issuerAndComponent.add(ci);
                        chainComponents.add(issuerAndComponent);
                    }
                }
            }
            LOGGER.info("Component failures: " + componentFailureString);
            for (ArrayList<Object> issuerAndComponent : chainComponents) {
                ArrayList<String> componentData = new ArrayList<String>();
                String issuer = (String) issuerAndComponent.get(0);
                issuer = issuer.replaceAll(",", " ");
                ComponentIdentifier ci = (ComponentIdentifier) issuerAndComponent.get(1);
                if (ci instanceof ComponentIdentifierV2) {
                    componentData.add(((ComponentIdentifierV2) ci)
                            .getComponentClass().toString());
                } else {
                    componentData.add("Platform Component");
                }
                componentData.add(ci.getComponentManufacturer().getString());
                componentData.add(ci.getComponentModel().getString());
                componentData.add(ci.getComponentSerial().getString());
                componentData.add(issuer);
                //Failing components are identified by hashcode
                if (componentFailureString.contains(String.valueOf(ci.hashCode()))) {
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
