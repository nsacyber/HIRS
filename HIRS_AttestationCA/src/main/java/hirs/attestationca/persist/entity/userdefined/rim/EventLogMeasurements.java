package hirs.attestationca.persist.entity.userdefined.rim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.service.ReferenceManifestService;
import hirs.attestationca.persist.service.selector.ReferenceManifestSelector;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Sub class that will just focus on PCR Values and Events.
 * Similar to {@link SupportReferenceManifest}
 * however this is the live log from the client.
 */
@Log4j2
@EqualsAndHashCode(callSuper=false)
@Entity
public class EventLogMeasurements extends ReferenceManifest {

    @Column
    @JsonIgnore
    @Getter @Setter
    private int pcrHash = 0;
    @Enumerated(EnumType.STRING)
    @Getter @Setter
    private AppraisalStatus.Status overallValidationResult = AppraisalStatus.Status.FAIL;

    /**
     * This class enables the retrieval of SupportReferenceManifest by their attributes.
     */
    public static class Selector extends ReferenceManifestSelector<EventLogMeasurements> {
        /**
         * Construct a new ReferenceManifestSelector that
         * will use the given (@link ReferenceManifestService}
         * to retrieve one or may SupportReferenceManifest.
         *
         * @param referenceManifestManager the reference manifest manager to be used to retrieve
         * reference manifests.
         */
        public Selector(final ReferenceManifestService referenceManifestManager) {
            super(referenceManifestManager, EventLogMeasurements.class, false);
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
         * Specify the device name that rims must have to be considered
         * as matching.
         * @param deviceName string for the deviceName
         * @return this instance
         */
        public Selector byDeviceName(final String deviceName) {
            setFieldValue("deviceName", deviceName);
            return this;
        }

        /**
         * Specify the RIM hash associated with the Event Log.
         * @param hexDecHash the hash of the file associated with the rim
         * @return this instance
         */
        public Selector byHexDecHash(final String hexDecHash) {
            setFieldValue(HEX_DEC_HASH_FIELD, hexDecHash);
            return this;
        }
    }

    /**
     * Support constructor for the RIM object.
     *
     * @param rimBytes byte array representation of the RIM
     * @throws java.io.IOException if unable to unmarshal the string
     */
    public EventLogMeasurements(final byte[] rimBytes) throws IOException {
        this("blank.measurement", rimBytes);
    }

    /**
     * Support constructor for the RIM object.
     *
     * @param fileName - string representation of the uploaded file.
     * @param rimBytes byte array representation of the RIM
     * @throws java.io.IOException if unable to unmarshal the string
     */
    public EventLogMeasurements(final String fileName,
                                final byte[] rimBytes) throws IOException {
        super(rimBytes);
        this.setFileName(fileName);
        this.archive("Event Log Measurement");
        this.setRimType(MEASUREMENT_RIM);
        this.pcrHash = 0;
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected EventLogMeasurements() {
        super();
        this.pcrHash = 0;
    }

    /**
     * Get a Selector for use in retrieving ReferenceManifest.
     *
     * @param rimMan the ReferenceManifestService to be used to retrieve
     * persisted RIMs
     * @return a Selector instance to use for retrieving RIMs
     */
    public static Selector select(final ReferenceManifestService rimMan) {
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
            log.error(cEx);
        } catch (NoSuchAlgorithmException noSaEx) {
            log.error(noSaEx);
        } catch (IOException ioEx) {
            log.error(ioEx);
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
            log.error(cEx);
        } catch (NoSuchAlgorithmException noSaEx) {
            log.error(noSaEx);
        } catch (IOException ioEx) {
            log.error(ioEx);
        }

        return new ArrayList<>();
    }
}