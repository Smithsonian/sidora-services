
package com.asoroka.sidora.excel2tabular.integration;

import static com.asoroka.sidora.excel2tabular.integration.IntegrationTestUtilities.compareLines;
import static com.asoroka.sidora.excel2tabular.integration.IntegrationTestUtilities.readLines;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Test;

import com.asoroka.sidora.excel2tabular.ExcelToTabular;

public class SIdoraData1IT {

    private final ExcelToTabular testExcel2Csv = new ExcelToTabular();

    @Test
    public void test() throws IOException {
        final URL inputUrl = new File("src/test/resources/xls/cjd-master-op.3-huesos.xls").toURI().toURL();
        final URL result = testExcel2Csv.process(inputUrl).get(0).toURI().toURL();
        final URL checkFile = new File("src/test/resources/tabular/cjd-master-op.3-huesos.csv").toURI().toURL();
        final List<String> resultLines = readLines(result);
        final List<String> checkLines = readLines(checkFile);
        assertEquals(checkLines.size(), resultLines.size());
        compareLines(checkLines, resultLines);
    }
}
