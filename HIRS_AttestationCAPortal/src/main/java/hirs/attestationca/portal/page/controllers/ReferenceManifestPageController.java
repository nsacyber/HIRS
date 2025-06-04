package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.xml.bind.UnmarshalException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URISyntaxException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Controller for the Reference Manifest page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/reference-manifests")
public class ReferenceManifestPageController extends PageController<NoPageParams> {

    private static final String BASE_RIM_FILE_PATTERN = "([^\\s]+(\\.(?i)swidtag)$)";
    private static final String SUPPORT_RIM_FILE_PATTERN = "([^\\s]+(\\.(?i)(rimpcr|rimel|bin|log))$)";
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final EntityManager entityManager;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceManifestRepository    the reference manifest manager
     * @param referenceDigestValueRepository reference digest value manager
     * @param entityManager                  entity manager
     */
    @Autowired
    public ReferenceManifestPageController(
            final ReferenceManifestRepository referenceManifestRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository,
            final EntityManager entityManager) {
        super(Page.REFERENCE_MANIFESTS);
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.entityManager = entityManager;
    }

    /**
     * Returns the filePath for the view and the data model for the Reference Manifest page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the filePath for the view and data model for the Reference Manifest page.
     */
    @Override
    public ModelAndView initPage(final NoPageParams params,
                                 final Model model) {
        return getBaseModelAndView(Page.REFERENCE_MANIFESTS);
    }

    /**
     * Processes the request to retrieve a list of RIMs for display on the RIM page.
     *
     * @param input data table input
     * @return data table of RIMs
     */
    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<ReferenceManifest> getRIMTableData(
            @Valid final DataTableInput input) {
        log.info("Received request to display list of reference manifests");
        log.debug("Request received a datatable input object for the reference manifest page "
                + " page: {}", input);

        String orderColumnName = input.getOrderColumnName();
        log.debug("Ordering on column: {}", orderColumnName);

        final String searchTerm = input.getSearch().getValue();
        final Set<String> searchableColumns =
                ControllerPagesUtils.findSearchableColumnsNames(ReferenceManifest.class,
                        input.getColumns());

        final int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        FilteredRecordsList<ReferenceManifest> rimFilteredRecordsList = new FilteredRecordsList<>();

        org.springframework.data.domain.Page<ReferenceManifest> pagedResult;

        if (StringUtils.isBlank(searchTerm)) {
            pagedResult = this.referenceManifestRepository.findByArchiveFlag(false, pageable);
        } else {
            pagedResult = findRIMSBySearchableColumnsAndArchiveFlag(searchableColumns, searchTerm, false,
                    pageable);
        }

        if (pagedResult.hasContent()) {
            rimFilteredRecordsList.addAll(pagedResult.getContent());
        }

        rimFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        rimFilteredRecordsList.setRecordsTotal(findRIMRepoCount());

        log.info("Returning the size of the list of reference manifests: {}",
                rimFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(rimFilteredRecordsList, input);
    }

    /**
     * Processes the request to upload one or more reference manifest(s) to the ACA.
     *
     * @param files the files to process
     * @param attr  the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView uploadRIMs(
            @RequestParam("file") final MultipartFile[] files,
            final RedirectAttributes attr) throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String fileName;
        Pattern baseRimPattern = Pattern.compile(BASE_RIM_FILE_PATTERN);
        Pattern supportRimPattern = Pattern.compile(SUPPORT_RIM_FILE_PATTERN);
        Matcher matcher;
        List<BaseReferenceManifest> baseRims = new ArrayList<>();
        List<SupportReferenceManifest> supportRims = new ArrayList<>();
        log.info("Processing {} uploaded files", files.length);

        // loop through the files
        for (MultipartFile file : files) {
            boolean isBaseRim;
            boolean isSupportRim = false;
            fileName = file.getOriginalFilename();
            matcher = baseRimPattern.matcher(fileName);
            isBaseRim = matcher.matches();
            if (!isBaseRim) {
                matcher = supportRimPattern.matcher(fileName);
                isSupportRim = matcher.matches();
            }
            if (isBaseRim || isSupportRim) {
                parseRIM(file, isSupportRim, messages, baseRims, supportRims);
            } else {
                String errorString = "The file extension of " + fileName + " was not recognized."
                        + " Base RIMs support the extension \".swidtag\", and support RIMs support "
                        + "\".rimpcr\", \".rimel\", \".bin\", and \".log\". "
                        + "Please verify your upload and retry.";
                log.error("File extension in {} not recognized as base or support RIM.", fileName);
                messages.addErrorMessage(errorString);
            }
        }
        baseRims.forEach((rim) -> {
            log.info("Storing swidtag {}", rim.getFileName());
            this.referenceManifestRepository.save(rim);
        });

        supportRims.forEach((rim) -> {
            log.info("Storing event log {}", rim.getFileName());
            this.referenceManifestRepository.save(rim);
        });

        // Prep a map to associated the swidtag payload hash to the swidtag.
        // pass it in to update support rims that either were uploaded
        // or already exist
        // create a map of the supports rims in case an uploaded swidtag
        // isn't one to one with the uploaded support rims.
        Map<String, SupportReferenceManifest> updatedSupportRims
                = updateSupportRimInfo(referenceManifestRepository.findAllSupportRims());

        // pass in the updated support rims
        // and either update or add the events
        processTpmEvents(new ArrayList<>(updatedSupportRims.values()));

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.REFERENCE_MANIFESTS,
                new NoPageParams(), model, attr);
    }

    /**
     * Processes the request to download the RIM .
     *
     * @param id       the UUID of the rim to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws java.io.IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadRIM(@RequestParam final String id,
                            final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download RIM id {}", id);

        try {
            final UUID uuid = UUID.fromString(id);
            ReferenceManifest referenceManifest = findSpecifiedRIM(uuid);

            if (referenceManifest == null) {
                final String notFoundMessage = "Unable to locate RIM with ID: " + uuid;
                log.warn(notFoundMessage);
                throw new EntityNotFoundException(notFoundMessage);
            }

            // Set filename for download.
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment;" + "filename=\"" + referenceManifest.getFileName()
            );
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            // write cert to output stream
            response.getOutputStream().write(referenceManifest.getRimBytes());

        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the"
                    + " specified RIM", exception);

            // send a 404 error when an exception is thrown while attempting to download the
            // specified RIM
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to bulk download RIMs .
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadRIMs(final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download all Reference Integrity Manifests");
        String fileName = "rims.zip";


        // Set filename for download.
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        // write cert to output stream
        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            bulkDownloadRIMS(zipOut);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the"
                    + "reference integrity manifests", exception);

            // send a 404 error when an exception is thrown while attempting to download the
            // reference manifests
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to archive/soft delete the provided Reference Integrity Manifest.
     *
     * @param id   the UUID of the rim to delete
     * @param attr RedirectAttributes used to forward data back to the original
     *             page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteRIM(@RequestParam final String id,
                                  final RedirectAttributes attr) throws URISyntaxException {
        log.info("Received request to delete RIM id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            final UUID uuid = UUID.fromString(id);
            ReferenceManifest referenceManifest = findSpecifiedRIM(uuid);

            if (referenceManifest == null) {
                String notFoundMessage = "Unable to locate RIM to delete with ID: " + id;
                messages.addErrorMessage(notFoundMessage);
                log.warn(notFoundMessage);
                throw new EntityNotFoundException(notFoundMessage);
            }

            this.referenceManifestRepository.delete(referenceManifest);

            String deleteCompletedMessage = "RIM successfully deleted";
            messages.addSuccessMessage(deleteCompletedMessage);
            log.info(deleteCompletedMessage);

        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to download the"
                    + " specified RIM";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.REFERENCE_MANIFESTS, new NoPageParams(), model, attr);
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * RIMS whose field values matches the provided search term.
     *
     * @param searchableColumns list of the searchable column names
     * @param searchTerm        text that was input in the search textbox
     * @param archiveFlag       archive flag
     * @param pageable          pageable
     * @return page full of reference manifests
     */
    private org.springframework.data.domain.Page<ReferenceManifest> findRIMSBySearchableColumnsAndArchiveFlag(
            final Set<String> searchableColumns,
            final String searchTerm,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceManifest> query = criteriaBuilder.createQuery(ReferenceManifest.class);
        Root<ReferenceManifest> rimRoot = query.from(ReferenceManifest.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchTerm)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {
                Predicate predicate =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(rimRoot.get(columnName)),
                                "%" + searchTerm.toLowerCase() + "%");
                predicates.add(predicate);
            }
        }

