
package com.asoroka.sidora.excel2csv;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.readLines;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;

import com.google.common.io.Resources;

public class Excel2CsvImplTest extends TestUtils {

    private final Excel2Csv testExcel2Csv = new Excel2CsvImpl();

    private static final Logger log = getLogger(Excel2CsvImplTest.class);

    @Test
    public void testOneSheetFile() throws IOException {
        final URL inputUrl = new File("src/test/resources/xls/small-test.xls").toURI().toURL();
        final URL result = testExcel2Csv.apply(inputUrl).get(0).toURI().toURL();
        log.debug("Result of extraction:\n{}", Resources.toString(result, UTF_8));
        final URL checkFile = new File("src/test/resources/tabular/small-test.csv").toURI().toURL();
        log.debug("File against which we're going to check:\n{}", Resources.toString(checkFile, UTF_8));

        final List<String> resultLines = readLines(result, UTF_8);
        final List<String> checkLines = readLines(checkFile, UTF_8);
        for (final Pair<String, String> line : zip(checkLines, resultLines)) {
            assertEquals("Got bad line in results!", line.a, line.b);
        }
    }

    @Test
    public void testThreeSheetFile() throws IOException {
        final URL inputUrl = new File("src/test/resources/xls/three-sheet.xls").toURI().toURL();
        final List<File> results = testExcel2Csv.apply(inputUrl);
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
        log.trace("Got {} checkfiles.", checkFiles.length);
        for (int i = 0; i < 3; i++) {
            log.debug("Using {} for checkfile.", checkFiles[i]);
            final URL checkFile = checkFiles[i].toURI().toURL();
            log.debug("File against which we're going to check:\n{}", Resources.toString(checkFile, UTF_8));

            final List<String> resultLines = readLines(results.get(i).toURI().toURL(), UTF_8);
            final List<String> checkLines = readLines(checkFile, UTF_8);
            for (final Pair<String, String> line : zip(checkLines, resultLines)) {
                assertEquals("Got bad line in results!", line.a, line.b);
            }
        }
    }
}
