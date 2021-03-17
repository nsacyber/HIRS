package hirs.attestationca.configuration;

import hirs.persist.DBReferenceDigestManager;
import hirs.persist.ReferenceDigestManager;
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
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import hirs.persist.DBDeviceGroupManager;
import hirs.persist.DBDeviceManager;
import hirs.persist.DeviceGroupManager;
import hirs.persist.DeviceManager;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.DBReferenceManifestManager;
import hirs.persist.HibernateConfiguration;
import hirs.structs.converters.SimpleStructConverter;
import hirs.structs.converters.StructConverter;
import hirs.utils.LogConfigurationUtil;

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
@Import(HibernateConfiguration.class)
@EnableWebMvc
public class AttestationCertificateAuthorityConfiguration extends WebMvcConfigurerAdapter {

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
     * Creates a {@link ReferenceDigestManager} ready to use.
     *
     * @return {@link ReferenceDigestManager}
     */
    @Bean
    public ReferenceDigestManager referenceDigestManager() {
        return new DBReferenceDigestManager(sessionFactory.getObject());
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
