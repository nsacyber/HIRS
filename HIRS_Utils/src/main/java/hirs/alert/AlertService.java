package hirs.alert;

import hirs.data.persist.Alert;

/**
 * Alert service for sending out alerts. This class is an interface for
 * components to use to send out <code>Alert</code>s. <code>Appraiser</code>
 * should use this interface when sending out <code>Alert</code>s when
 * an appraisal fail.
 */
public interface AlertService {

    /**
     * Send out an <code>Alert</code>. This will notify all interested parties
     * of a new alert.
     *
     * @param alert alert
     */
    void alert(Alert alert);

}
