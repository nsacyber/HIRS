package hirs.attestationca.portal.page.params;

import hirs.attestationca.portal.page.PageParams;
import java.util.LinkedHashMap;

/**
 * URL parameters object for the CertificateDetails page and controller.
 */
public class CertificateDetailsPageParams implements PageParams {

    private String id;
    private String type;

    /**
     * Constructor to set all Certificate Details URL parameters.
     *
     * @param id the String parameter to set
     * @param type the Integer parameter to set
     */
    public CertificateDetailsPageParams(final String id, final String type) {
        this.id = id;
        this.type = type;
    }

    /**
     * Constructor to set ID Certificate Details URL parameters.
     *
     * @param id the String parameter to set
     */
    public CertificateDetailsPageParams(final String id) {
        this.id = id;
    }

    /**
     * Default constructor for Spring.
     */
    public CertificateDetailsPageParams() {
        id = null;
        type = null;
    }

    /**
     * Returns the String id parameter.
     *
     * @return the String id parameter.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the String id parameter.
     *
     * @param id the String id parameter.
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the String type parameter.
     *
     * @return the String type parameter.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the String type parameter.
     *
     * @param type the String type parameter.
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Allows PageController to iterate over the url parameters.
     *
     * @return map containing the object's URL parameters.
     */
    @Override
    public LinkedHashMap<String, ?> asMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("type", type);
        return map;
    }

    @Override
    public String toString() {
        return "CertificateDetailsPageParams{"
                + "id:' " + id + "',"
                + "type: " + type
                + "}";
    }

}
