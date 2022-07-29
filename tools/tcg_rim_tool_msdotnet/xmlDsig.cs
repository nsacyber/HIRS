using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Security.Cryptography.Xml;
using System.Xml;
using System.CommandLine;

/**
 * This command line program has three commands:
 * 1. sign - append a signature calculated from a user-provided private key
 * 2. validate - validate a signature with a user-provided certificate
 * 3. debug - print out important components of a signed XML document
 *  
 * The validate functioin strictly checks the cryptographic integrity of the signature,
 * it does not verify the integrity of the certificate chain.
 */
class Rimtool
{

    static async Task<int> Main(String[] args)
    {
        var fileOption = new Option<string>(
            name: "--file",
            description: "The filename input for the command.",
            parseArgument: result =>
            {
                string? filePath = result.Tokens.Single().Value;
                if (!File.Exists(filePath))
                {
                    result.ErrorMessage = "File " + filePath + " does not exist.";
                    return null;
                }
                else
                {
                    return filePath;
                }
            });
        var privateKeyOption = new Option<string>(
            name: "--private-key",
            description: "The private key with which to sign."
            );
        var certificateOption = new Option<string>(
            name: "--certificate",
            description: "The certificate with which to validate the signature."
            );

        var rootCommand = new RootCommand("A tool for signing, validating, and debugging base RIMs.");
        var signCommand = new Command("sign", "Sign the given file with the given key.")
        {
            fileOption,
            privateKeyOption
        };
        var validateCommand = new Command("validate", "Validate the signature in the given base RIM.")
        {
            fileOption,
            certificateOption
        };
        var debugCommand = new Command("debug", "Print out the significant portions of a base RIM.")
        {
            fileOption
        };

        signCommand.SetHandler(async (file, privateKey) =>
        {
            await SignXml(file, privateKey);
        }, fileOption, privateKeyOption);
        validateCommand.SetHandler(async (file, certificate) =>
        {
            await ValidateXml(file, certificate);
        }, fileOption, certificateOption);
        debugCommand.SetHandler(async (file) =>
        {
            await DebugRim(file);
        }, fileOption);

        rootCommand.AddCommand(signCommand);
        rootCommand.AddCommand(validateCommand);
        rootCommand.AddCommand(debugCommand);

        return rootCommand.InvokeAsync(args).Result;
    }
    internal static async Task SignXml(string xmlFilename, string keyFilename)
    {
        if (String.IsNullOrWhiteSpace(xmlFilename))
            throw new ArgumentException(nameof(xmlFilename));
        if (String.IsNullOrWhiteSpace(keyFilename))
            throw new ArgumentException(nameof(keyFilename));

        Console.Write("Signing xml...");

        // Load an XML file into a SignedXML object.
        XmlDocument unsignedDoc = new XmlDocument();
        unsignedDoc.Load(xmlFilename);
        SignedXml signedXml = new SignedXml(unsignedDoc);

        //Load private key from file
        string privateKeyText = System.IO.File.ReadAllText(keyFilename);
        var privateKey = RSA.Create();
        privateKey.ImportFromPem(privateKeyText);

        // Add the key to the SignedXml document.
        signedXml.SigningKey = privateKey;

        // Create a reference to be signed.
        Reference reference = new Reference();
        reference.Uri = "";

        // Add an enveloped transformation to the reference.
        XmlDsigEnvelopedSignatureTransform env = new XmlDsigEnvelopedSignatureTransform();
        reference.AddTransform(env);

        // Add the reference to the SignedXml object.
        signedXml.AddReference(reference);

        // Add keyinfo block
        KeyInfo keyInfo = new KeyInfo();
        keyInfo.AddClause(new RSAKeyValue((RSA)privateKey));
        signedXml.KeyInfo = keyInfo;

        // Compute the signature.
        signedXml.ComputeSignature();

        // Get the XML representation of the signature and save
        // it to an XmlElement object.
        XmlElement xmlDigitalSignature = signedXml.GetXml();

        // Append the element to the XML document.
        unsignedDoc.DocumentElement.AppendChild(unsignedDoc.ImportNode(xmlDigitalSignature, true));
        string signedFilename = "signed_" + xmlFilename;
        unsignedDoc.Save(signedFilename);
        Console.WriteLine("Xml signed and written to " + signedFilename);
    }

    // Verify the signature of an XML file against an asymmetric
    // algorithm and return the result.
    internal static async Task ValidateXml(string signedFilename, string certFilename)
    {
        // Check arguments.
        if (String.IsNullOrWhiteSpace(signedFilename))
            throw new ArgumentException(nameof(signedFilename));
        if (certFilename == null)
            throw new ArgumentException(nameof(certFilename));

        Console.Write("Verifying signature...");
        // Create a new SignedXml object and pass it
        // the XML document class.
        XmlDocument signedDoc = new XmlDocument();
        signedDoc.Load(signedFilename);
        SignedXml signedXml = new SignedXml(signedDoc);

        //Load public cert from file
        X509Certificate2 signingCert = new X509Certificate2(certFilename);
        RSA publicKey = signingCert.GetRSAPublicKey();

        // Find the "Signature" node and create a new
        // XmlNodeList object.
        XmlNodeList nodeList = signedDoc.GetElementsByTagName("Signature");

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
            isValid = signedXml.CheckSignature(publicKey);
        }
        catch (Exception e)
        {
            Console.WriteLine(e.Message);
        }

        // Check the signature and return the result.
        if (isValid)
        {
            Console.WriteLine("Signature is valid!");
        }
        else
        {
            Console.WriteLine("Signature is not valid.");
        }
    }

    internal static async Task DebugRim(string filename)
    {
        if (String.IsNullOrWhiteSpace(filename))
        {
            throw new ArgumentException(nameof(filename));
        }
        XmlDocument xmlDoc = new XmlDocument();
        xmlDoc.Load(filename);

    }

}

