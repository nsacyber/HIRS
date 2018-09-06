package hirs.persist;

/**
 * List of valid IMAMeasurementRecord fields to prevent query injection since parameterization of
 * field names is not possible in HQL.
 */
public enum IMARecordField {

    /**
     * The path associated with the record.
     */
    PATH("path"),

    /**
     * The hash associated with the record.
     */
    HASH("hash");

    private final String field;

    /**
     * Constructor for {@link IMARecordField}.
     * @param field the field
     */
    IMARecordField(final String field) {
        this.field = field;
    }

    /**
     * Returns the HQL name for the field.
     *
     * @return the HQL name for the field.
     */
    public String getHQL() {
        return field;
    }

    /**
     * Translates the HQL name of the field to an IMARecordField object.
     *
     * @param field the field name to get the value of
     * @return the DBIMARecordField matching the field name
     */
    public static IMARecordField valueOfHQL(final String field) {
        for (IMARecordField f : IMARecordField.values()) {
            if (f.getHQL().equals(field)) {
                return f;
            }
        }
        throw new IllegalArgumentException("No field matched string '" + field + "'.");
    }

}
