package hirs.utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import hirs.utils.xjc.CanonicalizationMethodType;
import hirs.utils.xjc.DigestMethodType;
import hirs.utils.xjc.Directory;
import hirs.utils.xjc.Entity;
import hirs.utils.xjc.Link;
import hirs.utils.xjc.ObjectFactory;
import hirs.utils.xjc.ResourceCollection;
import hirs.utils.xjc.ReferenceType;
import hirs.utils.xjc.SignatureType;
import hirs.utils.xjc.SignatureValueType;
import hirs.utils.xjc.SignatureMethodType;
import hirs.utils.xjc.SignedInfoType;
import hirs.utils.xjc.SoftwareIdentity;
import hirs.utils.xjc.SoftwareMeta;
import hirs.utils.xjc.TransformType;
import hirs.utils.xjc.TransformsType;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This class provides interaction with the SWID Tag schema as defined in
 * http://standards.iso.org/iso/19770/-2/2015/schema.xsd
 *
 */
public class SwidTagGateway {

    private static final QName _DEFAULT_QNAME = new QName(
            "http://www.w3.org/2000/09/xmldsig#", "SHA256", "ds");
    private static final QName _SHA1Value_QNAME = new QName(
            "http://www.w3.org/2000/09/xmldsig#", "SHA1", "ds");
    private static final QName _SHA384Value_QNAME = new QName(
            "http://www.w3.org/2000/09/xmldsig#", "SHA384", "ds");
    private static final QName _SHA512Value_QNAME = new QName(
            "http://www.w3.org/2000/09/xmldsig#", "SHA512", "ds");
    private static final QName _SHA256_HASH = new QName(
            "http://www.w3.org/2001/04/xmlenc#sha256", "hash", "SHA256");
    private static final QName _RIM_PCURILOCAL = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "pcURILocal", "rim");
    private static final QName _RIM_BINDINGSPEC = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "bindingSpec", "rim");
    private static final QName _RIM_BINDINGSPECVERSION = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "bindingSpecVersion", "rim");
    private static final QName _RIM_PLATFORMMANUFACTURERID = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "platformManufacturerId", "rim");
    private static final QName _RIM_PLATFORMMANUFACTURERSTR = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "platformManufacturerStr", "rim");
    private static final QName _RIM_PLATFORMMODEL = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "platformModel", "rim");
    private static final QName _RIM_COMPONENTCLASS = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "componentClass", "rim");
    private static final QName _RIM_COMPONENTMANUFACTURER = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "componentManufacturer", "rim");
    private static final QName _RIM_COMPONENTMANUFACTURERID = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "componentManufacturerId", "rim");
    private static final QName _RIM_RIMLINKHASH  = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "rimLinkHash", "rim");
    private static final QName _N8060_ENVVARPREFIX = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "envVarPrefix", "n8060");
    private static final QName _N8060_ENVVARSUFFIX = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "envVarSuffix", "n8060");
    private static final QName _N8060_PATHSEPARATOR = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "pathSeparator", "n8060");

    private final ObjectFactory objectFactory = new ObjectFactory();
    
    private static final String ENTITY = "Entity";
    private static final String PAYLOAD = "Payload";
    private static final Logger LOGGER =
            LogManager.getLogger(SwidTagGateway.class);

    /**
     * This method validates the .swidtag file at the given filepath against the
     * schema. A successful validation results in the output of the tag's name
     * and tagId attributes, otherwise a generic error message is printed.
     *
     * @param path the location of the file to be validated
     */
    public boolean validateSwidTag(String path) throws IOException {
        JAXBElement jaxbe = unmarshallSwidTag(path, null);
        SoftwareIdentity swidTag = (SoftwareIdentity) jaxbe.getValue();
        String output = String.format("name: %s;\ntagId:  %s\n%s",
                swidTag.getName(), swidTag.getTagId(),
                SwidTagConstants.SCHEMA_STATEMENT);
        System.out.println("SWID Tag found: ");
        System.out.println(output);
        return true;
    }

    /**
     * This method validates the .swidtag file at the given filepath against the
     * schema. A successful validation results in the output of the tag's name
     * and tagId attributes, otherwise a generic error message is printed.
     *
     * @param fileStream the input stream of the file content to be validated
     */
    public SoftwareIdentity validateSwidTag(InputStream fileStream) throws IOException {
        JAXBElement jaxbe = unmarshallSwidTag(null, fileStream);
        SoftwareIdentity swidTag = (SoftwareIdentity) jaxbe.getValue();
        String output = String.format("name: %s;\ntagId:  %s\n%s",
                swidTag.getName(), swidTag.getTagId(),
                SwidTagConstants.SCHEMA_STATEMENT);

        LOGGER.error("SWID Tag found: ");
        LOGGER.error(output);
        return swidTag;
    }

    /**
     * This method calls the marshal() method that writes the swidtag data to the output file.
     *
     * @param jaxbe
     * @param outputFile
     */
    public void writeSwidTagFile(JAXBElement<SoftwareIdentity> jaxbe, File outputFile) {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(jaxbe, outputFile);
        } catch (JAXBException e) {
            System.out.println("Error generating xml: ");
            e.printStackTrace();
        } 
    }
    
    /**
     * Given an input swidtag at [path] parse any PCRs in the payload into an InputStream object.
     * This method will be used in a following pull request.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public ByteArrayInputStream parsePayload(String path) throws IOException {
        JAXBElement jaxbe = unmarshallSwidTag(path, null);
		SoftwareIdentity softwareIdentity = (SoftwareIdentity) jaxbe.getValue();
		String pcrs = "";
		if (!softwareIdentity.getEntityOrEvidenceOrLink().isEmpty()) {
			List<Object> swidtag = softwareIdentity.getEntityOrEvidenceOrLink();
			for (Object obj : swidtag) {
				try {
					JAXBElement element = (JAXBElement) obj;
					String elementName = element.getName().getLocalPart();
					if (elementName.equals(PAYLOAD)) {
						ResourceCollection rc = (ResourceCollection) element.getValue();
						if (!rc.getDirectoryOrFileOrProcess().isEmpty()) { // comment on this to chubtub
							pcrs = parsePCRs(rc.getDirectoryOrFileOrProcess());
						}
					}
				} catch (ClassCastException e) {
					System.out.println("Found a non-JAXBElement object!" + e.getMessage());
					throw new IOException("Found an invalid element in the swidtag file!");
				}
			}
		}
		return new ByteArrayInputStream(pcrs.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * This method creates an Entity object based on the parameters read in from
     * a properties file.
     *
     * @param properties the Properties object containing parameters from file
     * @return Entity object created from the properties
     */
    private Entity createEntity(Properties properties) {
        Entity entity = objectFactory.createEntity();
        entity.setName(properties.getProperty(SwidTagConstants.ENTITY_NAME));
        entity.setRegid(properties.getProperty(SwidTagConstants.ENTITY_REGID));
        String[] roles = properties.getProperty(SwidTagConstants.ENTITY_ROLE).split(",");
        for (int i = 0; i < roles.length; i++) {
            entity.getRole().add(roles[i]);
        }
        entity.setThumbprint(properties.getProperty(SwidTagConstants.ENTITY_THUMBPRINT));

        return entity;
    }

    /**
     * Thsi method creates a Link element based on the parameters read in from a properties
     * file.
     * @param properties the Properties object containing parameters from file
     * @return Link element created from the properties
     */
    private Link createLink(Properties properties) {
        Link link = objectFactory.createLink();
        link.setHref(properties.getProperty(SwidTagConstants.LINK_HREF));
        link.setRel(properties.getProperty(SwidTagConstants.LINK_REL));

        return link;
    }

    /**
     * This method creates a Meta element based on the parameters read in from a properties
     * file.
     * @param properties the Properties object containing parameters from file
     * @return the Meta element created from the properties
     */
    private SoftwareMeta createSoftwareMeta(Properties properties) {
        SoftwareMeta softwareMeta = objectFactory.createSoftwareMeta();
        Map<QName, String> attributes = softwareMeta.getOtherAttributes();
        attributes.put(_RIM_PCURILOCAL, properties.getProperty(SwidTagConstants.META_PCURILOCAL));
        attributes.put(_RIM_BINDINGSPEC, properties.getProperty(SwidTagConstants.META_BINDINGSPEC));
        attributes.put(_RIM_BINDINGSPECVERSION, properties.getProperty(SwidTagConstants.META_BINDINGSPECVERSION));
        attributes.put(_RIM_PLATFORMMANUFACTURERID, properties.getProperty(SwidTagConstants.META_PLATFORMMANUFACTURERID));
        attributes.put(_RIM_PLATFORMMANUFACTURERSTR, properties.getProperty(SwidTagConstants.META_PLATFORMMANUFACTURERSTR));
        attributes.put(_RIM_PLATFORMMODEL, properties.getProperty(SwidTagConstants.META_PLATFORMMODEL));
        attributes.put(_RIM_COMPONENTCLASS, properties.getProperty(SwidTagConstants.META_COMPONENTCLASS));
        attributes.put(_RIM_COMPONENTMANUFACTURER, properties.getProperty(SwidTagConstants.META_COMPONENTMANUFACTURER));
        attributes.put(_RIM_COMPONENTMANUFACTURERID, properties.getProperty(SwidTagConstants.META_COMPONENTMANUFACTURERID));
        attributes.put(_RIM_RIMLINKHASH, properties.getProperty(SwidTagConstants.META_RIMLINKHASH));

        return softwareMeta;
    }

    /**
     * This method creates a Directory from the parameters read in from a properties file.
     *
     * @param properties the Properties object containing parameters from file
     * @return Directory object created from the properties
     */
    private Directory createDirectory(Properties properties) {
        Directory directory = objectFactory.createDirectory();
        directory.setLocation(properties.getProperty(SwidTagConstants.DIRECTORY_LOCATION));
        directory.setName(properties.getProperty(SwidTagConstants.DIRECTORY_NAME));
        String directoryRoot = properties.getProperty(SwidTagConstants.DIRECTORY_ROOT);
        if (!directoryRoot.isEmpty()) {
            directory.setRoot(directoryRoot);
        }

        return directory;
    }

    /**
     * This method creates a hirs.swid.xjc.File from a java.nio.File object.
     * This method signature is not currently used and may be removed later.
     *
     * @param file
     * @return hirs.swid.xjc.File object from File object
     */
    private hirs.utils.xjc.File createFile(File file) {
        return createFile(file.getName(), "01.00", Long.toString(file.length()));
    }

    /**
     * This method creates a hirs.swid.xjc.File from three arguments, then calculates
     * and stores its hash as an attribute in itself.
     *
     * @param filename
     * @param version
     * @return hirs.utils.xjc.File object from File object
     */
    private hirs.utils.xjc.File createFile(String filename, String version, String size) {
        hirs.utils.xjc.File file = objectFactory.createFile();
        file.setName(filename);
        file.setVersion(version);
        file.setSize(new BigInteger(size));
        String hash = getHashValue(file.getName(), "SHA-256");
        file.getOtherAttributes().put(_SHA256_HASH, hash);

        return file;
    }

    /**
     * This method creates a Payload from the parameters read in from a properties file.
     *
     * @param properties the Properties object containing parameters from file
     * @return the Payload object created
     */
    private ResourceCollection createPayload(Properties properties) {
        ResourceCollection rc = objectFactory.createResourceCollection();

        rc.getOtherAttributes().put(_N8060_ENVVARPREFIX, properties.getProperty(SwidTagConstants.PAYLOAD_ENVVARPREFIX));
        rc.getOtherAttributes().put(_N8060_ENVVARSUFFIX, properties.getProperty(SwidTagConstants.PAYLOAD_ENVVARSUFFIX));
        rc.getOtherAttributes().put(_N8060_PATHSEPARATOR, properties.getProperty(SwidTagConstants.PAYLOAD_PATHSEPARATOR));

        return rc;
    }

    /**
     * This method creates a Payload from a list of Strings and a hash algorithm.
     * The Strings in the list are expected to be in the form of "[PCR_NUMBER],[PCR_VALUE]"
     * and the hash algorithm is attached as the file's xml namespace identifier.
     *
     * @param populate
     * @return
     */
    private ResourceCollection createPayload(List<String> populate, QName hashStr) {
        ResourceCollection rc = objectFactory.createResourceCollection();
        hirs.utils.xjc.File xjcFile = null;
        String[] tempArray = null;

        for (String item : populate) {
            xjcFile = objectFactory.createFile();

            tempArray = item.split(",");

            xjcFile.setName(tempArray[SwidTagConstants.PCR_NUMBER]);
            xjcFile.getOtherAttributes().put(hashStr, tempArray[SwidTagConstants.PCR_VALUE]);
            rc.getDirectoryOrFileOrProcess().add(xjcFile);
        }

        return rc;
    }

    /**
     * This method creates an xml signature based on the xmldsig schema.
     * This method is incomplete and not yet implemented.
     *
     * @return the Signature object created
     */
    private SignatureType createSignature() {
        SignatureType signature = objectFactory.createSignatureType();
        SignedInfoType signedInfo = objectFactory.createSignedInfoType();

        CanonicalizationMethodType canonicalizationMethod = objectFactory.createCanonicalizationMethodType();
        canonicalizationMethod.setAlgorithm("http://www.w3.org/TR/2001/REC-xml-c14n-20010315");

        SignatureMethodType signatureMethod = objectFactory.createSignatureMethodType();
        signatureMethod.setAlgorithm("http://www.w3.org/2000/09/xmldsig#rsa-sha512");

        ReferenceType reference = objectFactory.createReferenceType();
        TransformsType transforms = objectFactory.createTransformsType();

        TransformType transform = objectFactory.createTransformType();
        transform.setAlgorithm("http://www.w3.org/2000/09/xmldsig#enveloped-signature");
        transforms.getTransform().add(transform);

        DigestMethodType digestMethod = objectFactory.createDigestMethodType();
        digestMethod.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha256");

        reference.setTransforms(transforms);
        reference.setDigestMethod(digestMethod);
        reference.setDigestValue(new byte[10]);

        signedInfo.setCanonicalizationMethod(canonicalizationMethod);
        signedInfo.setSignatureMethod(signatureMethod);
        signedInfo.getReference().add(reference);

        SignatureValueType signatureValue = objectFactory.createSignatureValueType();
        signatureValue.setValue(new byte[10]);

        signature.setSignedInfo(signedInfo);

        return signature;
    }

    /**
     * This method traverses a hirs.swid.xjc.Directory recursively until it finds at
     * least one hirs.swid.xjc.File.  This File is expected to have an attribute of the form
     * "[hash algorithm]=[hash value]."
     *
     * @param list of swidtag elements
     * @return the hash value(s) parsed from the File object(s)
     */
    private String parsePCRs(List list) {
        final String newline = System.lineSeparator();
    	StringBuilder sb = new StringBuilder();
    	for (Object listItem : list) {
    		if (listItem instanceof Directory) {
    			Directory dir = (Directory) listItem;
    			if (!dir.getDirectoryOrFile().isEmpty()) {
    				parsePCRs(dir.getDirectoryOrFile());
    			}
    		} else if (listItem instanceof hirs.utils.xjc.File){
    			hirs.utils.xjc.File pcr = (hirs.utils.xjc.File) listItem;
    			String pcrHash = "";
    			if (!pcr.getOtherAttributes().isEmpty()) {
    				Object[] fileAttributes = pcr.getOtherAttributes().values().toArray();
    				pcrHash = (String) fileAttributes[0];
    			}
    			if (pcrHash.isEmpty()) {
    				pcrHash = "null";
    			}
    			sb.append(pcr.getName() + "," + pcrHash + newline);
    		}
    	}
    	System.out.println(sb.toString());
    	return sb.toString();
    }

    /**
     * This method unmarshalls the swidtag found at [path] and validates it according to the
     * schema.
     *
     * @param path to the input swidtag
     * @return the SoftwareIdentity element at the root of the swidtag
     * @throws IOException if the swidtag cannot be unmarshalled or validated
     */
    @SuppressWarnings("PMD")
    private JAXBElement unmarshallSwidTag(String path, InputStream stream) throws IOException {
    	File input = null;
    	InputStream is = null;
    	JAXBElement jaxbe = null;
        JAXBElement jaxbeFile = null;
    	try {
    		input = new File(path);
    		is = SwidTagGateway.class.getClassLoader().getResourceAsStream(SwidTagConstants.SCHEMA_URL);
    		SchemaFactory schemaFactory = SchemaFactory.newInstance(SwidTagConstants.SCHEMA_LANGUAGE);
    		Schema schema = schemaFactory.newSchema(new StreamSource(is));
    		JAXBContext jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
    		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    		unmarshaller.setSchema(schema);
    		jaxbeFile = (JAXBElement) unmarshaller.unmarshal(input);
    		jaxbe = (JAXBElement) unmarshaller.unmarshal(stream);
    	} catch (SAXException e) {
            LOGGER.error("Error setting schema for validation!");
        } catch (UnmarshalException e) {
            LOGGER.error("Error validating swidtag file!");
    	    LOGGER.error(e.getMessage());
    	    LOGGER.error(e.toString());
    	    for (StackTraceElement ste : e.getStackTrace()) {
    	        LOGGER.error(ste.toString());
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("Input file empty.");
        } catch (JAXBException e) {
            e.printStackTrace();
        } finally {
        	if (is != null) {
        		try {
        			is.close();
        		} catch (IOException e) {
                    LOGGER.error("Error closing input stream");
        		}
        	} else {
        	    LOGGER.error("input stream variable is null");
            }
            if (jaxbeFile != null) {
                return jaxbeFile;
            }
        	if (jaxbe != null) {
        	    return jaxbe;
        	} else {
                LOGGER.error(String.format("%s -> %s", "TDM", path));
                LOGGER.error(stream);
        	    throw new IOException("Invalid swidtag file!");
        	}
        }
    }

    /**
     * This method creates the hash based on the provided algorithm and salt
     * only accessible through helper methods.
     *
     * @param value string object to hash
     * @param sha the algorithm to use for the hash
     * @return
     */
    private static String getHashValue(String value, String sha) {
        String resultString = null;
        try {
            MessageDigest md = MessageDigest.getInstance(sha);
            byte[] bytes = md.digest(value.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            resultString = sb.toString();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException grex) {
            System.out.println(grex.getMessage());
        }

        return resultString;
    }
}
