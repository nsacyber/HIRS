package hirs.swid;

import hirs.swid.utils.CommandCreate;
import hirs.swid.utils.CommandMain;
import hirs.swid.utils.CommandPrint;
import hirs.swid.utils.CommandSign;
import hirs.swid.utils.CommandVerify;
import com.beust.jcommander.JCommander;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Log4j2
public class Main {

    public static void main(String[] args) {
        CommandMain mainCom = new CommandMain();
        CommandCreate createCom = new CommandCreate();
        CommandSign signCom = new CommandSign();
        CommandVerify verifyCom = new CommandVerify();
        CommandPrint printCom = new CommandPrint();
        JCommander jc = JCommander.newBuilder()
                .addObject(mainCom)
                .addCommand("create", createCom)
                .addCommand("sign", signCom)
                .addCommand("verify", verifyCom)
                .addCommand("print", printCom)
                .build();
        try {
            jc.parse(args);
        } catch (Exception e) {
            exitWithErrorCode(e.getMessage());
        }

        if (mainCom.isVersion()) {
            parseVersionFromJar();
        } else if(mainCom.isVerbose()) {
            System.out.println("Rimtool in verbose mode.");
        }
        switch(jc.getParsedCommand()) {
            case "create":
                System.out.println("Create " + createCom.getOutFile()
                + " using " + createCom.getAttributesFile()
                + " and " + createCom.getRimEventLog());
                break;
            case "sign":
                System.out.println("Sign " + signCom.getInFile()
                + " with credentials " + signCom.getTruststore() + ", "
                + signCom.getPublicCertificate() + ", "
                + signCom.getPrivateKey());
                break;
            case "verify":
                System.out.println("Verify " + verifyCom.getInFile()
                + " with " + verifyCom.getRimEventLog() + " and "
                + verifyCom.getTruststore());
                break;
            case "print":
                System.out.println("Print " + printCom.getInFile());
                break;
            default:
                System.out.println("No command given.");
        }
    }

    /**
     * Use cases that exit with an error code are redirected here.
     */
    private static void exitWithErrorCode(String errorMessage) {
        log.error(errorMessage);
        System.exit(1);
    }

    /**
     * This method parses the version number from the jar filename in the absence of
     * the VERSION file expected with an rpm installation.
     */
    private static void parseVersionFromJar() {
        System.out.println("Installation file VERSION not found.");
        String filename = new File(Main.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath()).getName();
        Pattern pattern = Pattern.compile("(?<=tcg_rim_tool-)[0-9]\\.[0-9]\\.[0-9]");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            System.out.println("TCG rimtool version: " + matcher.group());
        }
    }
}
