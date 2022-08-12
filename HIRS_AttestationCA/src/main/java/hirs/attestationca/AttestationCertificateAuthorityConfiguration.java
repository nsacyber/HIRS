package hirs.attestationca;

import hirs.structs.converters.SimpleStructConverter;
import hirs.structs.converters.StructConverter;
import hirs.utils.LogConfigurationUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides application context configuration for the Attestation Certificate
 * Authority application. The properties are processed in order and as such, the
 * last property file read in will override properties that may had already been
 * defined previously. In other words, the 'defaults.properties' file provides a
 * basic standard of properties that can be overrode by the
 */
@Configuration
@PropertySources({
    @PropertySource(value = "classpath:defaults.properties"),
        @PropertySource(value = "classpath:persistence.properties"),

    // detects if file exists, if not, ignore errors
    @PropertySource(value = "file:/etc/hirs/aca/aca.properties",
            ignoreResourceNotFound = true)
})
@EnableTransactionManagement
@ComponentScan({ "hirs.attestationca", "hirs.attestationca.service", "hirs.attestationca.rest",
    "hirs.validation", "hirs.data.service", "hirsattestationca.configuration" })
@EnableWebMvc
public class AttestationCertificateAuthorityConfiguration implements WebMvcConfigurer {

    private static final Logger LOG
            = LogManager.getLogger(AttestationCertificateAuthorityConfiguration.class);

    static {
        try {
            LogConfigurationUtil.applyConfiguration();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private Environment environment;

    /**
     * Creates a JPA transaction manager.
     * @return instance of the manager
     */
    @Bean
    public JpaTransactionManager jpaTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactoryBean().getObject());
        return transactionManager;
    }

    /**
     * @return bean to resolve injected annotation.Value property expressions
     * for beans.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
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
        Path certificatesPath = Paths.get(
                environment.getRequiredProperty("aca.directories.certificates"));

        // create base directories if they do not exist
        try {
            Files.createDirectories(certificatesPath);
        } catch (IOException e) {
            throw new BeanInitializationException(
                    "Encountered error while initializing ACA directories: " + e.getMessage(), e);
        }

        // create the ACA key store if it doesn't exist
        Path keyStorePath = Paths.get(environment.getRequiredProperty("aca.keyStore.location"));
        if (!Files.exists(keyStorePath)) {
            throw new IllegalStateException(
                    String.format("ACA Key Store not found at %s. Consult the HIRS User "
                                    + "Guide for ACA installation instructions.",
                            environment.getRequiredProperty("aca.keyStore.location")));
        }
    }

    private HibernateJpaVendorAdapter vendorAdaptor() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(true);
        return vendorAdapter;
    }

    /**
     * Configures a session factory bean that in turn configures the hibernate session factory.
     * Enables auto scanning of annotations such that entities do not need to be registered in a
     * hibernate configuration file.
     *
     * @return Entity Manager
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
         LocalContainerEntityManagerFactoryBean entityManagerFactoryBean
                 = new LocalContainerEntityManagerFactoryBean();
         entityManagerFactoryBean.setJpaVendorAdapter(vendorAdaptor());
         entityManagerFactoryBean.setDataSource(dataSource());
         entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
         entityManagerFactoryBean.setPackagesToScan("hirs");
         entityManagerFactoryBean.setJpaProperties(hibernateProperties());

         return entityManagerFactoryBean;
     }

    private Map<String, String> getSettings() {
        Map<String, String> settings = new HashMap<>();
        settings.put("connection.driver_class",
                environment.getRequiredProperty("persistence.db.driverClass"));
        settings.put("dialect", environment.getRequiredProperty("persistence.hibernate.dialect"));
        settings.put("hibernate.connection.url",
                environment.getRequiredProperty("persistence.db.url"));
        settings.put("hibernate.connection.username",
                environment.getRequiredProperty("persistence.db.username"));
        settings.put("hibernate.connection.password",
                environment.getRequiredProperty("persistence.db.password"));
//        settings.put("hibernate.current_session_context_class", );
        settings.put("hibernate.show_sql", environment.getRequiredProperty("hibernate.show_sql"));
        settings.put("hibernate.format_sql",
                environment.getRequiredProperty("hibernate.format_sql"));
        return settings;
    }

    /**
     * Configures the data source to be used by the hibernate session factory.
     *
     * @return configured data source
     */
    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(
                environment.getRequiredProperty("persistence.db.url"));
        dataSource.setUsername(
                environment.getRequiredProperty("persistence.db.username"));
        dataSource.setPassword(
                environment.getRequiredProperty("persistence.db.password"));
        dataSource.setDriverClassName(
                environment.getRequiredProperty("persistence.db.driverClass"));

//        dataSource.setMaximumPoolSize(Integer.parseInt(environment
//        .getRequiredProperty("persistence.db.maximumPoolSize"));
//        dataSource.setConnectionTimeout(Long.parseLong(environment.
//        getRequiredProperty("persistence.db.connectionTimeout"));
//        dataSource.setLeakDetectionThreshold(Long.parseLong(environment
//        .getRequiredProperty("persistence.db.leakDetectionThreshold"));

