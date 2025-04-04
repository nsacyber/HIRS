//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.02.16 at 06:40:19 PM UTC 
//


package hirs.utils.xjc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;


/**
 * Attributes common to all Elements in this schema
 *
 *
 * <p>Java class for BaseElement complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="BaseElement">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BaseElement", namespace = "http://standards.iso.org/iso/19770/-2/2015/schema.xsd")
@XmlSeeAlso({
        SoftwareIdentity.class,
        Entity.class,
        Link.class,
        Meta.class,
        ResourceCollection.class
})
public class BaseElement {

    /**
     * -- GETTER --
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * <p>
     * the map is keyed by the name of the attribute and
     * the value is the string value of the attribute.
     * <p>
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     */
    @XmlAnyAttribute
    private final Map<QName, String> otherAttributes = new HashMap<>();

    /**
     * -- GETTER --
     * Allow xml:lang attribute on any element.
     */
    @Setter
    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    protected String lang;

}
