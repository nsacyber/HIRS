package hirs.utils.dice;

import lombok.Getter;
import org.bouncycastle.asn1.ASN1IA5String;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * A named or numbered integrity register and its algorithm-qualified digests.
 */
@Getter
public final class DiceIntegrityRegister {
    private String registerName;
    private BigInteger registerNum;
    private List<DiceFwid> registerDigests;

    /**
     * Decodes an integrity register using the corrected DICE 1.2 layout.
     * @param sequence the register sequence
     * @throws IOException if required fields are absent or malformed
     */
    DiceIntegrityRegister(final ASN1Sequence sequence) throws IOException {
        for (ASN1TaggedObject field : DiceTcbInfoParser.fields(sequence, 2)) {
            switch (field.getTagNo()) {
                case 0 -> registerName = ASN1IA5String.getInstance(field, false).getString();
                case 1 -> registerNum = ASN1Integer.getInstance(field, false).getValue();
                case 2 -> registerDigests = DiceTcbInfoParser.fwids(ASN1Sequence.getInstance(field, false));
                default -> throw new IOException("Unsupported integrity register field");
            }
        }
        if ((registerName == null && registerNum == null) || registerDigests == null) {
            throw new IOException("A DICE integrity register requires a name or number and digests");
        }
    }

    @Override
    public String toString() {
        return "name=" + registerName + ", number=" + registerNum + ", digests=" + registerDigests;
    }
}
