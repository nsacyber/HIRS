package hirs.attestationca;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import hirs.attestationca.entity.AppraisalResult;

import java.io.IOException;

/**
 * Serializes <code>AppraisalResult</code> data for Json to process.
 */
public class AppraisalResultSerializer extends JsonSerializer<AppraisalResult> {
    @Override
    public void serialize(final AppraisalResult value,
                          final JsonGenerator gen,
                          final SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("appraisalStatus", value.getAppraisalStatus().toString());
        gen.writeStringField("appraisalResultMessage", value.getAppraisalResultMessage());
        gen.writeStringField("appraiser", value.getAppraiser().getName());
        gen.writeEndObject();
    }
}
