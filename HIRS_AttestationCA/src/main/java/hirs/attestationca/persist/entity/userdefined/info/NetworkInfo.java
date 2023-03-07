package hirs.attestationca.persist.entity.userdefined.info;

import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * This class is used to represent the network info of a device.
 */
@EqualsAndHashCode
@Embeddable
public class NetworkInfo implements Serializable {

    private static final Logger LOGGER = LogManager
            .getLogger(NetworkInfo.class);

    private static final int NUM_MAC_ADDRESS_BYTES = 6;

    @XmlElement
    @Setter
    @Getter
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = true)
    private String hostname;

    @XmlElement
//    @XmlJavaTypeAdapter(value = InetAddressXmlAdapter.class)
    @Setter
    @Getter
    @Column(length = DeviceInfoReport.SHORT_STRING_LENGTH, nullable = true)
//    @Convert(converter = hirs.attestationca.persist.type.InetAddressType.class)
    private InetAddress ipAddress;

    @XmlElement
    @Column(length = NUM_MAC_ADDRESS_BYTES, nullable = true)
    @SuppressWarnings("checkstyle:magicnumber")
    private byte[] macAddress;

    /**
     * Constructor used to create a NetworkInfo object.
     *
     * @param hostname
     *            String representing the hostname information for the device,
     *            can be null if hostname unknown
     * @param ipAddress
     *            InetAddress object representing the IP address for the device,
     *            can be null if IP address unknown
     * @param macAddress
     *            byte array representing the MAC address for the device, can be
     *            null if MAC address is unknown
     */
    public NetworkInfo(final String hostname, final InetAddress ipAddress,
                       final byte[] macAddress) {
        setHostname(hostname);
        setIpAddress(ipAddress);
        setMacAddress(macAddress);
    }

    /**
     * Default constructor necessary for marshalling/unmarshalling XML objects.
     */
    protected NetworkInfo() {
        this.hostname = null;
        this.ipAddress = null;
        this.macAddress = null;
    }

    /**
     * Used to retrieve the MAC address of the device.
     *
     * @return a String representing the MAC address, may return null if no
     *         value is set
     */
    public final byte[] getMacAddress() {
        if (macAddress == null) {
            return null;
        } else {
            return macAddress.clone();
        }
    }

    private void setMacAddress(final byte[] macAddress) {
        StringBuilder sb;
        if (macAddress == null) {
            sb = null;
        } else {
            if (macAddress.length != NUM_MAC_ADDRESS_BYTES) {
                LOGGER.error(
                        "MAC address is only {} bytes, must be {} bytes or "
                                + "null", macAddress.length,
                        NUM_MAC_ADDRESS_BYTES);
                throw new IllegalArgumentException(
                        "MAC address is invalid size");
            }
            sb = new StringBuilder();
            for (byte b : macAddress) {
                sb.append(String.format("%02X ", b));
            }
        }
        LOGGER.debug("setting MAC address to: {}", sb);
        this.macAddress = macAddress;
    }
}
