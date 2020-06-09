package hirs.tpm.eventlog.uefi;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.xml.bind.DatatypeConverter;

/**
 * Class for processing a Secure Boot certificate stored in the UEFI DB or DBX.
 * X509 certs are used by Secure Boot for validating EFI files.
 */
public class UefiX509Cert {
  /** Certificate object .*/
  private java.security.cert.Certificate cert = null;

/**
 * Constructor for the certificate.
 * @param certData byte array holding the certificate.
 * @throws CertificateException If the certificate cannot parse.
 * @throws NoSuchAlgorithmException if a hash cannot be generated from the cert.
 */
public UefiX509Cert(final byte[] certData) throws CertificateException, NoSuchAlgorithmException {
  CertificateFactory cf;
  cf = CertificateFactory.getInstance("X.509");
  InputStream targetStream = new ByteArrayInputStream(certData);
  cert = cf.generateCertificate(targetStream);
  MessageDigest md = MessageDigest.getInstance("SHA1");
  md.update(certData);
}

/**
 * Finds the byte length of the certificate.
 * @return the certificate length.
 * @throws CertificateEncodingException if the certificate failed to parse.
 */
public int getLength() throws CertificateEncodingException {
  int length = 0;
  X509Certificate x509Cert = (X509Certificate) cert;
  length = x509Cert.getEncoded().length;
  return length;
}

/**
 * Calculates the fingerprint per Microsoft's specs using SHA1 and colon based notation.
 * e.g. "44:d6:41:ca:ca:08:09:00:23:98:b4:87:7b:8e:98:2e:d2:6f:7b:76"
 * @return a string representation of the certificate fingerprint
 */
public String getSHA1FingerPrint() {
  byte[] der = null;
  MessageDigest md = null;
  try {
        md = MessageDigest.getInstance("SHA-1");
        der = cert.getEncoded();
      } catch (Exception e) {
        return ("Error creating Certificate Fingerprint: " + e.getMessage());
       }
  md.update(der);
  byte[] digest = md.digest();
  String digestHex = DatatypeConverter.printHexBinary(digest);
  digestHex = digestHex.replaceAll("..(?!$)", "$0:");   // places : every 2 digits
  return digestHex.toLowerCase();
}

/**
 * Provides a Sting of select fields of the Certificate data.
 * @return A string detailing select fields of the certificate.
 */
public String toString() {
  X509Certificate x509Cert = (X509Certificate) cert;
  String certData = "";
  certData += "Certificate Serial Number = "
               + x509Cert.getSerialNumber().toString(UefiConstants.SIZE_16) + "\n";
  certData += "Subject DN = " + x509Cert.getSubjectDN() + "\n";
  certData += "Issuer DN = " + x509Cert.getIssuerDN() + "\n";
  certData += "Not Before Date = " + x509Cert.getNotBefore() + "\n";
  certData += "Not After Date = " + x509Cert.getNotAfter() + "\n";
  certData += "Signature Algorithm = " + x509Cert.getSigAlgName() + "\n";
  certData += "SHA1 Fingerprint =  " + getSHA1FingerPrint() + "\n";
  return certData;
 }
}
