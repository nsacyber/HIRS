package hirs.alert;

import static org.apache.logging.log4j.LogManager.getLogger;

import hirs.data.persist.Alert;
import hirs.data.persist.ReportSummary;
import hirs.data.persist.alert.AlertMonitor;
import hirs.data.persist.alert.AlertServiceConfig;
import hirs.data.persist.alert.JsonAlertMonitor;
import hirs.persist.AlertServiceConfigManager;
import hirs.persist.BaselineManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import hirs.persist.PolicyManager;
import hirs.persist.PortalInfoManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hirs.data.persist.enums.AlertSeverity;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of a JavaScript Object Notation (JSON) Alert Service.
 * Upon receiving an Alert, the JSON service will wrap the alert data into a JSON object
 * and forward the object to all JSON alert monitors configured for the service.
 */
@Service
public class JsonAlertService extends ManagedAlertService {
    /**
     * Name of the JSON alert service.
     */
    public static final String NAME = "JSON";

    private static final Logger LOGGER = getLogger(JsonAlertService.class);

    /**
     * DB Manager for Portal information.
     */
    @Autowired
    private PortalInfoManager portalInfoManager;

    /**
     * DB Manager for policy information.
     */
    @Autowired
    private PolicyManager policyManager;

    /**
     * DB Manager for baseline information.
     */
    @Autowired
    private BaselineManager baselineManager;

    /**
     * Creates a new <code>JsonAlertService</code> for testing. The optional config
     * parameter is a file path to a Hibernate configuration file.
     */
    public JsonAlertService() {
        super(NAME);
    }

    /**
     * Send a test alert to the configured JSON Alert Monitor.
     * Used to ensure the target can receive forwarded alerts.
     * @param jsonMonitor configured JSON Alert Monitor
     * @return True if the test alert was sent.  Otherwise false.
     * @throws IOException If there was a problem.
     */
    public boolean sendTest(final JsonAlertMonitor jsonMonitor) throws IOException {
        HashMap<String, String> items = new HashMap<>();

        String url = portalInfoManager.getPortalUrlBase() + "jsp/alertmonitors.jsp";
        items.put("url", url);
        Date datetime = new Date();
        items.put("timestamp", formatDate(datetime));
        items.put("hostname", InetAddress.getLocalHost().getHostName());
        items.put("source", "PORTAL");
        items.put("type", "Test JSON");
        items.put("severity", AlertSeverity.INFO.toString());
        items.put("details", "This is a test alert sent by the HIRS portal.");

        return send(jsonMonitor, buildJson(items));
    }

    @Override
    public void persist(final AlertServiceConfigManager alertServiceConfigManager) {
        alertServiceConfigManager.saveAlertServiceConfig(new AlertServiceConfig(NAME));
    }

    /**
     * Sends a JSON alert.
     *
     * @param monitor an alert monitor configuration.
     * @param alert the individual alert to send to the remote monitor
     */
    @Override
    protected final void sendAlert(final AlertMonitor monitor, final Alert alert) {
        if (isEnabled()) {
            try {
                if (!send((JsonAlertMonitor) monitor, convertAlert(alert))) {
                    LOGGER.error("Alert was not sent to JSON monitor '" + monitor.getName() + ".");
                }
            } catch (IOException ioe) {
                LOGGER.error("Could not use properties file: " + ioe.getMessage());
            }
        }
    }

    /**
     * Sends a JSON summary.
     *
     * @param monitor an alert monitor configuration.
     * @param summary a summary of the report with the alert
     */
    @Override
    protected void sendAlertSummary(final AlertMonitor monitor, final ReportSummary summary) {
        throw new UnsupportedOperationException("Alert summary functionality not yet implemented.");
    }

    /**
     * Wraps the alert data into a JSON object.
     * @param alert Alert to wrap into JSON.
     * @return String of JSON data.
     * @throws IOException If there is a problem.
     */
    private String convertAlert(final Alert alert) throws IOException {
        LOGGER.info("Sending JSON Alert Type = " + alert.getType().toString());
        HashMap<String, String> items = new HashMap<>();
        Optional<UUID> firstUUID = alert.getBaselineIds().stream().findFirst();

        // Retrieve the url to the main page of the Portal
        String url = portalInfoManager.getPortalUrlBase()
                        + "jsp/alertdetails.jsp?alertID=" + alert.getId();
        items.put("url", url);
        items.put("id", alert.getId().toString());
        items.put("timestamp", formatDate(alert.getCreateTime()));
        items.put("hostname", alert.getDeviceName());
        if (alert.getPolicyId() != null) {
            items.put("policy", policyManager.getPolicy(alert.getPolicyId()).getName());
        }
        if (firstUUID.isPresent()) {
            items.put("baseline",
                    baselineManager.getBaseline(firstUUID.get()).getName());
        }
        items.put("source", alert.getSource().toString());
        items.put("type", alert.getType().toString());
        items.put("severity", alert.getSeverity().toString());
        items.put("details", alert.getDetails());

        return buildJson(items);
    }

    /**
     * Makes a String of JSON data from the elements provided.
     * @param items Map<String,String> of elements to convert into a string of JSON data.
     * The keys of the map will be used as field names, and the values will be the field values.
     * @return String of JSON data.
     */
    private String buildJson(final Map<String, String> items) throws IOException {
        return new ObjectMapper().writeValueAsString(items);
    }

    /**
     * Performs the transmission of data.
     * @param jsonMonitor configured JSON Alert Monitor.
     * @param json String of JSON data.
     * @return True if the data was sent. Otherwise false.
     * @throws IOException If there is a problem.
     */
    private boolean send(final JsonAlertMonitor jsonMonitor, final String json) throws IOException {

        // Open the appropriate socket type
        // Each protocol has a different delivery process
        if (jsonMonitor.isTCP()) {
            try (Socket tcp = new Socket(jsonMonitor.getIpAddress(), jsonMonitor.getPort())) {
                DataOutputStream out = new DataOutputStream(tcp.getOutputStream());
                out.writeBytes(json);
                out.flush();
                return true;
            }
        } else if (jsonMonitor.isUDP()) {
            try (DatagramSocket udp = new DatagramSocket()) {
                DatagramPacket packet = new DatagramPacket(
                        json.getBytes(StandardCharsets.UTF_8), json.length(),
                        jsonMonitor.getIpAddress(), jsonMonitor.getPort());
                udp.send(packet);
                return true;
            }
        }

        return false;
    }

    /**
     * Convert a Date object into a String using the format used on the Portal.
     * @param datetime Date object
     * @return date formatted String
     */
    private String formatDate(final Date datetime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        return sdf.format(datetime);
    }
}
