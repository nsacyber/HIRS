package hirs.utils.rim;

import hirs.utils.swid.SwidTagConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.log4j.Log4j2;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles parsing and validation Swidtag files.
 */
@Log4j2
public class SwidTagParser {

    /**
     * This method validates the Document against the schema.
     *
     * @param doc of the input swidtag.
     * @return document validated against the schema.
     */
    public static Document validateSwidtagSchema(final Document doc) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(SwidTagConstants.SCHEMA_LANGUAGE);
        try (InputStream is = SwidTagParser.class.getClassLoader().getResourceAsStream(
                SwidTagConstants.SCHEMA_URL)) {
            Schema schema = schemaFactory.newSchema(new StreamSource(is));
            JAXBContext jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            unmarshaller.unmarshal(doc);
        } catch (UnmarshalException e) {
            log.warn("Error validating swidtag file!");
        } catch (IllegalArgumentException e) {
            log.warn("Input file empty.");
        } catch (JAXBException | SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return doc;
    }

    /**
     * Retrieves the embedded X509Certificate structure(s)
     * after the RIM document has been set and unmarshalled.
     *
     * @return the embedded X509Certificate if present, null otherwise.
     */
    public static List<X509Certificate> getEmbeddedCertificates(Document rim) {
        if (rim == null) {
            log.warn("Cannot extract embedded certificate; RIM Document is null.");
            return null;
        }
        try {
            NodeList certNodes = rim.getElementsByTagNameNS(
                    XMLSignature.XMLNS, "X509Certificate"
            );
            List<X509Certificate> certs = new ArrayList<>();
            if (certNodes.getLength() > 0) {
                for (int i = 0; i < certNodes.getLength(); i++) {
                    String certBase64 = certNodes.item(i).getTextContent();
                    X509Certificate cert = parseCertFromPEMString(certBase64);
                    certs.add(cert);
                }
            }
            return certs;
        } catch (Exception e) {
            log.error("Failed to parse embedded certificate string from DOM element", e);
        }
        return null;
    }

    /**
     * This method extracts certificate bytes from a string. The bytes are assumed to be
     * PEM format, and a header and footer are concatenated with the input string to
     * facilitate proper parsing.
     *
     * @param pemString the input string
     * @return an X509Certificate created from the string, or null
     */
    public static X509Certificate parseCertFromPEMString(final String pemString) {
        String certificateHeader = "-----BEGIN CERTIFICATE-----";
        String certificateFooter = "-----END CERTIFICATE-----";
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            InputStream inputStream = new ByteArrayInputStream((certificateHeader
                    + System.lineSeparator()
                    + pemString
                    + System.lineSeparator()
                    + certificateFooter).getBytes(StandardCharsets.UTF_8));
            return (X509Certificate) factory.generateCertificate(inputStream);
        } catch (CertificateException e) {
            log.warn("Error creating CertificateFactory instance: {}", e.getMessage());
        }

        return null;
    }

    /**
     * This method strips all whitespace from an xml file, including indents and spaces
     * added for human-readability.
     *
     * @param source of the input xml.
     * @return Document representation of the xml.
     */
    public static Document removeXMLWhitespace(final StreamSource source) throws IOException {
        Document doc = null;
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Source identitySource = new StreamSource(
                    ReferenceManifestValidator.class.getClassLoader()
                            .getResourceAsStream(SwidTagConstants.IDENTITY_TRANSFORM));
            Transformer transformer = tf.newTransformer(identitySource);
            DOMResult result = new DOMResult();
            transformer.transform(source, result);
            doc = (Document) result.getNode();
        } catch (TransformerConfigurationException e) {
            log.warn("Error configuring transformer!");
            e.printStackTrace();
        } catch (TransformerException e) {
            log.warn("Error transforming input!");
            e.printStackTrace();
        }
        return doc;
    }
}
