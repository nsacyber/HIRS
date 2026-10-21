package hirs.attestationca.portal.configuration;

import jakarta.persistence.Entity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that every table Hibernate creates on MariaDB fits MariaDB's row size limit. The ACA database uses utf8mb4,
 * where a varchar(n) column counts as up to 4n bytes. When a table is over the limit, CREATE TABLE fails at startup
 * and the ACA reports the table as missing. The HSQLDB used by the other tests has no such limit.
 */
public class MariaDbRowSizeTest {
    private static final int MAX_ROW_SIZE = 65535;
    private static final int UTF8MB4_BYTES_PER_CHAR = 4;
    private static final int LENGTH_PREFIX_BYTES = 2;
    private static final Pattern TABLE = Pattern.compile("create table (\\w+) \\((.*?)\\) engine", Pattern.DOTALL);
    private static final Pattern VARIABLE_COLUMN = Pattern.compile("\\b(varchar|varbinary)\\((\\d+)\\)");

    /**
     * Generates the MariaDB schema without a database and adds up each table's variable length columns.
     *
     * @param tempDir where the schema script is written
     * @throws Exception if the schema cannot be generated
     */
    @Test
    public void testTablesFitMariaDbRowSize(@TempDir final Path tempDir) throws Exception {
        Map<String, Integer> oversized = new HashMap<>();
        Matcher table = TABLE.matcher(generateSchema(tempDir.resolve("schema.sql")));
        while (table.find()) {
            int rowSize = rowSize(table.group(2));
            if (rowSize > MAX_ROW_SIZE) {
                oversized.put(table.group(1), rowSize);
            }
        }
        assertEquals(Map.of(), oversized, "Tables over MariaDB's " + MAX_ROW_SIZE + " byte row size limit");
    }

    private static String generateSchema(final Path script) throws ClassNotFoundException, IOException {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        settings.put("hibernate.boot.allow_jdbc_metadata_access", "false");
        // hibernate.properties names a database; generate the script without connecting to it
        settings.put("hibernate.connection.provider_class",
                "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl");
        settings.put("hibernate.hbm2ddl.auto", "none");
        settings.put("jakarta.persistence.schema-generation.database.action", "none");
        settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
        settings.put("jakarta.persistence.schema-generation.scripts.create-target", script.toString());

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        MetadataSources sources = new MetadataSources(registry);
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        for (BeanDefinition entity : scanner.findCandidateComponents("hirs.attestationca.persist.entity")) {
            sources.addAnnotatedClass(Class.forName(entity.getBeanClassName()));
        }
        SchemaManagementToolCoordinator.process(sources.buildMetadata(), registry, settings, action -> { });
        StandardServiceRegistryBuilder.destroy(registry);
        return Files.readString(script);
    }

    private static int rowSize(final String columns) {
        int size = 0;
        Matcher column = VARIABLE_COLUMN.matcher(columns);
        while (column.find()) {
            int length = Integer.parseInt(column.group(2));
            size += (column.group(1).equals("varchar") ? length * UTF8MB4_BYTES_PER_CHAR : length)
                    + LENGTH_PREFIX_BYTES;
        }
        return size;
    }
}
