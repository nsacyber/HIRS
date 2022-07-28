package hirs.attestationca.persist;

import hirs.attestationca.configuration.PersistenceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import javax.persistence.EntityManager;

/**
 * Base class that autowires a session factory for use of
 * any tests that need a database connection.
 */
@ContextConfiguration(classes = PersistenceConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SpringPersistenceTest extends AbstractTestNGSpringContextTests {

    /**
     * Autowired session factory.
     */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    @Autowired
    protected EntityManager sessionFactory;
}
