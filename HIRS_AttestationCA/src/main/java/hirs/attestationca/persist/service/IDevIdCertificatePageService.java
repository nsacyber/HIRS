package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.IDevIDCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
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
 * A service layer class responsible for encapsulating all business logic related to the IDevId Certificate
 * Page.
 */
@Log4j2
@Service
public class IDevIdCertificatePageService {

    private final IDevIDCertificateRepository iDevIDCertificateRepository;

    /**
     * @param iDevIDCertificateRepository idevid certificate repository
     */
    @Autowired
    public IDevIdCertificatePageService(final IDevIDCertificateRepository iDevIDCertificateRepository) {
        this.iDevIDCertificateRepository = iDevIDCertificateRepository;
    }

    /**
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return page of idevid certificates
     */
    public Page<IDevIDCertificate> findByArchiveFlag(final boolean archiveFlag, final Pageable pageable) {
        return this.iDevIDCertificateRepository.findByArchiveFlag(archiveFlag, pageable);
    }

    /**
     * Retrieves the total number of records in the idevid certificate repository.
     *
     * @return total number of records in the idevid certificate repository.
     */
    public long findIDevIdCertificateRepositoryCount() {
        return iDevIDCertificateRepository.findByArchiveFlag(false).size();
    }

    /**
     * Attempts to parse the provided file in order to create an IDevId Certificate.
     *
     * @param file          file
     * @param errorMessages error messages
     * @return IDevId certificate
     */
    public IDevIDCertificate parseIDevIDCertificate(final MultipartFile file,
                                                    final List<String> errorMessages) {
        log.info("Received IDevId certificate file of size: {}", file.getSize());

        byte[] fileBytes;
        String fileName = file.getOriginalFilename();

        // attempt to retrieve file bytes from the provided file
        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to read uploaded IDevId certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        }

        // attempt to build the IDevId certificate from the uploaded bytes
        try {
            return new IDevIDCertificate(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded IDevId certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded IDevId certificate pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            errorMessages.add(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format(
                    "IDevId certificate format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            errorMessages.add(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage = String.format(
                    "Unexpected object while parsing IDevId certificate %s ", fileName);
            log.error(failMessage, isEx);
            errorMessages.add(failMessage + isEx.getMessage());
            return null;
        }
    }
}
