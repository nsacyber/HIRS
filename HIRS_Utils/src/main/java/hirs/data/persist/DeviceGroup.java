package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.persist.ScheduledJobInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a device group. A device group is used to manage a collection of devices
 * and associated measurement policies. Devices associated with a machine group undergo an identical
 * appraisal process and are expected to comply with the device group measurement policies and
 * associated measurement baselines.
 */
@Entity
@Access(AccessType.FIELD)
public class DeviceGroup extends UserDefinedEntity {

    /**
     * Name set for default instance of <code>DeviceGroup</code>.
     */
    public static final String DEFAULT_GROUP = "Default Group";
    private static final Logger LOGGER = LogManager.getLogger(DeviceGroup.class);

    /**
     * A second period in milliseconds.
     */
    public static final long SECOND_MS_INTERVAL = 1000;

    /**
     * A minute period in milliseconds.
     */
    public static final long MINUTE_MS_INTERVAL = 60 * SECOND_MS_INTERVAL;

    /**
     * five minutes period in milliseconds.
     */
    public static final long FIVE_MINUTES_MS_INTERVAL = 5  * MINUTE_MS_INTERVAL;

    /**
     * An hour period in milliseconds.
     */
    public static final long HOUR_MS_INTERVAL = 60 * MINUTE_MS_INTERVAL;

    /**
     * A day period in milliseconds.
     */
    public static final long DAY_MS_INTERVAL = 24 * HOUR_MS_INTERVAL;

    /**
     * The default for on demand and periodic report thresholds.
     */
    public static final long DEFAULT_REPORT_DELAY_THRESHOLD = 12 * HOUR_MS_INTERVAL;

    /**
     * Minimum Periodic report period is once every 500 millisecond.
     */
    public static final long MINIMUM_PERIODIC_REPORT_INTERVAL = FIVE_MINUTES_MS_INTERVAL;

    /**
     * Minimum allowed value for any report Threshold.
     */
    public static final long MINIMUM_THRESHOLD_INTERVAL_MS = MINUTE_MS_INTERVAL;

    /**
     * The default job frequency of 1 day in milliseconds.
     */
    public static final long DEFAULT_JOB_FREQUENCY_MS = DAY_MS_INTERVAL;

    /**
     * Creates a new <code>ScheduledJobInfo</code> with default values.
     *
     * @return the default ScheduledJobInfo
     */
    public static ScheduledJobInfo createDefaultScheduledJobInfo() {
        return new ScheduledJobInfo(DEFAULT_JOB_FREQUENCY_MS);
    }

