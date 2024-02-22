package hirs.attestationca.portal;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Base class that autowires a session factory for use of
 * any tests that need a database connection.
 */
//@ContextConfiguration(classes = PersistenceConfiguration.class)
//@ContextConfiguration(classes = PersistenceJPAConfig.class)
//@TestConfiguration
@SpringBootApplication
//@EnableJpaRepositories(basePackages = "hirs.attestationca.persist.entity.manager")
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
//public class SpringPersistenceTest extends AbstractJUnit4SpringContextTests {
public class SpringPersistenceTest2 {

    /**
     * Autowired session factory.
     */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    @Autowired
    protected SessionFactory sessionFactory;
}