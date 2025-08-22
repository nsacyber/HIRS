package hirs.utils.rim.unsignedRim.xml.tcgCompRimSwid;

import hirs.utils.rim.unsignedRim.xml.pcclientrim.PcClientRim;

/**
 * Class that holds a TCG Component RIM SWID object.
 */
public class TcgComponentRimSwid extends PcClientRim {

    /** Object to hold the TCG Comp RIM SWID Builder. */
    private TcgComponentRimSwidBuilder builder = new TcgComponentRimSwidBuilder();

    /**
     * Writes a TCG Component RIM SWID object to a file.
     * @param configFile path to TCG Component RIM config data file
     * @param rimEventLog path to TPM event log file
     * @param certificateFile path to cert file
     * @param privateKeyFile path to private key file
     * @param embeddedCert whether cert should be embedded
     * @param outFile file to hold the new TCG Component RIM
     */
    public void create(final String configFile, final String rimEventLog, final String certificateFile,
                       final String privateKeyFile, final boolean embeddedCert, final String outFile) {

        builder.setConfigFile(configFile);
        builder.setRimEventLog(rimEventLog);

        builder.setDefaultCredentials(false);
        builder.setPemCertificateFile(certificateFile);
        builder.setPemPrivateKeyFile(privateKeyFile);
        if (embeddedCert) {
            builder.setEmbeddedCert(true);
        }
        /* skip timestamp for now

        List<String> timestampArguments = commander.getTimestampArguments();
        if (timestampArguments.size() > 0) {
            if (new TimestampArgumentValidator(timestampArguments).isValid()) {
                gateway.setTimestampFormat(timestampArguments.get(0));
                if (timestampArguments.size() > 1) {
                    gateway.setTimestampArgument(timestampArguments.get(1));
                }
            } else {
                exitWithErrorCode("The provided timestamp argument(s) " +
                        "is/are not valid.");
            }
        }
        */
        builder.generateSwidTag(outFile);
    }
}
