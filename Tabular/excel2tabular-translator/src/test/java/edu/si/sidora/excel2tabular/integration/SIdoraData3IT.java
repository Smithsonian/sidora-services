/*
 * Copyright 2015-2016 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */

package edu.si.sidora.excel2tabular.integration;

import static edu.si.sidora.excel2tabular.integration.IntegrationTestUtilities.compareLines;
import static edu.si.sidora.excel2tabular.integration.IntegrationTestUtilities.readLines;
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

import edu.si.sidora.excel2tabular.ExcelToTabular;

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
