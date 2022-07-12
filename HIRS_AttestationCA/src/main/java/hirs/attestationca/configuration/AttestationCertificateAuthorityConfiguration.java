package hirs.attestationca.configuration;

import hirs.attestationca.persist.DBDeviceGroupManager;
import hirs.attestationca.persist.DBDeviceManager;
import hirs.attestationca.persist.DBReferenceEventManager;
import hirs.attestationca.persist.DBReferenceManifestManager;
import hirs.persist.DeviceGroupManager;
import hirs.persist.DeviceManager;
import hirs.persist.ReferenceEventManager;
import hirs.persist.ReferenceManifestManager;
import hirs.structs.converters.SimpleStructConverter;
import hirs.structs.converters.StructConverter;
import hirs.utils.LogConfigurationUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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

    // detects if file exists, if not, ignore errors
    @PropertySource(value = "file:/etc/hirs/aca/aca.properties",
            ignoreResourceNotFound = true)
})
@ComponentScan({ "hirs.attestationca", "hirs.attestationca.service", "hirs.attestationca.rest",
    "hirs.validation", "hirs.data.service" })
@Import(PersistenceConfiguration.class)
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

    private static final String CLIENT_FILES_PATH = "file:/etc/hirs/aca/client-files/";

    @Value("${persistence.db.url}")
    private String url;

    @Value("${persistence.db.username}")
    private String username;

    @Value("${persistence.db.password}")
    private String password;

    @Value("${persistence.db.driverClass}")
    private String driverClass;

    @Value("${persistence.db.maximumPoolSize}")
    private String maximumPoolSize;

    @Value("${persistence.db.connectionTimeout}")
    private String connectionTimeout;

    @Value("${persistence.db.leakDetectionThreshold}")
    private String leakDetectionThreshold;

    @Value("${persistence.hibernate.dialect}")
    private String dialect;

    @Value("${persistence.hibernate.ddl}")
    private String ddl;

    @Value("${persistence.hibernate.contextClass}")
    private String contextClass;

    @Value("${persistence.hibernate.provider}")
    private String provider;

    @Value("${persistence.db.maxTransactionRetryAttempts}")
    private int maxTransactionRetryAttempts;

    @Value("${persistence.db.retryWaitTimeMilliseconds}")
    private long retryWaitTimeMilliseconds;

    @Value("${aca.directories.certificates}")
    private String certificatesLocation;

    @Value("${aca.keyStore.location}")
    private String keyStoreLocation;

    @Value("${aca.keyStore.password:''}")
    private String keyStorePassword;

    @Value("${aca.keyStore.alias}")
    private String keyAlias;

    @Autowired
    private Environment environment;

    @Autowired
    private LocalSessionFactoryBean sessionFactory;

    /**
     * @return bean to resolve injected annotation.Value property expressions
     * for beans.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * Configures the data source to be used by the hibernate session factory.
     *
     * @return configured data source
     */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClass);

//        dataSource.setMaximumPoolSize(Integer.parseInt(maximumPoolSize));
//        dataSource.setConnectionTimeout(Long.parseLong(connectionTimeout));
//        dataSource.setLeakDetectionThreshold(Long.parseLong(leakDetectionThreshold));

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
        Path certificatesPath = Paths.get(certificatesLocation);

        // create base directories if they do not exist
        try {
            Files.createDirectories(certificatesPath);
        } catch (IOException e) {
            throw new BeanInitializationException(
                    "Encountered error while initializing ACA directories: " + e.getMessage(), e);
        }

        // create the ACA key store if it doesn't exist
        Path keyStorePath = Paths.get(keyStoreLocation);
        if (!Files.exists(keyStorePath)) {
            throw new IllegalStateException(
                    String.format("ACA Key Store not found at %s. Consult the HIRS User "
                            + "Guide for ACA installation instructions.", keyStoreLocation));
        }
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
        } catch (Exception e) {
            throw new BeanInitializationException("Encountered error loading ACA private key "
                    + "from key store: " + e.getMessage(), e);
        }
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
        properties.put("hibernate.hbm2ddl.auto", ddl);
        properties.put("hibernate.dialect", dialect);
        properties.put("hibernate.current_session_context_class", "thread");
        return properties;
    }

    /**
     * Configures a session factory bean that in turn configures the hibernate session factory.
     * Enables auto scanning of annotations such that entities do not need to be registered in a
     * hibernate configuration file.
     *
     * @return session factory
     */
    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource());
        sessionFactory.setHibernateProperties(hibernateProperties());
        sessionFactory.setPackagesToScan("hirs");
        return sessionFactory;
    }
    /**
     * Configure a transaction manager for the hibernate session factory.
     *
     * @return transaction manager
     */
    @Bean
    public HibernateTransactionManager transactionManager() {
        return new HibernateTransactionManager(sessionFactory().getObject());
    }

    /**
     * Bean holding the maximum retry attempts for a DB transaction.
     * @return the maximum retry count
     */
    @Bean(name = "maxTransactionRetryAttempts")
    public int maxTransactionRetryAttempts() {
        return maxTransactionRetryAttempts;
    }

    /**
     * Bean holding the time to wait until retrying a failed transaction.
     * @return the wait time, in milliseconds
     */
    @Bean(name = "retryWaitTimeMilliseconds")
    public long retryWaitTimeMilliseconds() {
        return retryWaitTimeMilliseconds;
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
        Path keyStorePath = Paths.get(keyStoreLocation);

        // attempt to open the key store. if that fails, log a meaningful message before failing.
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(Files.newInputStream(keyStorePath), keyStorePassword.toCharArray());
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

    /**
     * Creates a {@link DeviceGroupManager} ready to use.
     *
     * @return {@link DeviceGroupManager}
     */
    @Bean
    public DeviceGroupManager deviceGroupManager() {
        return new DBDeviceGroupManager(sessionFactory.getObject());
    }

    /**
     * Creates a {@link DeviceManager} ready to use.
     *
     * @return {@link DeviceManager}
     */
    @Bean
    public DeviceManager deviceManager() {
        return new DBDeviceManager(sessionFactory.getObject());
    }

    /**
     * Creates a {@link ReferenceManifestManager} ready to use.
     *
     * @return {@link ReferenceManifestManager}
     */
    @Bean
    public ReferenceManifestManager referenceManifestManager() {
        return new DBReferenceManifestManager(sessionFactory.getObject());
    }

    /**
     * Creates a {@link ReferenceEventManager} ready to use.
     *
     * @return {@link ReferenceEventManager}
     */
    @Bean
    public ReferenceEventManager referenceEventManager() {
        return new DBReferenceEventManager(sessionFactory.getObject());
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
