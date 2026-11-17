package hirs.utils.dice;

import lombok.Getter;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;

import java.io.IOException;
import java.util.HexFormat;

/**
 * A decoded firmware identifier. The algorithm OID is retained even when it is unknown to HIRS.
 * Decoding does not establish that the digest or algorithm is acceptable to an appraisal policy.
 */
public final class DiceFwid {
    @Getter
    private final String hashAlgorithmOid;
    private final byte[] digest;

    /**
     * Decodes a firmware identifier.
     * @param sequence the algorithm OID and digest sequence
     * @throws IOException if the sequence has the wrong number of fields
     */
    DiceFwid(final ASN1Sequence sequence) throws IOException {
        if (sequence.size() != 2) {
            throw new IOException("A DICE FWID must contain an algorithm OID and a digest");
        }
        hashAlgorithmOid = ASN1ObjectIdentifier.getInstance(sequence.getObjectAt(0)).getId();
        digest = ASN1OctetString.getInstance(sequence.getObjectAt(1)).getOctets();
    }

    /**
     * Returns the measured digest without inferring an algorithm from its size.
     * @return a copy of the digest
     */
    public byte[] getDigest() {
        return digest.clone();
    }

    @Override
    public String toString() {
        return hashAlgorithmOid + ": " + HexFormat.of().formatHex(digest);
    }
}
