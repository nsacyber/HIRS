package hirs.utils;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Utilities for working with XML.
 */
public final class XmlUtils {
    /**
     * // Default private constructor for checkstyle.
     */
    private XmlUtils() {
    }

    /**
     * Centralize DocumentBuilderFactory with project features.
     * @return DocumentBuilderFactory
     * @throws ParserConfigurationException if the XML parser does not support a feature
     */
    public static DocumentBuilderFactory newDocumentBuilderFactory()
            throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf;
    }
}
