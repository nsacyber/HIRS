package hirs.attestationca.portal.page;

import hirs.attestationca.entity.certificate.CertificateAuthorityCredential;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * A configuration class for testing Attestation CA Portal classes that require a database.
 * This apparently is needed to appease spring tests in the TestNG runner.
 */
@Configuration
@EnableTransactionManagement
@Import({ CommonPageConfiguration.class })
public class PageTestConfiguration {

    /**
     * Test ACA cert.
     */
    public static final String FAKE_ROOT_CA = "/certificates/fakeCA.pem";

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
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource());
        sessionFactory.setHibernateProperties(hibernateProperties());
        sessionFactory.setPackagesToScan("hirs");
        return sessionFactory;
    }

    /**
     * Generates properties using configuration file that will be used to configure the session
     * factory.
     *
     * @return properties for hibernate session factory
     */
    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.hbm2ddl.auto", "create");
        properties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        properties.put("hibernate.current_session_context_class", "thread");
        return properties;
    }
}
