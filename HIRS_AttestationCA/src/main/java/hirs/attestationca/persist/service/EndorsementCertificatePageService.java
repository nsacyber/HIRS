package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.EndorsementCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.DecoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service class responsible for encapsulating all business logic related to the Endorsement Certificates Page.
 */
@Service
@Log4j2
public class EndorsementCertificatePageService {
    private final EndorsementCertificateRepository endorsementCertificateRepository;

    /**
     * Constructor for the Endorsement Certificate Page Service.
     *
     * @param endorsementCertificateRepository endorsement certificate repository
     */
    @Autowired
    public EndorsementCertificatePageService(final EndorsementCertificateRepository endorsementCertificateRepository) {
        this.endorsementCertificateRepository = endorsementCertificateRepository;
    }

    /**
     * Retrieves a page of {@link EndorsementCredential} objects using the provided archive flag and pageable value.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return page of {@link EndorsementCredential} objects
     */
    public Page<EndorsementCredential> findEndorsementCertificatesByArchiveFlag(final boolean archiveFlag,
                                                                                final Pageable pageable) {
        return endorsementCertificateRepository.findByArchiveFlag(archiveFlag, pageable);
    }

    /**
     * Retrieves the total number of records stored in the {@link EndorsementCertificateRepository}.
     *
     * @return total number of records stored in the {@link EndorsementCertificateRepository}.
     */
    public long findEndorsementCertificateRepositoryCount() {
        return endorsementCertificateRepository.countByArchiveFlag(false);
    }

    /**
     * Attempts to parse the provided file in order to create an {@link EndorsementCredential} object.
     *
     * @param file          file
     * @param errorMessages contains any error messages that will be displayed on the page
     * @return an {@link EndorsementCredential} object
     */
    public EndorsementCredential parseEndorsementCertificate(final MultipartFile file,
                                                             final List<String> errorMessages) {
        log.info("Received endorsement certificate file of size: {}", file.getSize());

        byte[] fileBytes;
        final String fileName = file.getOriginalFilename();
        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage =
                    String.format("Failed to read uploaded endorsement certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        }
        try {
            return new EndorsementCredential(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage =
                    String.format("Failed to parse uploaded endorsement certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage =
                    String.format("Failed to parse uploaded endorsement certificate PEM file (%s): ", fileName);
            log.error(failMessage, dEx);
            errorMessages.add(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format("Endorsement certificate format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            errorMessages.add(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage =
                    String.format("Unexpected object while parsing endorsement certificate %s ", fileName);
            log.error(failMessage, isEx);
            errorMessages.add(failMessage + isEx.getMessage());
            return null;
        }
    }
}
