package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.util.CredentialHelper;
import hirs.attestationca.persist.util.DownloadFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.DecoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A service layer class responsible for encapsulating all business logic related to all the Certificate
 * pages.
 */
@Log4j2
@Service
public class CertificatePageService {
    private final CertificateRepository certificateRepository;
    private final ComponentResultRepository componentResultRepository;
    private final EntityManager entityManager;

    /**
     * Constructor for the Certificate Service.
     *
     * @param certificateRepository     certificate repository
     * @param componentResultRepository component result repository
     * @param entityManager             entity manager
     */
    @Autowired
    public CertificatePageService(final CertificateRepository certificateRepository,
                                  final ComponentResultRepository componentResultRepository,
                                  final EntityManager entityManager) {
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
        this.entityManager = entityManager;
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * certificates whose field values matches the provided search term.
     *
     * @param entityClass       generic certificate entity class
     * @param searchableColumns list of the searchable column names
     * @param searchTerm        text that was input in the search textbox
     * @param archiveFlag       archive flag
     * @param pageable          pageable
     * @param <T>               generic entity class that extends from certificate
     * @return page full of the generic certificates.
     */
    public <T extends Certificate> Page<T> findCertificatesBySearchableColumnsAndArchiveFlag(
            final Class<T> entityClass,
            final Set<String> searchableColumns,
            final String searchTerm,
            final boolean archiveFlag,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
        Root<T> rootCertificate = query.from(entityClass);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchTerm)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {
                Predicate predicate =
                        criteriaBuilder.like(criteriaBuilder.lower(rootCertificate.get(columnName)),
                                "%" + searchTerm.toLowerCase() + "%");
                predicates.add(predicate);
            }
        }

        Predicate likeConditions = criteriaBuilder.or(predicates.toArray(new Predicate[0]));

        // Add archiveFlag condition if specified
        query.where(criteriaBuilder.and(likeConditions,
                criteriaBuilder.equal(rootCertificate.get("archiveFlag"), archiveFlag)));

