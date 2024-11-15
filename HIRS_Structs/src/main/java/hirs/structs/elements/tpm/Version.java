package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElements;

/**
 * As specified in the TCPA Main Specification section 4.5. This structure represents the version of
 * the TPM.
 */
@StructElements(elements = {"major", "minor", "revisionMajor", "revisionMinor"})
public class Version implements Struct {

    private byte major;

    private byte minor;

    private byte revisionMajor;

    private byte revisionMinor;

    /**
     * @return the major version indicator. For version 1 this MUST be 0x01
     */
    public byte getMajor() {
        return major;
    }

    /**
     * @return the minor version indicator. For version 1 this MUST be 0x01
     */
    public byte getMinor() {
        return minor;
    }

    /**
     * @return the value of the TCPA_PERSISTENT_DATA -&gt; revMajor
     */
    public byte getRevisionMajor() {
        return revisionMajor;
    }

    /**
     * @return the value of the TCPA_PERSISTENT_DATA -&gt; revMinor
     */
    public byte getRevisionMinor() {
        return revisionMinor;
    }
}
