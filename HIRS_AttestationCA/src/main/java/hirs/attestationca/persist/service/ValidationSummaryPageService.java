package hirs.attestationca.persist.service;


import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import hirs.attestationca.persist.entity.manager.SupplyChainValidationSummaryRepository;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A service layer class responsible for encapsulating all business logic related to the Validation Summary
 * Page.
 */
@Service
@Log4j2
public class ValidationSummaryPageService {

    private static final String DEFAULT_COMPANY = "AllDevices";
    private static final String UNDEFINED = "undefined";
    private static final String SYSTEM_COLUMN_HEADERS = "Verified Manufacturer,"
            + "Model,SN,Verification Date,Device Status";
    private static final String COMPONENT_COLUMN_HEADERS = "Component name,Component manufacturer,"
            + "Component model,Component SN,Issuer,Component status";

    private final SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository;
    private final PlatformCertificateRepository platformCertificateRepository;
    private final EntityManager entityManager;
    private final CertificateRepository certificateRepository;
    private final DeviceRepository deviceRepository;

    /**
     * Constructor for the Validation Summary Page Service.
     *
     * @param supplyChainValidationSummaryRepository supply chain validation summary repository
     * @param platformCertificateRepository          platform certificate repository
     * @param certificateRepository                  certificate repository
     * @param deviceRepository                       device repository
     * @param entityManager                          entity manager
     */
    @Autowired
    public ValidationSummaryPageService(final SupplyChainValidationSummaryRepository
                                                supplyChainValidationSummaryRepository,
                                        final PlatformCertificateRepository platformCertificateRepository,
                                        final CertificateRepository certificateRepository,
                                        final DeviceRepository deviceRepository,
                                        final EntityManager entityManager) {
        this.supplyChainValidationSummaryRepository = supplyChainValidationSummaryRepository;
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
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
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
        TypedQuery<SupplyChainValidationSummary> typedQuery = this.entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<SupplyChainValidationSummary> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * @param pageable pageable
     * @return
     */
    public Page<SupplyChainValidationSummary> findSummaryReportsByPageable(final Pageable pageable) {
        return this.supplyChainValidationSummaryRepository.findByArchiveFlagFalse(pageable);
    }

    /**
     * @return
     */
    public long findValidationSummaryRepositoryCount() {
        return this.supplyChainValidationSummaryRepository.count();
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
        DateTimeFormatter dateTimeFormat =
                DateTimeFormatter.ofPattern("EEE, dd MMM uuuu HH:mm:ss z");
        LocalDate startDate = null;
        LocalDate endDate = null;
        ArrayList<LocalDate> createTimes = new ArrayList<>();
        String[] deviceNames = new String[]{};

        final Enumeration<String> parameters = request.getParameterNames();

        while (parameters.hasMoreElements()) {
            String parameter = parameters.nextElement();
            String parameterValue = request.getParameter(parameter);
            log.debug("HTTP Servlet Request Param: {}: HTTP Servlet Request Param Value: {}", parameter,
                    parameterValue);
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
                        String[] timestamps = parameterValue.split(";");

                        for (String timestamp : timestamps) {
                            ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, dateTimeFormat);

                            // Convert to LocalDateTime (drops time zone info)
                            LocalDate localDate = zonedDateTime.toLocalDate();

                            createTimes.add(localDate);
                        }
                    }
                    break;
                case "deviceNames":
                    if (!parameterValue.equals(UNDEFINED)
                            && !parameterValue.isEmpty()) {
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
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));

        StringBuilder reportData = new StringBuilder();

        for (int i = 0; i < deviceNames.length; i++) {
            if ((createTimes.get(i).isAfter(startDate) || createTimes.get(i).isEqual(
                    Objects.requireNonNull(startDate)))
                    && (createTimes.get(i).isBefore(endDate)
                    || createTimes.get(i).isEqual(Objects.requireNonNull(endDate)))) {
                Device device = deviceRepository.findByName(deviceNames[i]);
                PlatformCredential pc = platformCertificateRepository.findByDeviceId(device.getId()).get(0);

                if (i == 0) {
                    bufferedWriter.append("Company: ").append(company).append("\n");
                    bufferedWriter.append("Contract number: ").append(contractNumber).append("\n");
                }

                StringBuilder systemInfo = new StringBuilder();
                systemInfo.append(pc.getManufacturer())
                        .append(",")
                        .append(pc.getModel())
                        .append(",")
                        .append(pc.getPlatformSerial())
                        .append(",")
                        .append(LocalDateTime.now())
                        .append(",")
                        .append(device.getSupplyChainValidationStatus())
                        .append(",");

                ArrayList<ArrayList<String>> parsedComponents = parsePlatformCredentialComponents(pc);

                for (ArrayList<String> component : parsedComponents) {
                    reportData.append(systemInfo);
                    for (String data : component) {
                        reportData.append(data).append(",");
                    }
                    reportData.deleteCharAt(reportData.length() - 1);
                    reportData.append(System.lineSeparator());
                }
            }
        }

