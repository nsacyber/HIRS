package hirs.data.persist;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;

/**
 * This is a test class, used to create dummy objects for testing classes that
 * need Reports.
 */
@Entity
@Access(AccessType.FIELD)
public class TestReport extends Report {

    /**
     * Default constructor.
     */
    public TestReport() {
        /* do nothing */
    }

    @Override
    public final String getReportType() {
        return "Test Report";
    }

}
