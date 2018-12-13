package hirs.data.persist.certificate.attributes;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the ComponentClassTest class.
 */
public class ComponentClassTest {

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentNoneUNK() {
        int componentIdentifier = 1;
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Unknown", resultComponent);
        Assert.assertEquals("None", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentNoneOther() {
        int componentIdentifier = 0;
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Other", resultComponent);
        Assert.assertEquals("None", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentBlank() {
        String componentIdentifier = "";
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Unknown", resultCategory);
    }
    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentNFEx() {
        String componentIdentifier = "HIRS";
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Unknown", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentNull() {
        String componentIdentifier = null;
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Unknown", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentStandardQuery() {
        String componentIdentifier = "0x00040002";
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("SAS Bridgeboard", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentStandardQueryInt() {
        int componentIdentifier = 0x00040002;
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("SAS Bridgeboard", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentStandardQueryIntOther() {
        int componentIdentifier = 0x00040000;
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Other", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentStandardQueryIntUnk() {
        int componentIdentifier = 0x00040001;
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Unknown", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentStandardQuery2() {
        String componentIdentifier = "0x00060012";
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("DDR3 Memory", resultComponent);
        Assert.assertEquals("Memory", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentStandardQueryOther() {
        String componentIdentifier = "0x00060000";
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Other", resultComponent);
        Assert.assertEquals("Memory", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentStandardQueryUNK() {
        String componentIdentifier = "0x00060001";
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Unknown", resultComponent);
        Assert.assertEquals("Memory", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentNonStandardQuery() {
        String componentIdentifier = "00040002";
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("SAS Bridgeboard", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentNonExistentValue() {
        String componentIdentifier = "0x00040014";
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     */
    @Test
    public void testGetComponentNonExistentValue2() {
        String componentIdentifier = "0x0004FF14";
        ComponentClass instance = new ComponentClass(componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }
}
