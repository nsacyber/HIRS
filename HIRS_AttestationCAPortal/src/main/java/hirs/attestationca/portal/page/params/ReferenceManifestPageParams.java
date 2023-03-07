package hirs.attestationca.portal.page.params;

import hirs.attestationca.portal.page.PageParams;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;

/**
 * URL parameters object for the ReferenceManifest page and controller.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceManifestPageParams implements PageParams {

    private String id;
    private String type;

    /**
     *Constructor to set all Reference Integrity Manifest URL parameters.
     *
     * @param id the String parameter to set
     */
    public ReferenceManifestPageParams(final String id) {
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
        map.put("type", type);
        return map;
    }
}
