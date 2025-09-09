package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.EndorsementCredentialRepository;
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
 * A service layer class responsible for encapsulating all business logic related to the Platform Credential
 * Page.
 */
@Log4j2
@Service
public class PlatformCredentialPageService {
    private final PlatformCertificateRepository platformCertificateRepository;
    private final EndorsementCredentialRepository endorsementCredentialRepository;

    /**
     * Constructor for the Platform Credential Page service.
     *
     * @param platformCertificateRepository   platform certificate repository
     * @param endorsementCredentialRepository endorsement credential repository
     */
    @Autowired
    public PlatformCredentialPageService(final PlatformCertificateRepository platformCertificateRepository,
                                         final EndorsementCredentialRepository endorsementCredentialRepository) {
        this.platformCertificateRepository = platformCertificateRepository;
        this.endorsementCredentialRepository = endorsementCredentialRepository;
    }

    /**
     * Retrieves a page of platform credentials using the provided archive flag and pageable value.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return page of platform credentials
     */
    public Page<PlatformCredential> findPlatformCredentialsByArchiveFlag(final boolean archiveFlag,
                                                                         final Pageable pageable) {
        return this.platformCertificateRepository.findByArchiveFlag(archiveFlag, pageable);
    }

    /**
     * Retrieves an endorsement credential using the provided holder serial number.
     *
     * @param holderSerialNumber big integer representation of the holder serial number
     * @return endorsement credential
     */
    public EndorsementCredential findECBySerialNumber(final BigInteger holderSerialNumber) {
        return this.endorsementCredentialRepository.findBySerialNumber(holderSerialNumber);
    }

    /**
     * Retrieves the total number of records in the platform credential repository.
     *
     * @return total number of records in the platform credential repository.
     */
    public long findPlatformCredentialRepositoryCount() {
        return this.platformCertificateRepository.findByArchiveFlag(false).size();
    }

    /**
     * Attempts to parse the provided file in order to create a Platform Credential.
     *
     * @param file          file
     * @param errorMessages error messages
     * @return platform credential
     */
    public PlatformCredential parsePlatformCredential(final MultipartFile file,
                                                      final List<String> errorMessages
    ) {
        log.info("Received platform credential file of size: {}", file.getSize());

        byte[] fileBytes;
        final String fileName = file.getOriginalFilename();

        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to read uploaded platform credential file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        }

        try {
            return new PlatformCredential(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded platform credential file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded platform credential pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            errorMessages.add(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format(
                    "Platform credential format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            errorMessages.add(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage = String.format(
                    "Unexpected object while parsing platform credential %s ", fileName);
            log.error(failMessage, isEx);
            errorMessages.add(failMessage + isEx.getMessage());
            return null;
        }
    }
}
