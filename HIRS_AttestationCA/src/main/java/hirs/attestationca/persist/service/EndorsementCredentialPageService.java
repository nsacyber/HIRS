package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.EndorsementCredentialRepository;
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
 * A service layer class responsible for encapsulating all business logic related to the Endorsement
 * Credentials Page.
 */
@Log4j2
@Service
public class EndorsementCredentialPageService {

    private final EndorsementCredentialRepository endorsementCredentialRepository;

    /**
     * Constructor for the Endorsement Credential Page Service.
     *
     * @param endorsementCredentialRepository endorsement credential repository
     */
    @Autowired
    public EndorsementCredentialPageService(
            final EndorsementCredentialRepository endorsementCredentialRepository) {
        this.endorsementCredentialRepository = endorsementCredentialRepository;
    }

    /**
     * Retrieves a page of endorsement credentials using the provided archive flag and pageable value.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return page of endorsement credentials
     */
    public Page<EndorsementCredential> findEndorsementCredentialsByArchiveFlag(final boolean archiveFlag,
                                                                               final Pageable pageable) {
        return this.endorsementCredentialRepository.findByArchiveFlag(archiveFlag, pageable);
    }

    /**
     * Retrieves the total number of records in the endorsement credential repository.
     *
     * @return total number of records in the endorsement credential repository.
     */
    public long findEndorsementCredentialRepositoryCount() {
        return this.endorsementCredentialRepository.findByArchiveFlag(false).size();
    }

    /**
     * Attempts to parse the provided file in order to create an Endorsement Credential.
     *
     * @param file          file
     * @param errorMessages error messages
     * @return endorsement credential
     */
    public EndorsementCredential parseEndorsementCredential(final MultipartFile file,
                                                            final List<String> errorMessages) {
        log.info("Received endorsement credential file of size: {}", file.getSize());

        byte[] fileBytes;
        final String fileName = file.getOriginalFilename();

        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to read uploaded endorsement credential file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        }

        try {
            return new EndorsementCredential(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded endorsement credential file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded endorsement credential pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            errorMessages.add(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format(
                    "Endorsement credential format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            errorMessages.add(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage = String.format(
                    "Unexpected object while parsing endorsement credential %s ", fileName);
            log.error(failMessage, isEx);
            errorMessages.add(failMessage + isEx.getMessage());
            return null;
        }
    }
}
