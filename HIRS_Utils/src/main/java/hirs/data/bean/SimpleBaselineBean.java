package hirs.data.bean;

import java.util.Date;
import java.util.UUID;

import hirs.data.persist.enums.AlertSeverity;

/**
 * Provides a bean that can be used to encapsulate simple baseline data.
 */
public class SimpleBaselineBean {
    private UUID id;
    private Date createTime;
    private String name;
    private AlertSeverity severity;
    private String type;

    /**
     * Get the Baseline ID.
     * @return UUID.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get the Creation Timestamp.
     * @return Date.
     */
    public Date getCreateTime() {
        return (Date) createTime.clone();
    }

    /**
     * Get the Baseline name.
     * @return String.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the severity.
     * @return Alert.Severity.
     */
    public AlertSeverity getSeverity() {
        return severity;
    }

    /**
     * Get the Baseline type.
     * @return String.
     */
    public String getType() {
        return type;
    }
}
