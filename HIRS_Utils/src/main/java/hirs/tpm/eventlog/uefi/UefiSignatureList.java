package hirs.tpm.eventlog.uefi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import hirs.utils.HexUtils;

/**
 * Class for processing the contents of a Secure Boot DB or DBX contents.
 *  used for EFIVariables associated with Secure Boot
 *  as defined by Section 32.4.1 Signature Database from the UEFI 2.8 specification.
 *
 * An EFI Signature List is actual a list of Certificates used to verify a Signature.
 * This is mainly found in PCR[7] UEFI variables for the Secure Boot PK, KEK, Db and DBx variables.
 *
 *  typedef struct _EFI_SIGNATURE_LIST {
 *       EFI_GUID            SignatureType;
 *       UINT32              SignatureListSize;
 *       UINT32              SignatureHeaderSize;
 *       UINT32              SignatureSize;
 *    // UINT8               SignatureHeader[SignatureHeaderSize];
 *    // EFI_SIGNATURE_DATA  Signatures[...][SignatureSize];
 *  } EFI_SIGNATURE_LIST;
 *
 */
public class UefiSignatureList {
  /** Size of the signature list.*/
  private int listSize = 0;
  /** Size of a signature. */
  private int signatureSize = 0;
  /** Signature data. */
  private byte[]sigData = null;
  /** Number of Items in the list. */
  private int numberOfItems = 0;
  /** Signature validity. */
  private boolean valid = true;
  /** Current status. */
  private String status = "Signature List is Valid";
  /** Array List of Signature found in the list. */
  private ArrayList<UefiSignatureData> sigList = new ArrayList<UefiSignatureData>();
  /** Input Stream for processing. */
  private ByteArrayInputStream efiSigDataIS = null;
  /** Type of signature. */
  private UefiGuid signatureType = null;
/**
 * UefiSignatureList constructor.
 * @param list byte array holding the signature list.
 * @throws CertificateException If there a problem parsing the X509 certificate.
 * @throws NoSuchAlgorithmException if there's a problem hashing the certificate.
 * @throws IOException If there's a problem parsing the signature data.
 */
UefiSignatureList(final byte[] list)
        throws CertificateException, NoSuchAlgorithmException, IOException {

  byte[] guid = new byte[UefiConstants.SIZE_16];
  System.arraycopy(list, 0, guid, 0, UefiConstants.SIZE_16);
  signatureType = new UefiGuid(guid);

  byte[] lSize = new byte[UefiConstants.SIZE_4];
  System.arraycopy(list, UefiConstants.OFFSET_16, lSize, 0, UefiConstants.SIZE_4);
  listSize = HexUtils.leReverseInt(lSize);

  byte[] hSize = new byte[UefiConstants.SIZE_4];
  System.arraycopy(list, UefiConstants.OFFSET_20, hSize, 0, UefiConstants.SIZE_4);

  byte[] sSize = new byte[UefiConstants.SIZE_4];
  System.arraycopy(list, UefiConstants.OFFSET_24, sSize, 0, UefiConstants.SIZE_4);
  signatureSize = HexUtils.leReverseInt(sSize);

  sigData = new byte[signatureSize];
  System.arraycopy(list, UefiConstants.OFFSET_28, sigData, 0, signatureSize);
  processSignatureList(sigData);
}

/**
 * EFI Signature list constructor.
 * @param lists  ByteArrayInputStream containing an EFI Signature list.
 * @throws IOException If there's a problem in reading he input stream.
 * @throws CertificateException If there's a problem parsing the X509 certificate.
 * @throws NoSuchAlgorithmException if there's a problem hashing the certificate.
 */
UefiSignatureList(final ByteArrayInputStream lists)
                  throws IOException, CertificateException, NoSuchAlgorithmException {
  byte[] guid = new byte[UefiConstants.SIZE_16];
  lists.read(guid);
  signatureType = new UefiGuid(guid);

  if (!isValidSigListGUID(signatureType)) {
      processSignatureData(lists);
  } else { // valid SigData Processing
      byte[] lSize = new byte[UefiConstants.SIZE_4];
      lists.read(lSize);
      listSize = HexUtils.leReverseInt(lSize);

      byte[] hSize = new byte[UefiConstants.SIZE_4];
      lists.read(hSize);

      byte[] sSize = new byte[UefiConstants.SIZE_4];
      lists.read(sSize);
      signatureSize = listSize - UefiConstants.SIZE_28;
      sigData = new byte[signatureSize];
      lists.read(sigData);
      processSignatureList(sigData);
  }
}

/**
 * Method for processing a set of EFI SignatureList(s).
 * @param sigData  Byte array holding one or more SignatureLists
 * @throws CertificateException If there's a problem parsing the X509 certificate.
 * @throws NoSuchAlgorithmException if there's a problem hashing the certificate.
 * @throws IOException If there's a problem parsing the signature data.
 */
private void processSignatureList(final byte[] efiSigData)
             throws CertificateException, NoSuchAlgorithmException, IOException {
  efiSigDataIS = new ByteArrayInputStream(efiSigData);
  while (efiSigDataIS.available() > 0) {
    UefiSignatureData tmpSigData = new UefiSignatureData(efiSigDataIS, signatureType);
    if (!tmpSigData.isValid()) {
       valid = false;
       status = tmpSigData.getStatus();
       break;
    }
    sigList.add(tmpSigData);
   numberOfItems++;
  }
}

/**
 * Method for processing a set of EFI SignatureList(s).
 * @param sigData  Byte array holding one or more SignatureLists.
 * @throws CertificateException If there's a problem parsing the X509 certificate.
 * @throws NoSuchAlgorithmException if there's a problem hashing the certificate.
 * @throws IOException If there's a problem parsing the signature data.
 */
private void processSignatureData(final ByteArrayInputStream sigDataIS)
             throws CertificateException, NoSuchAlgorithmException, IOException {
  while (sigDataIS.available() > 0) {
     UefiSignatureData tmpigData = new UefiSignatureData(sigDataIS, signatureType);
      if (!tmpigData.isValid()) {
         valid = false;
          status = tmpigData.getStatus();
           break;
       }
         sigList.add(tmpigData);
         numberOfItems++;
       }
 }

/**
 * Returns an ArrayList of EFISignatureData objects.
 * @return ArrayList of EFISignatureData objects.
 */
public ArrayList<UefiSignatureData> getSigatureDataList() {
   return sigList;
}

/**
 * Return the number of certificates found within a certificate list.
 * @return int Number of certs.
 */
public int getNumberOfCerts() {
  return numberOfItems;
}

/**
 * Checks to see if GUID is listed on page 1729 of UEFI spec version 2.8.
 * @param guid GUID of the has algorithm.
 * @return true if the GUID is a valid GUID for Signature List Type, false if not.
 */
public boolean isValidSigListGUID(final UefiGuid guid) {
  switch (guid.getVendorTableReference()) {
    case "EFI_CERT_SHA256_GUID": return true;
    case "EFI_CERT_X509_SHA256": return true;
    case "EFI_CERT_X509_SHA384": return true;
    case "EFI_CERT_X509_SHA512": return true;
    case "EFI_CERT_X509_GUID":   return true;
    default: return false;
  }
}

/**
 * Provides a description of the fields within the EFI Signature Data field.
 * Which is essentially a list of X509 certificates.
 * @return human readable description.
 */
public String toString() {
  StringBuffer sigInfo = new StringBuffer();
  sigInfo.append("UEFI Signature List Type = " + signatureType.toString() + "\n");
  sigInfo.append("Number if items = " + numberOfItems + "\n");
  sigList.iterator();
  for (int i = 0; i < sigList.size(); i++) {
    UefiSignatureData certData = (UefiSignatureData) sigList.get(i);
    sigInfo.append(certData.toString());
  }
  if (!valid) {
      sigInfo.append("*** Invalid UEFI Signature data encountered: " + status + "\n");
  }
  return sigInfo.toString();
 }
}
