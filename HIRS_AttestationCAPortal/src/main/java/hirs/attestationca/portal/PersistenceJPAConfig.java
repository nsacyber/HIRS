package hirs.attestationca.portal;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * Provides application context configuration for the Attestation Certificate
 * Authority application. The properties are processed in order and as such, the
 * last property file read in will override properties that may had already been
 * defined previously. In other words, the 'defaults.properties' file provides a
 * basic standard of properties that can be overrode by the
 */
@Log4j2
@Configuration
@EnableWebMvc
@EnableTransactionManagement
@PropertySources({
        @PropertySource(value = "classpath:hibernate.properties"),

        // detects if file exists, if not, ignore errors
        @PropertySource(value = "file:/etc/hirs/aca/aca.properties"),
        @PropertySource(value = "file:/etc/hirs/aca/application.properties",
                ignoreResourceNotFound = true),
	@PropertySource(value = "file:C:/ProgramData/hirs/aca/application.win.properties",
                ignoreResourceNotFound = true)
})
@ComponentScan({"hirs.attestationca.portal", "hirs.attestationca.portal.page.controllers", "hirs.attestationca.persist", "hirs.attestationca.persist.entity", "hirs.attestationca.persist.service"})
@EnableJpaRepositories(basePackages = "hirs.attestationca.persist.entity.manager")
public class PersistenceJPAConfig implements WebMvcConfigurer {

//    @Value("${aca.directories.certificates}")
//    private String certificatesLocation;

    @Value("${server.ssl.key-store}")
    private String keyStoreLocation;

    @Value("${server.ssl.key-store-password:''}")
    private String keyStorePassword;

    @Value("${server.ssl.key-alias}")
    private String keyAlias;

    @Autowired
    private Environment environment;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        final LocalContainerEntityManagerFactoryBean entityManagerBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerBean.setDataSource(dataSource());
        entityManagerBean.setPackagesToScan("hirs.attestationca.persist.entity");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerBean.setJpaVendorAdapter(vendorAdapter);
        entityManagerBean.setJpaProperties(additionalProperties());

        return entityManagerBean;
    }

    @Bean
    public DataSource dataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(environment.getProperty("hibernate.connection.driver_class",
                "org.mariadb.jdbc.Driver"));
        dataSource.setUrl(environment.getProperty("hibernate.connection.url"));
        dataSource.setUsername(environment.getProperty("hibernate.connection.username"));
        dataSource.setPassword(environment.getProperty("hibernate.connection.password"));

        return dataSource;
    }

    /**
     * Initialization of the ACA. Detects environment and runs configuration
     * methods as required. This method is intended to be invoked by the Spring
     * application context.
     */
    @PostConstruct
    void initialize() {
        // ensure that Bouncy Castle is registered as a security provider
        Security.addProvider(new BouncyCastleProvider());

        // obtain path to ACA configuration
//        Path certificatesPath = Paths.get(certificatesLocation);
//
//        // create base directories if they do not exist
//        try {
//            Files.createDirectories(certificatesPath);
//        } catch (IOException ioEx) {
//            throw new BeanInitializationException(
//                    "Encountered error while initializing ACA directories: " + ioEx.getMessage(), ioEx);
//        }

        // create the ACA key store if it doesn't exist
//        Path keyStorePath = Paths.get(keyStoreLocation);
//        if (!Files.exists(keyStorePath)) {
//            throw new IllegalStateException(
//                    String.format("ACA Key Store not found at %s. Consult the HIRS User "
//                            + "Guide for ACA installation instructions.", keyStoreLocation));
//        }
    }

    /**
     * @return the {@link PrivateKey} of the ACA
     */
    @Bean
    public PrivateKey privateKey() {

        // obtain the key store
        KeyStore keyStore = keyStore();

        try {

            // load the key from the key store
            PrivateKey acaKey = (PrivateKey) keyStore.getKey(keyAlias,
                    keyStorePassword.toCharArray());

            // break early if the certificate is not available.
            if (acaKey == null) {
                throw new BeanInitializationException(String.format("Key with alias "
                        + "%s was not in KeyStore %s. Ensure that the KeyStore has the "
                        + "specified certificate. ", keyAlias, keyStoreLocation));
            }
            return acaKey;
        } catch (Exception ex) {
            throw new BeanInitializationException("Encountered error loading ACA private key "
                    + "from key store: " + ex.getMessage(), ex);
        }
    }

    /**
     * @return the {@link X509Certificate} of the ACA
     */
    @Bean
    public X509Certificate acaCertificate() {
        KeyStore keyStore = keyStore();

        try {
            X509Certificate acaCertificate = (X509Certificate) keyStore.getCertificate(keyAlias);

            // break early if the certificate is not available.
            if (acaCertificate == null) {
                throw new BeanInitializationException(String.format("Certificate with alias "
                        + "%s was not in KeyStore %s. Ensure that the KeyStore has the "
                        + "specified certificate. ", keyAlias, keyStoreLocation));
            }

            return acaCertificate;
        } catch (KeyStoreException ksEx) {
            throw new BeanInitializationException("Encountered error loading ACA certificate "
                    + "from key store: " + ksEx.getMessage(), ksEx);
        }
    }

    /**
     * @return the {@link java.security.KeyStore} that contains the certificates
     * for the ACA.
     */
    @Bean
    public KeyStore keyStore() {
        Path keyStorePath = Paths.get(keyStoreLocation);

        // attempt to open the key store. if that fails, log a meaningful message before failing.
        // empty
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(Files.newInputStream(keyStorePath), keyStorePassword.toCharArray());

            return keyStore;
        } catch (Exception ex) {
            log.error(String.format(
                    "Encountered error while loading ACA key store. The most common issue is "
                            + "that configured password does not work on the configured key"
                            + " store %s.", keyStorePath));
            log.error(String.format("Exception message: %s", ex.getMessage()));
            throw new BeanInitializationException(ex.getMessage(), ex);
        }
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    final Properties additionalProperties() {
        final Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto",
                environment.getProperty("hibernate.hbm2ddl.auto"));
        hibernateProperties.setProperty("hibernate.dialect",
                environment.getProperty("hibernate.dialect"));
        hibernateProperties.setProperty("hibernate.cache.use_second_level_cache",
                "false");

        return hibernateProperties;
    }

    /**
     * Creates a Spring Resolver for Multi-part form uploads. This is required
     * for spring controllers to be able to process Spring MultiPartFiles
     *
     * @return bean to handle multipart form requests
     */
    @Bean(name = "multipartResolver")
    public StandardServletMultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        return resolver;
    }

//    @Bean(name="default-settings")
//    public PolicySettings supplyChainSettings() {
//        PolicySettings scSettings = new PolicySettings("Default", "Settings are configured for no validation flags set.");
//
//        return scSettings;
//    }


    @Override
    public void configureDefaultServletHandling(final DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

}
