
package com.asoroka.sidora.excel2tabular;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;

public class SIdoraData1Test extends TestUtils {

    private final ExcelToTabular testExcel2Csv = new ExcelToTabular();

    private static final Logger log = getLogger(SIdoraData1Test.class);

    @Test
    public void testOneSheetFile() throws IOException {
        final URL inputUrl = new File("src/test/resources/xls/cjd-master-op.3-huesos.xls").toURI().toURL();
        final URL result = testExcel2Csv.process(inputUrl).get(0).toURI().toURL();
        // log.debug("Result of extraction:\n{}", Resources.toString(result, UTF_8));

        final URL checkFile = new File("src/test/resources/tabular/cjd-master-op.3-huesos.csv").toURI().toURL();
        // log.debug("File against which we're going to check:\n{}", Resources.toString(checkFile, UTF_8));
        final List<String> resultLines = readLines(result);
        final List<String> checkLines = readLines(checkFile);
        compareLines(resultLines, checkLines, log);
    }
}
