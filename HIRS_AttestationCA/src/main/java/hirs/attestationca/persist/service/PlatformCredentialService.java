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
 *
 */
@Log4j2
@Service
public class PlatformCredentialService {

    private final PlatformCertificateRepository platformCertificateRepository;
    private final EndorsementCredentialRepository endorsementCredentialRepository;

    @Autowired
    public PlatformCredentialService(final PlatformCertificateRepository platformCertificateRepository,
                                     final EndorsementCredentialRepository endorsementCredentialRepository) {
        this.platformCertificateRepository = platformCertificateRepository;
        this.endorsementCredentialRepository = endorsementCredentialRepository;
    }

    /**
     * @param archiveFlag
     * @param pageable
     * @return
     */
    public Page<PlatformCredential> findByArchiveFlag(boolean archiveFlag, Pageable pageable) {
        return this.platformCertificateRepository.findByArchiveFlag(archiveFlag, pageable);
    }

    /**
     * @param holderSerialNumber
     * @return
     */
    public EndorsementCredential findECBySerialNumber(BigInteger holderSerialNumber) {
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
     * @param file     file
     * @param messages page messages
     * @return platform credential
     */
    public PlatformCredential parsePlatformCredential(final MultipartFile file,
                                                      List<String> errorMessages
    ) {
        log.info("Received platform credential file of size: {}", file.getSize());

        byte[] fileBytes;
        String fileName = file.getOriginalFilename();

        // attempt to retrieve file bytes from the provided file
        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to read uploaded platform credential file (%s): ", fileName);
            log.error(failMessage, ioEx);
            errorMessages.add(failMessage + ioEx.getMessage());
            return null;
        }

        // attempt to build the platform credential from the uploaded bytes
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
