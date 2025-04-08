package hirs.attestationca.persist.service;

import hirs.attestationca.persist.DBServiceException;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service layer class that handles the storage and retrieval of all types of certificates.
 */
@Log4j2
@Service
public class CertificateService {

    private static final String TRUSTCHAIN = "trust-chain";
    private static final String PLATFORMCREDENTIAL = "platform-credentials";
    private static final String IDEVIDCERTIFICATE = "idevid-certificates";
    private static final String ENDORSEMENTCREDENTIAL = "endorsement-key-credentials";
    private static final String ISSUEDCERTIFICATES = "issued-certificates";

    private final CertificateRepository certificateRepository;
    private final ComponentResultRepository componentResultRepository;
    private final EntityManager entityManager;

    @Autowired
    public CertificateService(final CertificateRepository certificateRepository,
                              final ComponentResultRepository componentResultRepository,
                              final EntityManager entityManager) {
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
        this.entityManager = entityManager;
    }

    /**
     * @param entityClass       generic entity class
     * @param searchableColumns list of the searchable column name
     * @param searchText        text that waas input in the search textbox
     * @param archiveFlag       archive flag
     * @param pageable          pageable
     * @param <T>               generic entity class
     * @return page full of the generic certificates.
     */
    public <T> Page<T> findBySearchableColumnsAndArchiveFlag(Class<T> entityClass,
                                                             List<String> searchableColumns,
                                                             String searchText,
                                                             Boolean archiveFlag,
                                                             Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
        Root<T> certificate = query.from(entityClass);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchText)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {
                Predicate predicate =
                        criteriaBuilder.like(criteriaBuilder.lower(certificate.get(columnName)),
                                "%" + searchText.toLowerCase() + "%");
                predicates.add(predicate);
            }
        }

        Predicate likeConditions = criteriaBuilder.or(predicates.toArray(new Predicate[0]));

        // Add archiveFlag condition if specified
        query.where(criteriaBuilder.and(likeConditions,
                criteriaBuilder.equal(certificate.get("archiveFlag"), archiveFlag)));

        // Apply pagination
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<T> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * Store the given certificate in the database.
     *
     * @param certificateType String containing the certificate type
     * @param fileName        contain the name of the file of the certificate to
     *                        be stored
     * @param successMessages contains any messages that will be display on the page
     * @param certificate     the certificate to store
     */
    public void storeCertificate(
            final String certificateType,
            final String fileName,
            final List<String> successMessages,
            final List<String> errorMessages,
            final Certificate certificate) {

        Certificate existingCertificate;

        // look for an identical certificate in the database
        try {
            existingCertificate = getCertificateByHash(
                    certificateType,
                    certificate.getCertificateHash());
        } catch (DBServiceException dbsEx) {
            final String failMessage = "Querying for existing certificate failed ("
                    + fileName + "): ";
            errorMessages.add(failMessage + dbsEx.getMessage());
            log.error(failMessage, dbsEx);
            return;
        }

        try {
            // save the new certificate if no match is found
            if (existingCertificate == null) {
                if (certificateType.equals(PLATFORMCREDENTIAL)) {
                    PlatformCredential platformCertificate = (PlatformCredential) certificate;
                    if (platformCertificate.isPlatformBase()) {
                        List<PlatformCredential> sharedCertificates = getPlatformCertificateByBoardSN(
                                platformCertificate.getPlatformSerial());
                        for (PlatformCredential pc : sharedCertificates) {
                            if (pc.isPlatformBase()) {
                                final String failMessage = "Storing certificate failed: "
                                        + "platform credential "
                                        + "chain (" + pc.getPlatformSerial()
                                        + ") base already exists in this chain ("
                                        + fileName + ")";
                                errorMessages.add(failMessage);
                                log.error(failMessage);
                                return;
                            }
                        }
                    }
                }

                this.certificateRepository.save(certificate);
                parseAndSaveComponentResults(certificate);

                final String successMsg
                        = String.format("New certificate successfully uploaded (%s): ", fileName);
                successMessages.add(successMsg);
                log.info(successMsg);
                return;
            }
        } catch (DBServiceException dbsEx) {
            final String failMessage = String.format("Storing new certificate failed (%s): ",
                    fileName);
            errorMessages.add(failMessage + dbsEx.getMessage());
            log.error(failMessage, dbsEx);
            return;
        } catch (IOException ioException) {
            final String ioExceptionMessage = "Failed to save component results in the database";
            errorMessages.add(ioExceptionMessage + ioException.getMessage());
            log.error(ioExceptionMessage, ioException);
            return;
        }

        try {
            // if an identical certificate is archived, update the existing certificate to
            // unarchive it and change the creation date
            if (existingCertificate.isArchived()) {
                existingCertificate.restore();
                existingCertificate.resetCreateTime();
                this.certificateRepository.save(existingCertificate);

                List<ComponentResult> componentResults = componentResultRepository
                        .findByBoardSerialNumber(((PlatformCredential) existingCertificate)
                                .getPlatformSerial());
                for (ComponentResult componentResult : componentResults) {
                    componentResult.restore();
                    componentResult.resetCreateTime();
                    this.componentResultRepository.save(componentResult);
                }

                final String successMsg = String.format("Pre-existing certificate "
                        + "found and unarchived (%s): ", fileName);
                successMessages.add(successMsg);
                log.info(successMsg);
                return;
            }
        } catch (DBServiceException dbsEx) {
            final String failMessage = String.format("Found an identical"
                    + " pre-existing certificate in the "
                    + "archive, but failed to unarchive it (%s): ", fileName);
            errorMessages.add(failMessage + dbsEx.getMessage());
            log.error(failMessage, dbsEx);
            return;
        }

        // if an identical certificate is already unarchived, do nothing and show a fail message
        final String failMessage
                = String.format("Storing certificate failed: an identical"
                + " certificate already exists (%s): ", fileName);
        errorMessages.add(failMessage);
        log.error(failMessage);
    }

    public void deleteCertificate(UUID uuid, String certificateType,
                                  final List<String> successMessages,
                                  final List<String> errorMessages) {

        Certificate certificate = certificateRepository.getCertificate(uuid);

        if (certificate == null) {
            // Use the term "record" here to avoid user confusion b/t cert and cred
            String notFoundMessage = "Unable to locate record with ID: " + uuid;
            errorMessages.add(notFoundMessage);
            log.warn(notFoundMessage);
        } else {
            if (certificateType.equals(PLATFORMCREDENTIAL)) {
                PlatformCredential platformCertificate = (PlatformCredential) certificate;
                if (platformCertificate.isPlatformBase()) {
                    // only do this if the base is being deleted.
                    List<PlatformCredential> sharedCertificates = getPlatformCertificateByBoardSN(
                            platformCertificate.getPlatformSerial());

                    for (PlatformCredential pc : sharedCertificates) {
                        if (!pc.isPlatformBase()) {
                            pc.archive("User requested deletion via UI of the base certificate");
                            certificateRepository.save(pc);
                            deleteComponentResults(pc.getPlatformSerial());
                        }
                    }
                }
                deleteComponentResults(platformCertificate.getPlatformSerial());
            }

            certificate.archive("User requested deletion via UI");
            certificateRepository.save(certificate);

            String deleteCompletedMessage = "Certificate successfully deleted";
            successMessages.add(deleteCompletedMessage);
            log.info(deleteCompletedMessage);
        }
    }

    /**
     * Gets the certificate by the hash code of its bytes. Looks for both
     * archived and unarchived certificates.
     *
     * @param certificateType String containing the certificate type
     * @param certificateHash the hash of the certificate's bytes
     * @return the certificate or null if none is found
     */
    private Certificate getCertificateByHash(
            final String certificateType,
            final int certificateHash) {
        return switch (certificateType) {
            case PLATFORMCREDENTIAL -> this.certificateRepository
                    .findByCertificateHash(certificateHash,
                            "PlatformCredential");
            case ENDORSEMENTCREDENTIAL -> this.certificateRepository
                    .findByCertificateHash(certificateHash,
                            "EndorsementCredential");
            case TRUSTCHAIN -> this.certificateRepository
                    .findByCertificateHash(certificateHash,
                            "CertificateAuthorityCredential");
            case IDEVIDCERTIFICATE -> this.certificateRepository
                    .findByCertificateHash(certificateHash,
                            "IDevIDCertificate");
            default -> null;
        };
    }

    /**
     * Gets the certificate by the platform serial number.
     *
     * @param serialNumber the platform serial number
     * @return the certificate or null if none is found
     */
    private List<PlatformCredential> getPlatformCertificateByBoardSN(
            final String serialNumber) {
        List<PlatformCredential> associatedCertificates = new ArrayList<>();

        if (serialNumber != null) {
            associatedCertificates.addAll(this.certificateRepository.byBoardSerialNumber(serialNumber));
        }

        return associatedCertificates;
    }

    /**
     * Helper method that utilizes the components of the provided platform certificate to generate
     * a collection of component results and subsequently stores these results in the database.
     *
     * @param certificate certificate
     */
    private void parseAndSaveComponentResults(final Certificate certificate) throws IOException {
        PlatformCredential platformCredential;

        if (certificate instanceof PlatformCredential) {
            platformCredential = (PlatformCredential) certificate;
            List<ComponentResult> componentResults = componentResultRepository
                    .findByCertificateSerialNumberAndBoardSerialNumber(
                            platformCredential.getSerialNumber().toString(),
                            platformCredential.getPlatformSerial());

            if (componentResults.isEmpty()) {
                ComponentResult componentResult;

                if (platformCredential.getPlatformConfigurationV1() != null) {

                    List<ComponentIdentifier> componentIdentifiers =
                            platformCredential.getComponentIdentifiers();

                    for (ComponentIdentifier componentIdentifier : componentIdentifiers) {
                        componentResult = new ComponentResult(platformCredential.getPlatformSerial(),
                                platformCredential.getSerialNumber().toString(),
                                platformCredential.getPlatformChainType(),
                                componentIdentifier);
                        componentResult.setFailedValidation(false);
                        componentResult.setDelta(!platformCredential.isPlatformBase());
                        componentResultRepository.save(componentResult);
                    }
                } else if (platformCredential.getPlatformConfigurationV2() != null) {

                    List<ComponentIdentifierV2> componentIdentifiersV2 =
                            platformCredential.getComponentIdentifiersV2();

                    for (ComponentIdentifierV2 componentIdentifierV2 : componentIdentifiersV2) {
                        componentResult = new ComponentResult(platformCredential.getPlatformSerial(),
                                platformCredential.getSerialNumber().toString(),
                                platformCredential.getPlatformChainType(),
                                componentIdentifierV2);
                        componentResult.setFailedValidation(false);
                        componentResult.setDelta(!platformCredential.isPlatformBase());
                        componentResultRepository.save(componentResult);
                    }
                }
            } else {
                for (ComponentResult componentResult : componentResults) {
                    componentResult.restore();
                    componentResult.resetCreateTime();
                    componentResultRepository.save(componentResult);
                }
            }
        }
    }


    /**
     * Helper method that deletes component results based on the provided platform serial number.
     *
     * @param platformSerial platform serial number
     */
    private void deleteComponentResults(final String platformSerial) {
        List<ComponentResult> componentResults = componentResultRepository
                .findByBoardSerialNumber(platformSerial);

        for (ComponentResult componentResult : componentResults) {
            componentResult.archive();
            componentResultRepository.save(componentResult);
        }
    }

}
