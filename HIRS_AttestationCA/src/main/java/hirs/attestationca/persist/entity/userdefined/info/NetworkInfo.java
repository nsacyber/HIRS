package hirs.attestationca.persist.entity.userdefined.info;

import hirs.utils.enums.DeviceInfoEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * This class is used to represent the network info of a device.
 */
@Log4j2
@Embeddable
@EqualsAndHashCode
public class NetworkInfo implements Serializable {

    private static final int NUM_MAC_ADDRESS_BYTES = 6;

    @XmlElement
    @Getter
    @Column(length = DeviceInfoEnums.LONG_STRING_LENGTH)
    private String hostname;

    @XmlElement
    @Getter
//    @XmlJavaTypeAdapter(value = InetAddressXmlAdapter.class)
    @Column(length = DeviceInfoEnums.SHORT_STRING_LENGTH)
//    @JsonSubTypes.Type(type = "hirs.data.persist.type.InetAddressType")
    private InetAddress ipAddress;

    @XmlElement
    @Column(length = NUM_MAC_ADDRESS_BYTES)
    private byte[] macAddress;

    /**
     * Constructor used to create a NetworkInfo object.
     *
     * @param hostname   String representing the hostname information for the device,
     *                   can be null if hostname unknown
     * @param ipAddress  InetAddress object representing the IP address for the device,
     *                   can be null if IP address unknown
     * @param macAddress byte array representing the MAC address for the device, can be
     *                   null if MAC address is unknown
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
     * value is set
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
                log.error(
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
        log.debug("setting MAC address to: {}", sb);
        this.macAddress = macAddress;
    }

    private void setHostname(final String hostname) {
        log.debug("setting hostname to: {}", hostname);
        this.hostname = hostname;
    }

    private void setIpAddress(final InetAddress ipAddress) {
        log.debug("setting IP address to: {}", ipAddress);
        this.ipAddress = ipAddress;
    }
}
