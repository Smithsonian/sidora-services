
package edu.si.sidora.excel2tabular.integration;

import static edu.si.sidora.excel2tabular.integration.IntegrationTestUtilities.compareLines;
import static edu.si.sidora.excel2tabular.integration.IntegrationTestUtilities.readLines;
import static com.google.common.base.Charsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;

import edu.si.sidora.excel2tabular.ExcelToTabular;
import com.google.common.io.Resources;

public class RaggedEdgesIT {

    private final ExcelToTabular testExcel2Csv = new ExcelToTabular();

    private static final Logger log = getLogger(RaggedEdgesIT.class);

    @Test
    public void testOneSheetFile() throws IOException {
        final URL inputUrl = new File("src/test/resources/xls/ragged-edges-test.xls").toURI().toURL();
        final URL result = testExcel2Csv.process(inputUrl).get(0).toURI().toURL();
        log.debug("Result of extraction:\n{}", Resources.toString(result, UTF_8));

        final URL checkFile = new File("src/test/resources/tabular/ragged-edges-test.csv").toURI().toURL();
        log.debug("File against which we're going to check:\n{}", Resources.toString(checkFile, UTF_8));
        final List<String> resultLines = readLines(result);
        final List<String> checkLines = readLines(checkFile);
        compareLines(checkLines, resultLines);
    }
}
