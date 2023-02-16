package hirs.attestationca.portal.service;

import hirs.attestationca.portal.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.portal.entity.userdefined.ReferenceManifest;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ReferenceManifestServiceImpl {

    private static final Logger LOGGER = LogManager.getLogger(ReferenceManifestServiceImpl.class);
    /**
     * The variable that establishes a schema factory for xml processing.
     */
    public static final SchemaFactory SCHEMA_FACTORY
            = SchemaFactory.newInstance(ReferenceManifest.SCHEMA_LANGUAGE);

    @Autowired(required = false)
    private EntityManager entityManager;

    @Autowired
    private ReferenceManifestRepository repository;

    private static Schema schema;

    public ReferenceManifestServiceImpl() {
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
}
