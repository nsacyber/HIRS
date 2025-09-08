package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.util.DownloadFile;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.xml.bind.UnmarshalException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A service layer class responsible for encapsulating all business logic related to the Reference Manifest Page.
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
     * @param searchableColumns list of the searchable column names
     * @param searchTerm        text that was input in the search textbox
     * @param archiveFlag       archive flag
     * @param pageable          pageable
     * @return page full of reference manifests
     */
    public org.springframework.data.domain.Page<ReferenceManifest> findRIMSBySearchableColumnsAndArchiveFlag(
            final Set<String> searchableColumns,
            final String searchTerm,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
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
        TypedQuery<ReferenceManifest> typedQuery = this.entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<ReferenceManifest> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return
     */
    public Page<ReferenceManifest> findRIMsByArchiveFlag(final boolean archiveFlag, Pageable pageable) {
        return this.referenceManifestRepository.findByArchiveFlag(archiveFlag, pageable);
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
     * This method takes the parameter and looks for this information in the
     * Database.
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

        final List<ReferenceManifest> referenceManifestList =
                allRIMs.stream().filter(rim ->
                        rim instanceof BaseReferenceManifest
                                || rim instanceof SupportReferenceManifest).toList();
        String zipFileName;

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
            StreamUtils.copy(rim.getRimBytes(), zipOut);
            zipOut.closeEntry();
        }
        zipOut.finish();
    }

    /**
     * Deletes the specified RIM using the provided UUID.
     *
     * @param uuid            the UUID of the RIM to delete
     * @param successMessages success messages
     * @param errorMessages   error messages
     */
    public void deleteRIM(UUID uuid, List<String> successMessages, List<String> errorMessages) {
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

    public void uploadRIMS(List<BaseReferenceManifest> baseRims, List<SupportReferenceManifest> supportRims) {
        baseRims.forEach((baseRIM) -> {
            if (referenceManifestRepository.findByHexDecHashAndRimType(
                    baseRIM.getHexDecHash(), baseRIM.getRimType()) == null) {
                log.info("Storing swidtag {}", baseRIM.getFileName());
                this.referenceManifestRepository.save(baseRIM);
            }
        });

        supportRims.forEach((supportRIM) -> {
            if (referenceManifestRepository.findByHexDecHashAndRimType(
                    supportRIM.getHexDecHash(), supportRIM.getRimType()) == null) {
                log.info("Storing event log {}", supportRIM.getFileName());
                this.referenceManifestRepository.save(supportRIM);
            }
        });

        // Prep a map to associate the swidtag payload hash to the swidtag.
        // pass it in to update support rims that either were uploaded
        // or already exist create a map of the supports rims in case an uploaded swidtag
        // isn't one to one with the uploaded support rims.
        Map<String, SupportReferenceManifest> updatedSupportRims
                = updateSupportRimInfo(referenceManifestRepository.findAllSupportRims());

        // pass in the updated support rims
        // and either update or add the events
        processTpmEvents(new ArrayList<>(updatedSupportRims.values()));
    }

    /**
     * todo
     *
     * @param errorMessages contains any error messages that will be displayed on the page
     * @param file
     */
    public SupportReferenceManifest parseSupportRIM(final List<String> errorMessages,
                                                    final MultipartFile file) {
        byte[] fileBytes = new byte[0];
        final String fileName = file.getOriginalFilename();

        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            final String failMessage = String.format("Failed to read uploaded Support RIM file (%s): "
                    , fileName);
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
     * todo
     *
     * @param errorMessages contains any error messages that will be displayed on the page
     * @param file
     */
    public BaseReferenceManifest parseBaseRIM(final List<String> errorMessages,
                                              final MultipartFile file) {
        byte[] fileBytes = new byte[0];
        final String fileName = file.getOriginalFilename();

        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            final String failMessage = String.format("Failed to read uploaded Base RIM file (%s): ", fileName);
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

    private Map<String, SupportReferenceManifest> updateSupportRimInfo(
            final List<SupportReferenceManifest> dbSupportRims) {
        SupportReferenceManifest supportRim;
        String fileString;
        Map<String, SupportReferenceManifest> updatedSupportRims = new HashMap<>();
        Map<String, SupportReferenceManifest> hashValues = new HashMap<>();
        for (SupportReferenceManifest support : dbSupportRims) {
            hashValues.put(support.getHexDecHash(), support);
        }

        for (BaseReferenceManifest dbBaseRim : this.referenceManifestRepository.findAllBaseRims()) {
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
                        this.referenceManifestRepository.save(supportRim);
                        updatedSupportRims.put(supportHash, supportRim);
                    }
                }
            }
            this.referenceManifestRepository.save(dbBaseRim);
        }

        return updatedSupportRims;
    }

    public void storeRIM() {

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
