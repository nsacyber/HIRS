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
 * helper class to process CSV files.
 */
public class CsvParser {

    private static final char DEFAULT_SEPARATOR = ',';
    private static final char DEFAULT_QUOTE = '"';
    private List<String> content;

    /**
     * CsvParser constructor.
     * @param file name of the file contains the CSV data.
     */
    public CsvParser(final File file) {
        this(file.getAbsolutePath());
    }

    /**
     * CsvParser constructor.
     * @param csvfile Sting containing the contents of the SCV file.
     */
    public CsvParser(final String csvfile) {
        content = readerCsv(csvfile);
    }

    /**
     * This method takes an existing csv file and reads the file by line and
     * adds the contents to a list of Strings.
     *
     * @param file valid path to a csv file.
     * @return  List of Strings.
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

    /**
     * Gets content as a list of Stings.
     * @return List of Strings.
     */
    public final List<String> getContent() {
       return Collections.unmodifiableList(content);
    }

    /**
     * Gets a list of parsed lines.
     * @param csvLine
     * @return List of Strings.
     */
    public static List<String> parseLine(final String csvLine) {
        return parseLine(csvLine, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
    }

    /**
     * Parses a CSV Line.
     * @param csvLine
     * @param separators
     * @return List of Strings.
     */
    public static List<String> parseLine(final String csvLine, final char separators) {
        return parseLine(csvLine, separators, DEFAULT_QUOTE);
    }

    /**
     * Parses a CSV Line.
     * @param csvLine
     * @param separators
     * @param customQuote
     * @return List of Stings.
     */
    public static List<String> parseLine(final String csvLine, final char separators, final char customQuote) {
        char separator = separators;
        char quote = customQuote;
        List<String> result = new ArrayList<>();

        if (csvLine == null || csvLine.isEmpty()) {
            return result;
        }

        if (customQuote == ' ') {
            quote = DEFAULT_QUOTE;
        }

        if (separator == ' ') {
            separator = DEFAULT_SEPARATOR;
        }

        StringBuilder currVal = new StringBuilder();
        boolean inQuotes = false;
        boolean startCollectChar = false;
        boolean dbleQuotesInCol = false;

        char[] chars = csvLine.toCharArray();

        for (char ch : chars) {
            if (inQuotes) {
                startCollectChar = true;
                if (ch == quote) {
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
                if (ch == quote) {
                    inQuotes = true;
                    if (chars[0] != '"' && quote == '\"') {
                        currVal.append('"');
                    }
                    if (startCollectChar) {
                        currVal.append('"');
                    }
                } else if (ch == separator) {
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
