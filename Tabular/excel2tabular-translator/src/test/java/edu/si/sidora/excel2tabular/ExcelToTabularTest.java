
package edu.si.sidora.excel2tabular;

import static edu.si.sidora.excel2tabular.Utilities.createTempFile;
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
import org.mockito.runners.MockitoJUnitRunner;

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
        final File testFile = new File("src/test/resources/xls/bad-format-spreadsheet.xlsx");
        new ExcelToTabular().process(testFile.toURI().toURL());
    }

    @Test
    public void testOneLineSheet() throws IOException {
        final File testFile = new File("src/test/resources/xls/one-line.xls");
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
        final File testFile = new File("src/test/resources/xls/one-line.xls");
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
        final File testFile = new File("src/test/resources/xls/one-line.xls");
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
