package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.tpm.eventlog.TCGEventLog;
import hirs.tpm.eventlog.TpmPcrEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sub class that will just focus on PCR Values and Events.
 */
@Entity
public class SupportReferenceManifest extends ReferenceManifest {
    private static final Logger LOGGER = LogManager.getLogger(SupportReferenceManifest.class);

    @Column
    @JsonIgnore
    private int pcrHash = 0;

    /**
     * Support constructor for the RIM object.
     *
     * @param fileName - string representation of the uploaded file.
     * @param rimBytes byte array representation of the RIM
     * @throws IOException if unable to unmarshal the string
     */
    public SupportReferenceManifest(final String fileName,
                                    final byte[] rimBytes) throws IOException {
        this(rimBytes);
        this.setRimType(SUPPORT_RIM);
        this.setFileName(fileName);
    }

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
        this.pcrHash = 0;
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected SupportReferenceManifest() {
        super();
        this.pcrHash = 0;
    }

    /**
     * Getter method for the expected PCR values contained within the support
     * RIM.
     * @return a string array of the pcr values.
     */
    public String[] getExpectedPCRList() {
        try {
            TCGEventLog logProcessor = new TCGEventLog(this.getRimBytes());
            this.pcrHash = Arrays.hashCode(logProcessor.getExpectedPCRValues());
            return logProcessor.getExpectedPCRValues();
        } catch (CertificateException cEx) {
            LOGGER.error(cEx);
        } catch (NoSuchAlgorithmException noSaEx) {
            LOGGER.error(noSaEx);
        } catch (IOException ioEx) {
            LOGGER.error(ioEx);
        }

        return new String[0];
    }

    /**
     * Getter method for the event log that should be present in the support RIM.
     *
     * @return list of TPM PCR Events for display
     */
    public List<TpmPcrEvent> getEventLog() {
        TCGEventLog logProcessor = null;
        try {
            logProcessor = new TCGEventLog(this.getRimBytes());
            return Collections.unmodifiableList(logProcessor.getEventList());
        } catch (CertificateException cEx) {
            LOGGER.error(cEx);
        } catch (NoSuchAlgorithmException noSaEx) {
            LOGGER.error(noSaEx);
        } catch (IOException ioEx) {
            LOGGER.error(ioEx);
        }

        return new ArrayList<>();
    }

    /**
     * Getter for the PCR Hash contained in the support RIM.
     * @return hash in int form
     */
    public int getPcrHash() {
        return pcrHash;
    }

    /**
     * Setter for the PCR Hash.
     * @param pcrHash hash in int form
     */
    public void setPcrHash(final int pcrHash) {
        this.pcrHash = pcrHash;
    }
}
