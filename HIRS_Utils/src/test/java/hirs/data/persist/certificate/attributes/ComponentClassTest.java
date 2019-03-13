package hirs.data.persist.certificate.attributes;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * Tests for the ComponentClassTest class.
 */
public class ComponentClassTest {

    private static final String JSON_FILE = "/config/component-class.json";

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNoneUNK() throws URISyntaxException {
        int componentIdentifier = 1;
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Unknown", resultComponent);
        Assert.assertEquals("None", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNoneOther() throws URISyntaxException {
        int componentIdentifier = 0;
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Other", resultComponent);
        Assert.assertEquals("None", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentBlank() throws URISyntaxException {
        String componentIdentifier = "";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Unknown", resultCategory);
    }
    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNFEx() throws URISyntaxException {
        String componentIdentifier = "HIRS";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Unknown", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNull() throws URISyntaxException {
        String componentIdentifier = null;
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Unknown", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQuery() throws URISyntaxException {
        String componentIdentifier = "0x00040002";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("SAS Bridgeboard", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryInt() throws URISyntaxException {
        int componentIdentifier = 0x00040002;
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("SAS Bridgeboard", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryIntOther() throws URISyntaxException {
        int componentIdentifier = 0x00040000;
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Other", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryIntUnk() throws URISyntaxException {
        int componentIdentifier = 0x00040001;
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Unknown", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQuery2() throws URISyntaxException {
        String componentIdentifier = "0x00060012";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("DDR3 Memory", resultComponent);
        Assert.assertEquals("Memory", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryOther() throws URISyntaxException {
        String componentIdentifier = "0x00060000";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Other", resultComponent);
        Assert.assertEquals("Memory", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryUNK() throws URISyntaxException {
        String componentIdentifier = "0x00060001";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("Unknown", resultComponent);
        Assert.assertEquals("Memory", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonStandardQuery() throws URISyntaxException {
        String componentIdentifier = "00040002";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("SAS Bridgeboard", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonStandardQuery2() throws URISyntaxException {
        String componentIdentifier = "#00040002";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("SAS Bridgeboard", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonExistentValue() throws URISyntaxException {
        String componentIdentifier = "0x00040014";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonExistentValue2() throws URISyntaxException {
        String componentIdentifier = "0x0004FF14";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonExistentCategory() throws URISyntaxException {
        String componentIdentifier = "0x0015FF14";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategory();
        String resultComponent = instance.getComponent();
        Assert.assertEquals("None", resultComponent);
        Assert.assertEquals("Unknown", resultCategory);
    }
}