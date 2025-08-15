package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORTaggedItem;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import hirs.utils.signature.cose.Cbor.CborTagProcessor;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class corresponding to <i>measured values</i>, which are measurements associated with a particular
 * environment. See Section 5.1.4.4.2 of the IETF CoRIM specification.
 */
@Setter
@Getter
@JsonTypeName("measurement-values-map")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class MeasurementValuesMap {
    /** Corresponds to the {@code version} field. Changes whenever a measured environment is updated.*/
    private VersionMap version;
    /** Corresponds to the {@code svn} (security version number) field. See {@link ComidSvn} for details.*/
    private ComidSvn svn;
    /** Contains a list of {@code digest} objects for the measured environment, along with the hash
     * algorithm.*/
    private List<ComidDigest> digests;
    /** Corresponds to the {@code raw-value} field. A raw value is the actual (non-hashed) value of the
     * element.*/
    private RawValue rawValue;
    /** Corresponds to the {@code mac-addr} field. This can be either an EUI-48 or EUI-64 MAC address.*/
    private byte[] macAddr;
    /** Corresponds to the {@code ip-addr} field. This can be either an IPv4 or IPv6 address.*/
    private InetAddress ipAddr;
    /** Corresponds to the {@code serial-number} field, representing the product serial number.*/
    private String serialNumber;
    /** Corresponds to the {@code ueid} field. Represents a UEID associated with the measured environment.*/
    private byte[] ueid;
    /** Corresponds to the {@code ueid} field. Represents a UUID associated with the measured environment.*/
    private UUID uuid;
    /** Corresponds to the {@code name} field. Represents a name associated with the measured environment.*/
    private String name;

    /**
     * Parses measured values from a given {@link CBORItem}.
     *
     * @param mValuesMap The {@code measurement-values-map} to process.
     */
    public MeasurementValuesMap(final CBORItem mValuesMap) {
        var list = ((CBORPairList) mValuesMap).getPairs();

        for (var currItem : list) {
            var currKey = (int) currItem.getKey().parse();
            var currVal = MeasurementValuesMapItems.fromIndex(currKey);
            if (currVal != null) {
                switch (currVal) {
                    case VERSION_MAP -> {
                        version = new VersionMap(currItem.getValue());
                    } // Version map
                    case SVN -> {
                        // svn-type-choice (see section 5.1.4.1.4.4 of the IETF CoRIM specification)
                        var svnItem = currItem.getValue();
                        if (svnItem instanceof CBORTaggedItem) { // tagged-svn or tagged-min-svn
                            CborTagProcessor.process((CBORTaggedItem) svnItem)
                                    .ifPresent(item -> svn = (ComidSvn) item);
                        } else { // svn
                            svn = new ComidSvn((int) currItem.getValue().parse());
                        }
                    }
                    case DIGESTS -> {
                        digests = new ArrayList<>();
                        var mValDigests = ((CBORItemList) currItem.getValue()).getItems();
                        for (var mValDigest : mValDigests) {
                            digests.add(new ComidDigest(mValDigest));
                        }
                    }
                    case RAW_VALUE -> {
                        // $raw-value-type-choice can either be of type tagged-bytes or
                        // tagged-masked-raw-value
                        // See section 5.1.4.1.4.6 of the IETF CoRIM specification
                        if (rawValue == null) {
                            rawValue = new RawValue();
                        }
                        CborTagProcessor.process((CBORTaggedItem) currItem.getValue())
                                .ifPresent(parsedRawValue -> {
                                    if (parsedRawValue instanceof RawValue) { // tagged-masked-raw-value
                                        rawValue.setValue(((RawValue) parsedRawValue).getValue());
                                        rawValue.setMask(((RawValue) parsedRawValue).getMask());
                                    } else { // tagged-bytes
                                        rawValue.setMask((byte[]) parsedRawValue);
                                    }
                                });
                    }
                    case RAW_VALUE_MASK -> {
                        // Note: this field is deprecated per the IETF CoRIM specification, but retained for
                        // compatibility
                        if (rawValue == null) {
                            rawValue = new RawValue();
                        }
                        rawValue.setMask((byte[]) currItem.getValue().parse());
                    }
                    // Note: no standard Java library class exists for MAC addresses, so byte[] is used
                    // instead
                    case MAC_ADDR -> {
                        macAddr = (byte[]) currItem.getValue().parse();
                    }
                    case IP_ADDR -> {
                        try {
                            ipAddr = InetAddress.getByAddress((byte[]) currItem.getValue().parse());
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    case SERIAL_NUMBER -> {
                        serialNumber = (String) currItem.getValue().parse();
                    }
                    case UEID -> {
                        ueid = (byte[]) currItem.getValue().parse();
                    } // See draft-ietf-rats-eat, section 4.2.1.2
                    case UUID -> {
                        uuid = UUID.fromString((String) currItem.getValue().parse());
                    }
                    case NAME -> {
                        name = (String) currItem.getValue().parse();
                    }
                    // Note: FLAGS, CRYPTOKEYS, INTEGRITY_REGISTERS currently unsupported
                    default -> { }
                }
            }
        }
    }
}