        Predicate likeConditions = criteriaBuilder.or(predicates.toArray(new Predicate[0]));

        // Add archiveFlag condition if specified
        query.where(criteriaBuilder.and(likeConditions,
                criteriaBuilder.equal(rimRoot.get("archiveFlag"), archiveFlag)));

        // Apply pagination
        TypedQuery<ReferenceManifest> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<ReferenceManifest> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * Retrieves the total number of records in the RIM repository.
     *
     * @return total number of records in the RIM repository.
     */
    private long findRIMRepoCount() {
        return this.referenceManifestRepository.findByArchiveFlag(false).size();
    }

    /**
     * This method takes the parameter and looks for this information in the
     * Database.
     *
     * @param uuid RIM uuid
     * @return the associated RIM from the DB
     */
    private ReferenceManifest findSpecifiedRIM(final UUID uuid) {
        if (referenceManifestRepository.existsById(uuid)) {
            return referenceManifestRepository.getReferenceById(uuid);
        }
        return null;
    }

    /**
     * Packages a collection of RIMs into a zip file.
     *
     * @param zipOut zip outputs streams
     * @throws IOException if there are any issues packaging or downloading the zip file
     */
    private void bulkDownloadRIMS(final ZipOutputStream zipOut) throws IOException {
        List<ReferenceManifest> allRIMs = this.referenceManifestRepository.findAll();

        List<ReferenceManifest> referenceManifestList = new ArrayList<>();

        for (ReferenceManifest rim : allRIMs) {
            if ((rim instanceof BaseReferenceManifest)
                    || (rim instanceof SupportReferenceManifest)) {
                referenceManifestList.add(rim);
            }
        }

        String zipFileName;

        // get all files
        for (ReferenceManifest rim : referenceManifestList) {
            if (rim.getFileName().isEmpty()) {
                zipFileName = "";
            } else {
                // configure the zip entry, the properties of the 'file'
                zipFileName = rim.getFileName();
            }
            ZipEntry zipEntry = new ZipEntry(zipFileName);
            zipEntry.setSize((long) rim.getRimBytes().length * Byte.SIZE);
            zipEntry.setTime(System.currentTimeMillis());
            zipOut.putNextEntry(zipEntry);
            // the content of the resource
            StreamUtils.copy(rim.getRimBytes(), zipOut);
            zipOut.closeEntry();
        }
        zipOut.finish();
    }

