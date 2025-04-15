package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 */
@Service
@Log4j2
public class ReferenceManifestService {

    private final EntityManager entityManager;
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;


    public ReferenceManifestService(EntityManager entityManager,
                                    ReferenceManifestRepository referenceManifestRepository,
                                    ReferenceDigestValueRepository referenceDigestValueRepository) {
        this.entityManager = entityManager;
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * RIMS whose field values matches the provided search term.
     *
     * @param searchableColumns list of the searchable column names
     * @param searchText        text that was input in the search textbox
     * @param archiveFlag       archive flag
     * @param pageable          pageable
     * @return page full of reference manifests
     */
    public Page<ReferenceManifest> findRIMSBySearchableColumnsAndArchiveFlag(
            final List<String> searchableColumns,
            final String searchText,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceManifest> query = criteriaBuilder.createQuery(ReferenceManifest.class);
        Root<ReferenceManifest> rimRoot = query.from(ReferenceManifest.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchText)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {

                // there is a possibility that one of the column names
                // that matches one of the class fields is nested (e.g. device.name) ,
                // and we will need to do further work to extract the
                // field name
                // todo
                if (columnName.contains(".")) {

                }

                Predicate predicate =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(rimRoot.get(columnName)),
                                "%" + searchText.toLowerCase() + "%");
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
     * Retrieves all RIMs from the database based on the provided archive flag.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return page of RIMs
     */
    public Page<ReferenceManifest> findAllRIMsByArchiveFlag(final boolean archiveFlag,
                                                            final Pageable pageable) {
        return referenceManifestRepository.findByArchiveFlag(archiveFlag, pageable);
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
     * Packages a collection of RIMs into a zip file.
     *
     * @param zipOut zip outputs streams
     * @throws IOException if there are any issues packaging or downloading the zip file
     */
    public void bulkDownloadRIMS(ZipOutputStream zipOut) throws IOException {
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
     * Deletes the specified reference manifest.
     *
     * @param referencedManifest
     */
    public void deleteSpecifiedRIM(ReferenceManifest referencedManifest) {
        referenceManifestRepository.delete(referencedManifest);
    }
}
