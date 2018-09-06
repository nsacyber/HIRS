package hirs.data.persist;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Specifies properties for an object that can be examined.
 */
@MappedSuperclass
@Access(AccessType.FIELD)
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class ExaminableRecord {
    private static final Logger LOGGER = LogManager.getLogger(ExaminableRecord.class);

    @Column(nullable = false)
    // Decided on ORDINAL instead of STRING due to concerns surrounding overall size and retrieval
    // time of field from database. Consistent with other implementations of ExaminableRecord.
    @Enumerated(EnumType.ORDINAL)
    private ExamineState examineState = ExamineState.UNEXAMINED;

    /**
     * Default empty constructor is required for Hibernate. It is protected to
     * prevent code from calling it directly.
     */
    protected ExaminableRecord() {
    }

    /**
     * Gets the examine state for this record.
     * @return the ExamineState
     */
    public ExamineState getExamineState() {
        return examineState;
    }

    /**
     * Sets the examine state for this record.
     * @param examineState the examine state
     */
    public void setExamineState(final ExamineState examineState) {
        if (examineState == ExamineState.UNEXAMINED) {
            LOGGER.error("Can't set ExamineState on ExaminableRecord to Unexamined");
            throw new IllegalArgumentException(
                    "Can't set ExamineState on ExaminableRecord to Unexamined"
            );
        }

        this.examineState = examineState;
    }
}
