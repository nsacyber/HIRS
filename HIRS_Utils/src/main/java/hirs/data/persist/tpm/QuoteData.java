package hirs.data.persist.tpm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.UUID;

/**
 * Java class for QuoteData complex type, which was modified from code
 * auto-generated using the xjc command line tool. This code generates XML that
 * matches what is specified for the QuoteDataType ComplexType in the
 * IntegrityReportManifest_v1_0_1 schema file.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuoteData",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#", propOrder = {"quote2",
                "tpmSignature" })
@Embeddable
public class QuoteData implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger(QuoteData.class);

    @XmlElement(name = "Quote2")
    @Embedded
    private final Quote2 quote2;

    @XmlElement(name = "TpmSignature", required = true)
    @Embedded
    private final QuoteSignature tpmSignature;

    @XmlAttribute(name = "ID", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    @Column(name = "quoteDataId")
    private final String id;

    /**
     * Default constructor necessary for marshalling/unmarshalling xml.
     */
    protected QuoteData() {
        this.quote2 = new Quote2();
        this.tpmSignature = new QuoteSignature();
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Constructor used to create a QuoteData object.
     *
     * @param quote2
     *            {@link Quote2 } object
     * @param tpmSignature
     *            {@link QuoteSignature } object
     */
    public QuoteData(final Quote2 quote2, final QuoteSignature tpmSignature) {
        if (quote2 == null) {
            LOGGER.error("null quote2 value");
            throw new NullPointerException("quote2");
        }
        if (tpmSignature == null) {
            LOGGER.error("null tpmSignature value");
            throw new NullPointerException("tpmSignature");
        }
        this.quote2 = quote2;
        this.tpmSignature = tpmSignature;
        id = UUID.randomUUID().toString();
    }

    /**
     * Gets the value of the quote2 property.
     *
     * @return possible object is {@link Quote2 }
     *
     */
    public final Quote2 getQuote2() {
        return quote2;
    }

    /**
     * Gets the value of the tpmSignature property.
     *
     * @return possible object is {@link QuoteSignature }
     *
     */
    public final QuoteSignature getTpmSignature() {
        return tpmSignature;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is {@link String }
     *
     */
    public final String getID() {
        return id;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        QuoteData quoteData = (QuoteData) o;

        if (!id.equals(quoteData.id)) {
            return false;
        }
        if (!quote2.equals(quoteData.quote2)) {
            return false;
        }
        if (!tpmSignature.equals(quoteData.tpmSignature)) {
            return false;
        }

        return true;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = quote2.hashCode();
        result = prime * result + tpmSignature.hashCode();
        result = prime * result + id.hashCode();
        return result;
    }
}
