package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import hirs.utils.signature.cose.Cbor.CborBstr;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

/**
 * Class pertaining to a {@code corim-meta-map}. Defined in Section 4.2.2 of the IETF CoRIM specification.
 */
public class MetaMap {
    private JsonNode rootNode = null;
    @Setter
    @Getter
    private String signerName = "";
    @Setter
    @Getter
    private String signerUri = "";
    @Setter
    @Getter
    private long notBefore = 0;
    @Setter
    @Getter
    private String notBeforeStr = "";
    @Setter
    @Getter
    private long notAfter = 0;
    @Setter
    @Getter
    private String notAfterStr = "";

    /**
     * Process corim-signer-map. Defined in section 4.2.2.1 of the IETF Corim spec.
     * @param mapData a CBOR-encoded byte array representing a {@code corim-meta-map}
     */
    public MetaMap(final byte[] mapData) {
        ObjectMapper mapper = new ObjectMapper(new CBORFactory());
        Format format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        ZonedDateTime dateTime = null;
        try {
            byte[] map = CborBstr.removeByteStringIfPresent(mapData);
            Map<String, Object> parsedData = mapper.readValue(new ByteArrayInputStream(map), Map.class);
            rootNode = mapper.readTree(map);
            signerName = rootNode.path("0").get("0").textValue(); // Signer Name
            if (rootNode.path("0").get("1") != null) {
                signerUri = rootNode.path("0").get("1").textValue(); // Signer URI
            }
            if (rootNode.path("1").get("0") != null) {  // not before
                notBefore = rootNode.path("1").get("0").longValue();
                Date date = new Date(notBefore * 1000);
                notBeforeStr = format.format(date);
            }
            if (rootNode.path("1").get("1") != null) {  // not before
                notAfter = rootNode.path("1").get("1").longValue();
                Date date = new Date(notAfter * 1000);
                notAfterStr = format.format(date);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
