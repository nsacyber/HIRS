package hirs.data.persist.certificate.attributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;

/**
 * The General Names parser, pulls apart the string associated with the
 * Distinguished Names field and makes them separately available.
 */
public class GeneralNamesParser {

    private static final Logger LOGGER = LogManager.getLogger(
            GeneralNamesParser.class);

    private static final String SEPARATOR_PATTERN_STRING = "(?:^|,)((?:[^\",]|\"[^\"]*\")*)";
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile(SEPARATOR_PATTERN_STRING);
    private static final String EQUALS_SIGN = "=";
    private static final String SEPARATOR_COMMA = ",";
    private static final String SEPARATOR_PLUS = "+";
    private static final int KEY_INDEX = 0;
    private static final int ELEMENT_INDEX = 1;

    private String commonName;
    private String country;
    private String state;
    private String locality;
    private String organization;
    private List<String> organizationUnit;
    private String evaluationString;

    /**
     * Basic constructor that sets class variables based on what is provided.
     *
     * @param commonName general name
     * @param country country of origin
     * @param state state of origin
     * @param locality associated area
     * @param organization overall org
     * @param organizationUnit the specific org units
     */
    public GeneralNamesParser(final String commonName, final String country,
            final String state, final String locality,
            final String organization, final List<String> organizationUnit) {
        this.commonName = commonName;
        this.country = country;
        this.state = state;
        this.locality = locality;
        this.organization = organization;
        this.organizationUnit = organizationUnit;
    }

    public GeneralNamesParser(final X500Name generalName) {
        
    }

    /**
     * Central constructor that sets the class variables based on the string
     * provided.
     *
     * @param generalNamesString a string to parse that should contain the
     * different elements associated with a DN
     * @throws IllegalArgumentException if the parameter is null
     */
    public GeneralNamesParser(final String generalNamesString) throws IllegalArgumentException {
        if (generalNamesString == null) {
            throw new IllegalArgumentException("Provided DN string is null.");
        }
        evaluationString = generalNamesString;

        if (generalNamesString.contains(SEPARATOR_PLUS)) {
            evaluationString = generalNamesString.replace(SEPARATOR_PLUS, SEPARATOR_COMMA);
        }

        Matcher matcher = SEPARATOR_PATTERN.matcher(evaluationString);

        while (matcher.find()) {
            String[] elements = matcher.group(ELEMENT_INDEX).split(EQUALS_SIGN);
            switch (elements[KEY_INDEX]) {
                case "CN":
                    commonName = elements[ELEMENT_INDEX];
                    break;
                case "C":
                    country = elements[ELEMENT_INDEX];
                    break;
                case "L":
                    locality = elements[ELEMENT_INDEX];
                    break;
                case "ST":
                    state = elements[ELEMENT_INDEX];
                    break;
                case "O":
                    organization = elements[ELEMENT_INDEX];
                    break;
                case "OU":
                    organizationUnit = Arrays.asList(elements[ELEMENT_INDEX]);
                    break;
                default:
                    if (elements.length > 1) {
                        LOGGER.info(String.format("%s -> %s was not captured.",
                                elements[KEY_INDEX], elements[ELEMENT_INDEX]));
                    }
                    break;
            }
        }
    }

    /**
     * Getter for the common name associated with the certificate.
     *
     * @return string for the common name
     */
    public String getCommonName() {
        return commonName;
    }

    /**
     * Getter for the country of origin string.
     *
     * @return string for the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * Getter for the state string.
     *
     * @return string for the state
     */
    public String getState() {
        return state;
    }

    /**
     * Getter for the locality string.
     *
     * @return string for the locality
     */
    public String getLocality() {
        return locality;
    }

    /**
     * Getter for the organization string.
     *
     * @return string for the organization
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * Getter for the list of Org Units. A DN can potentially have multiple
     * units.
     *
     * @return list of org units
     */
    public List<String> getOrganizationUnit() {
        return Collections.unmodifiableList(organizationUnit);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeneralNamesParser other = (GeneralNamesParser) obj;
        if (!Objects.equals(this.commonName, other.commonName)) {
            return false;
        }
        if (!Objects.equals(this.country, other.country)) {
            return false;
        }
        if (!Objects.equals(this.state, other.state)) {
            return false;
        }
        if (!Objects.equals(this.locality, other.locality)) {
            return false;
        }
        if (!Objects.equals(this.organization, other.organization)) {
            return false;
        }
        if (!Objects.equals(this.organizationUnit, other.organizationUnit)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(commonName, country, state, locality,
                organization, organizationUnit);
    }

    @Override
    public String toString() {
        return String.format("CN=%s, C=%s, ST=%s, L=%s, O=%s, OU=%s",
                commonName, country, state, locality,
                organization, organizationUnit);
    }
}
