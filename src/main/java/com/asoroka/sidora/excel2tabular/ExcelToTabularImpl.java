
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.Utilities.createTempFile;
import static com.google.common.collect.Ordering.natural;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

/**
 * @author ajs6f
 */
public class ExcelToTabularImpl implements ExcelToTabular {

    private String delimiter = ",";

    private String quoteChar = "\"";

    final private static Logger log = getLogger(ExcelToTabularImpl.class);

    /**
     * Default constructor.
     */
    public ExcelToTabularImpl() {
    }

    @Override
    public List<File> apply(final URL inputUrl) {

        final File spreadsheet = createTempFile(this);

        final ByteSource source = Resources.asByteSource(inputUrl);
        final ByteSink sink = Files.asByteSink(spreadsheet);
        try {
            source.copyTo(sink);
        } catch (final IOException e) {
            throw new ExcelParsingException("Could not retrieve input URL: " + inputUrl, e);
        }

        try (final InputStream wbInputStream = new FileInputStream(spreadsheet)) {
            final Workbook wb = WorkbookFactory.create(spreadsheet);

            final int numberOfSheets = wb.getNumberOfSheets();
            // the row-coordinates of the "raw data" in each sheet
            final List<Range<Integer>> dataRows = new ArrayList<>(numberOfSheets);

            for (int i = 0; i < numberOfSheets; i++) {
                log.debug("Examining sheet number {} for data row-range...", i);
                final Sheet sheet = wb.getSheetAt(i);
                final int maximalRowIndex = compareByRowLength.max(sheet).getRowNum();
                log.trace("Found index of maximally long row at: {}", maximalRowIndex);
                final int lastRowIndex = sheet.getLastRowNum();

                // start by assuming that the rest of the sheet is not data
                int endOfDataRange = maximalRowIndex;
                boolean noBlankRowYet = true;
                while (noBlankRowYet && endOfDataRange <= lastRowIndex) {
                    // advance the row and check whether it's blank
                    noBlankRowYet = !rowIsBlank(sheet.getRow(++endOfDataRange));
                }

                // start by assuming that the sheet has no data before the maximal row
                int beginningOfDataRange = maximalRowIndex;
                noBlankRowYet = true;
                while (noBlankRowYet && beginningOfDataRange >= 0) {
                    // retreat the row and check whether it's blank
                    noBlankRowYet = !rowIsBlank(sheet.getRow(--beginningOfDataRange));
                }
                final Range<Integer> dataRange = Range.closed(++beginningOfDataRange, --endOfDataRange);
                log.trace("Found data range: {}", dataRange);
                dataRows.add(dataRange);
            }

            final XLS2CSV poiTransformer = new XLS2CSV()
                    .delimiter(delimiter)
                    .quoteChar(quoteChar).
                    rangesPerSheet(dataRows);
            final MissingRecordAwareHSSFListener listener = new MissingRecordAwareHSSFListener(
                    poiTransformer);
            final FormatTrackingHSSFListener ftListener = new FormatTrackingHSSFListener(
                    listener);

            poiTransformer.setFormatListener(ftListener);

            final HSSFRequest request = new HSSFRequest();
            request.addListenerForAllRecords(ftListener);
            final HSSFEventFactory factory = new HSSFEventFactory();
            factory.processWorkbookEvents(request, new POIFSFileSystem(wbInputStream));
            return poiTransformer.getOutputs();

        } catch (IOException | InvalidFormatException e) {
            throw new ExcelParsingException("Could not parse input spreadsheet: " + spreadsheet, e);
        }
    }

    private static final Function<Row, Short> rowLength = new Function<Row, Short>() {

        @Override
        public Short apply(final Row r) {
            return r.getLastCellNum();
        }
    };

    private static final Ordering<Row> compareByRowLength = natural().onResultOf(rowLength);

    private static boolean rowIsBlank(final Row currentRow) {
        if (currentRow != null)
            log.trace("Checking row {} for blankness", currentRow.getRowNum());
        final boolean isBlank = currentRow == null || currentRow.getLastCellNum() == -1;
        if (isBlank) log.trace("Found blank row");
        return isBlank;
    }

    /**
     * @param delimiter the delimiter to use in output between cells
     */
    public void setDelimiter(final String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * @param quoteChar the quote string to use in output around strings. May be more than one character.
     */
    public void setQuoteChar(final String quoteChar) {
        this.quoteChar = quoteChar;
    }

}
