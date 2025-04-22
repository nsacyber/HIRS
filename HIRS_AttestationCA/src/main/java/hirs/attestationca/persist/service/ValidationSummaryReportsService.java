package hirs.attestationca.persist.service;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service layer class that handles the storage and retrieval of validation reports.
 */
@Service
@Log4j2
public class ValidationSummaryReportsService {

    private static final String DEFAULT_COMPANY = "AllDevices";
    private static final String UNDEFINED = "undefined";
    private static final String TRUE = "true";
    private static final String SYSTEM_COLUMN_HEADERS = "Verified Manufacturer,"
            + "Model,SN,Verification Date,Device Status";
    private static final String COMPONENT_COLUMN_HEADERS = "Component name,Component manufacturer,"
            + "Component model,Component SN,Issuer,Component status";

    private final PlatformCertificateRepository platformCertificateRepository;
    private final EntityManager entityManager;
    private final CertificateRepository certificateRepository;
    private final DeviceRepository deviceRepository;

    /**
     * @param platformCertificateRepository platform certificate repository
     * @param certificateRepository         certificate repository
     * @param deviceRepository              device repository
     * @param entityManager                 entity manager
     */
    @Autowired
    public ValidationSummaryReportsService(final PlatformCertificateRepository platformCertificateRepository,
                                           final CertificateRepository certificateRepository,
                                           final DeviceRepository deviceRepository,
                                           final EntityManager entityManager) {
        this.platformCertificateRepository = platformCertificateRepository;
        this.certificateRepository = certificateRepository;
        this.deviceRepository = deviceRepository;
        this.entityManager = entityManager;

    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * validation summaries whose field values matches the provided search term.
     *
     * @param searchableColumns list of the searchable column name
     * @param searchTerm        text that was input in the search textbox
     * @param archiveFlag       archive flag
     * @param pageable          pageable
     * @return page full of the validation summaries.
     */
    public Page<SupplyChainValidationSummary> findValidationReportsBySearchableColumnsAndArchiveFlag(
            final Set<String> searchableColumns,
            final String searchTerm,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SupplyChainValidationSummary> query =
                criteriaBuilder.createQuery(SupplyChainValidationSummary.class);
        Root<SupplyChainValidationSummary> supplyChainValidationSummaryRoot =
                query.from(SupplyChainValidationSummary.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchTerm)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {

                // if there is no period and this is a non-nested field
                if (!columnName.contains(".")) {
                    Predicate predicate =
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(supplyChainValidationSummaryRoot.get(columnName)),
                                    "%" + searchTerm.toLowerCase() + "%");
                    predicates.add(predicate);
                } else { // If there's a period, we are dealing with a nested entity (e.g., "device.id")
                    String[] nestedColumnName = columnName.split("\\.");

                    // The first part is the name of the related entity (e.g., "device")
                    String entityName = nestedColumnName[0];

                    // The second part is the field name on the related entity (e.g., "name")
                    String fieldName = nestedColumnName[1];

                    // Handle the case where the related entity is the "device" field
                    if (entityName.equals("device")) {
                        // Join the device entity
                        Join<SupplyChainValidationSummary, Device> deviceJoin =
                                supplyChainValidationSummaryRoot.join("device", JoinType.LEFT);

                        // Add predicate for the nested field (e.g. or device.name)
                        Predicate predicate = criteriaBuilder.like(
                                criteriaBuilder.lower(deviceJoin.get(fieldName)),
                                "%" + searchTerm.toLowerCase() + "%");
                        predicates.add(predicate);
                    }
                }
            }
        }

        Predicate likeConditions = criteriaBuilder.or(predicates.toArray(new Predicate[0]));

        // Add archiveFlag condition if specified
        query.where(criteriaBuilder.and(likeConditions,
                criteriaBuilder.equal(supplyChainValidationSummaryRoot.get("archiveFlag"), archiveFlag)));

