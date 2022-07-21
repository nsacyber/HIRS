package hirs.attestationca.portal.page;

import hirs.attestationca.AttestationCertificateAuthorityConfiguration;
import hirs.attestationca.portal.persistence.PersistenceConfiguration;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class AcaInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {


    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[] {
                AttestationCertificateAuthorityConfiguration.class
        };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[] {
                CommonPageConfiguration.class, AttestationCertificateAuthorityConfiguration.class
        };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] {
                "/"
        };
    }
}
