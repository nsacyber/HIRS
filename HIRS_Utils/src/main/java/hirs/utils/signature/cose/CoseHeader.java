package hirs.utils.signature.cose;

import lombok.Getter;
import lombok.Setter;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to hold COSE header components common to both Protected and Unprotected headers, specified in
 * rfc rfc8152 (<a href="https://datatracker.ietf.org/doc/html/rfc8152#section-3.1">rfc rfc8152</a>).
 */
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

    protected Map<Object, Object> parameters = null;

    protected List<X509Certificate> x5chain = null;

    /**
     * Prints the processed COSE Header data that is common to both Protected and Unprotected headers.
     *
     * @return a formated string representation of the data in the COSE header object
     */
    public String printHeaderCommonContentsPretty() {
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

    /**
     * Returns a defensive copy of the parameters map.
     *
     * @return a copy of the parameters map
     */
    public Map<Object, Object> getParameters() {
        return new HashMap<>(parameters);
    }

    /**
     * Sets the parameters map with a defensive copy.
     *
     * @param parameters the map to set
     */
    public void setParameters(final Map<Object, Object> parameters) {
        this.parameters = new HashMap<>(parameters);
    }

    /**
     * Returns a defensive copy of the X.509 certificate chain.
     *
     * @return a copy of the certificate chain list
     */
    public List<X509Certificate> getX5chain() {
        return new ArrayList<>(x5chain);
    }

    /**
     * Sets the X.509 certificate chain with a defensive copy.
     *
     * @param x5chain the certificate chain to set
     */
    public void setX5chain(final List<X509Certificate> x5chain) {
        this.x5chain = new ArrayList<>(x5chain);
    }
}
