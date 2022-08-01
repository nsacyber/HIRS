package hirs.attestationca.service;

import hirs.FilteredRecordsList;
import hirs.attestationca.repository.CertificateRepository;
import hirs.data.persist.certificate.Certificate;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.OrderedQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A <code>CertificateServiceImpl</code> manages <code>Certificate</code>s. A
 * <code>CertificateServiceImpl</code> is used to store and manage certificates. It has
 * support for the basic create, read, update, and delete methods.
 */
@Service
public class CertificateServiceImpl implements DefaultService<Certificate>,
        CertificateService, OrderedQuery<Certificate> {

    private static final Logger LOGGER = LogManager.getLogger();
    @Autowired
    private CertificateRepository certificateRepository;

    @Override
    public Certificate saveCertificate(final Certificate certificate) {
        LOGGER.debug("Saving certificate: {}", certificate);
        return certificateRepository.save(certificate);
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

        }

        certificateRepository.save(dbCertificate);

        return dbCertificate;
    }

    @Override
    public List<Certificate> getList() {
        LOGGER.debug("Getting all certificates...");
        return this.certificateRepository.findAll();
    }

    @Override
    public void deleteObjectById(final UUID uuid) {
        LOGGER.debug("Deleting certificate by id: {}", uuid);
        this.certificateRepository.deleteById(uuid);
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
}
