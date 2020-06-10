package hirs.data.persist.enums;

import javax.xml.bind.annotation.XmlType;

/**
 * The 'source' of the <code>Alert</code>, which is a string enumeration
 * representing the component within the HIRS system that caused the
 * <code>Alert</code> to be generated. For example, if a record mismatch is
 * detected by the <code>IMAAppraiser</code>, the source of the
 * <code>Alert</code> will be "IMAAppraiser". In some cases the class name may
 * be used, and in other cases a more abstract name may be used to provide
 * clarity to the user, such as the <code>REPORT_PROCESSOR</code> type, which
 * can come from the <code>SOAPMessageProcessor</code>, the
 * <code>SOAPReportProcessor</code>, or the <code>HIRSAppraiser</code>.
 */
@XmlType(name = "AlertSource")
public enum AlertSource {

    /**
     * The alerts generated from an unspecified source.
     */
    UNSPECIFIED,
    /**
     * Alerts generated within <code>SOAPMessageProcessor</code>,
     * <code>SOAPReportProcessor</code>, or <code>HIRSAppraiser</code> will all
     * use the same source. This makes sense right now because those Alerts will
     * all be related to <code>Report</code>s that do not match the expected
     * format.
     */
    REPORT_PROCESSOR,
    /**
     * Alerts generated within the <code>IMAAppraiser</code>.
     */
    IMA_APPRAISER,
    /**
     * Alerts generated within the <code>TPMAppraiser</code>.
     */
    TPM_APPRAISER,
    /**
     * Alerts generated within <code>OnDemandReportRequestManager</code>.
     */
    REPORT_REQUESTOR
}