    @JsonIgnore
    @OneToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            fetch = FetchType.EAGER, mappedBy = "deviceGroup")
    private final Set<Device> devices = new HashSet<>();

    @Column(nullable = false)
    private long periodicReportDelayThreshold = DEFAULT_REPORT_DELAY_THRESHOLD;

    @Column(nullable = false)
    private boolean enablePeriodicReportDelayAlert = false;

    @Column(nullable = false)
    private long onDemandReportDelayThreshold = DEFAULT_REPORT_DELAY_THRESHOLD;

    @Column(nullable = false)
    private boolean enableOnDemandReportDelayAlert = false;

    @Column(nullable = false)
    private boolean waitForAppraisalCompletionEnabled = false;

    @Embedded
    private ScheduledJobInfo scheduledJobInfo;

    /**
     * Creates a new <code>DeviceGroup</code> with a specified name and a null description.
     *
     * @param name name of the device group
     */
    public DeviceGroup(final String name) {
        super(name);
        scheduledJobInfo = createDefaultScheduledJobInfo();
    }

    /**
     * Creates a new <code>DeviceGroup</code> with a specified name and description. The description
     * may be null.
     *
     * @param name        name of the device group
     * @param description description for the device group
     */
    public DeviceGroup(final String name, final String description) {
        super(name, description);
        scheduledJobInfo = createDefaultScheduledJobInfo();
    }

    /**
     * Default constructor used by Hibernate.
     */
    protected DeviceGroup() {
        super();
        scheduledJobInfo = createDefaultScheduledJobInfo();
    }

    /**
     * Returns an unmodifiable set of the <code>Device</code>s in the <code>DeviceGroup</code>.
     *
     * @return an unmodifiable Set of <code>Device</code>s
     */
    public final Set<Device> getDevices() {
        return Collections.unmodifiableSet(this.devices);
    }

    /**
     * Adds a device to this device group. If the device is not already part of the group, then it
     * is added. If an equal device is already part of the group, then the request to add the device
     * will be quietly ignored. This method also sets the DeviceGroup field on the Device.
     *
     * @param device device to add to the group
     */
    public final void addDevice(final Device device) {
        device.setDeviceGroup(this);
        addDeviceProtected(device);
    }

    /**
     * Adds a device to this device group. If the device is not already part of the group, then it
     * is added. If an equal device is already part of the group, then the request to add the device
     * will be quietly ignored.
     *
     * @param device device to add to the group
     */
    protected final void addDeviceProtected(final Device device) {
        if (device == null) {
            LOGGER.error("null device");
            throw new NullPointerException("device");
        }
        LOGGER.debug("adding device '{}' to device group '{}'",
                device.getName(), getName());

        boolean isDeviceSuccessfullyAdded = devices.add(device);
        if (isDeviceSuccessfullyAdded) {
            LOGGER.debug(String.format(
                    "added device '%s' to device group '%s'", device.getName(),
                    getName()));
        } else {
            LOGGER.info("device '{}' already exists in device group '{}'",
                    device.getName(), getName());
        }
    }

    /**
     * Remove device from the device group. This method also sets the Device's Device Group to
     * null.
     *
     * @param device device to remove
     * @return a boolean indicating if the removal was successful
     */
    public final boolean removeDevice(final Device device) {
        if (device == null) {
            LOGGER.error("null device");
            return false;
        }
        LOGGER.debug("removing device '{}' from device group '{}'",
                device.getName(), getName());
        boolean deviceRemovedSuccessfully = devices.remove(device);
        if (deviceRemovedSuccessfully) {
            device.setOnlyDeviceGroup(null);
        }
        return deviceRemovedSuccessfully;
    }

    /**
     * Remove device from the device group.
     *
     * @param device device to remove
     * @return a boolean indicating if the removal was successful
     */
    protected final boolean removeDeviceProtected(final Device device) {
        return devices.remove(device);
    }

    /**
     * Remove a device from the device group using the device's name. The device name is unique.
     *
     * @param deviceName unique name of device to be removed
     * @return a boolean indicating if the removal was successful
     */
    public final boolean removeDevice(final String deviceName) {
        if (deviceName == null) {
            LOGGER.error("null device");
            return false;
        }
        LOGGER.debug("removing device '{}' from device group '{}'", deviceName,
                getName());
        for (Device device : devices) {
            if (device.getName().equals(deviceName)) {
                return devices.remove(device);
            }
        }
        LOGGER.error(
                "device with name '{}' was not found in device group '{}'",
                deviceName, getName());
        return false;
    }

    /**
     * Sets the maximum number of milliseconds allowed to elapse without receiving a report from a
     * client. <p> This period should be greater than two or three times the periodicReportInterval
     * value. Note: this value should not be allowed to be set to a value less than the regular
     * periodic report interval for the client (periodicReportInterval); This method prevents
     * setting this value to very low setting (i.e. under MINIMUM_THRESHOLD_INTERVAL_MS) </p> This
     * logic does not verify/enforce that limit is lower than periodicReportInterval to avoid
     * restrictions on the order of setting various parameters of this policy.
     *
     * @param milliseconds the desired new threshold value
     */
    public final void setPeriodicReportDelayThreshold(final long milliseconds) {
        if (milliseconds < MINIMUM_THRESHOLD_INTERVAL_MS) {
            throw new IllegalArgumentException(
                    "Periodic Report Delay Threshold must be greater than or "
                            + "equal to "
                            + String.valueOf(MINIMUM_THRESHOLD_INTERVAL_MS)
                            + " milliseconds. Received "
                            + String.valueOf(milliseconds));
        }
        periodicReportDelayThreshold = milliseconds;
    }

    /**
     * Gets the maximum milliseconds allowed elapse without receiving a client report before
     * considering the client periodic report is late.
     * <p>
     * HIRS appraiser would issue an alert whenever late periodic report condition is detected and
     * the policy is configured to allow this type of alerts to be issued.
     *
     * @return periodicReportDelayThreshold maximum milliseconds for late periodic report condition
     */
    public final long getPeriodicReportDelayThreshold() {
        return periodicReportDelayThreshold;
    }

    /**
     * Sets the policy flag that controls if HIRS appraiser will issue a late periodic alert.
     * <p>
     * This flag should be set to false when portal user is not sure that it has set
     * periodicReportDelayThreshold with sufficient tolerance to avoid unnecessary excessive alerts
     * for clients. For example, if portal sets periodicReportDelayThreshold to value less than or
     * equal to the periodicReportInterval, this will cause unnecessary excessive alerts.
     *
     * @param flag true enables the alert, and false otherwise
     */
    public final void setEnablePeriodicReportDelayAlert(final boolean flag) {
        enablePeriodicReportDelayAlert = flag;
    }

    /**
     * Determines if periodic alert delay alerts should be issued whenever time elapsed since last
     * received report from a client exceeds the maximum allowed delay interval defined by
     * periodicReportDelayThreshold period.
     *
     * @return enablePeriodicReportDelayAlert true enables the alert, and false otherwise
     */
    public final boolean isEnablePeriodicReportDelayAlert() {
        return enablePeriodicReportDelayAlert;
    }

    /**
     * Sets the time threshold that determines the maximum milliseconds allowed to elapse after the
     * portal initiates a client on-demand report request without receiving a report from the
     * client. <p> if a report was not received in this milliseconds interval, and the
     * enableOndemandReportDelayAlert flag was set to true; HIRS appraiser will issue an alert. </p>
     * This period must be set to a value greater than three times the duration of the client's cron
     * invocation job that runs periodically to cause the client send ReportRequest query to HIRS
     * appraiser plus sufficient time for the client to collect and send a report.
     *
     * @param milliseconds desired new threshold value
     * @throws IllegalArgumentException thrown if less than MINIMUM_THRESHOLD_INTERVAL_MS
     */

    public final void setOnDemandReportDelayThreshold(final long milliseconds)
            throws IllegalArgumentException {
        if (milliseconds < MINIMUM_THRESHOLD_INTERVAL_MS) {
            throw new IllegalArgumentException(
                    "On Demand Report Delay Threshold must be greater than or "
                            + "equal to "
                            + String.valueOf(MINIMUM_THRESHOLD_INTERVAL_MS)
                            + " milliseconds. Received "
                            + String.valueOf(milliseconds));
        }
        onDemandReportDelayThreshold = milliseconds;
    }

    /**
     * Gets the maximum time HIRS appraiser will wait for a client to send a report after the portal
     * initiates On-Demand report request.
     * <p>
     * If the appraiser does not receive the report on time and the policy is configured to enable
     * On-demand report delay alert, this alert will be issued.
     *
     * @return onDemandReportDelayThreshold milliseconds time limit to trigger on-demand report late
     * alert
     */
    public final long getOnDemandReportDelayThreshold() {
        return onDemandReportDelayThreshold;
    }

    /**
     * Sets the policy flag that controls on-Demand client report delay alert.
     * <p>
     * This alert will be issued by HIRS appraiser whenever the portal initiates on-demand report
     * request and no client report is received within the maximum allowed milliseconds interval
     * defined by the onDemandReportDelayThreshold.
     *
     * @param flag true enables the alert, and false otherwise
     */
    public final void setEnableOnDemandReportDelayAlert(final boolean flag) {
        enableOnDemandReportDelayAlert = flag;
    }

    /**
     * Determines if the OnDemand report delay alert is allowed to be issued by HIRS appraiser.
     *
     * @return enableOnDemandReportDelayAlert true to enables the alert, and false otherwise
     */
    public final boolean isEnableOnDemandReportDelayAlert() {
        return enableOnDemandReportDelayAlert;
    }

    /**
     * Gets flag indicating if devices in this group should wait for appraisal completion.
     * @return true if devices are waiting for appraisal completion, false otherwise
     */
    public boolean isWaitForAppraisalCompletionEnabled() {
        return waitForAppraisalCompletionEnabled;
    }

    /**
     * Sets flag indicating if devices in this group should wait for appraisal completion.
     * @param waitForAppraisalCompletionEnabled true if devices are waiting for
     *                                          appraisal completion, false otherwise
     */
    public void setWaitForAppraisalCompletionEnabled(final boolean
                                                             waitForAppraisalCompletionEnabled) {
        this.waitForAppraisalCompletionEnabled = waitForAppraisalCompletionEnabled;
    }

    /**
     * Gets the ScheduleJobInfo for this Repository.
     * @return the SecheduleJobInfo
     */
    public ScheduledJobInfo getScheduledJobInfo() {
        return scheduledJobInfo;
    }

    /**
     * Sets the ScheduleJobInfo for this Repository.
     * @param scheduledJobInfo the ScheduleJobInfo
     */
    public void setScheduledJobInfo(final ScheduledJobInfo scheduledJobInfo) {
        Assert.notNull(scheduledJobInfo, "scheduledJobInfo");
        this.scheduledJobInfo = scheduledJobInfo;
    }

    /**
     * Gets the health status of this group, which is a summary of the set of devices for this
     * group. If at least one device is untrusted, then the group is untrusted. If at least one
     * device has unknown trust, and there are no untrusted devices, then the trust will be
     * unknown. If there are zero devices in this group, the trust will be unknown.
     * Otherwise, the group will be trusted.
     *
     * @return the group health
     */
    public HealthStatus getHealthStatus() {
        if (CollectionUtils.isEmpty(devices)) {
            return HealthStatus.UNKNOWN;
        }
        boolean hasUnknownTrusts = false;
        for (Device device : devices) {
            switch (device.getHealthStatus()) {
                case UNTRUSTED:
                    return HealthStatus.UNTRUSTED;
                case UNKNOWN:
                    hasUnknownTrusts = true;
                    break;
                default:
                    break;
            }
        }

        if (hasUnknownTrusts) {
            return HealthStatus.UNKNOWN;
        }
        return HealthStatus.TRUSTED;
    }

    /**
     * Gets the number of devices within the group.
     *
     * @return the number of devices
     */
    public int getNumberOfDevices() {
        int count = 0;

        if (devices != null) {
            count = devices.size();
        }

        return count;
    }

    /**
     * Gets the number of devices currently trusted within the group.
     *
     * @return the number of trusted devices
     */
    public int getNumberOfTrustedDevices() {
        int count = 0;

        if (devices != null) {
            for (final Device device : devices) {
                if (device.getHealthStatus() == HealthStatus.TRUSTED) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Gets only the devices for the device group.
     * (Return a set of devices without any reference
     * to the device groups to avoid an infinite loop.)
     *
     * @return a set of all the devices
     */
    public Set<Device> getAllDevices() {
        Set<Device> allDevices = new HashSet<>();

        for (Device device: devices) {
            device.setOnlyDeviceGroup(null);
            allDevices.add(device);
        }
        return Collections.unmodifiableSet(allDevices);
    }
}
