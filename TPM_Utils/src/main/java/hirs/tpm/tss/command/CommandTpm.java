package hirs.tpm.tss.command;

import hirs.tpm.tss.Tpm;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of a {@link Tpm} that uses the TPM_MODULE command as the interface to the TPM.
 */
public class CommandTpm implements Tpm {

    private static final String TPM_COMMAND = "/usr/bin/tpm_module";
    private static final String TPM_TOOLS_READ_COMMAND = "tpm_nvread";
    private static final String TPM_TOOLS_INFO_COMMAND = "tpm_nvinfo";

    // The fixed location of the endorsement credential (TPM 1.2 only)
    private static final int EC_INDEX = 0xf000;
    private static final int EC_INDEX_MASK = 0x0000ffff;

    private static final String IDENTITY_LABEL = "HISIdentityKey";

    @Override
    public void takeOwnership() {
        // take ownership if the TPM is currently unowned
        try {
            sendTPMCommand("-nr -m 1 -z");
        } catch (CommandException ex) {

            // if the error isn't that the TPM is already owned, bubble the exception up.
            if (ex.getCommandResult().getExitStatus() != CommandResult.TPM_PREVIOUSLY_OWNED_ERROR) {
                throw ex;
            }
        }
    }

    @Override
    public byte[] getEndorsementCredentialModulus() {
        return sendTPMCommand("-m 17 -z -nr -t ek").getOutput().getBytes();
    }

    @Override
    public byte[] getEndorsementCredential() throws IOException {
        File tempExtractFile = File.createTempFile("nvread-extract", ".tmp");

        try {
            int ecIndex = findEndorsementCredentialIndex();
            int ecSize = getEndorsementCredentialSize(ecIndex);
            // don't care about the stdout from this command. If the command fails, an exception
            // is thrown, otherwise, read the temp file content
            String argList = "-i " + ecIndex
                    + " -s " + ecSize + " -z -f " + tempExtractFile.getAbsolutePath();

            sendCommand(TPM_TOOLS_READ_COMMAND, argList);
            return FileUtils.readFileToByteArray(tempExtractFile);
        } finally {
            FileUtils.deleteQuietly(tempExtractFile);
        }
    }

    private int findEndorsementCredentialIndex() {
        CommandResult command = sendCommand(TPM_TOOLS_INFO_COMMAND, "");
        return parseEcIndexFromNvInfoOutput(command.getOutput());
    }

    /**
     * Parses the output from tpm_nvinfo to find the index for the endorsement credential.
     * @param commandOutput the output of the tpm_nvinfo command
     * @return the index of the EC
     */
    static int parseEcIndexFromNvInfoOutput(final String commandOutput) {
        String[] lines = commandOutput.trim().split("\\r?\\n");

        for (String line: lines) {
            if (line.startsWith("NVRAM index")) {
                String rawIndex = line.split(":")[1].trim();
                String hexIndexStr = rawIndex.split(" ")[0];
                int index = Integer.decode(hexIndexStr);
                if ((EC_INDEX_MASK & index) == EC_INDEX) {
                    return index;
                }
            }
        }

        throw new RuntimeException("Failed to find index for EC");
    }

    private int getEndorsementCredentialSize(final int index) {
        try {
            String args = "-i " + index;
            CommandResult command = sendCommand(TPM_TOOLS_INFO_COMMAND, args);
            return parseNvramSizeFromNvInfoOutput(command.getOutput());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get EC size: ", ex);
        }
    }


    /**
     * Parses the output from tpm_nvinfo for a particular address, returning the size of the
     * data at the queried address.
     * @param commandOutput the output of a tpm_nvinfo -i [address] command
     * @return the size at the address, as a decimal integer
     */
    static int parseNvramSizeFromNvInfoOutput(final String commandOutput) {
        // trim the ends and split on lines.
        String[] lines = commandOutput.trim().split("\\r?\\n");

        // search lines for the first "Size" line, which contains the # of bytes to
        // read at the location
        for (String line : lines) {
            if (line.startsWith("Size")) {
                String rawSizeValue = line.split(":")[1].trim();
                // The value after the ":" contains the decimal and hex values.
                // Parse out the decimal portion
                String decimalSizeStr = rawSizeValue.split(" ")[0];
                return Integer.parseInt(decimalSizeStr);
            }
        }

        throw new RuntimeException("Failed to find size from EC's NVRAM area");
    }

