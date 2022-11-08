package hirs.data.persist.policy;

import hirs.data.persist.TPMMeasurementRecord;
import hirs.data.persist.enums.AlertSeverity;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class represents TPM policy. TPM Policy identifies the TPMBaseline instance that the TPM
 * appraiser should use to assess the integrity of a device by comparing the TPM PCR values
 * against the expected values stored in the baseline. In addition, TPMPolicy includes masks to
 * identify the set of the PCR that a device must include in TPM report and which of these PCR(s)
 * should be compared against the expected values stored in the TPM Baseline.  A TPM policy also
 * maintains a set of the PCRs that should be appraised on a device-specific basis.
 */
@Entity
public final class TPMPolicy extends Policy {

    /**
     * Identifies all valid TPM PCRs bits (i.e. PCR 0-23) in any TPM PCR mask.
     */
    public static final int ALL_PCR_MASK = 0xFFFFFF;

    /**
     * Identifies PCRs whose default appraisal type is device specific.
     */
    public static final List<Integer> DEFAULT_DEVICE_SPECIFIC_PCRS
            = Collections.unmodifiableList(Arrays.asList(new Integer[]{0, 1, 2, 3, 4, 5, 6, 7 }));

    private static final int DEFAULT_KERNEL_PCRS = 0x0C0000; // defaulted to PCRs 18 and 19

    private static final Logger LOGGER = LogManager.getLogger(TPMPolicy.class);

    @Column(nullable = false)
    private int reportPCRs = ALL_PCR_MASK;

    @Column(nullable = false)
    private int appraisePCRs = 0;

    @Column(nullable = false)
    private boolean appraiseFullReport = true;

    @Column(nullable = false)
    private boolean validateSignature = true;

    @Column(nullable = false)
    private int kernelPCRs = DEFAULT_KERNEL_PCRS;

    @Column(nullable = false)
    private boolean detectKernelUpdateEnabled = true;

