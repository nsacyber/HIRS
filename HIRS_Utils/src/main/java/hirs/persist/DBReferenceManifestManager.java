package hirs.persist;

import hirs.data.persist.ReferenceManifest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.persistence.criteria.CriteriaBuilder;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used to persist and retrieve {@link ReferenceManifest}s into
 * and from the database.
 */
@Service
public class DBReferenceManifestManager extends DBManager<ReferenceManifest>
        implements ReferenceManifestManager {

    private static final Logger LOGGER = LogManager.getLogger(DBReferenceManifestManager.class);
    /**
     * The variable that establishes a schema factory for xml processing.
     */
    public static final SchemaFactory SCHEMA_FACTORY
            = SchemaFactory.newInstance(ReferenceManifest.SCHEMA_LANGUAGE);

    private static Schema schema;

    /**
     * Default Constructor.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBReferenceManifestManager(final SessionFactory sessionFactory) {
        super(ReferenceManifest.class, sessionFactory);
        getSchemaObject();
    }

    /**
     * This method sets the xml schema for processing RIMs.
     *
     * @return the schema
     */
    public static final Schema getSchemaObject() {
        if (schema == null) {
            InputStream is = null;
            try {
                is = ReferenceManifest.class
                        .getClassLoader()
                        .getResourceAsStream(ReferenceManifest.SCHEMA_URL);
                schema = SCHEMA_FACTORY.newSchema(new StreamSource(is));
            } catch (SAXException saxEx) {
                LOGGER.error(String.format("Error setting schema for validation!%n%s",
                    saxEx.getMessage()));
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioEx) {
                        LOGGER.error(String.format("Error closing input stream%n%s",
                                ioEx.getMessage()));
                    }
                } else {
                    LOGGER.error("Input stream variable is null");
                }
            }
        }
        return schema;
    }

    /**
     * This method does not need to be used directly as it is used by
     * {@link ReferenceManifestSelector}'s get* methods. Regardless, it may be
     * used to retrieve ReferenceManifest by other code in this package, given a
     * configured ReferenceManifestSelector.
     *
     * @param referenceManifestSelector a configured
     * {@link ReferenceManifestSelector} to use for querying
     * @return the resulting set of ReferenceManifest, possibly empty
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends  ReferenceManifest> Set<T> get(
            final ReferenceManifestSelector referenceManifestSelector) {
        LOGGER.info("Getting the full set of Reference Manifest files.");
        CriteriaBuilder builder = this.getFactory().getCriteriaBuilder();
        return new HashSet<>((List<T>) getWithCriteria(
                referenceManifestSelector.getReferenceManifestClass(),
                referenceManifestSelector.getCriterion(builder))
        );
    }

    /**
     * Remove a ReferenceManifest from the database.
     *
     * @param referenceManifest the referenceManifest to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteReferenceManifest(final ReferenceManifest referenceManifest) {
        LOGGER.info(String.format("Deleting reference to %s", referenceManifest.getTagId()));
        return deleteById(referenceManifest.getId());
    }
}