        // Apply pagination
        TypedQuery<SupplyChainValidationSummary> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<SupplyChainValidationSummary> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * Downloads the validation summary reports based on the provided request parameters.
     *
     * @param request  http request
     * @param response http response
     * @throws IOException if there are any issues while trying to download the summary reports
     */
    public void downloadValidationReports(final HttpServletRequest request,
                                          final HttpServletResponse response) throws IOException {
        String company = "";
        String contractNumber = "";
        Pattern pattern = Pattern.compile("^\\w*$");
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        LocalDate startDate = null;
        LocalDate endDate = null;
        ArrayList<LocalDate> createTimes = new ArrayList<>();
        String[] deviceNames = new String[] {};
        StringBuilder columnHeaders = new StringBuilder();
        boolean systemOnly = false;
        boolean componentOnly = false;
        String filterManufacturer = "";
        String filterSerial = "";
        boolean jsonVersion = false;

        final Enumeration<String> parameters = request.getParameterNames();

        while (parameters.hasMoreElements()) {
            String parameter = parameters.nextElement();
            String parameterValue = request.getParameter(parameter);
            log.info("{}: {}", parameter, parameterValue);
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
                    //todo issue #922
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
                            columnHeaders.insert(0, ",");
                        }
                        columnHeaders.insert(0, SYSTEM_COLUMN_HEADERS);
                    }
                    break;
                case "component":
                    if (parameterValue.equals(TRUE)) {
                        componentOnly = true;
                        if (!columnHeaders.isEmpty()) {
                            columnHeaders.append(",");
                        }
                        columnHeaders.append(COMPONENT_COLUMN_HEADERS);
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
                        bufferedWriter.append("Company: ").append(company).append("\n");
                        bufferedWriter.append("Contract number: ").append(contractNumber).append("\n");
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
                            reportData.append(pc.getManufacturer())
                                    .append(",")
                                    .append(pc.getModel())
                                    .append(",")
                                    .append(pc.getPlatformSerial())
                                    .append(",")
                                    .append(LocalDateTime.now())
                                    .append(",")
                                    .append(device.getSupplyChainValidationStatus())
                                    .append(",");
                        }

                        if (!systemOnly) {
                            ArrayList<ArrayList<String>> parsedComponents = parseComponents(pc);
                            for (ArrayList<String> component : parsedComponents) {
                                for (String data : component) {
                                    reportData.append(data).append(",");
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
                columnHeaders = new StringBuilder(SYSTEM_COLUMN_HEADERS + "," + COMPONENT_COLUMN_HEADERS);
            }
            bufferedWriter.append(columnHeaders.toString()).append(System.lineSeparator());
            bufferedWriter.append(reportData.toString());
        } else {
            bufferedWriter.append(jsonReportData.toString());
        }
        bufferedWriter.flush();
    }


    /**
     * This method builds a JSON object from the system and component data in a
     * validation report.
     *
     * @param pc               the platform credential used to validate.
     * @param parsedComponents component data parsed from the platform credential.
     * @param company          company name.
     * @param contractNumber   contract number.
     * @return the JSON object in String format.
     */
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
        final int componentDataPosition4 = 3;
        final int componentDataPosition5 = 4;
        final int componentDataPosition6 = 5;
        for (ArrayList<String> componentData : parsedComponents) {
            JsonObject component = new JsonObject();
            component.addProperty("Component name", componentData.get(0));
            component.addProperty("Component manufacturer", componentData.get(1));
            component.addProperty("Component model", componentData.get(2));
            component.addProperty("Component SN", componentData.get(componentDataPosition4));
            component.addProperty("Issuer", componentData.get(componentDataPosition5));
            component.addProperty("Component status", componentData.get(componentDataPosition6));
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
     *
     * @param pc the platform credential.
     * @return the ArrayList of ArrayLists containing the parsed component data.
     */
    private ArrayList<ArrayList<String>> parseComponents(final PlatformCredential pc) {
        ArrayList<ArrayList<String>> parsedComponents = new ArrayList<>();
        ArrayList<ArrayList<Object>> chainComponents = new ArrayList<>();

        StringBuilder componentFailureString = new StringBuilder();
        if (pc.getComponentIdentifiers() != null
                && !pc.getComponentIdentifiers().isEmpty()) {
            componentFailureString.append(pc.getComponentFailures());
            // get all the certificates associated with the platform serial
            List<PlatformCredential> chainCertificates =
                    certificateRepository.byBoardSerialNumber(pc.getPlatformSerial());
            // combine all components in each certificate
            for (ComponentIdentifier ci : pc.getComponentIdentifiers()) {
                ArrayList<Object> issuerAndComponent = new ArrayList<>();
                issuerAndComponent.add(pc.getHolderIssuer());
                issuerAndComponent.add(ci);
                chainComponents.add(issuerAndComponent);
            }

            for (PlatformCredential cert : chainCertificates) {
                componentFailureString.append(cert.getComponentFailures());
                if (!cert.isPlatformBase()) {
                    for (ComponentIdentifier ci : cert.getComponentIdentifiers()) {
                        ArrayList<Object> issuerAndComponent = new ArrayList<>();
                        issuerAndComponent.add(cert.getHolderIssuer());
                        issuerAndComponent.add(ci);
                        chainComponents.add(issuerAndComponent);
                    }
                }
            }
            log.info("Component failures: {}", componentFailureString);
            for (ArrayList<Object> issuerAndComponent : chainComponents) {
                ArrayList<String> componentData = new ArrayList<>();
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
