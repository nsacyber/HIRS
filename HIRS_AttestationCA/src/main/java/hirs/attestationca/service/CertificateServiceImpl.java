package hirs.attestationca.service;

import hirs.FilteredRecordsList;
import hirs.attestationca.repository.CertificateRepository;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.IssuedAttestationCertificate;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.persist.CertificateSelector;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.OrderedQuery;
import hirs.persist.service.CertificateService;
import hirs.persist.service.DefaultService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A <code>CertificateServiceImpl</code> manages <code>Certificate</code>s. A
 * <code>CertificateServiceImpl</code> is used to store and manage certificates. It has
 * support for the basic create, read, update, and delete methods.
 */
@Service
public class CertificateServiceImpl extends DbServiceImpl<Certificate>
        implements DefaultService<Certificate>,
        CertificateService, OrderedQuery<Certificate> {

    private static final Logger LOGGER = LogManager.getLogger(CertificateServiceImpl.class);
    @Autowired
    private CertificateRepository certificateRepository;

    /**
     * Default constructor.
     * @param em entity manager for jpa hibernate events
     */
    public CertificateServiceImpl(final EntityManager em) {
    }

    @Override
    public Certificate saveCertificate(final Certificate certificate) {
        LOGGER.debug("Saving certificate: {}", certificate);

        return getRetryTemplate().execute(new RetryCallback<Certificate,
                DBManagerException>() {
            @Override
            public Certificate doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return certificateRepository.save(certificate);
            }
        });
    }

    @Override
    public Certificate updateCertificate(final Certificate certificate,
                                         final UUID uuid) {
        LOGGER.debug("Updating certificate: {}", certificate);
        Certificate dbCertificate;

        if (uuid == null) {
            LOGGER.debug("Certificate not found: {}", certificate);
            dbCertificate = certificate;
        } else {
            // will not return null, throws and exception
            dbCertificate = certificateRepository.getReferenceById(uuid);

            // run through things that aren't equal and update

            getCertificateClass(dbCertificate); // need to coming

        }

        return saveCertificate(dbCertificate);
    }

    @Override
    public <T extends Certificate> Set<T> getCertificate(
            final CertificateSelector certificateSelector) {
        return new HashSet<>(0);
    }

    @Override
    public List<Certificate> getList() {
        LOGGER.debug("Getting all certificates...");

        return getRetryTemplate().execute(new RetryCallback<List<Certificate>,
                DBManagerException>() {
            @Override
            public List<Certificate> doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return certificateRepository.findAll();
            }
        });
    }

    @Override
    public void updateElements(final List<Certificate> certificates) {
        LOGGER.debug("Updating {} certificates...", certificates.size());

        certificates.stream().forEach((certificate) -> {
            if (certificate != null) {
                this.updateCertificate(certificate, certificate.getId());
            }
        });
        certificateRepository.flush();
    }

    @Override
    public void deleteObjectById(final UUID uuid) {
        LOGGER.debug("Deleting certificate by id: {}", uuid);

        getRetryTemplate().execute(new RetryCallback<Void,
                DBManagerException>() {
            @Override
            public Void doWithRetry(final RetryContext context)
                    throws DBManagerException {
                certificateRepository.deleteById(uuid);
                certificateRepository.flush();
                return null;
            }
        });
    }

    @Override
    public FilteredRecordsList getOrderedList(
            final Class<Certificate> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns)
            throws DBManagerException {
        return null;
    }

    @Override
    public FilteredRecordsList<Certificate> getOrderedList(
            final Class<Certificate> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns,
            final CriteriaModifier criteriaModifier)
            throws DBManagerException {
        return null;
    }

    /**
     * Gets the concrete certificate class type to query for.
     *
     * @param certificate the instance of the certificate to get type.
     * @return the certificate class type
     */
    private Class<? extends Certificate> getCertificateClass(final Certificate certificate) {
        if (certificate instanceof PlatformCredential) {
            return PlatformCredential.class;
        } else if (certificate instanceof EndorsementCredential) {
            return EndorsementCredential.class;
        } else if (certificate instanceof CertificateAuthorityCredential) {
            return CertificateAuthorityCredential.class;
        } else if (certificate instanceof IssuedAttestationCertificate) {
            return IssuedAttestationCertificate.class;
        } else {
            return null;
        }
    }
}
