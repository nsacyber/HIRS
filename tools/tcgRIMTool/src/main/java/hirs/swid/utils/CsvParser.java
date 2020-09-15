package hirs.swid.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class CsvParser {

    private static final char DEFAULT_SEPARATOR = ',';
    private static final char DEFAULT_QUOTE = '"';
    
    private List<String> content;

    public CsvParser(final File file) {
        this(file.getAbsolutePath());
    }
    
    public CsvParser(final String csvfile) {
        content = readerCsv(csvfile);
    }

    /**
     * This method takes an existing csv file and reads the file by line and
     * adds the contents to a list of Strings.
     * 
     * @param file valid path to a csv file
     * @return 
     */
    private List<String> readerCsv(final String file) {
        String line = "";
        String csvSplitBy = ",";
        List<String> tempList = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while ((line = br.readLine()) != null) {
                if (line.length() > 0 
                        && line.contains(csvSplitBy)) {
                 tempList.add(line);
                }
            }
        } catch (IOException ioEx) {
            System.out.println(String.format("Error reading in CSV file...(%s)", file));
            System.exit(1);
        }
        
        return tempList;
    }

    public final List<String> getContent() {
        return Collections.unmodifiableList(content);
    }
    
    public static List<String> parseLine(String csvLine) {
        return parseLine(csvLine, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
    }

    public static List<String> parseLine(String csvLine, char separators) {
        return parseLine(csvLine, separators, DEFAULT_QUOTE);
    }

    public static List<String> parseLine(String csvLine, char separators, char customQuote) {
        List<String> result = new ArrayList<>();

        if (csvLine == null || csvLine.isEmpty()) {
            return result;
        }

        if (customQuote == ' ') {
            customQuote = DEFAULT_QUOTE;
        }

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuilder currVal = new StringBuilder();
        boolean inQuotes = false;
        boolean startCollectChar = false;
        boolean dbleQuotesInCol = false;

        char[] chars = csvLine.toCharArray();

        for (char ch : chars) {
            if (inQuotes) {
                startCollectChar = true;
                if (ch == customQuote) {
                    inQuotes = false;
                    dbleQuotesInCol = false;
                } else {
                    if (ch == '\"') {
                        if (!dbleQuotesInCol) {
                            currVal.append(ch);
                            dbleQuotesInCol = true;
                        }
                    } else {
                        currVal.append(ch);
                    }
                }
            } else {
                if (ch == customQuote) {
                    inQuotes = true;

                    if (chars[0] != '"' && customQuote == '\"') {
                        currVal.append('"');
                    }

                    if (startCollectChar) {
                        currVal.append('"');
                    }
                } else if (ch == separators) {
                    result.add(currVal.toString());
                    currVal = new StringBuilder();
                    startCollectChar = false;
                } else if (ch == '\r') {
                    continue;
                } else if (ch == '\n') {
                    break;
                } else {
                    currVal.append(ch);
                }
            }
        }

        result.add(currVal.toString());

        return result;
    }
}
