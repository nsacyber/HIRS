package hirs.swid;

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

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.math.BigInteger;

import hirs.swid.utils.CsvParser;
import hirs.swid.utils.HashSwid;
import hirs.swid.xjc.BaseElement;
import hirs.swid.xjc.CanonicalizationMethodType;
import hirs.swid.xjc.DigestMethodType;
import hirs.swid.xjc.Directory;
import hirs.swid.xjc.Entity;
import hirs.swid.xjc.Link;
import hirs.swid.xjc.ObjectFactory;
import hirs.swid.xjc.ResourceCollection;
import hirs.swid.xjc.ReferenceType;
import hirs.swid.xjc.SignatureType;
import hirs.swid.xjc.SignatureValueType;
import hirs.swid.xjc.SignatureMethodType;
import hirs.swid.xjc.SignedInfoType;
import hirs.swid.xjc.SoftwareIdentity;
import hirs.swid.xjc.SoftwareMeta;
import hirs.swid.xjc.TransformType;
import hirs.swid.xjc.TransformsType;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.Location;
import com.eclipsesource.json.ParseException;



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

    private final ObjectFactory objectFactory = new ObjectFactory();
    private final File generatedFile = new File("generated_swidTag.swidtag");
    private final Path configFile = Paths.get("/etc/hirs/rim_fields.json");
    private QName hashValue = null;
    
    private static final String ENTITY = "Entity";
    private static final String PAYLOAD = "Payload";

    /**
     * default generator method that has no parameters
     */
    public void generateSwidTag() {
        generateSwidTag(generatedFile);
    }

    /**
     * This generator method is used by the create method.
     *
     * This method should be updated to incorporate the RIM fields that are implemented
     * in generateSwidTag(final File outputFile) below.
     *
     * @param inputFile - the file in csv format that is used as data
     * @param outputFile - output specific to the given file
     * @param hashType - the optional labeling of the hash type
     */
    public void generateSwidTag(final String inputFile,
            final String outputFile, final String hashType) {
        // create file instances
        File input = new File(inputFile);
        File output = new File(outputFile);
        List<String> tempList = new LinkedList<>();

        // I need to go over this again about which needs to be checked.
        if (input.exists()) {
            // parse the csv file
            CsvParser parser = new CsvParser(input);
            for (String line : parser.getContent()) {
                tempList.add(line);
            }

            if (hashType.contains("256")) {
                hashValue = _DEFAULT_QNAME;
            } else if (hashType.contains("384")) {
                hashValue = _SHA384Value_QNAME;
            } else if (hashType.contains("512")) {
                hashValue = _SHA512Value_QNAME;
            } else if (hashType.contains("1")) {
                hashValue = _SHA1Value_QNAME;
            } else {
                hashValue = _DEFAULT_QNAME;
            }

            // generate a swid tag
            Properties properties = new Properties();
            InputStream is = null;
            try {
                is = SwidTagGateway.class.getClassLoader().getResourceAsStream(SwidTagConstants.HIRS_SWIDTAG_HEADERS);
                properties.load(is);

                SoftwareIdentity swidTag = createSwidTag(new JsonObject());

                JAXBElement<Entity> entity = objectFactory.createSoftwareIdentityEntity(createEntity(new JsonObject()));
                swidTag.getEntityOrEvidenceOrLink().add(entity);

                // we should have resources, there for we need a collection
                JAXBElement<ResourceCollection> resources = objectFactory.createSoftwareIdentityPayload(createPayload(tempList, hashValue));
                swidTag.getEntityOrEvidenceOrLink().add(resources);

                JAXBElement<SoftwareIdentity> jaxbe = objectFactory.createSoftwareIdentity(swidTag);
                writeSwidTagFile(jaxbe, output);
            } catch (IOException e) {
                System.out.println("Error reading properties file: ");
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * This method generates a base RIM from the values in a JSON file.
     *
     * @param outputFile
     */
    public void generateSwidTag(final File outputFile) {
        SoftwareIdentity swidTag = null;
        try {
            BufferedReader jsonIn = Files.newBufferedReader(configFile, StandardCharsets.UTF_8);
            JsonObject configProperties = Json.parse(jsonIn).asObject();
            //SoftwareIdentity
            swidTag = createSwidTag(configProperties.get(SwidTagConstants.SOFTWARE_IDENTITY).asObject());
            //Entity
            JAXBElement<Entity> entity = objectFactory.createSoftwareIdentityEntity(
                    createEntity(configProperties.get(SwidTagConstants.ENTITY).asObject()));
            swidTag.getEntityOrEvidenceOrLink().add(entity);
            //Link
            JAXBElement<Link> link = objectFactory.createSoftwareIdentityLink(
                    createLink(configProperties.get(SwidTagConstants.LINK).asObject()));
            swidTag.getEntityOrEvidenceOrLink().add(link);
            //Meta
            JAXBElement<SoftwareMeta> meta = objectFactory.createSoftwareIdentityMeta(
                    createSoftwareMeta(configProperties.get(SwidTagConstants.META).asObject()));
            swidTag.getEntityOrEvidenceOrLink().add(meta);
            //File
            hirs.swid.xjc.File file = createFile(
                    configProperties.get(SwidTagConstants.PAYLOAD).asObject()
                                    .get(SwidTagConstants.DIRECTORY).asObject()
                                    .get(SwidTagConstants.FILE).asObject());
            //Directory
            Directory directory = createDirectory(
                    configProperties.get(SwidTagConstants.PAYLOAD).asObject()
                                    .get(SwidTagConstants.DIRECTORY).asObject());
            directory.getDirectoryOrFile().add(file);
            //Payload
            ResourceCollection payload = createPayload(
                    configProperties.get(SwidTagConstants.PAYLOAD).asObject());
            payload.getDirectoryOrFileOrProcess().add(directory);
            JAXBElement<ResourceCollection> jaxbPayload =
                    objectFactory.createSoftwareIdentityPayload(payload);
            swidTag.getEntityOrEvidenceOrLink().add(jaxbPayload);

        } catch (FileNotFoundException e) {
            System.out.println("File does not exist or cannot be read: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error in file reader: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("Invalid JSON detected at " + e.getLocation().toString());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        JAXBElement<SoftwareIdentity> jaxbe = objectFactory.createSoftwareIdentity(swidTag);
        writeSwidTagFile(jaxbe, outputFile);
    }

   /**
     * This method validates the .swidtag file at the given filepath against the
     * schema. A successful validation results in the output of the tag's name
     * and tagId attributes, otherwise a generic error message is printed.
     *
     * @param path the location of the file to be validated
     */
    public boolean validateSwidTag(String path) throws IOException {
        JAXBElement jaxbe = unmarshallSwidTag(path);
        SoftwareIdentity swidTag = (SoftwareIdentity) jaxbe.getValue();
        String output = String.format("name: %s;\ntagId:  %s\n%s",
                swidTag.getName(), swidTag.getTagId(),
                SwidTagConstants.SCHEMA_STATEMENT);
        System.out.println("SWID Tag found: ");
        System.out.println(output);
        return true;
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
        JAXBElement jaxbe = unmarshallSwidTag(path);
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
						if (!rc.getDirectoryOrFileOrProcess().isEmpty()) {
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
     * This method creates SoftwareIdentity element based on the parameters read in from
     * a properties file.
     *
     * @param properties the Properties object containing parameters from file
     * @return SoftwareIdentity object created from the properties
     */
    private SoftwareIdentity createSwidTag(JsonObject jsonObject) {
        SoftwareIdentity swidTag = objectFactory.createSoftwareIdentity();
        swidTag.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
        swidTag.setTagId(jsonObject.getString(SwidTagConstants.TAGID, ""));
        swidTag.setVersion(jsonObject.getString(SwidTagConstants.VERSION, ""));
        swidTag.setCorpus(jsonObject.getBoolean(SwidTagConstants.CORPUS, false));
        swidTag.setPatch(jsonObject.getBoolean(SwidTagConstants.PATCH, false));
        swidTag.setSupplemental(jsonObject.getBoolean(SwidTagConstants.SUPPLEMENTAL, false));

        return swidTag;
    }

    /**
     * This method creates an Entity object based on the parameters read in from
     * a properties file.
     *
     * @param properties the Properties object containing parameters from file
     * @return Entity object created from the properties
     */
    private Entity createEntity(JsonObject jsonObject) {
        Entity entity = objectFactory.createEntity();
        entity.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
        //entity.setRegid(jsonObject.getString(SwidTagConstants.REGID));
        String[] roles = jsonObject.getString(SwidTagConstants.ROLE, "").split(",");
        for (int i = 0; i < roles.length; i++) {
            entity.getRole().add(roles[i]);
        }
        entity.setThumbprint(jsonObject.getString(SwidTagConstants.THUMBPRINT, ""));

        return entity;
    }

    /**
     * Thsi method creates a Link element based on the parameters read in from a properties
     * file.
     * @param properties the Properties object containing parameters from file
     * @return Link element created from the properties
     */
    private Link createLink(JsonObject jsonObject) {
        Link link = objectFactory.createLink();
        link.setHref(jsonObject.getString(SwidTagConstants.HREF, ""));
        link.setRel(jsonObject.getString(SwidTagConstants.REL, ""));

        return link;
    }

    /**
     * This method creates a Meta element based on the parameters read in from a properties
     * file.
     * @param properties the Properties object containing parameters from file
     * @return the Meta element created from the properties
     */
    private SoftwareMeta createSoftwareMeta(JsonObject jsonObject) {
        SoftwareMeta softwareMeta = objectFactory.createSoftwareMeta();
        Map<QName, String> attributes = softwareMeta.getOtherAttributes();
        //attributes.put(SwidTagConstants._RIM_PCURILOCAL, jsonObject.getString(SwidTagConstants.PCURILOCAL, ""));
        attributes.put(SwidTagConstants._RIM_BINDINGSPEC, jsonObject.getString(SwidTagConstants.BINDING_SPEC, ""));
        attributes.put(SwidTagConstants._RIM_BINDINGSPECVERSION, jsonObject.getString(SwidTagConstants.BINDING_SPEC_VERSION, ""));
        attributes.put(SwidTagConstants._RIM_PLATFORMMANUFACTURERID, jsonObject.getString(SwidTagConstants.PLATFORM_MANUFACTURER_ID, ""));
        attributes.put(SwidTagConstants._RIM_PLATFORMMANUFACTURERSTR, jsonObject.getString(SwidTagConstants.PLATFORM_MANUFACTURER_STR, ""));
        attributes.put(SwidTagConstants._RIM_PLATFORMMODEL, jsonObject.getString(SwidTagConstants.PLATFORM_MODEL, ""));
        //attributes.put(SwidTagConstants._RIM_COMPONENTCLASS, jsonObject.getString(SwidTagConstants.COMPONENTCLASS, ""));
        //attributes.put(SwidTagConstants._RIM_COMPONENTMANUFACTURER, jsonObject.getString(SwidTagConstants.COMPONENTMANUFACTURER, ""));
        //attributes.put(SwidTagConstants._RIM_COMPONENTMANUFACTURERID, jsonObject.getString(SwidTagConstants.OMPONENTMANUFACTURERID, ""));
        attributes.put(SwidTagConstants._RIM_RIMLINKHASH, jsonObject.getString(SwidTagConstants.RIM_LINK_HASH, ""));

        return softwareMeta;
    }

    /**
     * This method creates a Payload from the parameters read in from a properties file.
     *
     * @param properties the Properties object containing parameters from file
     * @return the Payload object created
     */
    private ResourceCollection createPayload(JsonObject jsonObject) {
        ResourceCollection rc = objectFactory.createResourceCollection();
/*
        rc.getOtherAttributes().put(SwidTagConstants._N8060_ENVVARPREFIX, jsonObject.getString(SwidTagConstants.PAYLOAD_ENVVARPREFIX));
        rc.getOtherAttributes().put(SwidTagConstants._N8060_ENVVARSUFFIX, jsonObject.getString(SwidTagConstants.PAYLOAD_ENVVARSUFFIX));
        rc.getOtherAttributes().put(SwidTagConstants._N8060_PATHSEPARATOR, jsonObject.getString(SwidTagConstants.PAYLOAD_PATHSEPARATOR));
*/
        return rc;
    }

    /**
     * This method creates a Directory from the parameters read in from a properties file.
     *
     * @param properties the Properties object containing parameters from file
     * @return Directory object created from the properties
     */
    private Directory createDirectory(JsonObject jsonObject) {
        Directory directory = objectFactory.createDirectory();
        directory.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
//        System.out.println(jsonObject.getString(SwidTagConstants.NAME, ""));
/*
        directory.setLocation(jsonObject.getString(SwidTagConstants.DIRECTORY_LOCATION));
        String directoryRoot = jsonObject.getString(SwidTagConstants.DIRECTORY_ROOT);
        if (!directoryRoot.isEmpty()) {
            directory.setRoot(directoryRoot);
        }
*/
        return directory;
    }

   /**
     * This method creates a hirs.swid.xjc.File from three arguments, then calculates
     * and stores its hash as an attribute in itself.
     *
     * @param filename
     * @param location
     * @return hirs.swid.xjc.File object from File object
     */
    private hirs.swid.xjc.File createFile(JsonObject jsonObject) {
        hirs.swid.xjc.File file = objectFactory.createFile();
        file.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
        file.setSize(new BigInteger(jsonObject.getString(SwidTagConstants.SIZE, "")));
        file.getOtherAttributes().put(_SHA256_HASH, jsonObject.getString(SwidTagConstants.HASH, ""));

        return file;
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
        hirs.swid.xjc.File xjcFile = null;
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
    		} else if (listItem instanceof hirs.swid.xjc.File){
    			hirs.swid.xjc.File pcr = (hirs.swid.xjc.File) listItem;
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
    private JAXBElement unmarshallSwidTag(String path) throws IOException {
    	File input = null;
    	InputStream is = null;
    	JAXBElement jaxbe = null;
    	try {
    		input = new File(path);
    		is = SwidTagGateway.class.getClassLoader().getResourceAsStream(SwidTagConstants.SCHEMA_URL);
    		SchemaFactory schemaFactory = SchemaFactory.newInstance(SwidTagConstants.SCHEMA_LANGUAGE);
    		Schema schema = schemaFactory.newSchema(new StreamSource(is));
    		JAXBContext jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
    		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    		unmarshaller.setSchema(schema);
    		jaxbe = (JAXBElement) unmarshaller.unmarshal(input);
    	} catch (SAXException e) {
            System.out.println("Error setting schema for validation!");
        } catch (UnmarshalException e) {
            System.out.println("Error validating swidtag file!");
        } catch (IllegalArgumentException e) {
            System.out.println("Input file empty.");
        } catch (JAXBException e) {
            e.printStackTrace();
        } finally {
        	if (is != null) {
        		try {
        			is.close();
        		} catch (IOException e) {
        			System.out.println("Error closing input stream");
        		}
        	}
        	if (jaxbe != null) {
        	    return jaxbe;
        	} else {
        	    throw new IOException("Invalid swidtag file!");
        	}
        }
    }
}
