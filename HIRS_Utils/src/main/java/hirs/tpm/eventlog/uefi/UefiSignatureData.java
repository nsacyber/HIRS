package hirs.tpm.eventlog.uefi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import hirs.utils.HexUtils;

/**
 * Class for processing the contents of a Secure Boot DB or DBX contents.
 *  used for EFIVariables associated with Secure Boot
 *  as defined by Section 32.4.1 Signature Database from the UEFI 2.8 specification
 *
 * typedef struct _EFI_SIGNATURE_DATA {
 *            EFI_GUID    SignatureOwner;
 *            UINT8       SignatureData[...];
 *  } EFI_SIGNATURE_DATA;
 *
 *  However page 1729 0f UEFI 2.8 implies that SignatureListType of EFI_CERT_SHA256_GUID
 *  will contain the "the SHA-256 hash of the binary".
 *  So the Signature Data depends upon the Signature Type from the EFI Signature List.
 */
public class UefiSignatureData {
    /** UEFI Certificate GUID.*/
    private byte[] guid = new byte[UefiConstants.SIZE_16];
    /** UEFI Signature data.*/
    private byte[] sigData = null;
    /** UEFI Certificate object .*/
    private UefiX509Cert cert = null;
    /** UEFI Certificate GUID.*/
    private UefiGuid efiGuid = null;
    /** UEFI Signature type.*/
    private UefiGuid signatureType = null;
    /** UEFI Signature validity.*/
    private boolean valid = false;
    /** UEFI Certificate SHA1 hash.*/
    private byte[] binaryHash = new byte[UefiConstants.SIZE_40];
    /** UEFI Signature data status.*/
    private String status = "Signature Data contians a valid Certificate";

/**
 * UefiSignatureData constructor.
 * @param inputStream The Signature data.
 * @param sigType  UEFI defined signature type.
 * @throws IOException if there's an problem reading the input stream.
 * @throws CertificateException If there a problem parsing the X509 certificate.
 * @throws NoSuchAlgorithmException if there's a problem hashing the certificate.
 */
UefiSignatureData(final ByteArrayInputStream inputStream, final UefiGuid sigType)
                        throws IOException, CertificateException, NoSuchAlgorithmException {
  signatureType = sigType;
  // UEFI spec section 32.5.3.3 states that SignatureListType of EFI_CERT_SHA256_GUID
  // only contains a hash, not a cert
  if (sigType.getVendorTableReference().equals("EFI_CERT_SHA256_GUID")) {
      inputStream.read(guid);
      efiGuid = new UefiGuid(guid);
      // Should be a SHA256 hash of the "binary"
      inputStream.read(binaryHash);
  } else if (sigType.getVendorTableReference().equals("EFI_CERT_X509_GUID")) {
      inputStream.read(guid);
      efiGuid = new UefiGuid(guid);
      // Read in Type and Length separately so we calculate the rest of the cert size
      byte[] certType = new byte[UefiConstants.SIZE_2];
      inputStream.read(certType);
      byte[] certLength = new byte[UefiConstants.SIZE_2];
      inputStream.read(certLength);
      int cLength = new BigInteger(certLength).intValue() + UefiConstants.SIZE_4;
      byte[] certData = new byte[cLength];
      inputStream.read(certData);
      // put the cert back together
      byte[] certBlob = new byte[cLength + UefiConstants.SIZE_4];
      System.arraycopy(certType, 0, certBlob, 0, UefiConstants.SIZE_2);
      System.arraycopy(certLength, 0, certBlob, UefiConstants.OFFSET_2, UefiConstants.SIZE_2);
      System.arraycopy(certData, 0, certBlob, UefiConstants.OFFSET_4, cLength);
      cert = new UefiX509Cert(certBlob);
      } else if (sigType.isUnknownUUID()) {
          //status = "Signature List Type has an unknown GUID: " + efiGuid.toString();
          status = "Signature List Type has an unknown GUID";
          return;
      } else {    // else process as a cert (RH SHIM does this)
          processC509Cert(inputStream);
          efiGuid = sigType;
      }
     valid = true;
}

/**
  * Default EFISignatureData Constructor.
  * @param data byte array of the EFISignatureData to process
 * @throws CertificateException If there a problem parsing the X509 certificate.
 * @throws NoSuchAlgorithmException if there's a problem hashing the certificate.
  */
UefiSignatureData(final byte[] data) throws CertificateException, NoSuchAlgorithmException {
  System.arraycopy(data, 0, guid, 0, UefiConstants.SIZE_16);
  sigData = new byte[data.length - UefiConstants.SIZE_16];
  System.arraycopy(data, UefiConstants.OFFSET_16, sigData, 0, data.length - UefiConstants.SIZE_16);
  cert = new UefiX509Cert(sigData);
  efiGuid = new UefiGuid(guid);
}

/**
 * Processes an x509 Cert used by secure DB or DBx.
 * @param inputStream x509 certificate data.
 * @throws IOException is there's a problem reading the data.
 * @throws CertificateException if there's a problem parsing the certificate.
 * @throws NoSuchAlgorithmException if there's a problem creating a hash.
 */
private void processC509Cert(final ByteArrayInputStream inputStream)
        throws IOException, CertificateException, NoSuchAlgorithmException {
  byte[] certType = new byte[UefiConstants.SIZE_2];
  inputStream.read(certType);
  byte[] certLength = new byte[UefiConstants.SIZE_2];
  inputStream.read(certLength);
  int cLength = new BigInteger(certLength).intValue() + UefiConstants.SIZE_4;
  byte[] certData = new byte[cLength];
  inputStream.read(certData);
  // put the cert back together
  byte[] certBlob = new byte[cLength + UefiConstants.SIZE_4];
  System.arraycopy(certType, 0, certBlob, 0, 2);
  System.arraycopy(certLength, 0, certBlob, 2, 2);
  System.arraycopy(certData, 0, certBlob, UefiConstants.OFFSET_4, cLength);
  cert = new UefiX509Cert(certBlob);
}

/**
 *  Efi GUID of the signature owner.
 * @return EFIGuid object
 */
public UefiGuid getEfiVarGuid() {
   return efiGuid;
}

/**
 * Returns the signature type.
 * @return Guid: either EFI_CERT_SHA256_GUID or EFI_SHA256_GUID
 */
public UefiGuid getSignatureType() {
    return signatureType;
}

/**
 *  Returns a X509 Certificate object which is created from the sign data.
 * @return X509Cert Object
 */
public UefiX509Cert getCert() {
  return cert;
}

/**
 * Checks if EFI Signature/Certificate is valid.
 * @return true if EFI Signature is valid
 */
public boolean isValid() {
  return valid;
}

/**
 * Retrieves the last status of the object.
 * @return String with the latest status on this object.
 */
public String getStatus() {
  return status;
}

/**
 *  Provides a description of the fields within the EFI Signature Data.
 * @return X509Cert human readable description.
 */
public String toString() {
  String sigInfo = "";
  if (!valid) {
      sigInfo = status;
  } else {
        if (signatureType.getVendorTableReference().equals("EFI_CERT_SHA256_GUID")) {
           sigInfo += "UEFI Signature Owner = " + efiGuid.toString() + "\n";
           sigInfo += "Binary Hash = " + HexUtils.byteArrayToHexString(binaryHash) + "\n";
        } else {
              sigInfo += "UEFI Signature Owner = " +  efiGuid.toString() + "\n";
              sigInfo += cert.toString();
            }
        }
  return sigInfo;
 }
}
