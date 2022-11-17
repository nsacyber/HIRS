/**
 * Persistent DTOs for the HIRS system. These are the data objects that are
 * persisted via JPA/Hibernate in the HIRS database.
 * <p>
 * DTO's have an XML representation, generated via JAXB.  The XML namespace of
 * this representation is specified by the @XmlSchema annotation on the package
 * here.
 */

@XmlSchema(namespace = "hirs")
package hirs.attestationca.entity;
import javax.xml.bind.annotation.XmlSchema;

