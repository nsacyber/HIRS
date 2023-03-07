package hirs.attestationca.persist.entity.userdefined.rim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.enums.AppraisalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

/**
 * Sub class that will just focus on PCR Values and Events.
 * Similar to {@link SupportReferenceManifest}
 * however this is the live log from the client.
 */
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
}