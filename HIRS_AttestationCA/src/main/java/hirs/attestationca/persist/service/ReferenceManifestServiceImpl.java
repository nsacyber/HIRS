package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;

@Log4j2
@Service
public class ReferenceManifestServiceImpl {

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
                log.error(String.format("Error setting schema for validation!%n%s",
                        saxEx.getMessage()));
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioEx) {
                        log.error(String.format("Error closing input stream%n%s",
                                ioEx.getMessage()));
                    }
                } else {
                    log.error("Input stream variable is null");
                }
            }
        }
        return schema;
    }
}
