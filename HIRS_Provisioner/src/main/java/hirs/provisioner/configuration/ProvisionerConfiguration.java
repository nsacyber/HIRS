package hirs.provisioner.configuration;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import hirs.structs.converters.SimpleStructConverter;
import hirs.structs.converters.StructConverter;
import hirs.tpm.tss.Tpm;
import hirs.tpm.tss.command.CommandTpm;
import hirs.utils.LogConfigurationUtil;

/**
 * Spring Java configuration file that prepares beans for the provisioning application. This
 * provides an alternative implementation of the traditional Spring XML application context files.
 */
@Configuration
@PropertySources({
        @PropertySource(value = "classpath:defaults.properties"),

        // detects if file exists, if not, ignore errors
        @PropertySource(value = "file:/etc/hirs/provisioner/provisioner.properties",
                ignoreResourceNotFound = true)
})
@ComponentScan("hirs.provisioner")
public class ProvisionerConfiguration {

    /**
     * Path to the provisioner properties overrides. By default, these properties mirror those that
     * are in the classpath. These properties provide a means for the client to customize the
     * provisioner
     */
    public static final Path PROVISIONER_PROPERTIES_OVERRIDES =
            Paths.get("/etc/hirs/provisioner/provisioner.properties");

    /**
     * @return bean to resolve injected annotation.Value
     * property expressions for beans.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * Initialization of the ClientProvisioner. Detects environment and runs configuration methods
     * as required. This method is intended to be invoked by the Spring application context.
     */
    @PostConstruct
    final void initialize() {
        try {
            LogConfigurationUtil.applyConfiguration();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // if the properties overrides file doesn't exist (or directories), copy it over. capture
        // any exception while doing so and log a warning.
        if (!Files.exists(PROVISIONER_PROPERTIES_OVERRIDES)) {
            try {
                // obtain the embedded properties file
                ClassPathResource properties = new ClassPathResource("defaults.properties");

                // create the directories if they do not exist
                Files.createDirectories(PROVISIONER_PROPERTIES_OVERRIDES.getParent());

                // copy the defaults properties to the overrides location.
                Files.copy(properties.getInputStream(), PROVISIONER_PROPERTIES_OVERRIDES);

            } catch (IOException e) {
                throw new BeanInitializationException(
                        "Encountered error while initializing ClientProvisioner configuration: "
                                + e.getMessage(), e);
            }
        }
    }

    /**
     * @return configured ready to use RestTemplate.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * @return configured ready to use {@link Tpm}
     */
    @Bean
    public Tpm tpm() {
        return new CommandTpm();
    }

    /**
     * @return configured ready to use {@link StructConverter}
     */
    @Bean
    public StructConverter structConverter() {
        return new SimpleStructConverter();
    }

}
