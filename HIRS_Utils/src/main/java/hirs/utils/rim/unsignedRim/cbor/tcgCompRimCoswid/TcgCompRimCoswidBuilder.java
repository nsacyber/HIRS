package hirs.utils.rim.unsignedRim.cbor.tcgCompRimCoswid;

import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Class that is used to build a TCG Component RIM Coswid file based upon values previously
 * retrieved from a config file.
 * Uses the indexes found in both rfc 9393 Section 2.3 "The concise-swid-tag Map"
 * and the TCG Component RIM spec.
 */
public class TcgCompRimCoswidBuilder extends CoswidBuilder {

    /**
     * Holds the TCG Component RIM Coswid.
     */
    private final TcgCompRimCoswid tcRim = new TcgCompRimCoswid();

    /**
     * Constructor for the Component Rim Builder.
     *
     * @param config TcgComponentRimConfig config created from a json file.
     */
    public TcgCompRimCoswidBuilder(final TcgCompRimCoswidConfig config) {
        super(config);
        tcRim.setCrimBindingSpec(config.getBindingSpec());
        tcRim.setCrimBindingSpecVersion(config.getBindingSpecVersion());
        tcRim.setCrimComponentManufacturer(config.getComponentManufacturerStr());
        tcRim.setCrimComponentManufacturerID(config.getComponentManufacturerID());
        tcRim.setCrimPayloadType(config.getPayloadType());
        tcRim.setCrimSpdmMeasurementBlock(config.getSpdmMeasurementBlock());
        tcRim.setCrimSpdmMeasurementBlockIndex(config.getSpdmMeasurementBlockIndex());
        tcRim.setCrimSpdmVersion(config.getSpdmVersion());
        tcRim.setCrimSpdmMeasurementHash(config.getSpdmMeasurementHash());
        tcRim.setCrimSpdmMeasurementRawData(config.getSpdmMeasurementRawData());
        tcRim.setCrimSpdmMeasurementValueType(config.getSpdmMeasurementValueType());
    }

    /**
     * Writes a TCG Component RIM Coswid object to a file.
     *
     * @param fileName file to hold the new TCG Component rim
     * @throws IOException if any issues arise attempting to create a TCG Component RIM
     */
    public void createTcgComponentRim(final String fileName) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        initTcgRim(out);
        ByteArrayOutputStream taggedCoswid = addCborTag(out);
        try {
            Files.write(new File(fileName).toPath(), taggedCoswid.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a TCG Component RIM Coswid object.
     *
     * @param out ByteArrayOutputStream to wite the object to
     */
    public void initTcgRim(final ByteArrayOutputStream out) {
        initCoswid(out); // Add Coswid defined fields
        // Meta - add TCG Component RIM defined fields
        addStringAttribute(tcRim.getCrimBindingSpec(),
                TcgCompRimCoswid.CRIM_BINDING_SPEC_INT, out);
        addStringAttribute(tcRim.getCrimBindingSpecVersion(),
                TcgCompRimCoswid.CRIM_BINDING_SPEC_VERSION_INT, out);
        addStringAttribute(tcRim.getCrimComponentManufacturer(),
                TcgCompRimCoswid.CRIM_COMPONENT_MANUFACTURER_INT, out);
        addStringAttribute(tcRim.getCrimComponentManufacturerID(),
                TcgCompRimCoswid.CRIM_COMPONENT_MANUFACTURER_ID_INT, out);
        addStringAttribute(tcRim.getCrimPayloadType(),
                TcgCompRimCoswid.CRIM_PAYLOAD_TYPE_INT, out);
        addStringAttribute(tcRim.getCrimSpdmMeasurementBlock(),
                TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_BLOCK_INT, out);
        addStringAttribute(tcRim.getCrimSpdmMeasurementBlockIndex(),
                TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_INT, out);
        addStringAttribute(tcRim.getCrimSpdmVersion(),
                TcgCompRimCoswid.CRIM_SPDM_VERSION_INT, out);
        addStringAttribute(tcRim.getCrimSpdmMeasurementHash(),
                TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_HASH_INT, out);
        addStringAttribute(tcRim.getCrimSpdmMeasurementRawData(),
                TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_RAW_DATA_INT, out);
        addStringAttribute(tcRim.getCrimSpdmMeasurementValueType(),
                TcgCompRimCoswid.SPDM_MEASUREMENT_VALUE_TYPE_INT, out);
        createPayload(config.getPayloadNode(), out);
        completeCoswid(out); // Close out RIM
    }
}