        // Apply pagination
        TypedQuery<T> typedQuery = this.entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<T> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }

    /**
     * Attempts to find a certificate whose uuid matches the provided uuid.
     *
     * @param uuid certificate uuid
     * @return certificate
     */
    public Certificate findCertificate(final UUID uuid) {
        return this.certificateRepository.getCertificate(uuid);
    }

    /**
     * Stores the given certificate in the database.
     *
     * @param certificateType String containing the certificate type
     * @param fileName        contain the name of the file of the certificate to
     *                        be stored
     * @param successMessages contains any success messages that will be displayed on the page
     * @param errorMessages   contains any error messages that will be displayed on the page
     * @param certificate     the certificate to store
     */
    public void storeCertificate(final CertificateType certificateType,
                                 final String fileName,
                                 final List<String> successMessages,
                                 final List<String> errorMessages,
                                 final Certificate certificate) {
        Certificate existingCertificate;

        // look for an identical certificate in the database
        try {
            existingCertificate =
                    this.certificateRepository.findByCertificateHashAndDType(certificate.getCertificateHash(),
                            certificateType.getCertificateTypeName());
        } catch (Exception exception) {
            final String failMessage = "Querying for existing certificate failed (" + fileName + "): ";
            errorMessages.add(failMessage + exception.getMessage());
            log.error(failMessage, exception);
            return;
        }

        // save the new certificate if no match is found
        if (existingCertificate == null) {
            try {
                if (certificate instanceof PlatformCredential platformCertificate) {
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
                    parseAndSaveComponentResults(platformCertificate);
                }
                this.certificateRepository.save(certificate);

                final String successMsg
                        = String.format("New certificate successfully uploaded (%s): ", fileName);
                successMessages.add(successMsg);
                log.info(successMsg);
                return;
            } catch (Exception exception) {
                final String failMessage = String.format("Storing new certificate failed (%s): ",
                        fileName);
                errorMessages.add(failMessage + exception.getMessage());
                log.error(failMessage, exception);
                return;
            }
        }

        // if an identical certificate is archived, update the existing certificate to
        // unarchive it and change the creation date
        if (existingCertificate.isArchived()) {
            try {
                existingCertificate.restore();
                existingCertificate.resetCreateTime();
                this.certificateRepository.save(existingCertificate);

                if (existingCertificate instanceof PlatformCredential existingPlatformCredential) {
                    List<ComponentResult> componentResults = this.componentResultRepository
                            .findByBoardSerialNumber(existingPlatformCredential
                                    .getPlatformSerial());
                    for (ComponentResult componentResult : componentResults) {
                        componentResult.restore();
                        componentResult.resetCreateTime();
                        this.componentResultRepository.save(componentResult);
                    }
                }

                final String successMsg = String.format("Pre-existing certificate "
                        + "found and unarchived (%s): ", fileName);
                successMessages.add(successMsg);
                log.info(successMsg);
                return;
            } catch (Exception exception) {
                final String failMessage = String.format("Found an identical"
                        + " pre-existing certificate in the "
                        + "archive, but failed to unarchive it (%s): ", fileName);
                errorMessages.add(failMessage + exception.getMessage());
                log.error(failMessage, exception);
                return;
            }
        }

        // if an identical certificate is already unarchived, do nothing and show a fail message
        final String failMessage = String.format("Storing certificate failed: an identical"
                + " certificate already exists (%s): ", fileName);
        errorMessages.add(failMessage);
        log.error(failMessage);
    }

    /**
     * Soft deletes the provided certificate from the database.
     *
     * @param uuid            the UUID of the cert to delete
     * @param successMessages contains any success messages that will be displayed on the page
     * @param errorMessages   contains any error messages that will be displayed on the page
     */
    public void deleteCertificate(final UUID uuid,
                                  final List<String> successMessages,
                                  final List<String> errorMessages) {
        Certificate certificate = findCertificate(uuid);

        if (certificate == null) {
            // Use the term "record" here to avoid user confusion b/t cert and cred
            String notFoundMessage = "Unable to locate record with ID: " + uuid;
            errorMessages.add(notFoundMessage);
            log.warn(notFoundMessage);
            throw new EntityNotFoundException(notFoundMessage);
        }

        if (certificate instanceof PlatformCredential platformCertificate) {
            if (platformCertificate.isPlatformBase()) {
                // only do this if the base is being deleted.
                List<PlatformCredential> sharedCertificates = getPlatformCertificateByBoardSN(
                        platformCertificate.getPlatformSerial());

                for (PlatformCredential pc : sharedCertificates) {
                    if (!pc.isPlatformBase()) {
                        pc.archive("User requested deletion via UI of the base certificate");
                        this.certificateRepository.save(pc);
                        deleteComponentResults(pc.getPlatformSerial());
                    }
                }
            }
            deleteComponentResults(platformCertificate.getPlatformSerial());
        }

        certificate.archive("User requested deletion via UI");
        this.certificateRepository.save(certificate);

        final String deleteCompletedMessage = "Certificate successfully deleted";
        successMessages.add(deleteCompletedMessage);
        log.info(deleteCompletedMessage);
    }

    /**
     * Packages a collection of certificates into a zip file for download.
     *
     * @param zipOut          zip outputs stream
     * @param singleFileName  zip file name
     * @param certificateType certificate type
     * @throws IOException if there are any issues packaging or downloading the zip file
     */
    public void bulkDownloadCertificates(final ZipOutputStream zipOut,
                                         final CertificateType certificateType,
                                         final String singleFileName) throws IOException {
        String zipFileName;

        final List<Certificate> certificates =
                this.certificateRepository.findByType(certificateType.getCertificateTypeName());

        for (Certificate certificate : certificates) {
            zipFileName = String.format("%s[%s].cer", singleFileName,
                    Integer.toHexString(certificate.getCertificateHash()));
            ZipEntry zipEntry = new ZipEntry(zipFileName);
            zipEntry.setSize((long) certificate.getRawBytes().length * Byte.SIZE);
            zipEntry.setTime(System.currentTimeMillis());
            zipOut.putNextEntry(zipEntry);
            StreamUtils.copy(certificate.getRawBytes(), zipOut);
            zipOut.closeEntry();
        }
        zipOut.finish();
    }

    /**
     * Retrieves a certificate from the database and prepares its contents for download.
     *
     * @param certificateClass generic certificate class
     * @param uuid             certificate uuid
     * @param <T>              certificate type
     * @return download file of a certificate
     */
    public <T extends Certificate> DownloadFile downloadCertificate(final Class<T> certificateClass,
                                                                    final UUID uuid) {
        Certificate certificate = this.findCertificate(uuid);

        if (certificate == null) {
            final String errorMessage =
                    "Unable to locate " + certificateClass.getSimpleName() + " record with ID " + uuid;
            log.warn(errorMessage);
            throw new EntityNotFoundException(errorMessage);
        } else if (!certificateClass.isInstance(certificate)) {
            final String errorMessage =
                    "Unable to cast the found certificate to a(n) " + certificateClass.getSimpleName() + " object";
            log.warn(errorMessage);
            throw new ClassCastException(errorMessage);
        }

        final T typeCertificate = certificateClass.cast(certificate);

        final String fileName = "filename=\"" + certificateClass.getSimpleName()
                + "_"
                + typeCertificate.getSerialNumber()
                + ".cer\"";

        return new DownloadFile(fileName, typeCertificate.getRawBytes());
    }

    /**
     * Attempts to parse the provided file in order to create a trust chain certificate.
     *
     * @param file            file
     * @param successMessages contains any success messages that will be displayed on the pages
     * @param errorMessages   contains any error messages that will be displayed on the page
     * @return trust chain certificate
     */
    public CertificateAuthorityCredential parseTrustChainCertificate(final MultipartFile file,
                                                                     final List<String> successMessages,
                                                                     final List<String> errorMessages) {
        log.info("Received trust chain certificate file of size: {}", file.getSize());

        byte[] fileBytes;
        final String fileName = file.getOriginalFilename();

        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to read uploaded trust chain certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        }

        // attempt to build the trust chain certificates from the uploaded bytes
        try {
            if (CredentialHelper.isMultiPEM(new String(fileBytes, StandardCharsets.UTF_8))) {
                try (ByteArrayInputStream certInputStream = new ByteArrayInputStream(fileBytes)) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    Collection<? extends java.security.cert.Certificate> c = cf.generateCertificates(certInputStream);

                    for (java.security.cert.Certificate certificate : c) {
                        List<String> moreSuccessMessages = new ArrayList<>();
                        List<String> moreErrorMessages = new ArrayList<>();

                        this.storeCertificate(
                                CertificateType.TRUST_CHAIN,
                                file.getOriginalFilename(),
                                moreSuccessMessages,
                                moreErrorMessages,
                                new CertificateAuthorityCredential(
                                        certificate.getEncoded()));

                        successMessages.addAll(moreSuccessMessages);
                        errorMessages.addAll(moreErrorMessages);
                    }

                    // stop the main thread from saving/storing
                    return null;
                } catch (CertificateException e) {
                    throw new IOException("Cannot construct X509Certificate from the input stream",
                            e);
                }
            }
            return new CertificateAuthorityCredential(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded trust chain certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded trust chain certificate pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            errorMessages.add(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format("Trust chain certificate format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            errorMessages.add(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage = String.format(
                    "Unexpected object while parsing trust chain certificate %s ", fileName);
            log.error(failMessage, isEx);
            errorMessages.add(failMessage + isEx.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the platform certificate by the platform serial number.
     *
     * @param serialNumber the platform serial number
     * @return the certificate or null if none is found
     */
    private List<PlatformCredential> getPlatformCertificateByBoardSN(final String serialNumber) {
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
     * @param platformCredential certificate
     */
    private void parseAndSaveComponentResults(final PlatformCredential platformCredential) throws IOException {
        List<ComponentResult> componentResults = this.componentResultRepository
                .findByCertificateSerialNumberAndBoardSerialNumber(
                        platformCredential.getSerialNumber().toString(),
                        platformCredential.getPlatformSerial());

        if (!componentResults.isEmpty()) {
            for (ComponentResult componentResult : componentResults) {
                componentResult.restore();
                componentResult.resetCreateTime();
                this.componentResultRepository.save(componentResult);
            }
            return;
        }

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
                this.componentResultRepository.save(componentResult);
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
                this.componentResultRepository.save(componentResult);
            }
        }
    }


    /**
     * Helper method that deletes component results based on the provided platform serial number.
     *
     * @param platformSerial platform serial number
     */
    private void deleteComponentResults(final String platformSerial) {
        List<ComponentResult> componentResults = this.componentResultRepository
                .findByBoardSerialNumber(platformSerial);

        for (ComponentResult componentResult : componentResults) {
            componentResult.archive();
            this.componentResultRepository.save(componentResult);
        }
    }
}

