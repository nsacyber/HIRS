package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElements;
import lombok.Getter;

/**
 * As specified in the TCPA Main Specification section 4.5. This structure represents the version of
 * the TPM.
 */
@Getter
@StructElements(elements = {"major", "minor", "revisionMajor", "revisionMinor"})
public class Version implements Struct {

    /**
     * the major version indicator. For version 1 this MUST be 0x01.
     */
    private byte major;

    /**
     * the minor version indicator. For version 1 this MUST be 0x01.
     */
    private byte minor;

    /**
     * the value of the TCPA_PERSISTENT_DATA -&gt; revMajor.
     */
    private byte revisionMajor;

    /**
     * the value of the TCPA_PERSISTENT_DATA -&gt; revMinor.
     */
    private byte revisionMinor;

}
