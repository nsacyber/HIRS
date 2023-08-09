package hirs.utils;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Class used to help with marshalling and unmarshalling TPMInfo objects.
 */
public class X509CertificateAdapter extends XmlAdapter<byte[], X509Certificate> {

    @Override
    public final byte[] marshal(final X509Certificate arg0) throws Exception {
        return arg0.getEncoded();
    }

    @Override
    public final X509Certificate unmarshal(final byte[] arg0) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream inStream = new ByteArrayInputStream(arg0);
        return (X509Certificate) cf.generateCertificate(inStream);
    }
}
