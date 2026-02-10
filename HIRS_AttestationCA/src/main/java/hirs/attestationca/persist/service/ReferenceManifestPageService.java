package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.service.util.DataTablesColumn;
import hirs.attestationca.persist.service.util.PredicateFactory;
import hirs.attestationca.persist.util.DownloadFile;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A service layer class responsible for encapsulating all business logic related to the
 * Reference Manifest Page.
 */
@Log4j2
@Service
public class ReferenceManifestPageService {
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final EntityManager entityManager;

    /**
     * Constructor for the Reference Manifest Page Service.
     *
     * @param referenceManifestRepository    reference manifest repository
     * @param referenceDigestValueRepository reference digest value repository
     * @param entityManager                  entity manager
     */
    @Autowired
    public ReferenceManifestPageService(final ReferenceManifestRepository referenceManifestRepository,
                                        final ReferenceDigestValueRepository referenceDigestValueRepository,
                                        final EntityManager entityManager) {
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.entityManager = entityManager;
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * RIMS whose field values matches the provided search term.
     *
     * @param searchableColumnNames list of the searchable column names
     * @param globalSearchTerm      text that was input in the global search textbox
     * @param archiveFlag           archive flag
     * @param pageable              pageable
     * @return page full of reference manifests
     */
    public Page<ReferenceManifest> findRIMSByGlobalSearchTermAndArchiveFlag(
            final Set<String> searchableColumnNames,
            final String globalSearchTerm,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceManifest> query = criteriaBuilder.createQuery(ReferenceManifest.class);
        Root<ReferenceManifest> rimRoot = query.from(ReferenceManifest.class);

        final Predicate combinedGlobalSearchPredicates =
                createPredicatesForGlobalSearch(searchableColumnNames, criteriaBuilder,
                        rimRoot,
                        globalSearchTerm);

        // Define the conditions (predicates) for the query's WHERE clause.
        query.where(criteriaBuilder.and(
                combinedGlobalSearchPredicates,
                criteriaBuilder.equal(rimRoot.get("archiveFlag"), archiveFlag),
                criteriaBuilder.notEqual(rimRoot.get("rimType"), "Measurement")
        ));

        // Apply sorting if present in the Pageable
        query.orderBy(getSortingOrders(criteriaBuilder, rimRoot, pageable.getSort()));

        // Apply pagination
        TypedQuery<ReferenceManifest> typedQuery = this.entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<ReferenceManifest> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }


    /**
     * Takes the provided columns that come with a search criteria and attempts to find
     * reference manifests (RIMs) that match the column's specific search criteria's search value.
     *
     * @param columnsWithSearchCriteria columns that have a search criteria applied to them
     * @param archiveFlag               archive flag
     * @param pageable                  pageable
     * @return page full of reference manifests
     */
    public Page<ReferenceManifest> findRIMSByColumnSpecificSearchTermAndArchiveFlag(
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceManifest> query = criteriaBuilder.createQuery(ReferenceManifest.class);
        Root<ReferenceManifest> rimRoot = query.from(ReferenceManifest.class);

        final Predicate combinedColumnSearchPredicates =
                createPredicatesForColumnSpecificSearch(columnsWithSearchCriteria, criteriaBuilder,
                        rimRoot);

        // Define the conditions (predicates) for the query's WHERE clause.
        query.where(criteriaBuilder.and(
                combinedColumnSearchPredicates,
                criteriaBuilder.equal(rimRoot.get("archiveFlag"), archiveFlag),
                criteriaBuilder.notEqual(rimRoot.get("rimType"), "Measurement")
        ));

        // Apply sorting if present in the Pageable
        query.orderBy(getSortingOrders(criteriaBuilder, rimRoot, pageable.getSort()));

        // Apply pagination
        TypedQuery<ReferenceManifest> typedQuery = this.entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<ReferenceManifest> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * Finds RIMS based on both global search and column-specific search criteria.
     * The method applies the provided global search term across all searchable columns
     * and also applies column-specific filters based on the individual column search criteria.
     * The results are returned with pagination support.
     * <p>
     * This method combines the logic of two search functionalities:
     * <ul>
     * <li> Global search: Searches across all specified columns for a matching term.</li>
     * <li> Column-specific search: Filters based on individual column search criteria,
     * such as text or date searches.</li>
     * </ul>
     *
     * @param searchableColumnNames     list of the searchable column names
     * @param globalSearchTerm          text that was input in the global search textbox
     * @param columnsWithSearchCriteria columns that have a search criteria applied to them
     * @param pageable                  pageable
     * @param archiveFlag               archive flag
     * @return page full of reference manifests
     */
    public Page<ReferenceManifest> findRIMSByGlobalAndColumnSpecificSearchTerm(
            final Set<String> searchableColumnNames,
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceManifest> query = criteriaBuilder.createQuery(ReferenceManifest.class);
        Root<ReferenceManifest> rimRoot = query.from(ReferenceManifest.class);

        final Predicate globalSearchPartOfChainedPredicates =
                createPredicatesForGlobalSearch(searchableColumnNames, criteriaBuilder, rimRoot,
                        globalSearchTerm);

        final Predicate columnSearchPartOfChainedPredicates =
                createPredicatesForColumnSpecificSearch(columnsWithSearchCriteria, criteriaBuilder,
                        rimRoot);

        // Define the conditions (predicates) for the query's WHERE clause.
        // Combine global and column-specific predicates using AND logic
        query.where(criteriaBuilder.and(
                globalSearchPartOfChainedPredicates,
                columnSearchPartOfChainedPredicates,
                criteriaBuilder.equal(rimRoot.get("archiveFlag"), archiveFlag),
                criteriaBuilder.notEqual(rimRoot.get("rimType"), "Measurement")
        ));

        // Apply sorting if present in the Pageable
        query.orderBy(getSortingOrders(criteriaBuilder, rimRoot, pageable.getSort()));

        // Apply pagination
        TypedQuery<ReferenceManifest> typedQuery = this.entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<ReferenceManifest> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * Retrieves a page of RIMS using the provided archive flag and pageable value.
     *
     * @param pageable pageable
     * @return page of RIMs
     */
    public Page<ReferenceManifest> findAllBaseAndSupportRIMSByPageable(final Pageable pageable) {
        return this.referenceManifestRepository.findAllBaseAndSupportRimsPageable(pageable);
    }

    /**
     * Retrieves the total number of records in the RIM repository.
     *
     * @return total number of records in the RIM repository.
     */
    public long findRIMRepositoryCount() {
        return this.referenceManifestRepository.findByArchiveFlag(false).size();
    }

    /**
     * Retrieves the specified RIM using the provided uuid.
     *
     * @param uuid RIM uuid
     * @return the associated RIM from the DB
     */
    public ReferenceManifest findSpecifiedRIM(final UUID uuid) {
        if (referenceManifestRepository.existsById(uuid)) {
            return referenceManifestRepository.getReferenceById(uuid);
        }
        return null;
    }

    /**
     * Retrieves a RIM from the database and prepares its contents for download.
     *
     * @param uuid rim uuid
     * @return download file of a RIM
     */
    public DownloadFile downloadRIM(final UUID uuid) {
        final ReferenceManifest referenceManifest = this.findSpecifiedRIM(uuid);

        if (referenceManifest == null) {
            final String notFoundMessage = "Unable to locate RIM with ID: " + uuid;
            log.warn(notFoundMessage);
            throw new EntityNotFoundException(notFoundMessage);
        }

        return new DownloadFile(referenceManifest.getFileName(), referenceManifest.getRimBytes());
    }

    /**
     * Packages a collection of RIMs into a zip file.
     *
     * @param zipOut zip outputs streams
     * @throws IOException if there are any issues packaging or downloading the zip file
     */
    public void bulkDownloadRIMS(final ZipOutputStream zipOut) throws IOException {
        List<ReferenceManifest> allRIMs = this.referenceManifestRepository.findAll();

        // create a list of all the RIMs that are of base rim or support rim type
        final List<ReferenceManifest> referenceManifestList =
                allRIMs.stream().filter(rim ->
                                rim instanceof BaseReferenceManifest || rim instanceof SupportReferenceManifest)
                        .toList();

        String zipFileName;

        for (ReferenceManifest rim : referenceManifestList) {
            zipFileName = rim.getFileName().isEmpty() ? "" : rim.getFileName();
            ZipEntry zipEntry = new ZipEntry(zipFileName);
            zipEntry.setSize((long) rim.getRimBytes().length * Byte.SIZE);
            zipEntry.setTime(System.currentTimeMillis());
            zipOut.putNextEntry(zipEntry);
            StreamUtils.copy(rim.getRimBytes(), zipOut);
            zipOut.closeEntry();
        }
        zipOut.finish();
    }

    /**
     * Deletes the specified RIM using the provided UUID.
     *
     * @param uuid            the UUID of the RIM to delete
     * @param successMessages contains any success messages that will be displayed on the page
     * @param errorMessages   contains any error messages that will be displayed on the page
     */
    public void deleteRIM(final UUID uuid,
                          final List<String> successMessages,
                          final List<String> errorMessages) {
        ReferenceManifest referenceManifest = this.findSpecifiedRIM(uuid);

        if (referenceManifest == null) {
            final String notFoundMessage = "Unable to locate RIM to delete with ID: " + uuid;
            errorMessages.add(notFoundMessage);
            log.warn(notFoundMessage);
            throw new EntityNotFoundException(notFoundMessage);
        }

        this.referenceManifestRepository.delete(referenceManifest);

        final String deleteCompletedMessage = "RIM successfully deleted";
        successMessages.add(deleteCompletedMessage);
        log.info(deleteCompletedMessage);
    }

    /**
     * Bulk deletes the provided list of RIMs from the database.
     *
     * @param ids             the list of ids of the RIMs to be deleted
     * @param successMessages contains any success messages that will be displayed on the page
     * @param errorMessages   contains any error messages that will be displayed on the page
     */
    public void bulkDeleteRIMs(final List<String> ids,
                               final List<String> successMessages,
                               final List<String> errorMessages) {
        // convert the list of string ids to a set of uuids
        final Set<UUID> uuids = ids.stream().map(UUID::fromString).collect(Collectors.toSet());

        // loop through the provided RIM ids and delete each RIM
        for (UUID eachUUID : uuids) {
            deleteRIM(eachUUID, successMessages, errorMessages);
        }
    }

    /**
     * Stores the base and support reference manifests to the reference manifest repository.
     *
     * @param successMessages contains any success messages that will be displayed on the page
     * @param baseRims        list of base reference manifests
     * @param supportRims     list of support reference manifests
     */
    public void storeRIMS(final List<String> successMessages,
                          final List<BaseReferenceManifest> baseRims,
                          final List<SupportReferenceManifest> supportRims) {

        // save the base rims in the repo if they don't already exist in the repo
        baseRims.forEach((baseRIM) -> {
            if (this.referenceManifestRepository.findByHexDecHashAndRimType(
                    baseRIM.getHexDecHash(), baseRIM.getRimType()) == null) {
                final String successMessage = "Stored swidtag " + baseRIM.getFileName() + " successfully";
                this.referenceManifestRepository.save(baseRIM);
                log.info(successMessage);
                successMessages.add(successMessage);
            }
        });

        // save the support rims in the repo if they don't already exist in the repo
        supportRims.forEach((supportRIM) -> {
            if (this.referenceManifestRepository.findByHexDecHashAndRimType(
                    supportRIM.getHexDecHash(), supportRIM.getRimType()) == null) {
                final String successMessage =
                        "Stored event log " + supportRIM.getFileName() + " successfully";
                this.referenceManifestRepository.save(supportRIM);
                log.info(successMessage);
                successMessages.add(successMessage);
            }
        });

        // Prep a map to associate the swidtag payload hash to the swidtag.
        // pass it in to update support rims that either were uploaded
        // or already exist create a map of the supports rims in case an uploaded swidtag
        // isn't one to one with the uploaded support rims.
        Map<String, SupportReferenceManifest> updatedSupportRims
                = updateSupportRimInfo(this.referenceManifestRepository.findAllSupportRims());

        // pass in the updated support rims
        // and either update or add the events
        processTpmEvents(new ArrayList<>(updatedSupportRims.values()));
    }

    /**
     * Attempts to parse the provided file in order to create a Base Reference Manifest.
     *
     * @param errorMessages contains any error messages that will be displayed on the page
     * @param file          file
     * @return base reference manifest
     */
    public BaseReferenceManifest parseBaseRIM(final List<String> errorMessages, final MultipartFile file) {
        byte[] fileBytes = new byte[0];
        final String fileName = file.getOriginalFilename();

        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            final String failMessage =
                    String.format("Failed to read uploaded Base RIM file (%s): ", fileName);
            log.error(failMessage, e);
            errorMessages.add(failMessage + e.getMessage());
        }

        try {
            return new BaseReferenceManifest(fileName, fileBytes);
        } catch (Exception exception) {
            final String failMessage = String.format("Failed to parse Base RIM file (%s): ", fileName);
            log.error(failMessage, exception);
            errorMessages.add(failMessage + exception.getMessage());
            return null;
        }

    }

    /**
     * Attempts to parse the provided file in order to create a Support Reference Manifest.
     *
     * @param errorMessages contains any error messages that will be displayed on the page
     * @param file          file
     * @return support reference manifest
     */
    public SupportReferenceManifest parseSupportRIM(final List<String> errorMessages,
                                                    final MultipartFile file) {
        byte[] fileBytes = new byte[0];
        final String fileName = file.getOriginalFilename();

        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            final String failMessage =
                    String.format("Failed to read uploaded Support RIM file (%s): ", fileName);
            log.error(failMessage, e);
            errorMessages.add(failMessage + e.getMessage());
        }

        try {
            return new SupportReferenceManifest(fileName, fileBytes);
        } catch (Exception exception) {
            final String failMessage = String.format("Failed to parse support RIM file (%s): ", fileName);
            log.error(failMessage, exception);
            errorMessages.add(failMessage + exception.getMessage());
            return null;
        }
    }

    /**
     * Helper method that generates a list of sorting orders based on the provided {@link Pageable} object.
     * This method checks if sorting is enabled in the {@link Pageable} and applies the necessary sorting
     * to the query using the CriteriaBuilder and RIM Root.
     *
     * @param criteriaBuilder the CriteriaBuilder used to create the sort expressions.
     * @param rimRoot         the RIM Root to which the sorting should be applied.
     * @param pageableSort    the {@link Sort} object that contains the sort information.
     * @return a list of {@link Order} objects, which can be applied to a CriteriaQuery for sorting.
     */
    private List<Order> getSortingOrders(final CriteriaBuilder criteriaBuilder,
                                         final Root<ReferenceManifest> rimRoot,
                                         final Sort pageableSort) {
        List<Order> orders = new ArrayList<>();

        if (pageableSort.isSorted()) {
            pageableSort.forEach(order -> {
                Path<Object> path = rimRoot.get(order.getProperty());
                orders.add(order.isAscending() ? criteriaBuilder.asc(path) : criteriaBuilder.desc(path));
            });
        }
        return orders;
    }

    /**
     * Helper method that generates a combined predicate for global search across searchable columns.
     * For each column, if the field is of type `String`, a "contains" condition is created. If the field
     * is of type 'Integer', an "equals" condition is created.
     *
     * @param searchableColumnNames the columns to be searched globally
     * @param criteriaBuilder       the criteria builder to construct the predicates
     * @param referenceManifestRoot the root entity representing reference manifest
     * @param globalSearchTerm      the term to search for across columns
     * @return a combined `Predicate` representing the global search conditions
     */
    private Predicate createPredicatesForGlobalSearch(
            final Set<String> searchableColumnNames,
            final CriteriaBuilder criteriaBuilder,
            final Root<ReferenceManifest> referenceManifestRoot,
            final String globalSearchTerm) {
        List<Predicate> combinedGlobalSearchPredicates = new ArrayList<>();

        // Dynamically loop through columns and create LIKE conditions for each searchable column
        for (String columnName : searchableColumnNames) {
            if (String.class.equals(referenceManifestRoot.get(columnName).getJavaType())) {
                Path<String> stringFieldPath = referenceManifestRoot.get(columnName);

                Predicate predicate = PredicateFactory.createPredicateForStringFields(criteriaBuilder,
                        stringFieldPath, globalSearchTerm, PredicateFactory.STRING_FIELD_GLOBAL_SEARCH_LOGIC);
                combinedGlobalSearchPredicates.add(predicate);
            }
        }

        return criteriaBuilder.or(combinedGlobalSearchPredicates.toArray(new Predicate[0]));
    }


    /**
     * Helper method that generates a combined predicate for column-specific search criteria.
     * It constructs conditions based on the field type (e.g., `String` or `Integer`)
     * and the provided search term and logic for each column.
     *
     * @param columnsWithSearchCriteria the columns and their associated search criteria
     * @param criteriaBuilder           the criteria builder to construct the predicates
     * @param referenceManifestRoot     the root entity representing the reference manifest
     * @return a combined `Predicate` representing the column-specific search conditions
     */
    private Predicate createPredicatesForColumnSpecificSearch(
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final CriteriaBuilder criteriaBuilder,
            final Root<ReferenceManifest> referenceManifestRoot) {
        List<Predicate> combinedColumnSearchPredicates = new ArrayList<>();

        // loop through all the datatable columns that have an applied search criteria
        for (DataTablesColumn columnWithSearchCriteria : columnsWithSearchCriteria) {
            final String columnName = columnWithSearchCriteria.getColumnName();
            final String columnSearchTerm = columnWithSearchCriteria.getColumnSearchTerm();
            final String columnSearchLogic = columnWithSearchCriteria.getColumnSearchLogic();

            if (String.class.equals(referenceManifestRoot.get(columnName).getJavaType())) {
                Path<String> stringFieldPath = referenceManifestRoot.get(columnName);

                Predicate predicate =
                        PredicateFactory.createPredicateForStringFields(criteriaBuilder, stringFieldPath,
                                columnSearchTerm,
                                columnSearchLogic);
                combinedColumnSearchPredicates.add(predicate);
            }
        }

        return criteriaBuilder.and(combinedColumnSearchPredicates.toArray(new Predicate[0]));
    }

    private Map<String, SupportReferenceManifest> updateSupportRimInfo(
            final List<SupportReferenceManifest> dbSupportRims) {
        Map<String, SupportReferenceManifest> updatedSupportRims = new HashMap<>();
        Map<String, SupportReferenceManifest> hashValues = new HashMap<>();
        for (SupportReferenceManifest support : dbSupportRims) {
            hashValues.put(support.getHexDecHash(), support);
        }

        List<BaseReferenceManifest> baseReferenceManifests =
                this.referenceManifestRepository.findAllBaseRims();

        for (BaseReferenceManifest dbBaseRim : baseReferenceManifests) {
            for (Map.Entry<String, SupportReferenceManifest> entry : hashValues.entrySet()) {
                String supportHash = entry.getKey();
                SupportReferenceManifest supportRim = entry.getValue();

                String fileString = new String(dbBaseRim.getRimBytes(), StandardCharsets.UTF_8);

                // I have to assume the baseRim is from the database
                // Updating the id values, manufacturer, model
                if (fileString.contains(supportHash) && supportRim != null && !supportRim.isUpdated()) {
                    supportRim.setSwidTagVersion(dbBaseRim.getSwidTagVersion());
                    supportRim.setPlatformManufacturer(dbBaseRim.getPlatformManufacturer());
                    supportRim.setPlatformModel(dbBaseRim.getPlatformModel());
                    supportRim.setTagId(dbBaseRim.getTagId());
                    supportRim.setAssociatedRim(dbBaseRim.getId());
                    dbBaseRim.setAssociatedRim(supportRim.getId());
                    supportRim.setUpdated(true);
                    this.referenceManifestRepository.save(supportRim);
                    updatedSupportRims.put(supportHash, supportRim);
                }
            }
            this.referenceManifestRepository.save(dbBaseRim);
        }

        return updatedSupportRims;
    }

    /**
     * If the support rim is a supplemental or base, this method looks for the
     * original oem base rim to associate with each event.
     *
     * @param supportRim assumed db object
     * @return reference to the base rim
     */
    private ReferenceManifest findBaseRim(final SupportReferenceManifest supportRim) {
        if (supportRim != null && (supportRim.getId() != null && !supportRim.getId().toString().isEmpty())) {
            List<BaseReferenceManifest> baseRims = new LinkedList<>(this.referenceManifestRepository
                    .getBaseByManufacturerModel(supportRim.getPlatformManufacturer(),
                            supportRim.getPlatformModel()));

            for (BaseReferenceManifest base : baseRims) {
                if (base.isBase()) {
                    // there should be only one
                    return base;
                }
            }
        }
        return null;
    }

    private void processTpmEvents(final List<SupportReferenceManifest> dbSupportRims) {
        List<ReferenceDigestValue> referenceValues;
        TCGEventLog logProcessor;
        ReferenceManifest baseRim;
        ReferenceDigestValue newRdv;

        for (SupportReferenceManifest dbSupport : dbSupportRims) {
            // So first we'll have to pull values based on support rim
            // get by support rim id NEXT
            if (dbSupport.getPlatformManufacturer() != null) {
                referenceValues = referenceDigestValueRepository.findBySupportRimId(dbSupport.getId());
                baseRim = findBaseRim(dbSupport);
                if (referenceValues.isEmpty()) {
                    try {
                        logProcessor = new TCGEventLog(dbSupport.getRimBytes());
                        for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                            newRdv = new ReferenceDigestValue(baseRim.getId(),
                                    dbSupport.getId(), dbSupport.getPlatformManufacturer(),
                                    dbSupport.getPlatformModel(), tpe.getPcrIndex(),
                                    tpe.getEventDigestStr(), dbSupport.getHexDecHash(),
                                    tpe.getEventTypeStr(), false, false,
                                    true, tpe.getEventContent());

                            this.referenceDigestValueRepository.save(newRdv);
                        }
                    } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    for (ReferenceDigestValue referenceValue : referenceValues) {
                        if (!referenceValue.isUpdated()) {
                            referenceValue.updateInfo(dbSupport, baseRim.getId());
                            this.referenceDigestValueRepository.save(referenceValue);
                        }
                    }
                }
            }
        }
    }
}
