package hirs.data.persist;

import java.net.InetAddress;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class used to help with marshalling and unmarshalling NetworkInfo objects.
 */
public class InetAddressXmlAdapter extends XmlAdapter<byte[], InetAddress> {
    @Override
    public final byte[] marshal(final InetAddress v) throws Exception {
        return v.getAddress();
    }

    @Override
    public final InetAddress unmarshal(final byte[] v) throws Exception {
        return InetAddress.getByAddress(v);
    }

}
