
package com.asoroka.sidora.excel2tabular.integration;

import static com.asoroka.sidora.excel2tabular.integration.IntegrationTestUtilities.compareLines;
import static com.asoroka.sidora.excel2tabular.integration.IntegrationTestUtilities.readLines;
import static com.google.common.io.Files.getNameWithoutExtension;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.sort;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.excel2tabular.ExcelToTabular;

public class SIdoraData3IT {

    private final ExcelToTabular testExcel2Csv = new ExcelToTabular();

    private static final Logger log = getLogger(SIdoraData3IT.class);

    @Test
    public void test() throws IOException {
        final URL inputUrl = new File("src/test/resources/xls/ArtefactosCJD.xls").toURI().toURL();
        final List<File> results = testExcel2Csv.process(inputUrl);

        final File[] checkFiles =
                new File("src/test/resources/tabular/ArtefactosCJD-csvs").listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(final File dir, final String name) {
                        return name.endsWith(".csv");
                    }
                });
        sort(checkFiles);
        for (final File checkFile : checkFiles) {
            log.debug("Using {} for check file.", checkFile);
            final URL checkFileUrl = checkFile.toURI().toURL();
            final int sheetNumber = parseInt(getNameWithoutExtension(checkFile.getName()));
            log.trace("Checking against result sheet number: {}", sheetNumber);
            final File resultFile = results.get(sheetNumber - 1);
            log.trace("Reading translated sheet number {} from: {}", sheetNumber, resultFile.getAbsolutePath());

            final URL resultFileUrl = resultFile.toURI().toURL();
            final List<String> resultLines = readLines(resultFileUrl);
            final List<String> checkLines = readLines(checkFileUrl);

            assertEquals("Got mismatched number of lines for sheet number: " + sheetNumber, checkLines.size(),
                    resultLines.size());
            compareLines(checkLines, resultLines);
        }
    }
}
