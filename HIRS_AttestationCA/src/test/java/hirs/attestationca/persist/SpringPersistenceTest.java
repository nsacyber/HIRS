package hirs.attestationca.persist;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Base class that autowires a session factory for use of
 * any tests that need a database connection.
 */
@ContextConfiguration(classes = PersistenceConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SpringPersistenceTest extends AbstractJUnit4SpringContextTests {

    /**
     * Autowired session factory.
     */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    @Autowired
    protected SessionFactory sessionFactory;
}