package hirs.attestationca.portal.page;

import hirs.attestationca.portal.PageConfiguration;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import jakarta.xml.bind.DatatypeConverter;
import org.hibernate.AssertionFailure;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;
import java.util.*;

/**
 * A configuration class for testing Attestation CA Portal classes that require a database.
 * This apparently is needed to appease spring tests in the TestNG runner.
 */
@Import({ PageConfiguration.class })
@TestConfiguration
@EnableJpaRepositories(basePackages = "hirs.attestationca.persist.entity.manager")
public class PageTestConfiguration {

    /**
     * Test ACA cert.
     */
    public static final String FAKE_ROOT_CA = "/certificates/fakeCA.pem";

    @Autowired
    private Environment environment;

    /**
     * Gets a test x509 cert as the ACA cert for ACA portal tests.
     *
     * @return the {@link X509Certificate} of the ACA
     * @throws URISyntaxException if there's a syntax error on the path to the cert
     * @throws IOException exception reading the file
     */
    @Bean
    public X509Certificate acaCertificate() throws URISyntaxException, IOException {

        CertificateAuthorityCredential credential = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(FAKE_ROOT_CA).toURI()))
        );
        return credential.getX509Certificate();
    }


    /**
     * Overrides the {@link DataSource} with one that is configured against an in-memory HSQL DB.
     *
     * @return test data source
     */
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.HSQL).build();
    }

    /**
     * Configures a session factory bean that in turn configures the hibernate session factory.
     * Enables auto scanning of annotations such that entities do not need to be registered in a
     * hibernate configuration file.
     *
     * @return session factory
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

        final LocalContainerEntityManagerFactoryBean entityManagerBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerBean.setDataSource(dataSource());
        entityManagerBean.setPackagesToScan("hirs.attestationca.persist.entity");
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerBean.setJpaVendorAdapter(vendorAdapter);
        entityManagerBean.setJpaProperties(hibernateProperties());

        return entityManagerBean;
    }

    /**
     * Generates properties using configuration file that will be used to configure the session
     * factory.
     *
     * @return properties for hibernate session factory
     */
    final Properties hibernateProperties() {
        final Properties hibernateProperties = new Properties();

        hibernateProperties.setProperty("hibernate.hbm2ddl.auto",
                environment.getProperty("hibernate.hbm2ddl.auto"));
        hibernateProperties.setProperty("hibernate.dialect",
                environment.getProperty("hibernate.dialect"));
        hibernateProperties.setProperty("hibernate.current_session_context_class",
                "thread");

        return hibernateProperties;
    }



    /**
     * Generates JPA transaction manager.
     *
     * @return transaction manager
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean
    public PrivateKey privateKey() {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            PrivateKey dummy_privKey = keyGenerator.generateKeyPair().getPrivate();
            return dummy_privKey;
        }
        catch (GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @return a blank {@link java.security.KeyStore}
     * this function is only used to override the PersistenceJPAConfig keyStore bean during test
     */
    @Bean
    public KeyStore keyStore() {
        // attempt to create the key store. if that fails, print a message before failing.
        try {
            KeyStore dummy_keyStore = KeyStore.getInstance("JKS");
            dummy_keyStore.load(null);

            return dummy_keyStore;
        } catch (Exception ex) {
            System.out.println("\nEncountered error while creating a fake (blank) key store for testing");
            throw new BeanInitializationException(ex.getMessage(), ex);
        }
    }
}
