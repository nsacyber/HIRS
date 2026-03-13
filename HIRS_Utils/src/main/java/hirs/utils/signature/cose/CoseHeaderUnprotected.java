package hirs.utils.signature.cose;

import com.authlete.cose.COSEUnprotectedHeader;

import java.io.IOException;

/**
 * Class to hold COSE unprotected header components,
 * specified in rfc rfc8152 (https://datatracker.ietf.org/doc/html/rfc8152#section-3.1).
 */
public class CoseHeaderUnprotected extends CoseHeader {

    private boolean containedAlgId = false;
    private String toStringCborDiag = "";

    /**
     * Parser constructor to fill class variables.
     *
     * @param uheader COSEUnprotectedHeader holding the COSE unprotected header
     */
    public CoseHeaderUnprotected(final COSEUnprotectedHeader uheader) {
        toStringCborDiag = uheader.toString();
        // alg should not be in unprotected header but check just in case
        if (uheader.getAlg() != null) {
            algIdentifier = CoseAlgorithm.getAlgName((int) uheader.getAlg());
            containedAlgId = true;
        }
        if (uheader.getKid() != null) {
            keyIdentifier = CoseParser.hexToString(uheader.getKid());
        }
        if (uheader.getContentType() != null) {
            contentType = uheader.getContentType().toString();
        }
        if (uheader.getParameters() != null) {
            parameters = uheader.getParameters();
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
     * Prints the processed COSE Unprotected Header data.
     *
     * @param format empty (default String) or "pretty"
     * @return a formated string representation of the data in the COSE unprotected header object
     * @throws IOException if any issues arise while building the string representation of the COSE
     *                     Unprotected Header data
     */
    public String toString(final String format) throws IOException {
        String returnString = "";
        if (format.compareToIgnoreCase("pretty") == 0) {
            returnString = "Unprotected Header Contents: " + "\n";
            if (containedAlgId) {
                returnString = "  WARNING: Alg ID was found in Unprotected Header but should not be here\n";
            }
            returnString += printHeaderCommonContentsPretty();

            if (returnString.compareToIgnoreCase("Unprotected Header Contents: " + "\n") == 0) {
                returnString += "  No Contents\n";
            }
        } else if (format.compareToIgnoreCase("cbor-diag") == 0) {
            // return Authelete defined representation of COSE protected header
            returnString = toStringCborDiag;
        }
        return returnString;
    }
}
