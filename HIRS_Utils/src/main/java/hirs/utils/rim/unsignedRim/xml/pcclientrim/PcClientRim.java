package hirs.utils.rim.unsignedRim.xml.pcclientrim;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import lombok.NoArgsConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import hirs.utils.rim.unsignedRim.GenericRim;
import hirs.utils.rim.unsignedRim.common.measurement.Measurement;
import hirs.utils.swid.SwidTagConstants;
import hirs.utils.swid.SwidTagGateway;
import hirs.utils.rim.ReferenceManifestValidator;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Class that holds a PC Client RIM.
 */
@NoArgsConstructor
public class PcClientRim extends SwidTagGateway implements GenericRim {

    private boolean isValid = false;
    private Unmarshaller unmarshaller;
    private static final String SCHEMA_PACKAGE = "hirs.utils.xjc";
    private static final String IDENTITY_TRANSFORM = "identity_transform.xslt";
    private Schema schema;
    private Document rim;
    // private Measurement measurement = new Measurement();
    private String manufacturer = "";
    private String model = "";
    private String serialNumber = "";
    private String revision = "";
    private String digest = "";
    private UUID tagUuid = null; // private String tagId = "";
    private List<Measurement> measurements = new ArrayList<>();;

    /**
     * Validate a PC Client RIM.
     * @param verifyFile RIM to verify
     * @param certificateFile certificate
     * @param rimel RIM event log
     * @param trustStore certificate chain
     * @return true if validated
     */
    public boolean validate(final String verifyFile, final String certificateFile, final String rimel,
                            final String trustStore) throws IOException {
        boolean valid = false;
        ReferenceManifestValidator validator = new ReferenceManifestValidator();
        validator.setRim(verifyFile);
        validator.setSupportRimDirectory(rimel);
        validator.setTrustStoreFile(trustStore);
        HexFormat hexTool = HexFormat.of();

        File rimFile = new File(verifyFile);

        byte[] rimBytes = Files.readAllBytes(rimFile.toPath());

        rim = validateSwidtagSchema(
                removeXMLWhitespace(new StreamSource(new ByteArrayInputStream(rimBytes))));

        NodeList si = rim.getElementsByTagNameNS(SwidTagConstants.SWIDTAG_NAMESPACE,
                SwidTagConstants.SOFTWARE_IDENTITY);
        // Process TagID (Must be a GUID)
        Element siElement = (Element) si.item(0);
        if (siElement == null) {
            throw new RemoteException(verifyFile + " did not contain a TagId attribute in the Base Rim");
        }
        String tagId = siElement.getAttribute(SwidTagConstants.TAGID);
        tagUuid = UUID.fromString(tagId);

        // Process Meta Data
        NodeList metaNode = rim.getElementsByTagNameNS(SwidTagConstants.SWIDTAG_NAMESPACE,
                SwidTagConstants.META);
        Element mElement = (Element) metaNode.item(0);
        manufacturer = mElement.getAttributeNS(SwidTagConstants.TCG_NS,
                SwidTagConstants.PLATFORM_MANUFACTURER_STR);
        model = mElement.getAttributeNS(SwidTagConstants.TCG_NS, SwidTagConstants.PLATFORM_MODEL);
        revision = mElement.getAttributeNS(SwidTagConstants.TCG_NS, SwidTagConstants.REVISION);
        serialNumber = "N/A to PC Client RIMs";

        // Process payload

        // NodeList files = rim.getElementsByTagName(SwidTagConstants.FILE);
        NodeList files = rim.getElementsByTagNameNS(SwidTagConstants.SWIDTAG_NAMESPACE,
                SwidTagConstants.FILE);
        // Make a measurement for each Hash item and add it to the list of
        // measurements
        for (int count = 0; count < files.getLength(); count++) {
            Element file = (Element) files.item(count);
            digest = file.getAttributeNS(SwidTagConstants.SHA_256_HASH.getNamespaceURI(),
                    SwidTagConstants.HASH);
            byte[] digestBytes = hexTool.parseHex(digest);
            // Create a Measurement from what was collected from the PC RIM
            Measurement measurement = new Measurement();
            measurement.setManufacturer(manufacturer);
            measurement.setModel(model);
            measurement.setSerialNumber(serialNumber);
            measurement.setRevision(revision);
            measurement.setRimID(tagUuid);
            measurement.setMeasurementBytes(digestBytes);
            measurements.add(measurement);
        }

        if (validator.validateBaseRim(certificateFile)) {
            valid = true;
        } else {
            throw new RuntimeException("Failed to verify " + verifyFile);
        }
        isValid = valid;
        return valid;
    }

