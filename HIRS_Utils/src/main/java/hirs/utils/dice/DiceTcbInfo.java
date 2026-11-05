package hirs.utils.dice;

import lombok.Getter;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1UTF8String;
import org.bouncycastle.jcajce.util.MessageDigestUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static hirs.utils.dice.DiceTcbInfoParser.requireDer;

/**
 * One TCG DiceTcbInfo record, including its position and extension within the source certificate.
 * Optional scalars remain null when absent; absent lists are empty. Flags retain their raw bit-string
 * values and masks, without treating an absent or unmasked bit as an assertion of a secure state.
 * The caller retains the source certificate and any chain or collection context.
 */
@Getter
public final class DiceTcbInfo {
    private static final int SVN_TAG = 3;
    private static final int LAYER_TAG = 4;
    private static final int INDEX_TAG = 5;
    private static final int FWIDS_TAG = 6;
    private static final int FLAGS_TAG = 7;
    private static final int VENDOR_INFO_TAG = 8;
    private static final int TYPE_TAG = 9;
    private static final int FLAGS_MASK_TAG = 10;
    private static final int INTEGRITY_REGISTERS_TAG = 11;
    private final String extensionOid;
    private final boolean critical;
    private final int recordIndex;
    private final byte[] encoded;
    private String vendor;
    private String model;
    private String version;
    private BigInteger svn;
    private BigInteger layer;
    private BigInteger index;
    private List<DiceFwid> fwids = List.of();
    private DiceBitString flags;
    private byte[] vendorInfo;
    private byte[] type;
    private DiceBitString flagsMask;
    private List<DiceIntegrityRegister> integrityRegisters = List.of();

    /**
     * Decodes a record. Tag 10 is flagsMask and tag 11 is integrityRegisters, following the
     * January 2026 erratum and the DICE 1.2 ASN.1 appendix. The erroneous tag-10 register layout
     * and unknown fields are rejected rather than guessed or silently discarded.
     * @param extensionOid the originating extension OID
     * @param critical whether the source extension is critical
     * @param recordIndex zero-based position within that extension
     * @param sequence the TCB record
     * @throws IOException if a field is unsupported or malformed
     */
    DiceTcbInfo(final String extensionOid, final boolean critical, final int recordIndex,
                final ASN1Sequence sequence) throws IOException {
        this.extensionOid = extensionOid;
        this.critical = critical;
        this.recordIndex = recordIndex;
        this.encoded = sequence.getEncoded("DER");
        for (ASN1TaggedObject field : DiceTcbInfoParser.fields(sequence, INTEGRITY_REGISTERS_TAG)) {
            switch (field.getTagNo()) {
                case 0 -> vendor = ASN1UTF8String.getInstance(field, false).getString();
                case 1 -> model = ASN1UTF8String.getInstance(field, false).getString();
                case 2 -> version = ASN1UTF8String.getInstance(field, false).getString();
                case SVN_TAG -> svn = ASN1Integer.getInstance(field, false).getValue();
                case LAYER_TAG -> layer = ASN1Integer.getInstance(field, false).getValue();
                case INDEX_TAG -> index = ASN1Integer.getInstance(field, false).getValue();
                case FWIDS_TAG -> fwids = DiceTcbInfoParser.fwids(ASN1Sequence.getInstance(field, false));
                case FLAGS_TAG -> flags = new DiceBitString(requireDer(field, ASN1BitString.getInstance(field, false)));
                case VENDOR_INFO_TAG -> vendorInfo = requireDer(field, ASN1OctetString.getInstance(field, false))
                        .getOctets();
                case TYPE_TAG -> type = requireDer(field, ASN1OctetString.getInstance(field, false)).getOctets();
                case FLAGS_MASK_TAG -> flagsMask = new DiceBitString(requireDer(field,
                        ASN1BitString.getInstance(field, false)));
                case INTEGRITY_REGISTERS_TAG -> {
                    ASN1Sequence registers = ASN1Sequence.getInstance(field, false);
                    DiceTcbInfoParser.checkListSize(registers, "integrity registers");
                    List<DiceIntegrityRegister> decoded = new ArrayList<>();
                    for (var register : registers) {
                        decoded.add(new DiceIntegrityRegister(ASN1Sequence.getInstance(register)));
                    }
                    integrityRegisters = List.copyOf(decoded);
                }
                default -> throw new IOException("Unsupported DiceTcbInfo field");
            }
        }
    }

    /**
     * Returns the original DER record, excluding the extension's OCTET STRING and sequence wrapper.
     * @return a copy of the DER record
     */
    public byte[] getEncoded() {
        return encoded.clone();
    }

    /**
     * Returns vendor-specific state without interpreting its contents.
     * @return a copy of vendorInfo, or null when absent
     */
    public byte[] getVendorInfo() {
        return vendorInfo == null ? null : vendorInfo.clone();
    }

    /**
     * Returns the machine-readable measurement type.
     * @return a copy of type, or null when absent
     */
    public byte[] getType() {
        return type == null ? null : type.clone();
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("TCB record ").append(recordIndex).append('\n');
        String extensionName = switch (extensionOid) {
            case DiceTcbInfoParser.TCB_INFO_OID -> "TCB Info";
            case DiceTcbInfoParser.MULTI_TCB_INFO_OID -> "Multi TCB Info";
            default -> "Unknown";
        };
        append(text, "DICE Extension", extensionName);
        append(text, "Critical", critical);
        append(text, "Vendor", vendor);
        append(text, "Model", model);
        append(text, "Version", version);
        append(text, "Security version", svn);
        append(text, "Layer", layer);
        append(text, "Target index", index);
        append(text, "Type (hex)", type == null ? null : HexFormat.of().formatHex(type));
        for (DiceFwid fwid : fwids) {
            appendFwid(text, "    ", fwid);
        }
        append(text, "Flags (raw ASN.1 BIT STRING)", flags);
        append(text, "Flags mask (raw ASN.1 BIT STRING)", flagsMask);
        append(text, "Vendor info (hex)", vendorInfo == null ? null : HexFormat.of().formatHex(vendorInfo));
        for (DiceIntegrityRegister register : integrityRegisters) {
            text.append("    Integrity register\n");
            append(text, "    Name", register.getRegisterName());
            append(text, "    Number", register.getRegisterNum());
            for (DiceFwid digest : register.getRegisterDigests()) {
                appendFwid(text, "        ", digest);
            }
        }
        return text.toString().stripTrailing();
    }

    /**
     * Appends a present field to the inspection text.
     * @param text destination
     * @param label field label
     * @param value field value, or null if absent
     */
    private static void append(final StringBuilder text, final String label, final Object value) {
        if (value != null) {
            text.append("    ").append(label).append(": ").append(value).append('\n');
        }
    }

    /**
     * Appends a firmware identifier with its algorithm and digest on separate lines.
     * @param text destination
     * @param indent indentation for the firmware identifier heading
     * @param fwid firmware identifier
     */
    private static void appendFwid(final StringBuilder text, final String indent, final DiceFwid fwid) {
        text.append(indent).append("FWID\n");
        String oid = fwid.getHashAlgorithmOid();
        String name = MessageDigestUtils.getDigestName(new ASN1ObjectIdentifier(oid));
        append(text, indent + "Hash Algorithm", (name.equals(oid) ? "Unknown" : name));
        append(text, indent + "Digest (hex)", HexFormat.of().formatHex(fwid.getDigest()));
    }
}
