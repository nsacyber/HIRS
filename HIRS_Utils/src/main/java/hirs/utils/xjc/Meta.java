//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.02.16 at 06:40:19 PM UTC 
//


package hirs.utils.xjc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Meta complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Meta">
 *   &lt;complexContent>
 *     &lt;extension base="{http://standards.iso.org/iso/19770/-2/2015/schema.xsd}BaseElement">
 *       &lt;anyAttribute processContents='lax'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Meta", namespace = "http://standards.iso.org/iso/19770/-2/2015/schema.xsd")
@XmlSeeAlso({
        SoftwareMeta.class,
        Resource.class,
        Process.class,
        FilesystemItem.class
})
public class Meta
        extends BaseElement {


}
