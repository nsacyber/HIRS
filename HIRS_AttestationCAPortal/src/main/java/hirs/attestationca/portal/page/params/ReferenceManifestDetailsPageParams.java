
package hirs.attestationca.portal.page.params;

import hirs.attestationca.portal.page.PageParams;
import java.util.LinkedHashMap;

/**
 * URL parameters object for the Reference Manifest Details page and controller.
 */
public class ReferenceManifestDetailsPageParams implements PageParams {

    private String id;

    /**
     * Constructor to set all RIM Details URL parameters.
     *
     * @param id the String parameter to set
     */
    public ReferenceManifestDetailsPageParams(final String id) {
        this.id = id;
    }

    /**
     * Default constructor for Spring.
     *
     */
    public ReferenceManifestDetailsPageParams() {
        this.id = null;
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
     * Allows PageController to iterate over the url parameters.
     *
     * @return map containing the object's URL parameters.
     */
    @Override
    public LinkedHashMap<String, ?> asMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        return map;
    }

    @Override
    public String toString() {
        return "ReferenceManifestDetailsPageParams{"
                + "id:' " + id
                + "}";
    }
}
