package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import hirs.utils.signature.cose.Cbor.CborItems;

/**
 * Class to store CoRIM map items.
 * <p>Defined in Section 4.1 of the IETF CoRIM specification.</p>
 *
 * <pre>
 * corim-map = {
 *     &amp;(id: 0)               =&gt; $corim-id-type-choice
 *     &amp;(tags: 1)             =&gt; [ + $concise-tag-type-choice ]
 *     ? &amp;(dependent-rims: 2) =&gt; [ + corim-locator-map ]
 *     ? &amp;(profile: 3)        =&gt; $profile-type-choice
 *     ? &amp;(rim-validity: 4)   =&gt; validity-map
 *     ? &amp;(entities: 5)       =&gt; [ + corim-entity-map ]
 *     * $$corim-map-extension
 * }
 * </pre>
 */
public class CoRimItems extends CborItems {

    public static final int CORIM_ID_TYPE_CHOICE_INT = 0;
    public static final int CONCISE_TAG_TYPE_CHOICE_INT = 1;
    public static final int CORIM_LOCATOR_MAP_INT = 2;
    public static final int PROFILE_TYPE_CHOICE_INT = 3;
    public static final int VALIDITY_MAP_INT = 4;
    public static final int CORIM_ENTITY_MAP_INT = 5;

    public static final String CORIM_ID_TYPE_CHOICE_STR = "corim-id-type-choice";
    public static final String CONCISE_TAG_TYPE_CHOICE_STR = "concise-tag-type-choice";
    public static final String CORIM_LOCATOR_MAP_STR = "corim-locator-map";
    public static final String PROFILE_TYPE_CHOICE_STR = "profile-type-choice";
    public static final String VALIDITY_MAP_STR = "validity-map";
    public static final String CORIM_ENTITY_MAP_STR = "corim-entity-map";

    private static final String[][] INDEX_NAMES = {
            {Integer.toString(CORIM_ID_TYPE_CHOICE_INT), CORIM_ID_TYPE_CHOICE_STR},
            {Integer.toString(CONCISE_TAG_TYPE_CHOICE_INT), CONCISE_TAG_TYPE_CHOICE_STR},
            {Integer.toString(CORIM_LOCATOR_MAP_INT), CORIM_LOCATOR_MAP_STR},
            {Integer.toString(PROFILE_TYPE_CHOICE_INT), PROFILE_TYPE_CHOICE_STR},
            {Integer.toString(VALIDITY_MAP_INT), VALIDITY_MAP_STR},
            {Integer.toString(CORIM_ENTITY_MAP_INT), CORIM_ENTITY_MAP_STR}};

    /**
     * Default Constructor.
     */
    public CoRimItems() {
    }

}
