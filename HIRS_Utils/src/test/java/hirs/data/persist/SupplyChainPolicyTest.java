package hirs.data.persist;

import org.hibernate.Session;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test class for SupplyChainPolicy.
 */
public class SupplyChainPolicyTest extends HibernateTest<SupplyChainPolicy> {
    @Override
    protected SupplyChainPolicy getDefault(final Session session) {
        final SupplyChainPolicy policy =
                new SupplyChainPolicy("Default Supply Chain Policy");
        return policy;
    }

    @Override
    protected Class<?> getDefaultClass() {
        final SupplyChainPolicy policy = new SupplyChainPolicy("Default Supply Chain Policy");
        return policy.getClass();
    }

    @Override
    protected void update(final SupplyChainPolicy object) {
        object.setEcValidationEnabled(false);
    }

    @Override
    protected void assertGetEqual(final SupplyChainPolicy defaultObject,
                                  final SupplyChainPolicy retrieved) {
        Assert.assertEquals(defaultObject.isEcValidationEnabled(),
                retrieved.isEcValidationEnabled());
        Assert.assertEquals(defaultObject.isExpiredCertificateValidationEnabled(),
                retrieved.isExpiredCertificateValidationEnabled());
        Assert.assertEquals(defaultObject.isPcAttributeValidationEnabled(),
                retrieved.isPcAttributeValidationEnabled());
        Assert.assertEquals(defaultObject.isPcValidationEnabled(),
                retrieved.isPcValidationEnabled());
        Assert.assertEquals(defaultObject.isReplaceEC(),
                retrieved.isReplaceEC());
    }

    @Override
    protected void assertUpdateEqual(final SupplyChainPolicy defaultObject,
                                     final SupplyChainPolicy update) {
        Assert.assertFalse(update.isEcValidationEnabled());
    }

    @Override
    protected Class<?>[] getCleanupClasses() {
        return new Class<?>[] {SupplyChainPolicy.class};
    }

    /**
     * Tests that default policy settings are set correctly.
     */
    @Test
    public final void checkDefaultSettings() {
        SupplyChainPolicy policy = new SupplyChainPolicy("Default Supply Chain Policy");
        Assert.assertFalse(policy.isEcValidationEnabled());
        Assert.assertFalse(policy.isPcValidationEnabled());
        Assert.assertFalse(policy.isPcAttributeValidationEnabled());
        Assert.assertFalse(policy.isExpiredCertificateValidationEnabled());
        Assert.assertFalse(policy.isReplaceEC());
    }

    /**
     * Tests that all setters and getters work.
     */
    @Test
    public final void flipDefaultSettings() {
        SupplyChainPolicy policy = new SupplyChainPolicy("Default Supply Chain Policy");
        policy.setEcValidationEnabled(false);
        policy.setPcValidationEnabled(false);
        policy.setPcAttributeValidationEnabled(false);
        policy.setExpiredCertificateValidationEnabled(false);
        policy.setReplaceEC(true);
        Assert.assertFalse(policy.isEcValidationEnabled());
        Assert.assertFalse(policy.isPcValidationEnabled());
        Assert.assertFalse(policy.isPcAttributeValidationEnabled());
        Assert.assertFalse(policy.isExpiredCertificateValidationEnabled());
        Assert.assertTrue(policy.isReplaceEC());
    }

    /**
     * Tests that we can initiate a policy with a description.
     */
    @Test
    public final void createPolicyWithDescription() {
        final String description = "A default policy";
        SupplyChainPolicy policy = new SupplyChainPolicy("Default Supply Chain Policy",
                description);
        Assert.assertEquals(policy.getDescription(), description);
    }
}