    @Override
    public byte[] collateIdentityRequest(final byte[] acaPublicKey, final String uuid) {
        Assert.notNull(acaPublicKey, "acaPublicKey is null");
        Assert.hasLength(uuid, "uuid must not be empty or null");

        // encode the aca PK to a hex string.
        String hexAcaBlob = Hex.encodeHexString(acaPublicKey);

        // send the collate identity request
        String request = sendTPMCommand(
                String.format("-nr -z -o -m 6 -p %s -u %s -l %s -nvram -debug",
                hexAcaBlob, uuid, IDENTITY_LABEL)).getOutput();

        try {
            // attempt to decode the response
            return Hex.decodeHex(request.toCharArray());
        } catch (DecoderException e) {
            throw new RuntimeException(
                    "Encountered error decoding response from tpm_module: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] activateIdentity(final byte[] asymmetricBlob, final byte[] symmetricBlob,
            final String uuid) {
        Assert.notNull(asymmetricBlob, "asymmetricBlob is null");
        Assert.notNull(symmetricBlob, "symmetricBlob is null");
        Assert.hasLength(uuid, "uuid must not be empty or null");

        // encode the blobs into hex strings
        String hexAsymmetric = Hex.encodeHexString(asymmetricBlob);
        String hexSymmetric = Hex.encodeHexString(symmetricBlob);

        // issue the activate identity command
        CommandResult result = sendTPMCommand(String.format("-asym %s -sym %s -u %s -m 7 -z - nr",
                hexAsymmetric, hexSymmetric, uuid));

        try {
            return Hex.decodeHex(result.getOutput().toCharArray());
        } catch (DecoderException e) {
            throw new RuntimeException(
                    "Encountered error decoding response from tpm_module: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] getQuote(final String pcr, final String nonce, final String uuid) {
        Assert.hasLength(pcr, "pcr must not be empty or null");
        Assert.hasLength(nonce, "nonce must not be empty or null");
        Assert.hasLength(uuid, "uuid must not be empty or null");

        // issues the quote 2 command (-m 9) to retrieve the quote for the selected PCR (-p #).
        CommandResult result =
                sendTPMCommand(String.format("-m 9 -z -n %s -c -u %s -p %s", nonce, uuid, pcr));

        try {
            return Hex.decodeHex(result.getOutput().toCharArray());
        } catch (DecoderException e) {
            throw new RuntimeException(
                    "Encountered error decoding response from tpm_module: " + e.getMessage(), e);
        }
    }

    private CommandResult sendTPMCommand(final String arguments) {
        // always run the tpm_module with debug (-d).
        // otherwise the exit status and error messages are squelched.
        return sendCommand(TPM_COMMAND, "-d " + arguments);
    }

    /**
     * Issues the specified command to the tpm_module process.
     */
    private CommandResult sendCommand(final String application, final String arguments) {
        String[] command = {application};

        // add the specified arguments to the command
        command = ArrayUtils.addAll(command, arguments.split("\\s+"));

        // build up the process
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        // merge the error stream into the standard output
        processBuilder.redirectErrorStream(true);

        try {
            // issue the command
            Process process = processBuilder.start();

            // block and wait for the process to be complete
            int returnCode = process.waitFor();

            try (InputStream processInputStream = process.getInputStream()) {
                // grab the command output
                String output = processInputStream.toString();

                // construct the command result
                CommandResult result = new CommandResult(output, returnCode);

                // if the command wasn't successful, generate an exception with command output
                if (returnCode != 0) {
                    throw new CommandException(
                            String.format("Encountered error: %s while executing command: %s",
                                    output, StringUtils.join(command, " ")), result);
                }

                return result;
            }


        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
