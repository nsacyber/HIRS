package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPairList;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *  Class that processes a <i>CoMID</i>. A CoMID (Concise Module Identifier) contains various attributes
 *  pertaining to hardware, firmware, or modules on a device. See Section 5 of the IETF CoRIM specification
 *  for further information.
 *  <p>
 *  A CoRIM's measurement claims are contained in the {@code triples} field.
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-rats-corim-07">IETF CoRIM Specification</a>
 */
@Setter
@Getter
@JsonTypeName("concise-mid-tag")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class Comid {
    /** Corresponds to the {@code language} field, expected to conform with the
     * IANA Language Subtag Registry.*/
    private Locale language;
    /** Corresponds to the {@code tag-identity} field. Contains a {@link TagIdentityMap}.*/
    private TagIdentityMap tagIdentity;
    /** Corresponds to the {@code entities} field. Contains a list of entities responsible for CoMID
     * production.*/
    private List<ComidEntityMap> entities;
    /** Corresponds to the {@code linkedTags} field. Contains a list of {@link LinkedTagMap} objects
     * describing dCoMID relationships.*/
    private List<LinkedTagMap> linkedTags;
    /** Corresponds to the {@code triples} field. Contains a reference to a {@link TriplesMap} object
     * containing <i>triples</i>, which are records that link various assertions with security features (such
     * as cryptographic keys, etc.).*/
    private TriplesMap triples;

    /**
     * Parses a {@code concise-mid-tag} from section 5.1 of the IETF CoRIM specification.
     *
     * @param comidData The CoMID data to process.
     */
    public Comid(final byte[] comidData) {
        try {
            CBORDecoder cborDecoder = new CBORDecoder(comidData);
            CBORPairList comidPairList = (CBORPairList) cborDecoder.next();

            // Language
            var list = comidPairList.getPairs();
            for (var currItem : list) {
                var currKey = (int) currItem.getKey().parse();
                var currVal = ComidItems.fromIndex(currKey);
                if (currVal != null) {
                    switch (currVal) {
                        // Language (IETF BCP 47 language tags)
                        // Note: does not currently validate against IANA Language Subtag Registry
                        case LANGUAGE -> {
                            language = Locale.forLanguageTag((String) currItem.getValue().parse());
                        }
                        // Tag identity map
                        case TAG_ID -> {
                            tagIdentity = new TagIdentityMap(currItem.getValue());
                        }
                        case COMID_ENTITY_MAP -> {
                            entities = new ArrayList<>();
                            // Entities
                            var itemList = ((CBORItemList) currItem.getValue()).getItems();
                            for (var entityToAdd : itemList) {
                                entities.add(new ComidEntityMap(entityToAdd));
                            }
                        }
                        case LINKED_TAG_MAP -> {
                            linkedTags = new ArrayList<>();
                            var linkedTagList = ((CBORItemList) currItem.getValue()).getItems();
                            for (var linkedTagToAdd : linkedTagList) {
                                linkedTags.add(new LinkedTagMap(linkedTagToAdd)); // Linked tags
                            }
                        }
                        case TRIPLES_MAP -> {
                            triples = new TriplesMap(currItem.getValue()); // Triples
                        }
                        default -> { }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
