package hirs.data.persist.enums;

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
