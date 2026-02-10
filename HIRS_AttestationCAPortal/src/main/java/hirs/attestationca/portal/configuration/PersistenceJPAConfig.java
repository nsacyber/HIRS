package hirs.attestationca.portal.configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Provides application context configuration for the Attestation Certificate
 * Authority application. The properties are processed in order and as such, the
 * last property file read in will override properties that may had already been
 * defined previously. In other words, the 'defaults.properties' file provides a
 * basic standard of properties that can be overrode by the
 */
@Log4j2
@Configuration
@EnableTransactionManagement
@PropertySources({
        @PropertySource(value = "classpath:hibernate.properties"),

        // detects if file exists, if not, ignore errors
        @PropertySource(value = "file:/etc/hirs/aca/aca.properties",
                ignoreResourceNotFound = true),
        @PropertySource(value = "file:/etc/hirs/aca/application.properties",
                ignoreResourceNotFound = true),
        @PropertySource(value = "file:C:/ProgramData/hirs/aca/application.win.properties",
                ignoreResourceNotFound = true)
})
@ComponentScan({"hirs.attestationca.portal", "hirs.attestationca.portal.page.controllers",
        "hirs.attestationca.persist", "hirs.attestationca.persist.entity",
        "hirs.attestationca.persist.service"})
@EnableJpaRepositories(basePackages = "hirs.attestationca.persist.entity.manager")
public class PersistenceJPAConfig {
    @Value("${server.ssl.key-store}")
    private String keyStoreLocation;

    @Value("${server.ssl.key-store-password:''}")
    private String keyStorePassword;

    @Value("${aca.certificates.leaf-three-key-alias}")
    private String leaf3KeyAlias;

    @Value("${aca.certificates.intermediate-key-alias}")
    private String intermediateKeyAlias;

    @Value("${aca.certificates.root-key-alias}")
    private String rootKeyAlias;

    @Autowired
    private Environment environment;

    /**
     * Initialization of the ACA. Detects environment and runs configuration
     * methods as required. This method is intended to be invoked by the Spring
     * application context.
     */
    @PostConstruct
    void initialize() {
        // ensure that Bouncy Castle is registered as a security provider
        Security.addProvider(new BouncyCastleProvider());
    }


