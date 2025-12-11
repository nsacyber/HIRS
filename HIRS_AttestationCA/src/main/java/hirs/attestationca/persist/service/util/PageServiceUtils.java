package hirs.attestationca.persist.service.util;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for the Page Service classes.
 */
public final class PageServiceUtils {

    /**
     * Private constructor was created to silence checkstyle error.
     */
    private PageServiceUtils() {
    }

    /**
     * Helper method that converts the string value entered in the column search dropdown into a timestamp.
     *
     * @param columnSearchTerm  value entered in the column search dropdown
     * @param columnSearchLogic logic operator used in the column search dropdown
     * @return a timestamp
     */
    public static Timestamp convertColumnSearchTermIntoTimeStamp(final String columnSearchTerm,
                                                                 final String columnSearchLogic) {
        Timestamp columnSearchTimestamp;

        // Handle "empty" or "notempty" logic - no need for a real search term, use a default timestamp
        if (columnSearchLogic.equalsIgnoreCase("empty")
                || columnSearchLogic.equalsIgnoreCase("notempty")) {
            // Use a Unix epoch timestamp
            columnSearchTimestamp = Timestamp.valueOf("1970-01-01 00:00:00");
        } else {   //  if the search logic is anything else but empty or not empty
            
            // Define the DateTimeFormatter matching the input date format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Parse the string into LocalDate (no time info here)
            LocalDate localDate = LocalDate.parse(columnSearchTerm, formatter);

            // Convert LocalDate to Timestamp, assuming midnight (00:00:00) as the time
            // atStartOfDay() gives 00:00:00
            columnSearchTimestamp = Timestamp.valueOf(localDate.atStartOfDay());
        }
        return columnSearchTimestamp;
    }
}
