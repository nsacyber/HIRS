package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORTaggedItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import hirs.utils.signature.cose.Cbor.CborTagProcessor;
import lombok.Getter;
import lombok.Setter;
import org.ietf.jgss.Oid;

import java.util.UUID;

/**
 * Defines an <i>environment class</i>, pertaining to the {@code class-map} structure in Section 5.1.4.1.1
 * of the IETF CoRIM specification. An environment class contains uniquely identifying information that is
 * used by an {@link EnvironmentMap}.
 */
@Getter
@Setter
@JsonTypeName("class-map")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class ComidClass {
    /** Corresponds to the {@code class-id} field, uniquely identifying the environment.
     * Represents the {@code tagged-oid-type} choice.*/
    @JsonProperty("class-id (oid-type)")
    private Oid classIdOid;
    /** Corresponds to the {@code class-id} field, uniquely identifying the environment.
     * Represents the {@code tagged-uuid-type} choice.*/
    @JsonProperty("class-id (uuid-type)")
    private UUID classIdUUID;
    /** Corresponds to the {@code class-id} field, uniquely identifying the environment.
     * Represents the {@code tagged-bytes} choice.*/
    @JsonProperty("class-id (bytes)")
    private byte[] classIdBytes;
    /** Corresponds to the {@code vendor} field. The entity responsible for choosing values for the other
     * class attributes without naming authority.*/
    private String vendor;
    /** Corresponds to the {@code model} field: describes a product, generation, and family.*/
    private String model;
    /** Corresponds to the {@code layer} field. Captures the sequence in which the environment exists
     * (contextual).*/
    private Integer layer;
    /** Corresponds to the {@code index} field. Used to disambiguate identical instances of the same class of
     * environment.*/
    private Integer index;

    /** Parses a {@code ComidClass} from a given {@link CBORItem}.
     *
     * @param comidClassItem The {@code class-map} item to process.
     */
    public ComidClass(final CBORItem comidClassItem) {
        var pairs = ((CBORPairList) comidClassItem).getPairs();

        for (var pair : pairs) {
            var currKey = (int) pair.getKey().parse();
            var currVal = ClassItems.fromIndex(currKey);
            if (currVal != null) {
                switch (currVal) {
                    case CLASS_ID ->
                            CborTagProcessor.process((CBORTaggedItem) pair.getValue()).ifPresent(obj -> {
                                if (obj instanceof Oid) { // $tagged-oid-type
                                    classIdOid = (Oid) obj;
                                } else if (obj instanceof UUID) { // $tagged-uuid-type
                                    classIdUUID = (UUID) obj;
                                } else { // $tagged-bytes
                                    classIdBytes = (byte[]) obj;
                                }
                            });
                    case VENDOR -> {
                        vendor = ((String) pair.getValue().parse());
                    }
                    case MODEL -> {
                        model = ((String) pair.getValue().parse());
                    }
                    case LAYER -> {
                        layer = ((int) pair.getValue().parse());
                    }
                    case INDEX -> {
                        index = ((int) pair.getValue().parse());
                    }
                    default -> { }
                }
            }
        }
    }
}