    @Column(nullable = false)
    private boolean alertOnKernelUpdateEnabled = true;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertSeverity kernelUpdateAlertSeverity = AlertSeverity.UNSPECIFIED;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "TPMPolicyDeviceSpecificPCRs",
            joinColumns = { @JoinColumn(name = "PolicyID", nullable = false) })
    private final Set<Integer> deviceSpecificPCRs = new HashSet<>();

    /**
     * Constructor used to initialize TPMPolicy object.
     *
     * @param name
     *        A name used to uniquely identify and reference the TPM policy.
     */
    public TPMPolicy(final String name) {
        super(name);
    }

    /**
     * Constructor used to initialize TPMPolicy object.
     *
     * @param name
     *        A name used to uniquely identify and reference the TPM policy.
     * @param description
     *        Optional description of the policy that can be added by the user
     */
    public TPMPolicy(final String name, final String description) {
        super(name, description);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected TPMPolicy() {
        super();
    }

    /**
     * Returns a set of the device-specific PCRs. These are PCR IDs used by the appraiser to know
     * that the hash for that PCR should be compared against values for those PCRs from a previous
     * report.
     *
     * @return set of device-specific PCR IDs found in the TPM policy
     */
    public Set<Integer> getDeviceSpecificPCRs() {
        return Collections.unmodifiableSet(deviceSpecificPCRs);
    }

    /**
     * Searches this policy for a device-specific PCR with the given PCR ID. This will return true
     * if an equivalent ID was found, false otherwise.
     *
     * @param pcrId
     *            ID for PCR to add
     * @return boolean indicating if the ID was found
     */
    public boolean isInDeviceSpecificPCRs(final int pcrId) {
        return deviceSpecificPCRs.contains(pcrId);
    }

    /**
     * Adds a device-specific PCR ID to the policy. This will indicate to the appraiser that the
     * hash for this PCR should come from the previous report. If there is already an equivalent PCR
     * ID in the policy, the request to add it will be silently ignored.
     *
     * @param pcrId
     *            ID of the PCR to measure from previous report
     */
    public void addToDeviceSpecificPCRs(final int pcrId) {
        LOGGER.debug("adding device-specific PCR ID# {} to policy {}", pcrId, getName());
        TPMMeasurementRecord.checkForValidPcrId(pcrId);

        if (deviceSpecificPCRs.contains(pcrId)) {
            LOGGER.info("PCR ID already exists in list: {}", pcrId);
        } else {
            deviceSpecificPCRs.add(pcrId);
            LOGGER.debug("pcr ID added");
        }
    }

    /**
     * Removes the PCR ID from the list of device-specific PCR IDs in the policy. If the ID is
     * found and successfully removed then true is returned. Otherwise false is returned.
     *
     * @param pcrId
     *            ID of PCR to remove from policy
     * @return true if found and removed, otherwise false
     */
    public boolean removeFromDeviceSpecificPCRs(final int pcrId) {
        LOGGER.debug("removing PCR ID {} from policy {}", pcrId, getName());
        return deviceSpecificPCRs.remove(pcrId);
    }

    /**
     * Clears the list of device-specific PCR IDs in the policy.
     */
    public void clearDeviceSpecificPCRs() {
        deviceSpecificPCRs.clear();
    }

    /**
     * Sets the TPM policy PCRs mask to force device to include specific PCRs in
     * <code>TPMreport</code>.
     *
     * @param mask
     *        Bit mask to identify all TPM PCRs that should be part of
     *        a device TPM report.
     */
    public void setReportPcrMask(final int mask) {
        //verify that PCRs mask cover range between 0-24; maximum per TPM1.2
        //specifications.
        if ((mask & ~ALL_PCR_MASK) != 0) {
            String msg = "invalid PCR mask -- bit(s) outside range 0-23 set: "
                    + Integer.toHexString(mask & ~ALL_PCR_MASK);
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (mask == 0) {
            String msg = "Invalid PCR Mask of 0. At least one PCR must be"
                    + "reported on in a given policy.";
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.reportPCRs = mask;
    }

    /**
     * Gets the PCR mask to identify all PCRs that should be part of a device TPM report.
     *
     * @return int the PCRs mask for all PCRs that must be included in TPM
     *         report.
     */
    public int getReportPcrMask() {
        return this.reportPCRs;
    }

    /**
     * Returns true if the specified PCR id is being reported in this policy, at all.
     *
     * @param pcrId the PCR id to check
     * @return true if the pcrId is being reported
     */
    public boolean isPcrReported(final int pcrId) {
        return (reportPCRs & (1 << pcrId)) != 0;
    }

    /**
     * Sets the policy PCRs bit mask to identify which PCRs in a device TPM
     * report should be appraised against the TPM baselines associated with this
     * policy.
     * @param mask
     *        Bit mask to identify every PCR to be appraised against the TPM
     *        baselines associated with this policy.
     */
    public void setAppraisePcrMask(final int mask) {
        //verify that PCRs mask cover range between 0-24; maximum per TPM1.2
        //specifications.
        if ((mask & ~ALL_PCR_MASK) != 0) {
            String msg = "Cannot set appraise PCR mask, valid PCR range"
                    + " is 0-23. found the following invalid PCR bits in mask: "
                    + Integer.toHexString(mask & ~ALL_PCR_MASK);
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.appraisePCRs = mask;
    }

    /**
     * Gets the policy PCRs bit mask to identify which PCRs
     * in a device TPM report should be appraised against the
     * TPM baselines associated with this policy.
     * @return int bit mask to identify every PCR to be appraised against the
     *         TPM baselines associated with this policy.
     */
    public int getAppraisePcrMask() {
        return this.appraisePCRs;
    }

    /**
     * Returns true if the specified kernel PCR id is selected according to this policy.
     *
     * @param pcrId the kernel PCR id to check
     * @return true if the kernel pcrId is selected
     */
    public boolean isKernelPcrSelected(final int pcrId) {
        return (kernelPCRs & (1 << pcrId)) != 0;
    }

    /**
     * Sets the kernel PCRs bit mask to identify which PCRs in a device TPM
     * report should be used to detect a kernel update when appraised against
     * the TPM baseline(s) associated with this policy.
     * @param mask
     *        Bit mask to identify the PCRs which must be exclusively
     *        mismatched in order to detect a kernel update.
     */
    public void setKernelPcrMask(final int mask) {
        // verify that PCRs mask cover range between 0-23; maximum per TPM1.2
        // specifications.
        if ((mask & ~ALL_PCR_MASK) != 0) {
            String msg = "Cannot set kernel PCR mask, valid PCR range"
                    + " is 0-23. found the following invalid PCR bits in mask: "
                    + Integer.toHexString(mask & ~ALL_PCR_MASK);
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.kernelPCRs = mask;
    }

    /**
     * Gets the policy PCRs bit mask to identify which PCRs
     * in a device TPM report should be appraised used to detect
     * a kernel update when appraised against the
     * TPM baseline(s) associated with this policy.
     * @return int bit mask to identify the PCRs which must be exclusively
     *        mismatched in order to detect a kernel update.
     */
    public int getKernelPcrMask() {
        return this.kernelPCRs;
    }

    /**
     * Returns true if the specified PCR id is appraised according to this policy.
     *
     * @param pcrId the PCR id to check
     * @return true if the pcrId is appraised
     */
    public boolean isPcrAppraised(final int pcrId) {
        return (appraisePCRs & (1 << pcrId)) != 0;
    }

    /**
     * Sets the specified PCR to appraised in the appraise PCR mask.
     *
     * @param pcrId the PCR to set to appraised.
     */
    public void setPcrAppraised(final int pcrId) {
        TPMMeasurementRecord.checkForValidPcrId(pcrId);
        if (!isPcrReported(pcrId)) {
            String msg = "Cannot set PCR to be Appraised."
                    + " It is not being reported on by this Policy.";
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        appraisePCRs |= 1 << pcrId;
    }

    /**
     * Sets the specified PCR to not appraised in the appraise PCR mask.
     *
     * @param pcrId the PCR to set to appraised.
     */
    public void setPcrNotAppraised(final int pcrId) {
        appraisePCRs &= ~(1 << pcrId);
    }

    /**
     * Sets the PCR mask to the first 8 PCRs and sets default device specific PCRs.
     */
    public void setDefaultPcrAppraisalValues() {
        for (int i : DEFAULT_DEVICE_SPECIFIC_PCRS) {
            setPcrAppraised(i);
            addToDeviceSpecificPCRs(i);
        }
    }

    /**
     * Sets the PCR mask to 0 and clears device specific PCRs.
     */
    public void clearAllPcrAppraisalValues() {
        setAppraisePcrMask(0);
        clearDeviceSpecificPCRs();
    }

    /**
     * Sets flag to control the behavior of TPM appraiser to
     * either identify all problems in a report before generating
     * TPM appraise failure alert; or identify only the first TPM
     * appraise problem (Nonce, device certificate, Quote, signature,
     * or PCR mismatch against TPM baseline) and generate alert.
     * @param flag
     *        true causes TPM appraiser to identify all appraise problems in a
     *        received TPM report from a device.
     */
    public void setAppraiseFullReport(final boolean flag) {
        this.appraiseFullReport = flag;
    }

    /**
     * Returns the flag that was set by <code>setAppraiseFullReport</code>
     * method.
     * @return boolean the AppraiseFullReport value; True causes TPM appraiser
     *         to identify all appraise problems in a device TPM report.
     */
    public boolean isAppraiseFullReport() {
        return this.appraiseFullReport;
    }

    /**
     * Sets flag to force <code>TPMAppraiser</code> to ignore TPM signature
     * verification when appraising <code>TPMReport</code>. This flag may be set
     * to true, when HIRS appraiser is deployed in sites where TPM certificate
     * is not, or can not, be provisioned. It also allows old unit tests to
     * continue to work with older XML report files that did not include TPM
     * certificates. When the <code>allowAppraiseWithoutCertficate</code> flag
     * is set to true, TPM appraiser will not verify TPM signature, but will
     * check TPM PCRs against baseline and TPM digest value.
     * @param flag
     *        true causes  TPM appraiser to not verify TPM signature and allows
     *        Appraising TPM reports that don't contain the device's TPM
     *        certificate.
     */
    public void setValidateSignature(final boolean flag) {
        this.validateSignature = flag;
    }

    /**
     * Returns the flag that determines if <code>TPMAppraiser</code> is required
     * to verify TPM Signature when appraising a <code>TPMReport</code>.
     * @return boolean the allowAppraiseWithoutCertficate value. False causes TPM
     * appraiser to ignore TPM signature and TPM certificate verification.
     */
    public boolean isValidateSignature() {
        return this.validateSignature;
    }

    /**
     * Sets a flag to enable the <code>TPMAppraiser</code> to detect a kernel
     * update when appraising a <code>TPMReport</code>.
     * @param flag
     *        true to set kernel detection to enabled on the TPM appraiser.
     */
    public void setDetectKernelUpdate(final boolean flag) {
        this.detectKernelUpdateEnabled = flag;
    }

    /**
     * Returns the flag that determines if <code>TPMAppraiser</code> is authorized
     * to perform kernel update detection when appraising a <code>TPMReport</code>.
     * @return boolean True if kernel update detection is enabled.
     */
    public boolean isDetectKernelUpdateEnabled() {
        return this.detectKernelUpdateEnabled;
    }

    /**
     * Sets a flag to enable the <code>TPMAppraiser</code> to send a kernel update
     * alert when appraising a <code>TPMReport</code>.
     * @param flag
     *        If true, kernel update alerts will be sent by the TPM appraiser. Setting this
     *        value to false will suppress this alert.
     */
    public void setAlertOnKernelUpdate(final boolean flag) {
        this.alertOnKernelUpdateEnabled = flag;
    }

    /**
     * Returns the flag that determines if <code>TPMAppraiser</code> will send a kernel
     * update alert if one is detected while appraising a <code>TPMReport</code>.
     * @return boolean True if alert on kernel update is enabled.
     */
    public boolean isAlertOnKernelUpdateEnabled() {
        return this.alertOnKernelUpdateEnabled;
    }

    /**
     * Gets the severity of kernel update alerts.
     * @return the severity
     */
    public AlertSeverity getKernelUpdateAlertSeverity() {
        return kernelUpdateAlertSeverity;
    }

    /**
     * Sets the severity of kernel update alerts.
     * @param severity The desired severity of kernel update alerts.
     */
    public void setKernelUpdateAlertSeverity(final AlertSeverity severity) {
        kernelUpdateAlertSeverity = severity;
    }

    /**
     * Format the list of kernel pcrs into a String for display purposes.
     * @return String
     */
    public String getKernelPcrListForOutput() {
        String result = "No PCRs were selected"; // the default response

        if (kernelPCRs != 0) {
            result = StringUtils.join(getKernelPcrList(), ", ");
        }

        return result;
    }

    /**
     * Convert the kernel PCR mask into a list of integers.
     * @return List&lt;Integer&gt;. An empty list indicates no kernel PCRs
     * are selected.
     */
    public List<Integer> getKernelPcrList() {
        final ArrayList<Integer> list = new ArrayList<Integer>();
        final int numPcrs = 24;

        for (int i = 0; i < numPcrs; i++) {
            if (isKernelPcrSelected(i)) {
                list.add(i);
            }
        }

        return list;
    }

    /**
     * Tomcat6 requires method calls to be static.  Wraps the TPMPolicy instance method
     * {@link #getKernelPcrListForOutput()}.
     * @param tpmPolicy TPMPolicy to query.
     * @return String
     */
    public static String serializeKernelPcrList(final TPMPolicy tpmPolicy) {
        return tpmPolicy.getKernelPcrListForOutput();
    }

    /**
     * Tomcat6 requires method calls to be static.  Wraps the TPMPolicy instance method
     * {@link #isKernelPcrSelected(int)}.
     * @param tpmPolicy TPMPolicy to query.
     * @param pcrId int.
     * @return boolean True or false.
     */
    public static boolean isKernelPcrSelected(final TPMPolicy tpmPolicy, final int pcrId) {
        return tpmPolicy.isKernelPcrSelected(pcrId);
    }

    /**
    * Calculates the PCR mask for a given set of PCR IDs.
    * @param pcrIdSet Iterable&lt;Integer&gt;, iterable set of PCR IDs
    * @return The mask that represents the set of PCR IDs to HIRS
    */
    public static int calculatePcrMask(final Iterable<Integer> pcrIdSet) {
        int mask = 0;

        for (final int pcrId : pcrIdSet) {
            mask |= 1 << pcrId;
        }

        return mask;
    }
}
