package hirs.attestationca.portal;

import hirs.attestationca.persist.entity.userdefined.SupplyChainSettings;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
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

import javax.sql.DataSource;
import java.security.cert.X509Certificate;
import java.util.Properties;

@Log4j2
@Configuration
@EnableTransactionManagement
@PropertySource({ "classpath:hibernate.properties", "classpath:portal.properties" })
@ComponentScan({ "hirs.attestationca.portal.page.controllers", "hirs.attestationca.persist.entity" })
@EnableJpaRepositories(basePackages = "hirs.attestationca.persist.entity.manager")
public class PersistenceJPAConfig {

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

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        final LocalContainerEntityManagerFactoryBean entityManagerBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerBean.setDataSource(dataSource());
        entityManagerBean.setPackagesToScan(new String[] {"hirs.attestationca.persist"});

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerBean.setJpaVendorAdapter(vendorAdapter);
        entityManagerBean.setJpaProperties(additionalProperties());

        return entityManagerBean;
    }

    @Bean
    public DataSource dataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(environment.getProperty("hibernate.connection.driver_class"));
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
//    @PostConstruct
//    void initialize() {
//        // ensure that Bouncy Castle is registered as a security provider
//        Security.addProvider(new BouncyCastleProvider());
//
//        // obtain path to ACA configuration
//        Path certificatesPath = Paths.get(certificatesLocation);
//
//        // create base directories if they do not exist
//        try {
//            Files.createDirectories(certificatesPath);
//        } catch (IOException ioEx) {
//            throw new BeanInitializationException(
//                    "Encountered error while initializing ACA directories: " + ioEx.getMessage(), ioEx);
//        }
//
//        // create the ACA key store if it doesn't exist
//        Path keyStorePath = Paths.get(keyStoreLocation);
////        if (!Files.exists(keyStorePath)) {
////            throw new IllegalStateException(
////                    String.format("ACA Key Store not found at %s. Consult the HIRS User "
////                            + "Guide for ACA installation instructions.", keyStoreLocation));
////        }
//    }

    /**
     * @return the {@link X509Certificate} of the ACA
     */
//    @Bean
//    public X509Certificate acaCertificate() {
//        KeyStore keyStore = keyStore();
//
//        try {
//            X509Certificate acaCertificate = (X509Certificate) keyStore.getCertificate(keyAlias);
//
//            // break early if the certificate is not available.
//            if (acaCertificate == null) {
//                throw new BeanInitializationException(String.format("Certificate with alias "
//                        + "%s was not in KeyStore %s. Ensure that the KeyStore has the "
//                        + "specified certificate. ", keyAlias, keyStoreLocation));
//            }
//
//            return acaCertificate;
//        } catch (KeyStoreException ksEx) {
//            throw new BeanInitializationException("Encountered error loading ACA certificate "
//                    + "from key store: " + ksEx.getMessage(), ksEx);
//        }
//    }

    /**
     * @return the {@link java.security.KeyStore} that contains the certificates
     * for the ACA.
     */
//    @Bean
//    public KeyStore keyStore() {
//        Path keyStorePath = Paths.get(keyStoreLocation);
//
//        // creating empty store
//        String storePassword = "storePassword";
//        String storeName = "emptyStore.jks";
//        String storeType = "jks";
//
//        // attempt to open the key store. if that fails, log a meaningful message before failing.
////        try {
////            KeyStore keyStore = KeyStore.getInstance("JKS");
////            keyStore.load(Files.newInputStream(keyStorePath), keyStorePassword.toCharArray());
//
//            // empty
//            try (FileOutputStream fileOutputStream = new FileOutputStream(storeName)) {
//                KeyStore keyStore = KeyStore.getInstance(storeType);
//                keyStore.load(null, storePassword.toCharArray());
////                keyStore.setCertificateEntry(keyAlias,);
//                keyStore.store(fileOutputStream, storePassword.toCharArray());
//
//
//            return keyStore;
//        } catch (Exception e) {
//            log.error(String.format(
//                    "Encountered error while loading ACA key store. The most common issue is "
//                            + "that configured password does not work on the configured key"
//                            + " store %s.", keyStorePath));
//            log.error(String.format("Exception message: %s", e.getMessage()));
//            throw new BeanInitializationException(e.getMessage(), e);
//        }
//    }

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

    @Bean(name="default-settings")
    public SupplyChainSettings supplyChainSettings() {
        SupplyChainSettings scSettings = new SupplyChainSettings("Default", "Settings are configured for no validation flags set.");

        return scSettings;
    }
}
