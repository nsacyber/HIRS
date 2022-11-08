package hirs.attestationca.portal.page;

import hirs.attestationca.AttestationCertificateAuthorityConfiguration;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
/**
 * Simply holds a contextInitialized method which will be called when the web app starts.
 */
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
