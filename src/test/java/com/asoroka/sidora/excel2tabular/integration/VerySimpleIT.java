
package com.asoroka.sidora.excel2tabular.integration;

import static com.google.common.base.Charsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.excel2tabular.ExcelToTabular;
import com.google.common.io.Resources;

public class VerySimpleIT extends TestUtils {

    private final ExcelToTabular testExcel2Csv = new ExcelToTabular();

    private static final Logger log = getLogger(VerySimpleIT.class);

    @Test
    public void testOneSheetFile() throws IOException {
        final URL inputUrl = new File("src/test/resources/xls/small-test.xls").toURI().toURL();
        final URL result = testExcel2Csv.process(inputUrl).get(0).toURI().toURL();
        log.debug("Result of extraction:\n{}", Resources.toString(result, UTF_8));
        final URL checkFile = new File("src/test/resources/tabular/small-test.csv").toURI().toURL();
        log.debug("File against which we're going to check:\n{}", Resources.toString(checkFile, UTF_8));

        final List<String> resultLines = readLines(result);
        final List<String> checkLines = readLines(checkFile);
        compareLines(checkLines, resultLines, log);
    }

    @Test
    public void testOneSheetFileToTabbed() throws IOException {
        final ExcelToTabular testExcel2Tabbed = new ExcelToTabular();
        testExcel2Tabbed.setDelimiter("\t");

        final URL inputUrl = new File("src/test/resources/xls/small-test.xls").toURI().toURL();
        final URL result = testExcel2Tabbed.process(inputUrl).get(0).toURI().toURL();
        log.debug("Result of extraction:\n{}", Resources.toString(result, UTF_8));
        final URL checkFile = new File("src/test/resources/tabular/small-test.tsv").toURI().toURL();
        log.debug("File against which we're going to check:\n{}", Resources.toString(checkFile, UTF_8));

        final List<String> resultLines = readLines(result);
        final List<String> checkLines = readLines(checkFile);
        compareLines(checkLines, resultLines, log);
    }

    @Test
    public void testThreeSheetFile() throws IOException {
        final URL inputUrl = new File("src/test/resources/xls/three-sheet.xls").toURI().toURL();
        final List<File> results = testExcel2Csv.process(inputUrl);
        log.debug("Results of extraction:\n");
        for (final File result : results) {
            log.debug("\n{}", Resources.toString(result.toURI().toURL(), UTF_8));
        }

        final File[] checkFiles =
                new File("src/test/resources/tabular/three-sheet-csvs").listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(final File dir, final String name) {
                        return name.startsWith("three-sheet");
                    }
                });
        for (int i = 0; i < checkFiles.length; i++) {
            log.debug("Using {} for checkfile.", checkFiles[i]);
            final URL checkFile = checkFiles[i].toURI().toURL();
            log.debug("File against which we're going to check:\n{}", Resources.toString(checkFile, UTF_8));

            final List<String> resultLines = readLines(results.get(i).toURI().toURL());
            final List<String> checkLines = readLines(checkFile);
            compareLines(checkLines, resultLines, log);
        }
    }
}
