package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.util.CredentialHelper;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.DecoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A service layer class responsible for encapsulating all business logic related to the Trust Chain
 * Certificates Management Page.
 */
@Log4j2
@Service
public class TrustChainCertificatePageService {

    private final CACredentialRepository caCredentialRepository;
    private final CertificateService certificateService;

    /**
     * @param caCredentialRepository certificate authority credential repository
     */
    @Autowired
    public TrustChainCertificatePageService(final CACredentialRepository caCredentialRepository,
                                            final CertificateService certificateService) {
        this.caCredentialRepository = caCredentialRepository;
        this.certificateService = certificateService;
    }

    /**
     * Retrieves the total number of records in the certificate authority (trust chain) repository.
     *
     * @return total number of records in the certificate authority (trust chain) repository.
     */
    public long findTrustChainCertificateRepoCount() {
        return this.caCredentialRepository.findByArchiveFlag(false).size();
    }

    /**
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return page of certificate authority credentials
     */
    public Page<CertificateAuthorityCredential> findByArchiveFlag(final boolean archiveFlag,
                                                                  final Pageable pageable) {
        return this.caCredentialRepository.findByArchiveFlag(archiveFlag, pageable);
    }

    /**
     * Attempts to parse the provided file in order to create a trust chain certificate.
     *
     * @param file          file
     * @param errorMessages error messages
     * @return trust chain certificate
     */
    public CertificateAuthorityCredential parseTrustChainCertificate(final MultipartFile file,
                                                                     final List<String> successMessages,
                                                                     final List<String> errorMessages) {
        log.info("Received trust chain certificate file of size: {}", file.getSize());

        byte[] fileBytes;
        String fileName = file.getOriginalFilename();

        // attempt to retrieve file bytes from the provided file
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
                    Collection<? extends Certificate> c =
                            cf.generateCertificates(certInputStream);

                    for (java.security.cert.Certificate certificate : c) {
                        List<String> moreSuccessMessages = new ArrayList<>();
                        List<String> moreErrorMessages = new ArrayList<>();

                        this.certificateService.storeCertificate(
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
            final String failMessage = String.format(
                    "Trust chain certificate format not recognized(%s): ", fileName);
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
}
