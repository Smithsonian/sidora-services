
package com.asoroka.sidora.excel2tabular;

import static com.google.common.base.Charsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.excel2tabular.ExcelToTabular;
import com.asoroka.sidora.excel2tabular.ExcelToTabularImpl;
import com.google.common.io.Resources;

public class RaggedEdgesTest extends TestUtils {

    private final ExcelToTabular testExcel2Csv = new ExcelToTabularImpl();

    private static final Logger log = getLogger(RaggedEdgesTest.class);

    @Test
    public void testOneSheetFile() throws IOException {
        final URL inputUrl = new File("src/test/resources/xls/ragged-edges-test.xls").toURI().toURL();
        final URL result = testExcel2Csv.apply(inputUrl).get(0).toURI().toURL();
        log.debug("Result of extraction:\n{}", Resources.toString(result, UTF_8));

        final URL checkFile = new File("src/test/resources/tabular/ragged-edges-test.csv").toURI().toURL();
        log.debug("File against which we're going to check:\n{}", Resources.toString(checkFile, UTF_8));
        final List<String> resultLines = readLines(result);
        final List<String> checkLines = readLines(checkFile);
        compareLines(resultLines, checkLines);
    }
}
