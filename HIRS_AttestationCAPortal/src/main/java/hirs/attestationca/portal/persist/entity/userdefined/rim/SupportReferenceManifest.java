package hirs.attestationca.portal.persist.entity.userdefined.rim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.utils.tpm.eventlog.TCGEventLog;
import hirs.attestationca.utils.tpm.eventlog.TpmPcrEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Sub class that will just focus on PCR Values and Events.
 */
@Getter
@Setter
@Entity
public class SupportReferenceManifest extends ReferenceManifest {

    private static final Logger LOGGER = LogManager.getLogger(SupportReferenceManifest.class);
    @Column
    @JsonIgnore
    private int pcrHash = 0;
    @Column
    private boolean updated = false;
    @Column
    private boolean processed = false;

    /**
     * Main constructor for the RIM object. This takes in a byte array of a
     * valid swidtag file and parses the information.
     *
     * @param fileName - string representation of the uploaded file.
     * @param rimBytes byte array representation of the RIM
     * @throws java.io.IOException if unable to unmarshal the string
     */
    public SupportReferenceManifest(final String fileName,
                                    final byte[] rimBytes) throws IOException {
        super(rimBytes);
        this.setFileName(fileName);
        this.setRimType(SUPPORT_RIM);
        this.pcrHash = 0;
    }

    /**
     * Main constructor for the RIM object. This takes in a byte array of a
     * valid swidtag file and parses the information.
     *
     * @param rimBytes byte array representation of the RIM
     * @throws java.io.IOException if unable to unmarshal the string
     */
    public SupportReferenceManifest(final byte[] rimBytes) throws IOException {
        this("blank.rimel", rimBytes);
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
    public Collection<TpmPcrEvent> getEventLog() {
        TCGEventLog logProcessor = null;
        try {
            logProcessor = new TCGEventLog(this.getRimBytes());
            return logProcessor.getEventList();
        } catch (CertificateException cEx) {
            LOGGER.error(cEx);
        } catch (NoSuchAlgorithmException noSaEx) {
            LOGGER.error(noSaEx);
        } catch (IOException ioEx) {
            LOGGER.error(ioEx);
        }

        return new ArrayList<>();
    }
}
