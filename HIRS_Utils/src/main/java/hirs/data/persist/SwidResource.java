package hirs.data.persist;

import com.google.common.base.Preconditions;
import hirs.utils.xjc.File;
import java.util.Map;
import java.math.BigInteger;
import javax.xml.namespace.QName;

/**
 * This object is used to represent the content of a Swid Tags Directory
 * section.
 */
public class SwidResource {

    private String name, size;

    private String rimFormat, rimType, rimUriGlobal, hashValue;

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
     * @param file {@link hirs.utils.xjc.File}
     */
    public SwidResource(final File file) {
        Preconditions.checkArgument(file != null,
                "Cannot construct a RIM Resource from a null File object");

        this.name = file.getName();
        // at this time, there is a possibility to get an object with
        // not size even though it is required.
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
    }

    /**
     * Getter for the file name.
     * @return string of the file name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the file size.
     * @return string of the file size.
     */
    public String getSize() {
        return size;
    }

    /**
     * Getter for the RIM format for the resource.
     * @return string of the format
     */
    public String getRimFormat() {
        return rimFormat;
    }

    /**
     * Getter for the RIM resource type.
     * @return string of the resource type.
     */
    public String getRimType() {
        return rimType;
    }

    /**
     * Getter for the RIM Global URI.
     * @return string of the URI
     */
    public String getRimUriGlobal() {
        return rimUriGlobal;
    }

    /**
     * Getter for the associated Hash.
     * @return string of the hash
     */
    public String getHashValue() {
        return hashValue;
    }
}
