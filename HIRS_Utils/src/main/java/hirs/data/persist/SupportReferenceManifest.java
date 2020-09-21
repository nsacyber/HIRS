package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import java.io.IOException;

/**
 * Sub class that will just focus on PCR Values and Events
 */
public class SupportReferenceManifest extends ReferenceManifest {
    private static final Logger LOGGER = LogManager.getLogger(SupportReferenceManifest.class);

    @Column(nullable = false)
    @JsonIgnore
    private int pcrHash;

    /**
     * Main constructor for the RIM object. This takes in a byte array of a
     * valid swidtag file and parses the information.
     *
     * @param rimBytes byte array representation of the RIM
     * @throws IOException if unable to unmarshal the string
     */
    public SupportReferenceManifest(final byte[] rimBytes) throws IOException {
        super(rimBytes);
        this.setRimType(SUPPORT_RIM);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected SupportReferenceManifest() {

    }

    public int getPcrHash() {
        return pcrHash;
    }

    public void setPcrHash(final int pcrHash) {
        this.pcrHash = pcrHash;
    }
}
