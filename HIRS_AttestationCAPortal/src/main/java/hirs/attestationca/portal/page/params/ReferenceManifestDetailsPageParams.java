package hirs.attestationca.portal.page.params;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;

/**
 * URL parameters object for the Reference Manifest Details page and controller.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReferenceManifestDetailsPageParams implements PageParams {

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
}