    /**
     * Platform Transaction Manager bean.
     *
     * @return platform transaction manager bean
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    /**
     * Entity manager factory bean.
     *
     * @return a local container entity manager factory bean
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        final LocalContainerEntityManagerFactoryBean entityManagerBean =
                new LocalContainerEntityManagerFactoryBean();
        entityManagerBean.setDataSource(dataSource());
        entityManagerBean.setPackagesToScan("hirs.attestationca.persist.entity");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerBean.setJpaVendorAdapter(vendorAdapter);
        entityManagerBean.setJpaProperties(additionalProperties());

        return entityManagerBean;
    }

    /**
     * Data source bean.
     *
     * @return a data source
     */
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
     * @return the {@link PrivateKey} of the ACA
     */
    @Bean
    public PrivateKey privateKey() {

        // obtain the key store
        KeyStore keyStore = keyStore();

        try {

            // load the key from the key store
            PrivateKey acaKey = (PrivateKey) keyStore.getKey(leaf3KeyAlias,
                    keyStorePassword.toCharArray());

            // break early if the certificate is not available.
            if (acaKey == null) {
                throw new BeanInitializationException(String.format("Key with alias "
                        + "%s was not in KeyStore %s. Ensure that the KeyStore has the "
                        + "specified certificate. ", leaf3KeyAlias, keyStoreLocation));
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
    @Qualifier("leafACACert")
    public X509Certificate leafACACertificate() {
        KeyStore keyStore = keyStore();

        try {
            X509Certificate leafACACertificate = (X509Certificate) keyStore.getCertificate(leaf3KeyAlias);

            // throw an exception if the certificate is not available.
            if (leafACACertificate == null) {
                throw new BeanInitializationException(String.format("Leaf ACA certificate with alias "
                        + "%s was not in KeyStore %s. Ensure that the KeyStore has the "
                        + "specified certificate. ", leaf3KeyAlias, keyStoreLocation));
            }

            return leafACACertificate;
        } catch (KeyStoreException ksEx) {
            throw new BeanInitializationException("Encountered error loading leaf ACA certificate "
                    + "from key store: " + ksEx.getMessage(), ksEx);
        }
    }

    /**
     * @return the array of {@link X509Certificate} ACA trust chain certificates
     */
    @Bean
    @Qualifier("acaTrustChainCerts")
    public X509Certificate[] acaTrustChainCertificates() {
        KeyStore keyStore = keyStore();

        try {
            X509Certificate rootACACert = (X509Certificate) keyStore.getCertificate(rootKeyAlias);

            X509Certificate intermediateACACert =
                    (X509Certificate) keyStore.getCertificate(intermediateKeyAlias);

            X509Certificate leafThreeACACert = (X509Certificate) keyStore.getCertificate(leaf3KeyAlias);

            // verify that the leaf ACA cert has been signed by the intermediate certificate and that
            // intermediate certificate has been signed by the root certificate
            validateCertificateChain(leafThreeACACert, intermediateACACert, rootACACert);

            X509Certificate[] certsChainArray = new X509Certificate[] {leafThreeACACert,
                    intermediateACACert, rootACACert};

            log.info("The ACA certificate chain is valid and trusted");
            return certsChainArray;
        } catch (KeyStoreException ksEx) {
            throw new BeanInitializationException("Encountered error loading ACA certificates "
                    + "from key store: " + ksEx.getMessage(), ksEx);
        } catch (CertificateException certificateException) {
            throw new BeanInitializationException("Encountered an error loading up the certificate factory.",
                    certificateException);
        } catch (CertPathValidatorException | InvalidAlgorithmParameterException exception) {
            throw new BeanInitializationException(
                    "Encountered an error while validating the leaf, intermediate and root "
                            + " ACA certificates.", exception);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new BeanInitializationException(
                    "Encountered an error while initializing Cert Path validator",
                    noSuchAlgorithmException);
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
            log.error("Encountered error while loading ACA key store. The most common issue is "
                    + "that configured password does not work on the configured key"
                    + " store {}.", keyStorePath);
            log.error("Exception message: {}", ex.getMessage());
            throw new BeanInitializationException(ex.getMessage(), ex);
        }
    }

    /**
     * Helper method that validates the provided leaf certificate against the
     * established intermediate and root certificates.
     *
     * @param leafCert         leaf certificate
     * @param intermediateCert intermediate certificate
     * @param rootCert         root certificate
     * @throws CertificateException               if there is an error parsing certificates
     *                                            /creating the CertPath
     * @throws InvalidAlgorithmParameterException if the PKIX parameters are invalid
     * @throws NoSuchAlgorithmException           if the PKIX algorithm is not available in the environment
     * @throws CertPathValidatorException         if the certificate chain is invalid or cannot be validated
     */
    private void validateCertificateChain(final X509Certificate leafCert,
                                          final X509Certificate intermediateCert,
                                          final X509Certificate rootCert)
            throws CertificateException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            CertPathValidatorException {
        List<X509Certificate> certChain =
                List.of(leafCert, intermediateCert);

        // Create a CertPath from the certificate chain
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        CertPath certPath = certFactory.generateCertPath(certChain);

        // Initialize CertPathValidator
        CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX");

        // Create TrustAnchor from the root certificate
        Set<TrustAnchor> trustAnchors = Set.of(new TrustAnchor(rootCert, null));

        // Initialize PKIX parameters
        PKIXParameters pkixParams = new PKIXParameters(trustAnchors);
        pkixParams.setRevocationEnabled(false);

        certPathValidator.validate(certPath, pkixParams);
    }

    private Properties additionalProperties() {
        final Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto",
                environment.getProperty("hibernate.hbm2ddl.auto"));
        hibernateProperties.setProperty("hibernate.dialect",
                environment.getProperty("hibernate.dialect"));
        hibernateProperties.setProperty("hibernate.cache.use_second_level_cache",
                "false");

        return hibernateProperties;
    }
}
