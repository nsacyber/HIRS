 using System;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Security.Cryptography.Xml;
using System.Xml;

public class VerifyXML
{

    public static void Main(String[] args)
    {
        try
        {
            const string signingAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";
            const string signingCertName = "RimSignCert.pem";
            const string privateKeyFilename = "privateRimKey.pem";

            //Load public cert from file
            X509Certificate2 signingCert = new X509Certificate2(signingCertName);
            RSA publicKey = signingCert.GetRSAPublicKey();
            //Load private key from file
            string privateKeyText = System.IO.File.ReadAllText(privateKeyFilename);
            //System.Console.WriteLine("Using private key: " + privateKeyText);
            var privateKey = RSA.Create();
            privateKey.ImportFromPem(privateKeyText);

            // Load an XML file into the XmlDocument object.
            XmlDocument unsignedDoc = new XmlDocument();
            unsignedDoc.PreserveWhitespace = true;
            unsignedDoc.Load("unsigned.xml");
            SignXml(unsignedDoc, privateKey);
            unsignedDoc.Save("signed.xml");

            // Verify the signature of the signed XML.
            XmlDocument signedDoc = new XmlDocument();
            signedDoc.Load("signed.xml");
            bool result = VerifyXml(signedDoc, publicKey);

            // Display the results of the signature verification to
            // the console.
            if (result)
            {
                Console.WriteLine("The XML signature is valid.");
            }
            else
            {
                Console.WriteLine("The XML signature is not valid.");
            }
        }
        catch (Exception e)
        {
            Console.WriteLine(e.Message);
        }
    }
    private static void SignXml(XmlDocument xmlDoc, RSA rsaKey)
    {
        if (xmlDoc == null)
            throw new ArgumentException(nameof(xmlDoc));
        if (rsaKey == null)
            throw new ArgumentException(nameof(rsaKey));

        Console.Write("Signing xml...");

        // Create a SignedXml object.
        SignedXml signedXml = new SignedXml(xmlDoc);

        // Add the key to the SignedXml document.
        signedXml.SigningKey = rsaKey;

        // Create a reference to be signed.
        Reference reference = new Reference();
        reference.Uri = "";

        // Add an enveloped transformation to the reference.
        XmlDsigEnvelopedSignatureTransform env = new XmlDsigEnvelopedSignatureTransform();
        reference.AddTransform(env);

        // Add the reference to the SignedXml object.
        signedXml.AddReference(reference);

        // Compute the signature.
        signedXml.ComputeSignature();

        // Get the XML representation of the signature and save
        // it to an XmlElement object.
        XmlElement xmlDigitalSignature = signedXml.GetXml();

        // Append the element to the XML document.
        xmlDoc.DocumentElement.AppendChild(xmlDoc.ImportNode(xmlDigitalSignature, true));
        Console.WriteLine("Xml signed.");
    }

    // Verify the signature of an XML file against an asymmetric
    // algorithm and return the result.
    public static Boolean VerifyXml(XmlDocument xmlDoc, RSA key)
    {
        // Check arguments.
        if (xmlDoc == null)
            throw new ArgumentException("xmlDoc");
        if (key == null)
            throw new ArgumentException("key");

        Console.Write("Verifying signature...");
        // Create a new SignedXml object and pass it
        // the XML document class.
        SignedXml signedXml = new SignedXml(xmlDoc);

        // Find the "Signature" node and create a new
        // XmlNodeList object.
        XmlNodeList nodeList = xmlDoc.GetElementsByTagName("Signature");

        // Throw an exception if no signature was found.
        if (nodeList.Count <= 0)
        {
            throw new CryptographicException("Verification failed: No Signature was found in the document.");
        }

        // This example only supports one signature for
        // the entire XML document.  Throw an exception
        // if more than one signature was found.
        if (nodeList.Count >= 2)
        {
            throw new CryptographicException("Verification failed: More than one signature was found for the document.");
        }

        // Load the first <signature> node.
        signedXml.LoadXml((XmlElement)nodeList[0]);
        Boolean isValid = false;
        try
        {
            isValid = signedXml.CheckSignature(key);
        } catch (Exception e)
        {
            Console.WriteLine(e.Message);
        }
        
        // Check the signature and return the result.
        return isValid;
    }

}
//6/17/2022
//Copied RimSignCert.pem, RimCertChain.pem, and generated_user_cert_embed.swidtag to EC2. 
//Tried reading RimSignCert and RimCertChain (both certs and each cert by itself) and extracting the PK, validation failed all attempts.
//Verified that swidtag is in fact signed by RimSignCert's private key.
//
//7/11/2022
//Copied RimSignCert.pem, generated_user_cert_embed.swidtag (less the signature block), and privateRimKey.pem to EC2.
//Tried signing the unsigned RIM with the private key and then validated with the public key from the cert; unsuccessful.

