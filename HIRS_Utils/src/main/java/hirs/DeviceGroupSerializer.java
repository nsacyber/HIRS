package hirs;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import hirs.data.persist.DeviceGroup;

import java.io.IOException;
import java.util.Date;

/**
 * Serializes <code>DeviceGroup</code> data for Json to process.
 */
public class DeviceGroupSerializer extends JsonSerializer<DeviceGroup> {

    @Override
    public void serialize(final DeviceGroup value, final JsonGenerator gen,
                          final SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        if (value.getId() != null) {
            gen.writeStringField("id", value.getId().toString());
        } else {
            gen.writeNullField("id");
        }
        gen.writeNumberField("createTime", value.getCreateTime().getTime());
        Date archivedTime = value.getArchivedTime();
        if (archivedTime != null) {
            gen.writeNumberField("archivedTime", archivedTime.getTime());
        } else {
            gen.writeNullField("archivedTime");
        }
        gen.writeStringField("archivedDescription", value.getArchivedDescription());
        gen.writeStringField("name", value.getName());
        gen.writeStringField("description", value.getDescription());
        gen.writeNumberField("periodicReportDelayThreshold",
                value.getPeriodicReportDelayThreshold());
        gen.writeBooleanField("enablePeriodicReportDelayAlert",
                value.isEnablePeriodicReportDelayAlert());
        gen.writeNumberField("onDemandReportDelayThreshold",
                value.getOnDemandReportDelayThreshold());
        gen.writeBooleanField("enableOnDemandReportDelayAlert",
                value.isEnableOnDemandReportDelayAlert());
        gen.writeBooleanField("waitForAppraisalCompletionEnabled",
                value.isWaitForAppraisalCompletionEnabled());
        gen.writeObjectField("scheduledJobInfo", value.getScheduledJobInfo());
        gen.writeNumberField("numberOfDevices", value.getNumberOfDevices());
        gen.writeNumberField("numberOfTrustedDevices", value.getNumberOfTrustedDevices());
        gen.writeStringField("healthStatus", value.getHealthStatus().toString());
        gen.writeBooleanField("archived", value.isArchived());
        gen.writeEndObject();
    }
}
