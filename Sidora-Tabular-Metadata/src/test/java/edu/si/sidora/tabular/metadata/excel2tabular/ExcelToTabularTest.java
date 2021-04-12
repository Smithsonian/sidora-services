/*
 * Copyright 2018-2019 Smithsonian Institution.
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

package edu.si.sidora.tabular.metadata.excel2tabular;

import static edu.si.sidora.tabular.metadata.excel2tabular.Utilities.createTempFile;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.readLines;
import static com.google.common.io.Files.write;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExcelToTabularTest {

    @Test(expected = ExcelParsingException.class)
    public void testBadUrl() throws MalformedURLException {
        final File testFile = createTempFile(this);
        testFile.delete();
        new ExcelToTabular().process(testFile.toURI().toURL());
    }

    @Test(expected = ExcelParsingException.class)
    public void testBadSheet() throws IOException {
        final File testFile = createTempFile(this);
        write("THIS IS NOT A SPREADSHEET", testFile, UTF_8);
        new ExcelToTabular().process(testFile.toURI().toURL());
    }

    @Test(expected = ExcelParsingException.class)
    public void testBadFormatSheet() throws IOException {
        final File testFile = new File("src/test/resources/excel2tabular/xls/bad-format-spreadsheet.xlsx");
        new ExcelToTabular().process(testFile.toURI().toURL());
    }

    @Test
    public void testOneLineSheet() throws IOException {
        final File testFile = new File("src/test/resources/excel2tabular/xls/one-line.xls");
        final List<File> results = new ExcelToTabular().process(testFile.toURI().toURL());
        assertEquals(1, results.size());
        final File result = results.get(0);
        final List<String> lines = readLines(result, UTF_8);
        assertEquals(1, lines.size());
        final String line = lines.get(0);
        assertEquals("\"Value\",1,\"Value\"", line);
    }

    @Test
    public void testOneLineSheetWithTabDelimiter() throws IOException {
        final File testFile = new File("src/test/resources/excel2tabular/xls/one-line.xls");
        final ExcelToTabular excelToTabular = new ExcelToTabular();
        excelToTabular.setDelimiter("\t");
        final List<File> results = excelToTabular.process(testFile.toURI().toURL());
        assertEquals(1, results.size());
        final File result = results.get(0);
        final List<String> lines = readLines(result, UTF_8);
        assertEquals(1, lines.size());
        final String line = lines.get(0);
        assertEquals("\"Value\"\t1\t\"Value\"", line);
    }

    @Test
    public void testOneLineSheetWithSingleQuote() throws IOException {
        final File testFile = new File("src/test/resources/excel2tabular/xls/one-line.xls");
        final ExcelToTabular excelToTabular = new ExcelToTabular();
        excelToTabular.setDelimiter("\t");
        excelToTabular.setQuoteChar("'");
        final List<File> results = excelToTabular.process(testFile.toURI().toURL());
        assertEquals(1, results.size());
        final File result = results.get(0);
        final List<String> lines = readLines(result, UTF_8);
        assertEquals(1, lines.size());
        final String line = lines.get(0);
        assertEquals("'Value'\t1\t'Value'", line);
    }

}
