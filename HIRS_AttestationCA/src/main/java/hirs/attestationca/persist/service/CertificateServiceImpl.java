package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CertificateServiceImpl {
    @Autowired(required = false)
    private EntityManager entityManager;

    @Autowired
    private CertificateRepository repository;

    private void saveCertificate(Certificate certificate) {
        repository.save(certificate);
    }
}
