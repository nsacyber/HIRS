package hirs.data.persist.certificate.attributes;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.RFC4519Style;

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

    private String commonName;
    private String country;
    private String state;
    private String locality;
    private List<String> organization;
    private List<String> organizationUnit;
    private String originalString;

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
            final List<String> organization, final List<String> organizationUnit) {
        this.commonName = commonName;
        this.country = country;
        this.state = state;
        this.locality = locality;
        this.organization = organization;
        this.organizationUnit = organizationUnit;
    }

    /**
     * Central constructor that sets the class variables based on the string
     * provided.
     *
     * @param generalName a string to parse that should contain the
     * different elements associated with a DN
     * @throws IllegalArgumentException if the parameter is null
     */
    public GeneralNamesParser(final String generalName) throws IllegalArgumentException {
        if (generalName == null) {
            throw new IllegalArgumentException("Provided DN string is null.");
        }

        originalString = generalName;

        if (originalString.isEmpty()) {
            return;
        }

        if (generalName.contains(SEPARATOR_PLUS)) {
            originalString = generalName.replace(SEPARATOR_PLUS, SEPARATOR_COMMA);
        }

        X500Name name = new X500Name(originalString);
        RDN[] rdns = name.getRDNs();
        organization = new LinkedList<>();
        organizationUnit = new LinkedList<>();

        for (RDN rdn : rdns) {
            if (rdn.getFirst().getType().equals(RFC4519Style.o)) {
                organization.add(rdn.getFirst().getValue().toString());
            } else if (rdn.getFirst().getType().equals(RFC4519Style.ou)) {
                organizationUnit.add(rdn.getFirst().getValue().toString());
            } else if (rdn.getFirst().getType().equals(RFC4519Style.cn)) {
                commonName = rdn.getFirst().getValue().toString();
            } else if (rdn.getFirst().getType().equals(RFC4519Style.c)) {
                country = rdn.getFirst().getValue().toString();
            } else if (rdn.getFirst().getType().equals(RFC4519Style.l)) {
                locality = rdn.getFirst().getValue().toString();
            } else if (rdn.getFirst().getType().equals(RFC4519Style.st)) {
                state = rdn.getFirst().getValue().toString();
            }
        }

//        Matcher matcher;
//
//        if (generalNamesString.contains(SEPARATOR_PLUS)) {
//            matcher = SEPARATOR_PATTERN.matcher(generalNamesString.replace(
//                    SEPARATOR_PLUS, SEPARATOR_COMMA));
//        } else {
//            matcher = SEPARATOR_PATTERN.matcher(generalNamesString);
//        }
//
//        while (matcher.find()) {
//            String[] elements = matcher.group(ELEMENT_INDEX).split(EQUALS_SIGN);
//            switch (elements[KEY_INDEX]) {
//                case "CN":
//                    commonName = elements[ELEMENT_INDEX];
//                    break;
//                case "C":
//                    country = elements[ELEMENT_INDEX];
//                    break;
//                case "L":
//                    locality = elements[ELEMENT_INDEX];
//                    break;
//                case "ST":
//                    state = elements[ELEMENT_INDEX];
//                    break;
//                case "O":
//                    organization = elements[ELEMENT_INDEX];
//                    break;
//                case "OU":
//                    if (organizationUnit != null) {
//                        organizationUnit.add(elements[ELEMENT_INDEX]);
//                    } else {
//                        organizationUnit = Arrays.asList(elements[ELEMENT_INDEX]);
//                    }
//                    break;
//                default:
//                    if (elements.length > 1) {
//                        LOGGER.info(String.format("Type %s -> %s was not captured.",
//                                elements[KEY_INDEX], elements[ELEMENT_INDEX]));
//                    }
//                    break;
//            }
//        }
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
    public List<String> getOrganization() {
        return Collections.unmodifiableList(organization);
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

    /**
     * This will only return what was originally given.  Since the purpose
     * of this class is to compare and not display.
     * @return original string.
     */
    @Override
    public String toString() {
        return String.format("CN=%s, C=%s, ST=%s, L=%s, O=%s, OU=%s",
                commonName, country, state, locality,
                StringUtils.join(organization, ","),
                StringUtils.join(organizationUnit, ","));
//        return originalString;
    }
}
