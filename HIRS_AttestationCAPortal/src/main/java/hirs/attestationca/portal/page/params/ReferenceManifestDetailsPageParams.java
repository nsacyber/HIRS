package hirs.attestationca.portal.page.params;

import hirs.attestationca.portal.page.PageParams;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;

/**
 * URL parameters object for the Reference Manifest Details page and controller.
 */
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceManifestDetailsPageParams implements PageParams {

    @Getter
    @Setter
    private String id;

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
