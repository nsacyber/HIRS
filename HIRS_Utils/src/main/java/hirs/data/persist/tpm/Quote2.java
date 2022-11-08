package hirs.data.persist.tpm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * Java class for Quote2 complex type, which was modified from code
 * auto-generated using the xjc command line tool. This code generates XML that
 * matches what is specified for the Quote2Type ComplexType in the
 * IntegrityReportManifest_v1_0_1 schema file.
 * <p>
 * The original auto-generated code included a CapVersionInfo attribute as part
 * of the Quote2 object. As the same information is now included in the
 * DeviceInfoReport, this was removed from Quote2. This change does not affect
 * TCG compatibility as the cap version info is optional.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Quote2",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#", propOrder = {"quoteInfo2" })
@Embeddable
public class Quote2 implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger(Quote2.class);

    @XmlElement(name = "QuoteInfo2", required = true)
    @Embedded
    private final QuoteInfo2 quoteInfo2;

    /**
     * Default constructor necessary for marshalling/unmarshalling xml.
     */
    protected Quote2() {
        this.quoteInfo2 = new QuoteInfo2();
    }

    /**
     * Constructor used to create a Quote2 object.
     *
     * @param quoteInfo2
     *            {@link QuoteInfo2 } object
     */
    public Quote2(final QuoteInfo2 quoteInfo2) {
        if (quoteInfo2 == null) {
            LOGGER.error("null quoteInfo2 value");
            throw new NullPointerException("quoteInfo2");
        }
        this.quoteInfo2 = quoteInfo2;
    }

    /**
     * Gets the value of the quoteInfo2 property.
     *
     * @return possible object is {@link QuoteInfo2 }
     */
    public final QuoteInfo2 getQuoteInfo2() {
        return quoteInfo2;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Quote2 quote2 = (Quote2) o;

        if (!quoteInfo2.equals(quote2.quoteInfo2)) {
            return false;
        }

        return true;
    }

    @Override
    public final int hashCode() {
        return quoteInfo2.hashCode();
    }
}
