package hirs.utils.rim.unsignedRim.xml.pcclientrim;

import hirs.utils.rim.ReferenceManifestValidator;
import hirs.utils.rim.unsignedRim.GenericRim;
import hirs.utils.rim.unsignedRim.common.measurement.Measurement;
import hirs.utils.swid.SwidTagConstants;
import hirs.utils.swid.SwidTagGateway;
import lombok.NoArgsConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Class that holds a PC Client RIM.
 */
@NoArgsConstructor
public class PcClientRim extends SwidTagGateway implements GenericRim {

    private final List<Measurement> measurements = new ArrayList<>();
    private Document rim;
    // private Measurement measurement = new Measurement();
    private String manufacturer = "";
    private String model = "";
    private String serialNumber = "";
    private String revision = "";
    private String digest = "";
    private UUID tagUuid = null; // private String tagId = "";
    private boolean isValid;

    /**
     * Validate a PC Client RIM.
     *
     * @param verifyFile      RIM to verify
     * @param certificateFile certificate
     * @param rimel           RIM event log
     * @param trustStore      certificate chain
     * @return true if validated
     * @throws IOException if there is an I/O error during the operation.
     */
    public boolean validate(final String verifyFile, final String certificateFile, final String rimel,
                            final String trustStore) throws IOException {
        boolean valid;
        ReferenceManifestValidator validator = new ReferenceManifestValidator();
        validator.setRim(verifyFile);
        validator.setTrustStoreFile(trustStore);
        HexFormat hexTool = HexFormat.of();
        if (rimel != null) {
            validator.setHasSupportRim(true);
            validator.setSupportRimDirectory(rimel);
        }

        rim = copyDoc(validator.getRim());

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
     * This method clones a Document object's data into a new Document
     * @param doc to copy from
     * @return a new, identical doc
     */
    private Document copyDoc(Document doc) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            DOMSource originalDoc = new DOMSource(doc);
            DOMResult newDoc = new DOMResult();
            t.transform(originalDoc, newDoc);
            return (Document) newDoc.getNode();
        } catch (TransformerException e) {
            throw new RuntimeException("Error while copying XML for validation: " + e.getMessage());
        }
    }

    /**
     * Get RIM type.
     *
     * @return PC Client RIM
     */
    @Override
    public String getRimType() {
        return RIMTYPE_PCRIM;
    }

    /**
     * Create a PC Client RIM.
     *
     * @param configFile      config file
     * @param rimEventLog     event log
     * @param certificateFile certificate
     * @param privateKeyFile  private key
     * @param embeddedCert    true if cert should be embedded
     * @param outFile         ouptut RIM
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
     *
     * @return n/a
     */
    @Override
    public String getRimID() {
        return tagUuid.toString();
    }

    /**
     * Default getSignerId.
     *
     * @return n/a
     */
    @Override
    public String getSignerId() {
        return "";
    }

    /**
     * Default getReferenceMeasurements.
     *
     * @return n/a
     */
    @Override
    public List<Measurement> getReferenceMeasurements() {
        return new ArrayList<>(measurements);
    }

    /**
     * Default getReferencedRims.
     *
     * @return n/a
     */
    @Override
    public String getReferencedRims() {
        return "";
    }

    /**
      * Default isValid.
      *
      * @return n/a
      */
    @Override
    public boolean isValid() {
        return isValid;
    }

    /**
     * Default toString.
     *
     * @return n/a
     */
    @Override
    public String toString() {
        return "";
    }
}
