package hirs.data.persist;

import com.google.common.base.Preconditions;
import hirs.data.persist.baseline.TpmWhiteListBaseline;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.tpm.eventlog.TCGEventLog;
import hirs.utils.xjc.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.DecimalFormat;
import java.util.Arrays;
import javax.xml.namespace.QName;
import org.apache.commons.codec.DecoderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This object is used to represent the content of a Swid Tags Directory
 * section.
 */
public class SwidResource {

    private static final Logger LOGGER = LogManager.getLogger(SwidResource.class);

    private static final String CATALINA_HOME = System.getProperty("catalina.base");
    private static final String TOMCAT_UPLOAD_DIRECTORY
            = "/webapps/HIRS_AttestationCAPortal/upload/";

    /**
     * String holder for location for storing binaries.
     */
    public static final String RESOURCE_UPLOAD_FOLDER
            = CATALINA_HOME + TOMCAT_UPLOAD_DIRECTORY;

    private String name, size;

    private String rimFormat, rimType, rimUriGlobal, hashValue;
    private List<String> pcrValues;
    private TpmWhiteListBaseline tpmWhiteList;
    private DigestAlgorithm digest = DigestAlgorithm.SHA1;

    /**
     * Default constructor.
     */
    public SwidResource() {
        name = null;
        size = null;
        rimFormat = null;
        rimType = null;
        rimUriGlobal = null;
        hashValue = null;
        pcrValues = null;
    }

    /**
     * The main constructor that processes a {@code hirs.utils.xjc.File}.
     *
     * @param file {@link hirs.utils.xjc.File}
     * @param digest algorithm associated with pcr values
     */
    public SwidResource(final File file, final DigestAlgorithm digest) {
        Preconditions.checkArgument(file != null,
                "Cannot construct a RIM Resource from a null File object");

        this.name = file.getName();
        // at this time, there is a possibility to get an object with
        // no size even though it is required.
        if (file.getSize() != null) {
            this.size = file.getSize().toString();
        } else {
            this.size = BigInteger.ZERO.toString();
        }

        for (Map.Entry<QName, String> entry
                : file.getOtherAttributes().entrySet()) {
            switch (entry.getKey().getLocalPart()) {
                case "supportRIMFormat":
                    this.rimFormat = entry.getValue();
                    break;
                case "supportRIMType":
                    this.rimType = entry.getValue();
                    break;
                case "supportRIMURIGlobal":
                    this.rimUriGlobal = entry.getValue();
                    break;
                case "hash":
                    this.hashValue = entry.getValue();
                    break;
                default:
            }
        }

        this.digest = digest;
        parsePcrValues();
        tpmWhiteList = new TpmWhiteListBaseline(this.name);
        if (!pcrValues.isEmpty()) {
            int i = 0;
            for (String pcr : pcrValues) {
                if (this.digest == null) {
                    // determine by length of pcr value
                    this.digest = AbstractDigest.getDigestAlgorithm(pcr);
                }
                try {
                    tpmWhiteList.addToBaseline(
                            new TPMMeasurementRecord(i++, pcr));
                } catch (DecoderException deEx) {
                    LOGGER.error(deEx);
                }
            }
        }
    }

    /**
     * Getter for the file name.
     *
     * @return string of the file name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the file size.
     *
     * @return string of the file size.
     */
    public String getSize() {
        return size;
    }

    /**
     * Getter for the RIM format for the resource.
     *
     * @return string of the format
     */
    public String getRimFormat() {
        return rimFormat;
    }

    /**
     * Getter for the RIM resource type.
     *
     * @return string of the resource type.
     */
    public String getRimType() {
        return rimType;
    }

    /**
     * Getter for the RIM Global URI.
     *
     * @return string of the URI
     */
    public String getRimUriGlobal() {
        return rimUriGlobal;
    }

    /**
     * Getter for the associated Hash of the file.
     *
     * @return string of the hash
     */
    public String getHashValue() {
        return hashValue;
    }

    /**
     * Getter for the list of PCR Values.
     *
     * @return an unmodifiable list
     */
    public List<String> getPcrValues() {
        return Collections.unmodifiableList(pcrValues);
    }

    /**
     * Setter for the list of associated PCR Values.
     *
     * @param pcrValues a collection of PCRs
     */
    public void setPcrValues(final List<String> pcrValues) {
        this.pcrValues = pcrValues;
    }

    /**
     * Getter for a generated map of the PCR values.
     *
     * @return mapping of PCR# to the actual value.
     */
    public LinkedHashMap<String, String> getPcrMap() {
        LinkedHashMap<String, String> innerMap = new LinkedHashMap<>();
        DecimalFormat df = new DecimalFormat("00");

        if (!this.pcrValues.isEmpty()) {
            long iterate = 0;
            String pcrNum;
            for (String string : this.pcrValues) {
                pcrNum = df.format(iterate++);
                innerMap.put(String.format("PCR%s:", pcrNum), string);
            }
        }

        return innerMap;
    }

    private void parsePcrValues() {
        TCGEventLog logProcessor = new TCGEventLog();

        try {
            Path logPath = Paths.get(String.format("%s/%s",
                    SwidResource.RESOURCE_UPLOAD_FOLDER,
                    this.getName()));
            if (Files.exists(logPath)) {
                logProcessor = new TCGEventLog(
                        Files.readAllBytes(logPath));
            }
            this.setPcrValues(Arrays.asList(
                        logProcessor.getExpectedPCRValues()));
        } catch (NoSuchFileException nsfEx) {
            LOGGER.error(String.format("File Not found!: %s",
                    this.getName()));
            LOGGER.error(nsfEx);
        } catch (IOException ioEx) {
            LOGGER.error(ioEx);
        } catch (CertificateException cEx) {
            LOGGER.error(cEx);
        } catch (NoSuchAlgorithmException naEx) {
            LOGGER.error(naEx);
        }
    }
}
