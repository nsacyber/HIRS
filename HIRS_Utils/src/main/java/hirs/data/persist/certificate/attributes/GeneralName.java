package hirs.data.persist.certificate.attributes;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GeneralNames {

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
    private String organizationUnit;
    private String evaluationString;

    public GeneralNames(final String commonName, final String country,
                        final String state, final String locality,
                        final String organization, final String organizationUnit) {
        this.commonName = commonName;
        this.country = country;
        this.state = state;
        this.locality = locality;
        this.organization = organization;
        this.organizationUnit = organizationUnit;
    }

    public GeneralNames(final String generalNamesString) throws IllegalArgumentException {
        if (generalNamesString == null) {
            throw new IllegalArgumentException("");
        }
        evaluationString = generalNamesString;

        if (generalNamesString.contains(SEPARATOR_PLUS)) {
            evaluationString = generalNamesString.replace(SEPARATOR_PLUS, SEPARATOR_COMMA);
        }

        Matcher matcher = SEPARATOR_PATTERN.matcher(evaluationString);

        while (matcher.find()) {
            String elements[] = matcher.group(ELEMENT_INDEX).split(EQUALS_SIGN);
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
                    organizationUnit = elements[ELEMENT_INDEX];
                    break;
            }
        }
    }

    public String getCommonName() {
        return commonName;
    }

    public String getCountry() {
        return country;
    }

    public String getState() {
        return state;
    }

    public String getLocality() {
        return locality;
    }

    public String getOrganization() {
        return organization;
    }

    public String getOrganizationUnit() {
        return organizationUnit;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeneralNames other = (GeneralNames) obj;
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
        return Objects.hash(commonName, country, state, locality, organization, organizationUnit);
    }

    @Override
    public String toString() {
        return String.format("CN=%s, C=%s, ST=%s, L=%s, O=%s, OU=%s",
                commonName, country, state, locality,
                organization, organizationUnit);
    }
}
