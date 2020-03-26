package hirs.ima;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import hirs.data.persist.baseline.ImaBlacklistBaseline;
import hirs.data.persist.ImaBlacklistRecord;

/**
 * Tests that the {@link ImaBlacklistBaselineGenerator} works as expected.
 */
public class ImaBlacklistBaselineGeneratorTest {
    /**
     * Tests that a CSV representing a blacklist baseline, whose entries have descriptions,
     * can be imported successfully.
     *
     * @throws IOException if there is a problem deserializing the baseline records
     */
    @Test
    public void testImportCsvWithDescriptions() throws IOException {
        InputStream is = ClassLoader.class.getResourceAsStream("/ima/IMABlacklistBaseline.csv");
        ImaBlacklistBaseline imported = ImaBlacklistBaselineGenerator.generateBaselineFromCSV(
                "Test Baseline", is
        );
        ImaBlacklistBaseline baseline =
                CSVGeneratorTest.getTestImaBlacklistBaselineWithDescriptions();
        Assert.assertEquals(imported, baseline);
        Assert.assertEquals(imported.getRecords(), baseline.getRecords());

        // baseline records don't include descriptions in equality checks, so check manually
        for (ImaBlacklistRecord baselineRecord : baseline.getRecords()) {
            boolean found = false;
                for (ImaBlacklistRecord importedRecord : imported.getRecords()) {
                if (baselineRecord.getDescription().equals(importedRecord.getDescription())) {
                    found = true;
                }
            }
            if (!found) {
                Assert.fail("Did not find message in imported baseline: "
                        + baselineRecord.getDescription());
            }
        }
    }

    /**
     * Tests that a CSV representing a blacklist baseline, whose entries do not have descriptions,
     * can be imported successfully.
     *
     * @throws IOException if there is a problem deserializing the baseline records
     */
    @Test
    public void testImportCsvWithoutDescriptions() throws IOException {
        InputStream is = ClassLoader.class.getResourceAsStream(
                "/ima/IMABlacklistBaselineNoDescriptions.csv"
        );
        ImaBlacklistBaseline imported = ImaBlacklistBaselineGenerator.generateBaselineFromCSV(
                "Test Baseline", is
        );
        ImaBlacklistBaseline baseline =
                CSVGeneratorTest.getTestImaBlacklistBaselineWithDescriptions();
        Assert.assertEquals(imported, baseline);
        Assert.assertEquals(imported.getRecords(), baseline.getRecords());
    }
}