    /**
     * Takes the rim files provided and returns a {@link ReferenceManifest}
     * object.
     *
     * @param file        the provide user file via browser.
     * @param supportRIM  matcher result
     * @param messages    the object that handles displaying information to the
     *                    user.
     * @param baseRims    object to store multiple files
     * @param supportRims object to store multiple files
     */
    private void parseRIM(
            final MultipartFile file, final boolean supportRIM,
            final PageMessages messages, final List<BaseReferenceManifest> baseRims,
            final List<SupportReferenceManifest> supportRims) {

        byte[] fileBytes = new byte[0];
        String fileName = file.getOriginalFilename();
        BaseReferenceManifest baseRim;
        SupportReferenceManifest supportRim;

        // build the manifest from the uploaded bytes
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            final String failMessage
                    = String.format("Failed to read uploaded file (%s): ", fileName);
            log.error(failMessage, e);
            messages.addErrorMessage(failMessage + e.getMessage());
        }

        try {
            if (supportRIM) {
                supportRim = new SupportReferenceManifest(fileName, fileBytes);
                if (referenceManifestRepository.findByHexDecHashAndRimType(
                        supportRim.getHexDecHash(), supportRim.getRimType()) == null) {
                    supportRims.add(supportRim);
                    messages.addInfoMessage("Saved support RIM " + fileName);
                }
            } else {
                baseRim = new BaseReferenceManifest(fileName, fileBytes);
                if (referenceManifestRepository.findByHexDecHashAndRimType(
                        baseRim.getHexDecHash(), baseRim.getRimType()) == null) {
                    baseRims.add(baseRim);
                    messages.addInfoMessage("Saved base RIM " + fileName);
                }
            }
        } catch (IOException | NullPointerException ioEx) {
            final String failMessage
                    = String.format("Failed to parse support RIM file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addErrorMessage(failMessage + ioEx.getMessage());
        } catch (UnmarshalException e) {
            final String failMessage
                    = String.format("Failed to parse base RIM file (%s): ", fileName);
            log.error(failMessage, e);
            messages.addErrorMessage(failMessage + e.getMessage());
        } catch (Exception e) {
            final String failMessage
                    = String.format("Failed to parse (%s): ", fileName);
            log.error(failMessage, e);
        }
    }

    private Map<String, SupportReferenceManifest> updateSupportRimInfo(
            final List<SupportReferenceManifest> dbSupportRims) {
        SupportReferenceManifest supportRim;
        String fileString;
        Map<String, SupportReferenceManifest> updatedSupportRims = new HashMap<>();
        Map<String, SupportReferenceManifest> hashValues = new HashMap<>();
        for (SupportReferenceManifest support : dbSupportRims) {
            hashValues.put(support.getHexDecHash(), support);
        }

        for (BaseReferenceManifest dbBaseRim : referenceManifestRepository.findAllBaseRims()) {
            for (String supportHash : hashValues.keySet()) {
                fileString = new String(dbBaseRim.getRimBytes(), StandardCharsets.UTF_8);

                if (fileString.contains(supportHash)) {
                    supportRim = hashValues.get(supportHash);
                    // I have to assume the baseRim is from the database
                    // Updating the id values, manufacturer, model
                    if (supportRim != null && !supportRim.isUpdated()) {
                        supportRim.setSwidTagVersion(dbBaseRim.getSwidTagVersion());
                        supportRim.setPlatformManufacturer(dbBaseRim.getPlatformManufacturer());
                        supportRim.setPlatformModel(dbBaseRim.getPlatformModel());
                        supportRim.setTagId(dbBaseRim.getTagId());
                        supportRim.setAssociatedRim(dbBaseRim.getId());
                        dbBaseRim.setAssociatedRim(supportRim.getId());
                        supportRim.setUpdated(true);
                        referenceManifestRepository.save(supportRim);
                        updatedSupportRims.put(supportHash, supportRim);
                    }
                }
            }
            referenceManifestRepository.save(dbBaseRim);
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
        if (supportRim != null && (supportRim.getId() != null
                && !supportRim.getId().toString().isEmpty())) {
            List<BaseReferenceManifest> baseRims = new LinkedList<>();
            baseRims.addAll(this.referenceManifestRepository
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
