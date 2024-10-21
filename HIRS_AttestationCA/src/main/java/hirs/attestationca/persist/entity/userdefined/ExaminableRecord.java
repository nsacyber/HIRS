package hirs.attestationca.persist.entity.userdefined;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Specifies properties for an object that can be examined.
 */
@Log4j2
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@Access(AccessType.FIELD)
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class ExaminableRecord {

    @Getter
    @Column(nullable = false)
    // Decided on ORDINAL instead of STRING due to concerns surrounding overall size and retrieval
    // time of field from database. Consistent with other implementations of ExaminableRecord.
    @Enumerated(EnumType.ORDINAL)
    private ExamineState examineState = ExamineState.UNEXAMINED;

    /**
     * Sets the examine state for this record.
     *
     * @param examineState the examine state
     */
    public void setExamineState(final ExamineState examineState) {
        if (examineState == ExamineState.UNEXAMINED) {
            log.error("Can't set ExamineState on ExaminableRecord to Unexamined");
            throw new IllegalArgumentException(
                    "Can't set ExamineState on ExaminableRecord to Unexamined"
            );
        }

        this.examineState = examineState;
    }

    /**
     * State capturing if a record was examined during appraisal or not.
     */
    public enum ExamineState {
        /**
         * If the record was never examined.
         */
        UNEXAMINED,

        /**
         * If the record was compared against a baseline during the appraisal process.
         */
        EXAMINED,

        /**
         * If a record was visited but ignored.
         */
        IGNORED
    }
}
