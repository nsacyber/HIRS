package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.EndorsementCertificateRepository;
import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.DecoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * Service class responsible for encapsulating all business logic related to the Platform Certificates
 * Page.
 */
@Service
@Log4j2
public class PlatformCertificatePageService {
    private final PlatformCertificateRepository platformCertificateRepository;
    private final EndorsementCertificateRepository endorsementCertificateRepository;

    /**
     * Constructor for the Platform Certificate Page service.
     *
     * @param platformCertificateRepository    platform certificate repository
     * @param endorsementCertificateRepository endorsement certificate repository
     */
    @Autowired
    public PlatformCertificatePageService(final PlatformCertificateRepository platformCertificateRepository,
                                          final EndorsementCertificateRepository endorsementCertificateRepository) {
        this.platformCertificateRepository = platformCertificateRepository;
        this.endorsementCertificateRepository = endorsementCertificateRepository;
    }

    /**
     * Retrieves a page of platform certificates using the provided archive flag and pageable value.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return page of platform certificates
     */
    public Page<PlatformCredential> findPlatformCertificatesByArchiveFlag(final boolean archiveFlag,
                                                                          final Pageable pageable) {
        return platformCertificateRepository.findByArchiveFlag(archiveFlag, pageable);
    }

    /**
     * Retrieves an endorsement certificate using the provided holder serial number.
     *
     * @param holderSerialNumber big integer representation of the holder serial number
     * @return endorsement certificate
     */
    public EndorsementCredential findECBySerialNumber(final BigInteger holderSerialNumber) {
        return endorsementCertificateRepository.findBySerialNumber(holderSerialNumber);
    }

    /**
     * Retrieves the total number of records in the platform certificate repository.
     *
     * @return total number of records in the platform certificate repository.
     */
    public long findPlatformCertificateRepositoryCount() {
        return platformCertificateRepository.countByArchiveFlag(false);
    }

    /**
     * Attempts to parse the provided file in order to create a Platform Certificate.
     *
     * @param file          file
     * @param errorMessages contains any error messages that will be displayed on the page
     * @return platform certificate
     */
    public PlatformCredential parsePlatformCertificate(final MultipartFile file, final List<String> errorMessages) {
        log.info("Received platform certificate file of size: {}", file.getSize());

        byte[] fileBytes;
        final String fileName = file.getOriginalFilename();

        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage =
                    String.format("Failed to read uploaded platform certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        }

        try {
            return new PlatformCredential(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage =
                    String.format("Failed to parse uploaded platform certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage =
                    String.format("Failed to parse uploaded platform certificate pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            errorMessages.add(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format("Platform certificate format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            errorMessages.add(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage =
                    String.format("Unexpected object while parsing platform certificate %s ", fileName);
            log.error(failMessage, isEx);
            errorMessages.add(failMessage + isEx.getMessage());
            return null;
        }
    }
}
