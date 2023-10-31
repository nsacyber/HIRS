package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

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
        String componentIdentifier = "00000001";
        ComponentClass instance = new ComponentClass("TCG",
                Paths.get(this.getClass().getResource(JSON_FILE).toURI()),
                componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNoneOther() throws URISyntaxException {
        String componentIdentifier = "00000000";
        ComponentClass instance = new ComponentClass("TCG", Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
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
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
    }
    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNFEx() throws URISyntaxException {
        String componentIdentifier = "99999999";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
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
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryTCG() throws URISyntaxException {
        String componentIdentifier = "0x00040002";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "SAS Bridgeboard");
        assertEquals(resultCategory, "Modules");
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQuerySMBIOS() throws URISyntaxException {
        String componentIdentifier = "0x00040003";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.3", Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals("Central Processor", resultComponent);
        assertEquals("Processor", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryIntTCG() throws URISyntaxException {
        String componentIdentifier = "0x00040002";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.1", Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "SAS Bridgeboard");
        assertEquals(resultCategory, "Modules");
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryIntSMBIOS() throws URISyntaxException {
        String componentIdentifier = "0x00040003";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.3", Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals("Central Processor", resultComponent);
        assertEquals("Processor", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryIntOther() throws URISyntaxException {
        String componentIdentifier = "0x00040000";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.1", Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals("Other", resultComponent);
        assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryIntUnk() throws URISyntaxException {
        String componentIdentifier = "0x00040001";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.1", Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals("Unknown", resultComponent);
        assertEquals("Modules", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQuery2() throws URISyntaxException {
        String componentIdentifier = "0x00060015";
        ComponentClass instance = new ComponentClass(Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals("DDR3 Memory", resultComponent);
        assertEquals("Memory", resultCategory);
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
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals("Unknown", resultComponent);
        assertEquals("Memory", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonStandardQuery() throws URISyntaxException {
        String componentIdentifier = "0x00040002";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.1", Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "SAS Bridgeboard");
        assertEquals(resultCategory, "Modules");
    }

    /**
     * Test of getComponent method, of class ComponentClass.
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonStandardQuery2() throws URISyntaxException {
        String componentIdentifier = "0x00040002";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.1", Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI()), componentIdentifier);
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "SAS Bridgeboard");
        assertEquals(resultCategory, "Modules");
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
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertNull(resultComponent);
        assertEquals(resultCategory, "Modules");
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
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertNull(resultComponent);
        assertEquals(resultCategory, "Modules");
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
        String resultCategory = instance.getCategoryStr();
        String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
    }
}