        return dataSource;
    }

    /**
     * Generates properties using configuration file that will be used to configure the session
     * factory.
     *
     * @return properties for hibernate session factory
     */
    @Bean
    public Properties hibernateProperties() {
        Properties properties = new Properties();
//        properties.put("hibernate.dialect",
//                environment.getRequiredProperty("persistence.hibernate.dialect"));
//        properties.put("hibernate.show_sql",
//                environment.getRequiredProperty("hibernate.show_sql"));
//        properties.put("hibernate.format_sql",
//                environment.getRequiredProperty("hibernate.format_sql"));
        properties.put("hibernate.hbm2ddl.auto",
                environment.getRequiredProperty("persistence.hibernate.ddl"));
//        properties.put("hibernate.current_session_context_class", "thread");
        return properties;
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
            PrivateKey acaKey = (PrivateKey) keyStore.getKey("aca.keyStore.alias",
                    environment.getRequiredProperty("aca.keyStore.password").toCharArray());

            // break early if the certificate is not available.
            if (acaKey == null) {
                throw new BeanInitializationException(String.format("Key with alias "
                        + "%s was not in KeyStore %s. Ensure that the KeyStore has the "
                        + "specified certificate. ",
                        environment.getRequiredProperty("aca.keyStore.alias"),
                        environment.getRequiredProperty("aca.keyStore.location")));
            }
            return acaKey;
        } catch (Exception e) {
            throw new BeanInitializationException("Encountered error loading ACA private key "
                    + "from key store: " + e.getMessage(), e);
        }
    }

    /**
     * Bean holding the maximum retry attempts for a DB transaction.
     * @return the maximum retry count
     */
    @Bean(name = "maxTransactionRetryAttempts")
    public int maxTransactionRetryAttempts() {
        return environment.getRequiredProperty("persistence.db.maxTransactionRetryAttempts",
                Integer.class);
    }

    /**
     * Bean holding the time to wait until retrying a failed transaction.
     * @return the wait time, in milliseconds
     */
    @Bean(name = "retryWaitTimeMilliseconds")
    public long retryWaitTimeMilliseconds() {
        return environment.getRequiredProperty("persistence.db.retryWaitTimeMilliseconds",
                Long.class);
    }

    /**
     * @return the {@link X509Certificate} of the ACA
     */
    @Bean
    public X509Certificate acaCertificate() {
        KeyStore keyStore = keyStore();

        try {
            X509Certificate acaCertificate = (X509Certificate) keyStore.getCertificate(
                    environment.getRequiredProperty("aca.keyStore.alias"));

            // break early if the certificate is not available.
            if (acaCertificate == null) {
                throw new BeanInitializationException(String.format("Certificate with alias "
                        + "%s was not in KeyStore %s. Ensure that the KeyStore has the "
                        + "specified certificate. ",
                        environment.getRequiredProperty("aca.keyStore.alias"),
                        environment.getRequiredProperty("aca.keyStore.location")));
            }

            return acaCertificate;
        } catch (KeyStoreException e) {
            throw new BeanInitializationException("Encountered error loading ACA certificate "
                    + "from key store: " + e.getMessage(), e);
        }
    }

    /**
     * @return the {@link java.security.KeyStore} that contains the certificates
     * for the ACA.
     */
    @Bean
    public KeyStore keyStore() {
        Path keyStorePath = Paths.get(environment.getRequiredProperty("aca.keyStore.location"));

        // attempt to open the key store. if that fails, log a meaningful message before failing.
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(Files.newInputStream(keyStorePath),
                    environment.getRequiredProperty("aca.keyStore.password").toCharArray());
            return keyStore;
        } catch (Exception e) {
            LOG.error(String.format(
                    "Encountered error while loading ACA key store. The most common issue is "
                    + "that configured password does not work on the configured key"
                    + " store %s.", keyStorePath));
            LOG.error(String.format("Exception message: %s", e.getMessage()));
            throw new BeanInitializationException(e.getMessage(), e);
        }
    }

    /**
     * Prototyped {@link StructConverter}. In other words, all instances
     * returned by this method will be configured identically, but subsequent
     * invocations will return a new instance.
     *
     * @return ready to use {@link StructConverter}.
     */
    @Bean
    @Scope("prototype")
    public static StructConverter structConverter() {
        return new SimpleStructConverter();
    }


}