    /**
     * Get RIM type.
     * @return PC Client RIM
     */
    @Override
    public String getRimType() {
        return RIMTYPE_PCRIM;
    }

    /**
     * Create a PC Client RIM.
     * @param configFile config file
     * @param rimEventLog event log
     * @param certificateFile certificate
     * @param privateKeyFile private key
     * @param embeddedCert true if cert should be embedded
     * @param outFile ouptut RIM
     */
    public void create(final String configFile, final String rimEventLog, final String certificateFile,
                       final String privateKeyFile, final boolean embeddedCert, final String outFile) {
        SwidTagGateway gateway = new SwidTagGateway();
        gateway.setAttributesFile(configFile);
        gateway.setRimEventLog(rimEventLog);

        gateway.setDefaultCredentials(false);
        gateway.setPemCertificateFile(certificateFile);
        gateway.setPemPrivateKeyFile(privateKeyFile);
        if (embeddedCert) {
            gateway.setEmbeddedCert(true);
        }
//         skip timestamp for now
//
//        List<String> timestampArguments = commander.getTimestampArguments();
//        if (timestampArguments.size() > 0) {
//            if (new TimestampArgumentValidator(timestampArguments).isValid()) {
//                gateway.setTimestampFormat(timestampArguments.get(0));
//                if (timestampArguments.size() > 1) {
//                    gateway.setTimestampArgument(timestampArguments.get(1));
//                }
//            } else {
//                exitWithErrorCode("The provided timestamp argument(s) " +
//                        "is/are not valid.");
//            }
//        }
        gateway.generateSwidTag(outFile);
    }

    /**
     * Default getRimID.
     * @return n/a
     */
    @Override
    public String getRimID() {
        return tagUuid.toString();
    }

    /**
     * Default getSignerId.
     * @return n/a
     */
    @Override
    public String getSignerId() {
        return "";
    }

    /**
     * Default isValid.
     * @return n/a
     */
    @Override
    public boolean isValid() {
        return isValid;
    }

    /**
     * Default getReferenceMeasurements.
     * @return n/a
     */
    @Override
    public List<Measurement> getReferenceMeasurements() {
        return new ArrayList<>(measurements);
    }

    /**
     * Default getReferencedRims.
     * @return n/a
     */
    @Override
    public String getReferencedRims() {
        return "";
    }

    /**
     * Default toString.
     * @return n/a
     */
    @Override
    public String toString() {
        return "";
    }

    /**
     * This method validates the Document against the schema.
     *
     * @param doc of the input swidtag.
     * @return document validated against the schema.
     */
    private Document validateSwidtagSchema(final Document doc) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SCHEMA_PACKAGE);
            unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            unmarshaller.unmarshal(doc);
        } catch (UnmarshalException e) {
            // log.warn("Error validating swidtag file!");
        } catch (IllegalArgumentException e) {
            // log.warn("Input file empty.");
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return doc;
    }

    /**
     * This method strips all whitespace from an xml file, including indents and spaces
     * added for human-readability.
     *
     * @param source of the input xml.
     * @return Document representation of the xml.
     */
    private Document removeXMLWhitespace(final StreamSource source) throws IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Source identitySource = new StreamSource(
                ReferenceManifestValidator.class.getClassLoader().getResourceAsStream(IDENTITY_TRANSFORM));
        Document doc = null;
        try {
            Transformer transformer = tf.newTransformer(identitySource);
            DOMResult result = new DOMResult();
            transformer.transform(source, result);
            doc = (Document) result.getNode();
        } catch (TransformerConfigurationException e) {
//log.warn("Error configuring transformer!");
            e.printStackTrace();
        } catch (TransformerException e) {
            // log.warn("Error transforming input!");
            e.printStackTrace();
        }
        return doc;
    }
}
