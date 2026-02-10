package hirs.attestationca.persist.configuration;

import hirs.structs.converters.SimpleStructConverter;
import hirs.structs.converters.StructConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Persistence Configuration for Spring enabled applications. Constructs a Hibernate SessionFactory
 * backed powered by a HikariCP connection pooled data source. Module-specific settings
 * need to be set in the persistence-extended.properties file on the classpath. If another module
 * such as the HIRS_Portal uses this class and doesn't have a persistence-extended.properties
 * file, the default persistence file will be used instead.
 */
@Configuration
public class PersistenceConfiguration {

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
