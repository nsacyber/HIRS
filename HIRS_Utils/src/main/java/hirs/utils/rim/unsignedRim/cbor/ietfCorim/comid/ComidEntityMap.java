package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORTaggedItem;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import hirs.utils.signature.cose.Cbor.CborTagProcessor;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *  Class that processes a {@code comid-entity-map} containing hash information relevant to CoMID
 *  measurements. A <i>CoMID Entity</i> contains information about an organization responsible for CoMID
 *  contents. See Section 5.1.2 of the IETF CoRIM specification.
 */
@Getter
@Setter
@JsonTypeName("comid-entity-map")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class ComidEntityMap {
    /** Corresponds to the {@code entity-name} field. The name of the entity responsible for the role's
     * actions.*/
    private String entityName;
    /** Corresponds to the {@code reg-id} field. A URI associated with the owner organization for the entity
     * name.*/
    private URI regId;
    /** Corresponds to the {@code role-type-choice} field. Defines a list of roles that the entity is
     * claiming.*/
    private List<ComidRoleTypeChoice> roleTypeChoice;

    /**
     * Parses a {@code comid-entity-map} from a given {@link CBORItem}.
     *
     * @param comidEntityMap The {@code comid-entity-map} item to parse.
     */
    public ComidEntityMap(final CBORItem comidEntityMap) {
        var list = ((CBORPairList) comidEntityMap).getPairs();

        for (var currItem : list) {
            var currKey = (int) currItem.getKey().parse();
            var currVal = EntityItems.fromIndex(currKey);
            if (currVal != null) {
                switch (currVal) {
                    case ENTITY_NAME -> {
                        entityName = (String) currItem.getValue().parse();
                    } // Entity name
                    case REG_ID -> {
                        var tag = (CBORTaggedItem) currItem.getValue();
                        CborTagProcessor.process(tag).ifPresent(value -> regId = (URI) value); // Reg ID
                    }
                    case ROLE -> {
                        roleTypeChoice = new ArrayList<>();
                        var roleObj = currItem.getValue().parse(); // Role
                        if (roleObj instanceof List<?> roleTypeList) {
                            for (var currRole : roleTypeList) {
                                var currRoleVal = ComidRoleTypeChoice.fromIndex((Integer) currRole);
                                if (currRoleVal != null) {
                                    roleTypeChoice.add(currRoleVal);
                                }
                            }
                        }
                    }
                    default -> { }
                }
            }
        }
    }
}
