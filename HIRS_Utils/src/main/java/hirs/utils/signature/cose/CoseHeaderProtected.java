package hirs.utils.signature.cose;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPair;
import com.authlete.cose.COSEProtectedHeader;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.CoRim;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.MetaMap;
import lombok.Getter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Class to hold COSE protected header components,
 * specified in rfc rfc8152 (https://datatracker.ietf.org/doc/html/rfc8152#section-3.1).
 */
public class CoseHeaderProtected extends CoseHeader {
    // criticality
    @Getter
    private final String crit = "";
    // CBor Pairs (currently only 2 being processed: metamap and x5t for corim)
    private MetaMap mmap = null;
    @Getter
    private String x5tHashAlg = "";
    @Getter
    private String x5tHashVal = "";
    private String toStringCborDiag = "";

    /**
     * Parser constructor to fill class variables.
     *
     * @param pheader COSEUnprotectedHeader holding the COSE protected header
     */
    public CoseHeaderProtected(final COSEProtectedHeader pheader) {
        toStringCborDiag = pheader.toString();
        final int certhash = 34; // perf rfc 9360
        if (pheader.getAlg() != null) {
            algIdentifier = CoseAlgorithm.getAlgName((int) pheader.getAlg());
        }
        if (pheader.getKid() != null) {
            keyIdentifier = CoseParser.hexToString(pheader.getKid());
        }
        if (pheader.getContentType() != null) {
            contentType = pheader.getContentType().toString();
        }
        if (pheader.getParameters() != null) {
            parameters = pheader.getParameters();
        }
        if (pheader.getX5Chain() != null) {
            x5chain = pheader.getX5Chain();
        }
        // ------------- COSE header info that can reside only in Protected header -------------
        // Cbor pairs
        if (pheader.getDecodedContent() != null) {
            List<CBORPair> cborPairs = (List<CBORPair>) pheader.getPairs();
            Iterator pairs = cborPairs.iterator();
            while (pairs.hasNext()) {
                CBORPair pair = (CBORPair) pairs.next();
                // Look for corim-meta (index 8)
                if (Integer.parseInt(pair.getKey().toString()) == CoRim.CORIM_META_MAP) {
                    byte[] corimMap = pair.getValue().encode();
                    mmap = new MetaMap(corimMap);
                }
                // Look for x5t (thumbprint) (index 34)
                if (Integer.parseInt(pair.getKey().toString()) == certhash) {
                    try {
                        CBORItemList x5tItemList = (CBORItemList) (pair.getValue());
                        List<CBORItem> x5tList = (List<CBORItem>) x5tItemList.getItems();
                        CBORInteger hashAlg = (CBORInteger) x5tList.get(0);
                        CBORByteArray hashVal = (CBORByteArray) x5tList.get(1);
                        x5tHashAlg = CoseAlgorithm.getAlgName(Integer.parseInt(hashAlg.toString()));
                        x5tHashVal = hashVal.toString();
                    } catch (Exception e) {
                        x5tHashAlg = "Found x5t tag, but unable to process contents, error retrieved: " + e;
                        x5tHashVal = "";
                    }
                }
            }
        }
    }

    /**
     * Default toString.
     *
     * @return default "pretty" version
     */
    public String toString() {
        try {
            return toString("pretty");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prints the processed COSE Protected Header data.
     *
     * @param format empty (default String) or "pretty"
     * @return a formated string representation of the data in the COSE protected header object
     * @throws IOException if any issues trying to create the string representation of the COSE Protected
     *                     Header object.
     */
    public String toString(final String format) throws IOException {
        String returnString = "";
        if (format.compareToIgnoreCase("pretty") == 0) {
            returnString = "Protected Header Contents: " + "\n";
            returnString += printHeaderCommonContentsPretty();

            if (!crit.isEmpty()) {
                returnString += "  Criticality = " + crit + "\n";
            }
            if (mmap != null) {
                returnString += "  Signer Name = " + mmap.getSignerName() + "\n";
                if (!mmap.getSignerUri().isEmpty()) {
                    returnString += "  Signer URI = " + mmap.getSignerUri() + "\n";
                }
                if (!mmap.getNotBeforeStr().isEmpty()) {
                    returnString += "  Validity notBefore = " + mmap.getNotBeforeStr() + "\n";
                }
                if (!mmap.getNotAfterStr().isEmpty()) {
                    returnString += "  Validity notAfter = " + mmap.getNotAfterStr() + "\n";
                }
            }
            if (x5tHashAlg != null) {
                returnString += "  x5t: \n";
                returnString += "    x5t hash algorithm: " + x5tHashAlg + "\n";
                returnString += "    x5t hash value: " + x5tHashVal + "\n";
            }
            if (returnString.compareToIgnoreCase("Protected Header Contents: " + "\n") == 0) {
                returnString += "  No Contents\n";
            }
        } else if (format.compareToIgnoreCase("cbor-diag") == 0) {
            // return Authelete defined representation of COSE protected header
            returnString = toStringCborDiag;
        }
        return returnString;
    }
}
