package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.ReferenceManifestSelector;
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
import java.util.Collection;

/**
 * Sub class that will just focus on PCR Values and Events.
 */
@Entity
public class SupportReferenceManifest extends ReferenceManifest {
    private static final Logger LOGGER = LogManager.getLogger(SupportReferenceManifest.class);

    @Column
    @JsonIgnore
    private int pcrHash = 0;
    @Column
    private boolean updated = false;

    /**
     * This class enables the retrieval of SupportReferenceManifest by their attributes.
     */
    public static class Selector extends ReferenceManifestSelector<SupportReferenceManifest> {
        /**
         * Construct a new ReferenceManifestSelector that will
         * use the given (@link ReferenceManifestManager}
         * to retrieve one or may SupportReferenceManifest.
         *
         * @param referenceManifestManager the reference manifest manager to be used to retrieve
         * reference manifests.
         */
        public Selector(final ReferenceManifestManager referenceManifestManager) {
            super(referenceManifestManager, SupportReferenceManifest.class);
        }

        /**
         * Specify the platform manufacturer that rims must have to be considered
         * as matching.
         * @param manufacturer string for the manufacturer
         * @return this instance
         */
        public Selector byManufacturer(final String manufacturer) {
            setFieldValue(PLATFORM_MANUFACTURER, manufacturer);
            return this;
        }

        /**
         * Specify the platform manufacturer id that rims must have to be considered
         * as matching.
         * @param manufacturerId string for the id of the manufacturer
         * @return this instance
         */
        public Selector byManufacturerId(final String manufacturerId) {
            setFieldValue(PLATFORM_MANUFACTURER_ID, manufacturerId);
            return this;
        }

        /**
         * Specify the platform model that rims must have to be considered
         * as matching.
         * @param model string for the model
         * @return this instance
         */
        public Selector byModel(final String model) {
            setFieldValue(PLATFORM_MODEL, model);
            return this;
        }

        /**
         * Specify the file name that rims should have.
         * @param fileName the name of the file associated with the rim
         * @return this instance
         */
        public Selector byFileName(final String fileName) {
            setFieldValue(RIM_FILENAME_FIELD, fileName);
            return this;
        }

        /**
         * Specify the RIM hash associated with the support RIM.
         * @param rimHash the hash of the file associated with the rim
         * @return this instance
         */
        public Selector byRimHash(final String rimHash) {
            setFieldValue(RIM_HASH_FIELD, rimHash);
            return this;
        }
    }

    /**
     * Main constructor for the RIM object. This takes in a byte array of a
     * valid swidtag file and parses the information.
     *
     * @param fileName - string representation of the uploaded file.
     * @param rimBytes byte array representation of the RIM
     * @throws IOException if unable to unmarshal the string
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
     * @throws IOException if unable to unmarshal the string
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
     * Get a Selector for use in retrieving ReferenceManifest.
     *
     * @param rimMan the ReferenceManifestManager to be used to retrieve
     * persisted RIMs
     * @return a Selector instance to use for retrieving RIMs
     */
    public static Selector select(final ReferenceManifestManager rimMan) {
        return new Selector(rimMan);
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

    /**
     * Indicates if the support rim has updated information from the base.
     * @return flag indicating that it is up to date
     */
    public boolean isUpdated() {
        return updated;
    }

    /**
     * Setter for the support RIM flag status.
     * @param updated updated flag status
     */
    public void setUpdated(final boolean updated) {
        this.updated = updated;
    }
}
