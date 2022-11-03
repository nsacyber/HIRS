package hirs.attestationca.portal.page;

import hirs.attestationca.configuration.AttestationCertificateAuthorityConfiguration;
import hirs.attestationca.portal.config.BaseWebConfig;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Main Spring configuration class for the ACA Portal. Uses the Common page configuration,
 * as well as the ACA configuration for accessing the ACA certificate.
 */
@Import({ BaseWebConfig.class, AttestationCertificateAuthorityConfiguration.class })
public class PageConfiguration implements WebMvcConfigurer {

}
