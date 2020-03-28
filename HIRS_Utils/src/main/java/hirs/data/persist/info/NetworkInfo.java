package hirs.data.persist.info;

import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.InetAddressXmlAdapter;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.hibernate.annotations.Type;

/**
 * This class is used to represent the network info of a device.
 */
@Embeddable
public class NetworkInfo implements Serializable {

    private static final Logger LOGGER = LogManager
            .getLogger(NetworkInfo.class);

    private static final int NUM_MAC_ADDRESS_BYTES = 6;

    @XmlElement
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = true)
    private String hostname;

    @XmlElement
    @XmlJavaTypeAdapter(value = InetAddressXmlAdapter.class)
    @Column(length = DeviceInfoReport.SHORT_STRING_LENGTH, nullable = true)
    @Type(type = "hirs.data.persist.type.InetAddressType")
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
     * Used to retrieve the hostname of the device.
     *
     * @return a String representing the hostname, may return null if no value
     *         is set
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Used to retrieve the IP address of the device.
     *
     * @return an InetAddress object representing the IP address, may return
     *         null if no value is set
     */
    public final InetAddress getIpAddress() {
        return ipAddress;
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

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        int hostnameHashCode;
        int ipAddressHashCode;

        if (hostname == null) {
            hostnameHashCode = 0;
        } else {
            hostnameHashCode = hostname.hashCode();
        }
        if (ipAddress == null) {
            ipAddressHashCode = 0;
        } else {
            ipAddressHashCode = ipAddress.hashCode();
        }

        result = prime * result + hostnameHashCode;
        result = prime * result + ipAddressHashCode;
        result = prime * result + Arrays.hashCode(macAddress);
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NetworkInfo)) {
            return false;
        }
        NetworkInfo other = (NetworkInfo) obj;
        if (hostname == null) {
            if (other.hostname != null) {
                return false;
            }
        } else if (!hostname.equals(other.hostname)) {
            return false;
        }
        if (ipAddress == null) {
            if (other.ipAddress != null) {
                return false;
            }
        } else if (!ipAddress.equals(other.ipAddress)) {
            return false;
        }
        if (!Arrays.equals(macAddress, other.macAddress)) {
            return false;
        }
        return true;
    }

    private void setHostname(final String hostname) {
        LOGGER.debug("setting hostname to: {}", hostname);
        this.hostname = hostname;
    }

    private void setIpAddress(final InetAddress ipAddress) {
        LOGGER.debug("setting IP address to: {}", ipAddress);
        this.ipAddress = ipAddress;
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
