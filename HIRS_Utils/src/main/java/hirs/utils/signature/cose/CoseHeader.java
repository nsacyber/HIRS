package hirs.utils.signature.cose;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Class to hold COSE header components common to both Protected and Unprotected headers,
 * specified in rfc rfc8152 (https://datatracker.ietf.org/doc/html/rfc8152#section-3.1).
 */
@SuppressWarnings("VisibilityModifier")
public class CoseHeader {

    @Setter
    @Getter
    protected String algIdentifier = "";
    @Setter
    @Getter
    protected String keyIdentifier = "";
    @Setter
    @Getter
    protected String contentType = "";
    @Setter
    @Getter
    protected Map<Object, Object> parameters = null;
    @Setter
    @Getter
    protected List<X509Certificate> x5chain = null;

    /**
     * Prints the processed COSE Header data that is common to both Protected and Unprotected headers.
     * @return a formated string representation of the data in the COSE header object
     */
    public String printHeaderCommonContentsPretty() throws IOException {
        String returnString = "";

        if (!algIdentifier.isEmpty()) {
            returnString += "  Algorithm = " + algIdentifier + "\n";
        }
        if (!keyIdentifier.isEmpty()) {
            returnString += "  KeyId = " + keyIdentifier + "\n";
        }
        if (!contentType.isEmpty()) {
            returnString += "  Content Type = " + contentType + "\n";
        }
        if (x5chain != null) {
            returnString += "\n  X5Chain = \n";
            returnString += "  -----------------------------------------------------------------------\n";
            returnString += x5chain + "\n";
            returnString += "  -----------------------------------------------------------------------\n\n";
        }

        return returnString;
    }
}
