package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import java.util.ArrayList;
import java.util.List;

import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORString;
import com.authlete.cbor.CBORTaggedItem;

/**
 * Class pertaining to a {@code corim-locator-map}. Defined in Section 4.1.3 of the IETF CoRIM specification.
 */
public class CoRimLocatorMap {
    private List<String> href;
    private CoRimDigest thumbprint;
    static final int CORIM_REG_ID_TAG = 32;

    /**
     * Builds a CBOR representation of the locator map.
     *
     * @return The CBOR object representing the locator map.
     */
    public CBORPairList build() {
        final List<CBORPair> pairList = new ArrayList<>();
        final List<CBORItem> drItems = new ArrayList<>();
        if (href.size() > 1) {
            href.forEach(u -> {
                final CBORTaggedItem hrefURI = new CBORTaggedItem(CORIM_REG_ID_TAG,
                        new CBORString(u)); // href (URI tag)
                drItems.add(hrefURI);
            });
            final CBORItemList drList = new CBORItemList(drItems);
            pairList.add(new CBORPair(new CBORInteger(0), drList));
        } else {
            final CBORTaggedItem hrefURI = new CBORTaggedItem(CORIM_REG_ID_TAG,
                    new CBORString(href.get(0)));
            pairList.add(new CBORPair(new CBORInteger(0), hrefURI));
        }
        if (thumbprint != null) {
            pairList.add(new CBORPair(new CBORInteger(1), thumbprint.build()));
        }
        return new CBORPairList(pairList);
    }

    /**
     * Returns a copy of the href list.
     *
     * @return a defensive copy of the href list
     */
    public List<String> getHref() {
        return new ArrayList<>(href);
    }

    /**
     * Sets the href list using a defensive copy.
     *
     * @param href the list to set
     */
    public void setHref(final List<String> href) {
        this.href = new ArrayList<>(href);
    }

    /**
     * Returns a copy of the thumbprint.
     *
     * @return a defensive copy of the thumbprint
     */
    public CoRimDigest getThumbprint() {
        return thumbprint.copy();
    }

    /**
     * Sets the thumbprint using a defensive copy.
     *
     * @param thumbprint the object to set
     */
    public void setThumbprint(final CoRimDigest thumbprint) {
        this.thumbprint = thumbprint.copy();
    }
}