        bufferedWriter.append(new StringBuilder(SYSTEM_COLUMN_HEADERS + "," + COMPONENT_COLUMN_HEADERS))
                .append(System.lineSeparator());
        bufferedWriter.append(reportData.toString());
        bufferedWriter.flush();
    }

    /**
     * This method parses the provided platform credential's list of ComponentIdentifiers into an ArrayList
     * of ArrayLists.
     * - ComponentClass
     * - Manufacturer
     * - Model
     * - Serial number
     * - Pass/fail status (based on componentFailures string)
     *
     * @param pc the platform credential.
     * @return the ArrayList of ArrayLists containing the parsed component data.
     */
    private ArrayList<ArrayList<String>> parsePlatformCredentialComponents(final PlatformCredential pc)
            throws IOException {
        ArrayList<ArrayList<String>> parsedComponents = new ArrayList<>();
        ArrayList<ArrayList<Object>> chainComponents = new ArrayList<>();

        // get all the certificates associated with the platform serial
        final List<PlatformCredential> chainCertificates =
                certificateRepository.byBoardSerialNumber(pc.getPlatformSerial());

        StringBuilder componentFailureString = new StringBuilder();
        componentFailureString.append(pc.getComponentFailures());
        log.debug("Component failures: {}", componentFailureString);

        // if the platform credential's has a list of version 1 component identifiers
        if (pc.getPlatformConfigurationV1() != null && pc.getComponentIdentifiers() != null) {
            List<ComponentIdentifier> componentIdentifiers = pc.getComponentIdentifiers();

            // combine all components in each certificate
            for (ComponentIdentifier ci : componentIdentifiers) {
                ArrayList<Object> issuerAndComponent = new ArrayList<>();
                issuerAndComponent.add(pc.getHolderIssuer());
                issuerAndComponent.add(ci);
                chainComponents.add(issuerAndComponent);
            }

            for (PlatformCredential cert : chainCertificates) {
                componentFailureString.append(cert.getComponentFailures());
                if (!cert.isPlatformBase()) {
                    List<ComponentIdentifier> chainComponentIdentifiers = cert.getComponentIdentifiers();
                    for (ComponentIdentifier ci : chainComponentIdentifiers) {
                        ArrayList<Object> issuerAndComponent = new ArrayList<>();
                        issuerAndComponent.add(cert.getHolderIssuer());
                        issuerAndComponent.add(ci);
                        chainComponents.add(issuerAndComponent);
                    }
                }
            }

            for (ArrayList<Object> issuerAndComponent : chainComponents) {
                ArrayList<String> componentData = new ArrayList<>();
                String issuer = (String) issuerAndComponent.get(0);
                issuer = issuer.replaceAll(",", " ");
                ComponentIdentifier ci = (ComponentIdentifier) issuerAndComponent.get(1);

                componentData.add("Platform Component");
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
                log.debug("Parsed Component Identifiers V1: {}",
                        String.join(",", componentData));
            }
        } else if (pc.getPlatformConfigurationV2() != null && pc.getComponentIdentifiersV2() != null) {
            List<ComponentIdentifierV2> componentIdentifiersV2 = pc.getComponentIdentifiersV2();

            // combine all components in each certificate
            for (ComponentIdentifierV2 ci2 : componentIdentifiersV2) {
                ArrayList<Object> issuerAndComponent = new ArrayList<>();
                issuerAndComponent.add(pc.getHolderIssuer());
                issuerAndComponent.add(ci2);
                chainComponents.add(issuerAndComponent);
            }

            for (PlatformCredential cert : chainCertificates) {
                componentFailureString.append(cert.getComponentFailures());
                if (!cert.isPlatformBase()) {
                    List<ComponentIdentifierV2> chainComponentIdentifiersV2 =
                            cert.getComponentIdentifiersV2();
                    for (ComponentIdentifierV2 ci2 : chainComponentIdentifiersV2) {
                        ArrayList<Object> issuerAndComponent = new ArrayList<>();
                        issuerAndComponent.add(cert.getHolderIssuer());
                        issuerAndComponent.add(ci2);
                        chainComponents.add(issuerAndComponent);
                    }
                }
            }

            for (ArrayList<Object> issuerAndComponent : chainComponents) {
                ArrayList<String> componentData = new ArrayList<>();
                String issuer = (String) issuerAndComponent.get(0);
                issuer = issuer.replaceAll(",", " ");
                ComponentIdentifierV2 ci2 = (ComponentIdentifierV2) issuerAndComponent.get(1);

                String componentClass = ci2.getComponentClass().toString();
                String[] splitStrings = componentClass.split("\r\n|\n|\r");

                componentData.add(String.join(" ", splitStrings));
                componentData.add(ci2.getComponentManufacturer().getString());
                componentData.add(ci2.getComponentModel().getString());
                componentData.add(ci2.getComponentSerial().getString());
                componentData.add(issuer);
                //Failing components are identified by hashcode
                if (componentFailureString.toString().contains(String.valueOf(ci2.hashCode()))) {
                    componentData.add("Fail");
                } else {
                    componentData.add("Pass");
                }
                parsedComponents.add(componentData);
                log.debug("Parsed Component Identifiers V2: {}",
                        String.join(",", componentData));
            }
        }

        return parsedComponents;
    }
}
