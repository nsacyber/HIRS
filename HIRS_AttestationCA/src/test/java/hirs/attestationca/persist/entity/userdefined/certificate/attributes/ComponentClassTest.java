package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the ComponentClassTest class.
 */
public class ComponentClassTest {

    private static final String JSON_FILE = "/config/component-class.json";

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNoneUNK() throws URISyntaxException {
        final String componentIdentifier = "00000001";
        ComponentClass instance = new ComponentClass("TCG",
                Paths.get(Objects.requireNonNull(this.getClass().getResource(JSON_FILE)).toURI()),
                componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNoneOther() throws URISyntaxException {
        final String componentIdentifier = "00000000";
        ComponentClass instance = new ComponentClass("TCG", Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentBlank() throws URISyntaxException {
        final String componentIdentifier = "";
        ComponentClass instance = new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNFEx() throws URISyntaxException {
        final String componentIdentifier = "99999999";
        ComponentClass instance = new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNull() throws URISyntaxException {
        final String componentIdentifier = null;
        ComponentClass instance = new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
    }

    /**
     * Tests the getComponent method from the ComponentClass class where the
     * registry type is of type TCG.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryTCG() throws URISyntaxException {
        final String componentIdentifier = "0x00040002";
        ComponentClass instance = new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "SAS Bridgeboard");
        assertEquals(resultCategory, "Modules");
    }

    /**
     * Tests the getComponent method from the ComponentClass class where the
     * registry type is of type SMBIOS.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQuerySMBIOS() throws URISyntaxException {
        final String componentIdentifier = "0x00040003";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.3", Paths.get(
                Objects.requireNonNull(this.getClass()
                        .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        final String resultRegistry = instance.getRegistryType();

        assertEquals("SMBIOS", resultRegistry);
        assertEquals("Central Processor", resultComponent);
        assertEquals("Processor", resultCategory);
    }

    /**
     * Test of getComponent method, of class ComponentClass where the
     * registry type is of type PCIE.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryPCIE() throws URISyntaxException {
        final String componentIdentifier = "0x00080004"; // TODO placeholder for now
        ComponentClass instance = new ComponentClass("2.23.133.18.3.4", Paths.get(
                Objects.requireNonNull(this.getClass()
                        .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        final String resultRegistry = instance.getRegistryType();

        assertEquals("PCIE", resultRegistry);

        //TODO Once the component-class.json file is updated to reflect the two new component
        // registries, we will then write tests that test the component class' category/component
        // properties.
    }

    /**
     * Tests the getComponent method from the ComponentClass class where the
     * registry type is of type STORAGE.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQuerySTORAGE() throws URISyntaxException {
        final String componentIdentifier = "0x00080004"; // TODO placeholder for now
        ComponentClass instance = new ComponentClass("2.23.133.18.3.5", Paths.get(
                Objects.requireNonNull(this.getClass()
                        .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        final String resultRegistry = instance.getRegistryType();

        assertEquals("STORAGE", resultRegistry);

        //TODO Once the component-class.json file is updated to reflect the two new component
        // registries, we will then write tests that test the component class' category/component
        // properties.

    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryIntOther() throws URISyntaxException {
        final String componentIdentifier = "0x00040000";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.1", Paths.get(
                Objects.requireNonNull(this.getClass()
                        .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals("Other", resultComponent);
        assertEquals("Modules", resultCategory);
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryIntUnk() throws URISyntaxException {
        final String componentIdentifier = "0x00040001";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.1", Paths.get(
                Objects.requireNonNull(this.getClass()
                        .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals("Unknown", resultComponent);
        assertEquals("Modules", resultCategory);
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQuery2() throws URISyntaxException {
        final String componentIdentifier = "0x00060015";
        ComponentClass instance = new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals("DDR3 Memory", resultComponent);
        assertEquals("Memory", resultCategory);
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentStandardQueryUNK() throws URISyntaxException {
        final String componentIdentifier = "0x00060001";
        ComponentClass instance = new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals("Unknown", resultComponent);
        assertEquals("Memory", resultCategory);
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonStandardQuery() throws URISyntaxException {
        final String componentIdentifier = "0x00040002";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.1", Paths.get(
                Objects.requireNonNull(this.getClass()
                        .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "SAS Bridgeboard");
        assertEquals(resultCategory, "Modules");
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonStandardQuery2() throws URISyntaxException {
        final String componentIdentifier = "0x00040002";
        ComponentClass instance = new ComponentClass("2.23.133.18.3.1", Paths.get(
                Objects.requireNonNull(this.getClass()
                        .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "SAS Bridgeboard");
        assertEquals(resultCategory, "Modules");
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonExistentValue() throws URISyntaxException {
        final String componentIdentifier = "0x00040014";
        ComponentClass instance = new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertNull(resultComponent);
        assertEquals(resultCategory, "Modules");
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonExistentValue2() throws URISyntaxException {
        final String componentIdentifier = "0x0004FF14";
        ComponentClass instance = new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertNull(resultComponent);
        assertEquals(resultCategory, "Modules");
    }

    /**
     * Tests the getComponent method from the ComponentClass class.
     *
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testGetComponentNonExistentCategory() throws URISyntaxException {
        final String componentIdentifier = "0x0015FF14";
        ComponentClass instance = new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
                .getResource(JSON_FILE)).toURI()), componentIdentifier);
        final String resultCategory = instance.getCategoryStr();
        final String resultComponent = instance.getComponentStr();
        assertEquals(resultComponent, "Unknown");
        assertEquals(resultCategory, "None");
    }
}
