package hirs.attestationca.persist.entity.userdefined;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        assertFalse(policy.isEcValidationEnabled());
        assertFalse(policy.isPcValidationEnabled());
        assertFalse(policy.isPcAttributeValidationEnabled());
        assertFalse(policy.isExpiredCertificateValidationEnabled());
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
