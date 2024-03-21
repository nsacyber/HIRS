package hirs.attestationca.persist.entity.userdefined;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

/**
 * Unit test class for PolicySettings.
 */
public class PolicySettingsTest {

    /**
     * Tests that default policy settings are set correctly.
     */
    @Test
    public final void checkDefaultSettings() {
        PolicySettings policy = new PolicySettings("Default Supply Chain Policy");
        assertFalse(policy.isEcValidationEnabled());
        assertFalse(policy.isPcValidationEnabled());
        assertFalse(policy.isPcAttributeValidationEnabled());
        assertFalse(policy.isExpiredCertificateValidationEnabled());
        assertFalse(policy.isReplaceEC());
    }

    /**
     * Tests that all setters and getters work.
     */
    @Test
    public final void flipDefaultSettings() {
        PolicySettings policy = new PolicySettings("Default Supply Chain Policy");
        policy.setEcValidationEnabled(false);
        policy.setPcValidationEnabled(false);
        policy.setPcAttributeValidationEnabled(false);
        policy.setExpiredCertificateValidationEnabled(false);
        policy.setReplaceEC(true);
        assertFalse(policy.isEcValidationEnabled());
        assertFalse(policy.isPcValidationEnabled());
        assertFalse(policy.isPcAttributeValidationEnabled());
        assertFalse(policy.isExpiredCertificateValidationEnabled());
        assertTrue(policy.isReplaceEC());
    }

    /**
     * Tests that we can initiate a policy with a description.
     */
    @Test
    public final void createPolicyWithDescription() {
        final String description = "A default policy";
        PolicySettings policy = new PolicySettings("Default Supply Chain Policy",
                description);
        assertEquals(description, policy.getDescription());
    }
}
