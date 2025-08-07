package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines an <i>svn</i>, or security version number. An svn can track security-relevant changes to an object.
 * In addition, a <i>min-svn</i> can be used to specify the minimum value for an svn that is acceptable.
 * <p>
 * See Section 5.1.4.1.4.4 of the IETF CoRIM specification.
 */
@Getter
@Setter
@JsonTypeName("svn")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class ComidSvn {

    /** A positive value corresponding to an {@code svn} or {@code min-svn}, depending on the value of the
     * {@link #isMinSvn} field.*/
    private Integer svn;
    /** Marks whether or not the {@link #svn} field denotes an svn or min-svn.*/
    private boolean isMinSvn;

    /**
     * Constructs a new svn from a given value.
     * @param newSvn The value to assign.
     */
    public ComidSvn(final Integer newSvn) {
        svn = newSvn;
    }
}
