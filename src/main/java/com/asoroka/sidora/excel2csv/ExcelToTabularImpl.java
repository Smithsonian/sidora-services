
package com.asoroka.sidora.excel2csv;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.slf4j.Logger;

/**
 * @author ajs6f
 */
public class ExcelToTabularImpl implements ExcelToTabular {

    private final int minColumns;

    private String delimiter = ",";

    private String quoteChar = "\"";

    final private static Logger log = getLogger(ExcelToTabularImpl.class);

    /**
     * Default constructor.
     */
    public ExcelToTabularImpl() {
        this.minColumns = -1;
    }

    /**
     * @param minColumns
     */
    public ExcelToTabularImpl(final int minColumns) {
        this.minColumns = minColumns;
    }

    @Override
    public List<File> apply(final URL inputUrl) {

        try (final InputStream inputStream = inputUrl.openStream()) {
            final POIFSFileSystem poiFileSystem = new POIFSFileSystem(inputStream);

            final XLS2CSV poiTransformer =
                    new XLS2CSV(poiFileSystem, minColumns).delimiter(delimiter).quoteChar(quoteChar);

            final MissingRecordAwareHSSFListener listener = new MissingRecordAwareHSSFListener(
                    poiTransformer);
            final FormatTrackingHSSFListener ftListener = new FormatTrackingHSSFListener(
                    listener);

            poiTransformer.setFormatListener(ftListener);

            final HSSFRequest request = new HSSFRequest();
            request.addListenerForAllRecords(ftListener);
            final HSSFEventFactory factory = new HSSFEventFactory();
            factory.processWorkbookEvents(request, poiFileSystem);
            return poiTransformer.getOutputs();

        } catch (final IOException e) {
            throw new ExcelParsingException("Could not process input URL: " + inputUrl, e);
        }
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
