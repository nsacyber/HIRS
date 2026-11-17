package hirs.utils.dice;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERTags;
import org.bouncycastle.asn1.DERTaggedObject;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Decodes the TCG DICE TcbInfo and MultiTcbInfo X.509 extensions into a common ordered list.
 * This utility does not validate signatures, trust, critical-extension policy, freshness, or reference values.
 * Unknown algorithm OIDs are retained. Unknown record fields and malformed encodings fail explicitly.
 */
public final class DiceTcbInfoParser {
    /** OID for one DiceTcbInfo structure. */
    public static final String TCB_INFO_OID = "2.23.133.5.4.1";
    /** OID for a sequence of DiceTcbInfo structures. */
    public static final String MULTI_TCB_INFO_OID = "2.23.133.5.4.5";
    private static final int MAX_EXTENSION_BYTES = 1024 * 1024;
    private static final int MAX_TCB_RECORDS = 256;
    private static final int MAX_LIST_ENTRIES = 64;

    /** Prevents instantiation of the decoder utility. */
    private DiceTcbInfoParser() {
    }

    /**
     * Reads evidence from a certificate, including certificates without DICE EKUs.
     * If both extensions occur, the single record precedes the MultiTcbInfo records as required by TCG.
     * Each extension is limited to 1 MiB; at most 256 records are accepted across both extensions.
     * Each FWID or integrity-register list is limited to 64 entries.
     * @param certificate the source X.509 certificate
     * @return immutable records, or an empty list when neither supported extension is present
     * @throws IOException if evidence is malformed, unsupported, or exceeds the decoding limits
     */
    public static List<DiceTcbInfo> parse(final X509Certificate certificate) throws IOException {
        if (certificate == null) {
            throw new IOException("Certificate must be an X.509 certificate");
        }
        List<DiceTcbInfo> records = new ArrayList<>();
        for (String oid : List.of(TCB_INFO_OID, MULTI_TCB_INFO_OID)) {
            byte[] value = certificate.getExtensionValue(oid);
            if (value == null) {
                continue;
            }
            try {
                if (value.length > MAX_EXTENSION_BYTES) {
                    throw new IOException("DICE extension exceeds 1 MiB");
                }
                if (!(ASN1Primitive.fromByteArray(value) instanceof ASN1OctetString wrapper)) {
                    throw new IOException("DICE extension value must be an OCTET STRING");
                }
                if (!Arrays.equals(value, wrapper.getEncoded("DER"))) {
                    throw new IOException("DICE extension wrapper must use DER encoding");
                }
                byte[] contents = wrapper.getOctets();
                if (!(ASN1Primitive.fromByteArray(contents) instanceof ASN1Sequence sequence)) {
                    throw new IOException("DICE extension payload must be a SEQUENCE");
                }
                if (!Arrays.equals(contents, sequence.getEncoded("DER"))) {
                    throw new IOException("DICE evidence must use DER encoding");
                }
                Set<String> criticalOids = certificate.getCriticalExtensionOIDs();
                boolean critical = criticalOids != null && criticalOids.contains(oid);
                if (TCB_INFO_OID.equals(oid)) {
                    records.add(new DiceTcbInfo(oid, critical, 0, sequence));
                } else {
                    if (sequence.size() == 0 || sequence.size() + records.size() > MAX_TCB_RECORDS) {
                        throw new IOException("MultiTcbInfo must contain 1 to " + MAX_TCB_RECORDS + " TCB records");
                    }
                    for (int i = 0; i < sequence.size(); i++) {
                        records.add(new DiceTcbInfo(oid, critical, i,
                                ASN1Sequence.getInstance(sequence.getObjectAt(i))));
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                throw new IOException("Cannot decode DICE extension " + oid + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(records);
    }

    /**
     * Reads strictly ordered, unique, context-specific fields.
     * @param sequence the containing structure
     * @param lastTag the highest supported tag
     * @return the validated fields
     * @throws IOException if a tag is unsupported, repeated, or out of order
     */
    static List<ASN1TaggedObject> fields(final ASN1Sequence sequence, final int lastTag) throws IOException {
        List<ASN1TaggedObject> fields = new ArrayList<>();
        int previous = -1;
        for (var value : sequence) {
            ASN1TaggedObject field = ASN1TaggedObject.getInstance(value);
            int tag = field.getTagNo();
            if (field.getTagClass() != BERTags.CONTEXT_SPECIFIC || tag > lastTag) {
                throw new IOException("Unsupported DICE field tag: " + tag);
            }
            if (tag <= previous) {
                throw new IOException("DICE fields must have unique tags in increasing order");
            }
            fields.add(field);
            previous = tag;
        }
        return fields;
    }

    /**
     * Reads algorithm-qualified digests without guessing measurement semantics.
     * @param sequence the FWID list
     * @return immutable firmware identifiers
     * @throws IOException if the list is empty, too large, or malformed
     */
    static List<DiceFwid> fwids(final ASN1Sequence sequence) throws IOException {
        checkListSize(sequence, "FWIDs");
        List<DiceFwid> fwids = new ArrayList<>();
        for (var value : sequence) {
            fwids.add(new DiceFwid(ASN1Sequence.getInstance(value)));
        }
        return List.copyOf(fwids);
    }

    /**
     * Rejects implicit values whose decoding normalizes constructed strings or nonzero unused bits.
     * @param <T> the decoded ASN.1 type
     * @param field the original tagged field
     * @param value the decoded value
     * @return the value after checking its DER encoding
     * @throws IOException if the field is not canonical DER
     */
    static <T extends ASN1Primitive> T requireDer(final ASN1TaggedObject field, final T value) throws IOException {
        if (!Arrays.equals(field.getEncoded("DER"),
                new DERTaggedObject(false, field.getTagNo(), value).getEncoded("DER"))) {
            throw new IOException("DICE field " + field.getTagNo() + " must use DER encoding");
        }
        return value;
    }

    /**
     * Enforces bounded nonempty FWID and integrity-register lists.
     * @param sequence the list
     * @param name field name for diagnostics
     * @throws IOException if the list is empty or too large
     */
    static void checkListSize(final ASN1Sequence sequence, final String name) throws IOException {
        if (sequence.size() == 0 || sequence.size() > MAX_LIST_ENTRIES) {
            throw new IOException("DICE " + name + " must contain 1 to " + MAX_LIST_ENTRIES + " entries");
        }
    }
}
