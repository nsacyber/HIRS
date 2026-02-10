package hirs.attestationca.portal.page.params;

import hirs.attestationca.portal.page.PageParams;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;

/**
 * URL parameters object for the CertificateDetails page and controller.
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
public class CertificateDetailsPageParams implements PageParams {

    private String id;
    private String type;

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
