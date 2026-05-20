package hirs.attestationca.persist.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests methods in the {@link CredentialHelper} utility class.
 */
public class CredentialHelperTest {

    private static final String DN_WITH_ESCAPED_COMMA =
            "CN=PCTEST\\, FANCY.,L=Here,ST=There,C=US";

    /**
     * Tests that sorting a DN preserves escaped commas inside RDN values.
     */
    @Test
    public void testParseSortDNsPreservesEscapedComma() {
        String sortedDn = CredentialHelper.parseSortDNs(DN_WITH_ESCAPED_COMMA);

        assertEquals("c=us,cn=pctest\\, fancy.,l=here,st=there", sortedDn);
    }
}
