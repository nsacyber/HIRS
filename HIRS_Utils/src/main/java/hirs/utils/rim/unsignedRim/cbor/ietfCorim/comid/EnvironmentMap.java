package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORPairList;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

/**
 * A class that defines an <i>environment</i>, which is context-dependent but can represent either an
 * Attester, an Attesting Environment, or a Target Environment.
 * <p>
 * See Section 5.1.4.1 of the IETF CoRIM specification.
 */
@Getter
@Setter
@JsonTypeName("environment-map")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class EnvironmentMap {
    /**
     * Corresponds to the {@code class} field. Defines a {@link ComidClass} that identifies the environment.
     */
    private ComidClass comidClass;
    /**
     * Corresponds to the {@code instance} field, containing a unique identifier for the instance of a module.
     */
    private String instance;
    /**
     * Corresponds to the {@code group} field, containing an identifier for a group of instances.
     */
    private String group;

    /**
     * Parses an environment from a given {@link CBORItem}.
     *
     * @param envMapNode The {@code environment-map} item to parse.
     */
    public EnvironmentMap(final CBORItem envMapNode) {
        var pairs = ((CBORPairList) envMapNode).getPairs();
        for (var pair : pairs) {
            var currKey = (int) pair.getKey().parse();
            var currVal = EnvironmentMapItems.fromIndex(currKey);
            if (currVal != null) {
                switch (currVal) {
                    case CLASS_MAP -> {
                        comidClass = new ComidClass(pair.getValue());
                    }
                    // Note: INSTANCE_ID_TYPE_CHOICE, GROUP_ID_TYPE_CHOICE currently unsupported
                    default -> { }
                }
            }
        }
    }
}
