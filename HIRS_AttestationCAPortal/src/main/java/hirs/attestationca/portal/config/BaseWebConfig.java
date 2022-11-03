package hirs.attestationca.portal.config;

import hirs.attestationca.configuration.AttestationCertificateAuthorityConfiguration;
import hirs.attestationca.portal.datatables.DataTableView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

import java.nio.charset.StandardCharsets;

/**
 * Specifies the location to scan for page controllers, view resolver for JSON data, and view
 * resolver to map view names to jsp files.
 */
@Repository
@Configuration
@EnableTransactionManagement
@EnableWebMvc
@ComponentScan("hirs.attestationca.portal.page.controllers")
@Import({ AttestationCertificateAuthorityConfiguration.class })
public class BaseWebConfig implements WebMvcConfigurer {

    private static final String CLIENT_FILES_PATH = "file:/etc/hirs/aca/client-files/";

    /**
     * @return bean to resolve injected annotation.Value
     * property expressions for beans.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * Makes all URLs that end in "dataTable" use DataTableView to serialize DataTableResponse.
     *
     * @return ViewResolver that uses DataTableView.
     */
    @Bean
    public ViewResolver dataTableViewResolver() {
        UrlBasedViewResolver resolver = new UrlBasedViewResolver();
        resolver.setViewClass(DataTableView.class);
        resolver.setViewNames("*dataTable");
        resolver.setOrder(0);
        return resolver;
    }

    /**
     * Maps view names to the appropriate jsp file.
     *
     * Only seems to apply to GET requests.
     *
     * @return a ViewResolver bean containing the mapping.
     */
    @Bean
    public ViewResolver pageViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/jsp/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    /**
     * Creates a Spring Resolver for Multi-part form uploads. This is required
     * for spring controllers to be able to process Spring MultiPartFiles
     *
     * @return bean to handle multipart form requests
     */
    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return resolver;
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry resourceHandlerRegistry) {
        resourceHandlerRegistry.addResourceHandler("/client-files/**")
                .addResourceLocations(CLIENT_FILES_PATH);
    }

    @Override
    public void configureDefaultServletHandling(final DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
}
