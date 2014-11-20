
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.TabularCell.defaultQuote;
import static com.asoroka.sidora.excel2tabular.TabularRow.defaultDelimiter;
import static com.asoroka.sidora.excel2tabular.Utilities.createTempFile;
import static com.google.common.collect.Iterables.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

/**
 * @author ajs6f
 */
public class ExcelToTabular {

    private String delimiter = defaultDelimiter;

    private String quote = defaultQuote;

    private static final Logger log = getLogger(ExcelToTabular.class);

    public List<File> process(final URL inputUrl) {

        final File spreadsheet = createTempFile(this);

        final ByteSource source = Resources.asByteSource(inputUrl);
        final ByteSink sink = Files.asByteSink(spreadsheet);
        try {
            source.copyTo(sink);
        } catch (final IOException e) {
            throw new ExcelParsingException("Could not retrieve input URL: " + inputUrl, e);
        }

        try {
            final Workbook wb = WorkbookFactory.create(spreadsheet);

            final int numberOfSheets = wb.getNumberOfSheets();
            final List<File> outputs = new ArrayList<>(numberOfSheets);

            log.trace("Translating sheets with data.");

            for (int sheetIndex = 0; sheetIndex < numberOfSheets; sheetIndex++) {
                final FilteredSheet filteredSheet = new FilteredSheet(wb.getSheetAt(sheetIndex));

                // we only need to operate over those sheets with rows in them
                if (!isEmpty(filteredSheet)) {
                    final File tabularFile = createTempFile(this);
                    outputs.add(tabularFile);

                    try (PrintStream output = new PrintStream(tabularFile)) {
                        for (final Row row : filteredSheet) {
                            output.println(new TabularRow(row, quote, delimiter));
                        }
                    }
                }
            }
            return outputs;
        } catch (final InvalidFormatException e) {
            throw new ExcelParsingException("Could not parse input spreadsheet: " + inputUrl, e);
        } catch (final IOException e) {
            throw new ExcelParsingException("Could not translate input spreadsheet: " + inputUrl, e);
        }
    }

    /**
     * @param delimiter the delimiter to use in output between cells
     */
    public void setDelimiter(final String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * @param quote the quote string to use in output around strings. May be more than one character.
     */
    public void setQuoteChar(final String quoteChar) {
        this.quote = quoteChar;
    }
}
