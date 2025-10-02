package hirs.attestationca.portal;

import hirs.attestationca.portal.datatables.DataTableView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * Configuration file for the Page Controllers.
 */
@Configuration
@EnableWebMvc
@ComponentScan("hirs.attestationca.portal.page.controllers")
public class PageConfiguration {

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
}
