package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.service.selector.CertificateSelector;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CertificateService<T extends Certificate> {

    Certificate saveCertificate(Certificate certificate);

    <T extends Certificate> List<T> fetchCertificates(Class<T> classType);

    Certificate updateCertificate(Certificate certificate, UUID certificateId);

    Certificate updateCertificate(Certificate certificate);

    void deleteCertificate(Certificate certificate);

    <T extends Certificate> Set<T> get(CertificateSelector certificateSelector);
}
