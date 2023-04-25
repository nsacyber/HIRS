package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.service.selector.CertificateSelector;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CertificateServiceImpl<T extends Certificate> extends DefaultDbService<Certificate> implements CertificateService<Certificate> {

    @Autowired(required = false)
    private EntityManager entityManager;

    @Autowired
    private CertificateRepository repository;

    @Override
    public Certificate saveCertificate(Certificate certificate) {
        return repository.save(certificate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Certificate> List<T> fetchCertificates(Class<T> classType) {
        return (List<T>) repository.findAll(Sort.sort(classType));
    }

    @Override
    public Certificate updateCertificate(Certificate certificate, UUID certificateId) {
        return saveCertificate(certificate);
    }

    @Override
    public Certificate updateCertificate(Certificate certificate) {
        return saveCertificate(certificate);
    }

    /**
     * This method does not need to be used directly as it is used by {@link CertificateSelector}'s
     * get* methods.  Regardless, it may be used to retrieve certificates by other code in this
     * package, given a configured CertificateSelector.
     *
     * Example:
     *
     * <pre>
     * {@code
     * CertificateSelector certSelector =
     *      new CertificateSelector(Certificate.Type.CERTIFICATE_AUTHORITY)
     *      .byIssuer("CN=Some certain issuer");
     *
     * Set<Certificate> certificates = certificateManager.get(certSelector);}
     * </pre>
     *
     * @param <T> the type of certificate that will be retrieved
     * @param certificateSelector a configured {@link CertificateSelector} to use for querying
     * @return the resulting set of Certificates, possibly empty
     */
    @SuppressWarnings("unchecked")
    public <T extends Certificate> Set<T> get(final CertificateSelector certificateSelector) {
//        return new HashSet<>(
//                (List<T>) getWithCriteria(
//                        certificateSelector.getCertificateClass(),
//                        Collections.singleton(certificateSelector.getCriterion())
//                )
//        );
        return null;
    }

    /**
     * Remove a certificate from the database.
     *
     * @param certificate the certificate to delete
     * @return true if deletion was successful, false otherwise
     */
    public void deleteCertificate(final Certificate certificate) {
        repository.delete(certificate);
    }
}
