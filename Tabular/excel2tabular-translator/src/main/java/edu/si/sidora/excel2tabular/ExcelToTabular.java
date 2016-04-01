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

package edu.si.sidora.excel2tabular;

import static edu.si.sidora.excel2tabular.TabularCell.defaultQuote;
import static edu.si.sidora.excel2tabular.TabularRow.defaultDelimiter;
import static edu.si.sidora.excel2tabular.Utilities.createTempFile;
import static com.google.common.collect.Iterables.size;
import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Resources.asByteSource;
import static org.apache.poi.ss.usermodel.WorkbookFactory.create;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;

/**
 * @author ajs6f
 */
public class ExcelToTabular {

    private String delimiter = defaultDelimiter;

    private String quote = defaultQuote;

    private static final Logger log = getLogger(ExcelToTabular.class);

    public List<File> process(final URL inputUrl) {

        final File spreadsheet = createTempFile(this);

        final ByteSource source = asByteSource(inputUrl);
        final ByteSink sink = asByteSink(spreadsheet);
        try {
            source.copyTo(sink);
        } catch (final IOException e) {
            throw new ExcelParsingException("Could not retrieve input URL: " + inputUrl, e);
        }

        try {
            final Worksheets worksheets = new Worksheets(create(spreadsheet));
            final List<File> outputs = new ArrayList<>(size(worksheets));

            log.trace("Translating sheets with data.");

            for (final FilteredSheet sheet : worksheets) {
                final File tabularFile = createTempFile(this);
                outputs.add(tabularFile);
                try (PrintStream output = new PrintStream(tabularFile)) {
                    for (final Row row : sheet) {
                        output.println(new TabularRow(row, quote, delimiter));
                    }
                }
            }
            spreadsheet.delete();
            return outputs;
        } catch (final InvalidFormatException e) {
            throw new ExcelParsingException(
                    "Could not parse input spreadsheet because it is not in a format registered to Apache POI: " +
                            inputUrl, e);
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
