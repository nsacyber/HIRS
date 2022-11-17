package hirs.attestationca.entity;

import com.google.common.base.Preconditions;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.utils.xjc.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.Map;

/**
 * This object is used to represent the content of a Swid Tags Directory
 * section.
 */
public class SwidResource {

    private static final Logger LOGGER = LogManager.getLogger(SwidResource.class);

    private String name, size;
    private String rimFormat, rimType, rimUriGlobal, hashValue;
    private DigestAlgorithm digest = DigestAlgorithm.SHA1;
    private boolean validFileSize = false;

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
     * flag for if the file sizes match with the swidtag.
     * @return true if they match
     */
    public boolean isValidFileSize() {
        return validFileSize;
    }
}
