package hirs.attestationca.portal.portal.page;

import java.util.LinkedHashMap;

/**
 * Interface for a page's url parameters.
 */
public interface PageParams {

    /**
     * Allows PageController to iterate over the url parameters.
     *
     * @return map containing the object's url parameters.
     */
    LinkedHashMap<String, ?> asMap();

}