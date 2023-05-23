package hirs.attestationca.persist.service;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.service.selector.CertificateSelector;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Log4j2
@NoArgsConstructor
@Service
public class CertificateServiceImpl<T extends Certificate> extends DefaultDbService<T>  {

//    @PersistenceContext  // I'll need this if I want to make custom native calls
//    private EntityManager entityManager;

    @Autowired
    private CertificateRepository certificateRepository;

    /**
     * Default Constructor.
     */
    public CertificateServiceImpl(final Class<T> clazz) {
        super(clazz);
        this.defineRepository(certificateRepository);
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
     * Archives the named object and updates it in the database.
     *
     * @param id UUID of the object to archive
     * @return true if the object was successfully found and archived, false if the object was not
     * found
     * @throws hirs.attestationca.persist.DBManagerException if the object is not an instance of <code>ArchivableEntity</code>
     */
    public final boolean archive(final UUID id) throws DBManagerException {
        log.debug("archiving object: {}", id);
        if (id == null) {
            log.debug("null id argument");
            return false;
        }

        T target = get(id);
        if (target == null) {
            return false;
        }

        ((ArchivableEntity) target).archive();
        this.certificateRepository.save(target);
        return true;
    }
}
