package hirs.attestationca.portal.utils;

import com.google.common.base.Preconditions;
import hirs.attestationca.utils.digest.DigestAlgorithm;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import hirs.attestationca.utils.xjc.File;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.Map;

/**
 * This object is used to represent the content of a Swid Tags Directory
 * section.
 */
public class SwidResource {

    private static final Logger LOGGER = LogManager.getLogger(SwidResource.class);

    @Getter
    private String name, size;
    @Getter
    private String rimFormat, rimType, rimUriGlobal, hashValue;
//    private TpmWhiteListBaseline tpmWhiteList;
    private DigestAlgorithm digest = DigestAlgorithm.SHA1;
    @Getter
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
     * @param file {@link File}
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