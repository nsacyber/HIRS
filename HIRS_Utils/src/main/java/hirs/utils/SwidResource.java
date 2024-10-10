package hirs.utils;

import com.google.common.base.Preconditions;
import hirs.utils.digest.DigestAlgorithm;
import hirs.utils.xjc.File;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.Map;

/**
 * This object is used to represent the content of a Swid Tags Directory
 * section.
 */
@ToString
public class SwidResource {

    @Getter
    private final boolean validFileSize = false;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String size;

    @Getter
    @Setter
    private String hashValue;

    @Getter
    private String rimFormat;

    @Getter
    private String rimType;

    @Getter
    private String rimUriGlobal;
    //    private TpmWhiteListBaseline tpmWhiteList;
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
    }

    /**
     * The main constructor that processes a {@code hirs.utils.xjc.File}.
     *
     * @param file   {@link File}
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
//        tpmWhiteList = new TpmWhiteListBaseline(this.name);
    }
}
