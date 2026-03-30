package hirs.attestationca.portal.page.params;

import java.util.LinkedHashMap;

/**
 * Minimal implementation of PageParams for pages that do not have url parameters.
 */
public class NoPageParams implements PageParams {

    /**
     * Returns empty map so when iteration is required, nothing happens.
     *
     * @return empty map.
     */
    @Override
    public LinkedHashMap<String, ?> asMap() {
        return new LinkedHashMap<>();
    }

}
