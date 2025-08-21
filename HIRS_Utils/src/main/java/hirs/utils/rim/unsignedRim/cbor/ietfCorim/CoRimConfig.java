package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import java.util.ArrayList;
import java.util.List;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORString;
import com.authlete.cbor.CBORTaggedItem;
import com.fasterxml.jackson.annotation.JsonProperty;

import hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.builders.ComidBuilder;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.Coswid;
import lombok.Getter;
import lombok.Setter;

/**
 * The configuration object for the CoRIM object. This object contains the
 * format and supported fields for the CoRIM, which will then be used to build
 * out the CoRIM.
 */
public class CoRimConfig {
    @Getter @Setter
    private String id;
    @Getter @Setter
    private String profile;
    @JsonProperty("validity-map") @Getter @Setter
    private CoRimValidity validityMap;
    @JsonProperty("comid-tags")
    private List<ComidBuilder> comidTags;
    @JsonProperty("coswid-tags")
    private List<Coswid> coswidTags;
    @JsonProperty("dependent-rims")
    private List<CoRimLocatorMap> dependentRims;
    private List<CoRimEntityMap> entities;

    /**
     * Builds a CBOR representation of the CoRIM.
     *
     * @return The CBOR object representing the CoRIM.
     */
    public CBORItem build() {
        // corim-map
        final List<CBORPair> corimMapPairs = new ArrayList<>();
        corimMapPairs.add(new CBORPair(new CBORInteger(0),
                new CBORString(this.id))); // id tags (consolidate both CoSWID and CoMID tags)
        final List<CBORItem> tagList = new ArrayList<>();
        // CoMID tags
        if (comidTags != null) {
            comidTags.forEach(comidTag -> {
                final CBORTaggedItem taggedComid = new CBORTaggedItem(506,
                        comidTag.build()); // Tagged CoMID
                tagList.add(new CBORByteArray(taggedComid.encode()));
            });
        }
        corimMapPairs.add(new CBORPair(new CBORInteger(1), new CBORItemList(tagList))); // tags
        if (dependentRims != null) {
            final List<CBORItem> drItems = new ArrayList<>();
            dependentRims.forEach(rim -> drItems.add(rim.build()));
            final CBORItemList drList = new CBORItemList(drItems);
            corimMapPairs.add(new CBORPair(new CBORInteger(2), drList));
        }
        if (profile != null) {
            final CBORTaggedItem profileUri = new CBORTaggedItem(32, new CBORString(this.profile));
            corimMapPairs.add(new CBORPair(new CBORInteger(3),
                    profileUri)); // profile (URI tag)
        }
        if (validityMap != null) {
            corimMapPairs.add(new CBORPair(new CBORInteger(4), this.validityMap.build())); // validity-map
        }
        if (entities != null) {
            final List<CBORItem> eItems = new ArrayList<>();
            entities.forEach(entity -> eItems.add(entity.build()));
            final CBORItemList eList = new CBORItemList(eItems);
            corimMapPairs.add(new CBORPair(new CBORInteger(5), eList)); // entities
        }
        return new CBORPairList(corimMapPairs);
    }

    /**
     * Returns a copy of the list of ComidBuilder tags.
     *
     * @return a defensive copy of the comidTags list
     */
    public List<ComidBuilder> getComidTags() {
        return new ArrayList<>(comidTags);
    }

    /**
     * Sets the list of ComidBuilder tags using a defensive copy.
     *
     * @param comidTags the list to set
     */
    public void setComidTags(final List<ComidBuilder> comidTags) {
        this.comidTags = new ArrayList<>(comidTags);
    }

    /**
     * Returns a copy of the list of Coswid tags.
     *
     * @return a defensive copy of the coswidTags list
     */
    public List<Coswid> getCoswidTags() {
        return new ArrayList<>(coswidTags);
    }

    /**
     * Sets the list of Coswid tags using a defensive copy.
     *
     * @param coswidTags the list to set
     */
    public void setCoswidTags(final List<Coswid> coswidTags) {
        this.coswidTags = new ArrayList<>(coswidTags);
    }

    /**
     * Returns a copy of the list of dependent RIMs.
     *
     * @return a defensive copy of the dependentRims list
     */
    public List<CoRimLocatorMap> getDependentRims() {
        return new ArrayList<>(dependentRims);
    }

    /**
     * Sets the list of dependent RIMs using a defensive copy.
     *
     * @param dependentRims the list to set
     */
    public void setDependentRims(final List<CoRimLocatorMap> dependentRims) {
        this.dependentRims = new ArrayList<>(dependentRims);
    }

    /**
     * Returns a copy of the list of entities.
     *
     * @return a defensive copy of the entities list
     */
    public List<CoRimEntityMap> getEntities() {
        return new ArrayList<>(entities);
    }

    /**
     * Sets the list of entities using a defensive copy.
     *
     * @param entities the list to set
     */
    public void setEntities(final List<CoRimEntityMap> entities) {
        this.entities = new ArrayList<>(entities);
    }
}
