package hirs.data.persist;

import hirs.data.persist.policy.PCRPolicy;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the PCRPolicyTest class.
 */
public class PCRPolicyTest {

    private static final String[] BASELINE_PCRS = new String[]{
        "cc8695ee470cd21f32de41df451376d9e751cadb",
        "b2a83b0ebf2f8374299a5b2bdfc31ea955ad7236",
        "b2a83b0ebf2f8374299a5b2bdfc31ea955ad7236",
        "b2a83b0ebf2f8374299a5b2bdfc31ea955ad7236",
        "e6e2c4ee5c8c79fa9ca827dd2b1b8341872a141f",
        "e44e1f53a4f2b2eabcdc9f2a450d84c0c3999364",
        "b2a83b0ebf2f8374299a5b2bdfc31ea955ad7236",
        "c18a8143298f26eff6b01c1c8d859b6583261467",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "335425f6d34a0a601b1debfd34752f90226253f5",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000"};
    private static final String[] QUOTE_PCRS = new String[]{
        "cc8695ee470cd21f32de41df451376d9e751cadb",
        "b2a83b0ebf2f8374299a5b2bdfc31ea955ad7236",
        "b2a83b0ebf2f8374299a5b2bdfc31ea955ad7236",
        "b2a83b0ebf2f8374299a5b2bdfc31ea955ad7236",
        "e6e2c4ee5c8c79fa9ca827dd2b1b8341872a141f",
        "e44e1f53a4f2b2eabcdc9f2a450d84c0c3999364",
        "b2a83b0ebf2f8374299a5b2bdfc31ea955ad7236",
        "c18a8143298f26eff6b01c1c8d859b6583261467",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "96c28d350ed092531d3f6f27d05a58d9f5dd8765",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "335425f6d34a0a601b1debfd34752f90226253f5",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "ffffffffffffffffffffffffffffffffffffffff",
        "ffffffffffffffffffffffffffffffffffffffff",
        "ffffffffffffffffffffffffffffffffffffffff",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000"};

    /**
     * Test of testValidatePcrsPass method, of class PCRPolicy.
     */
    @Test
    public void testValidatePcrsPass() {
        PCRPolicy instance = new PCRPolicy(BASELINE_PCRS);
        instance.setEnableIgnoreIma(false);
        instance.setEnableIgnoretBoot(false);
        StringBuilder result = instance.validatePcrs(BASELINE_PCRS);
        Assert.assertEquals(result.length(), 0);
    }

    /**
     * Test of testValidatePcrsFail method, of class PCRPolicy.
     */
    @Test
    public void testValidatePcrsFail() {
        PCRPolicy instance = new PCRPolicy(BASELINE_PCRS);
        instance.setEnableIgnoreIma(false);
        instance.setEnableIgnoretBoot(false);
        StringBuilder result = instance.validatePcrs(QUOTE_PCRS);
        Assert.assertNotEquals(result.length(), 0);
    }

    /**
     * Test of testValidatePcrsFailIgnoreIma method, of class PCRPolicy.
     */
    @Test
    public void testValidatePcrsFailIgnoreIma() {
        PCRPolicy instance = new PCRPolicy(BASELINE_PCRS);
        instance.setEnableIgnoreIma(true);
        instance.setEnableIgnoretBoot(false);
        StringBuilder result = instance.validatePcrs(QUOTE_PCRS);
        Assert.assertNotEquals(result.length(), 0);
    }

    /**
     * Test of testValidatePcrsFailIgnoreTBoot method, of class PCRPolicy.
     */
    @Test
    public void testValidatePcrsFailIgnoreTBoot() {
        PCRPolicy instance = new PCRPolicy(BASELINE_PCRS);
        instance.setEnableIgnoreIma(false);
        instance.setEnableIgnoretBoot(true);
        StringBuilder result = instance.validatePcrs(QUOTE_PCRS);
        Assert.assertNotEquals(result.length(), 0);
    }

    /**
     * Test of testValidatePcrsPassIgnoreImaAndTBoot method, of class PCRPolicy.
     */
    @Test
    public void testValidatePcrsPassIgnoreImaAndTBoot() {
        PCRPolicy instance = new PCRPolicy(BASELINE_PCRS);
        instance.setEnableIgnoreIma(true);
        instance.setEnableIgnoretBoot(true);
        StringBuilder result = instance.validatePcrs(QUOTE_PCRS);
        Assert.assertEquals(result.length(), 0);
    }
}
