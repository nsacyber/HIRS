package hirs.swid.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * This class is a helper for creating hash values for the files associated
 * with the swidtag program
 */
public class HashSwid {

    public static final String ENCODING = "UTF-8";
    public static final String SHA256 = "SHA-256";
    public static final String SHA384 = "SHA-384";
    public static final String SHA512 = "SHA-512";

    /**
     * Getter method for the hash that uses 256 bit hash
     * @param value
     * @return 
     */
    public static String get256Hash(String filepath) {
        return getHashValue(filepath, SHA256);
    }

    /**
     * Getter method for the hash that uses 384 bit hash
     * @param value
     * @return 
     */
    public String get384Hash(String filepath) {
        return getHashValue(filepath, SHA384);
    }

    /**
     * Getter method for the hash that uses 512 bit hash
     * @param value
     * @return 
     */
    public String get512Hash(String filepath) {
        return getHashValue(filepath, SHA512);
    }

    /**
     * This method creates the hash based on the provided algorithm
     * only accessible through helper methods.
     *
     * This method assumes an input file that is small enough to read in its
     * entirety.  Large files should be handled similarly to the public static
     * getHashValue() below.
     * 
     * @param filepath file contents to hash
     * @param sha the algorithm to use for the hash
     * @return 
     */
    private static String getHashValue(String filepath, String sha) {
        String resultString = null;
        try {
            MessageDigest md = MessageDigest.getInstance(sha);            
            byte[] bytes = md.digest(Files.readAllBytes(Paths.get(filepath)));
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            resultString = sb.toString();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException grex) {
            System.out.println(grex.getMessage());
        } catch (IOException e) {
            System.out.println("Error reading in file to hash: " + e.getMessage());
        }

        return resultString;
    }

    /**
     * This method is a public access hash function that operates on a string
     * value and uses default assumptions on the salt and algorithm
     * @param value string object to hash
     * @return 
     */
    public static String getHashValue(String value) {
        byte[] buffer = new byte[8192];
        int count;
        byte[] hash = null;
        BufferedInputStream bis = null;

        try {
            MessageDigest md = MessageDigest.getInstance(SHA256);
            bis = new BufferedInputStream(new FileInputStream(value));
            while ((count = bis.read(buffer)) > 0) {
                md.update(buffer, 0, count);
            }

            hash = md.digest();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException grex) {
            System.out.println(grex.getMessage());
        } catch (IOException ioEx) {
            System.out.println(String.format("%s: \n%s is not valid...",
                    ioEx.getMessage(), value));
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException ioEx) {
                // ignored, system issue that won't affect further execution
            }
            
            if (hash == null) {
                return "";
            }
        }

        return Base64.getEncoder().encodeToString(hash);
    }
}
