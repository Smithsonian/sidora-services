
package com.asoroka.sidora.excel2csv;

import static com.google.common.base.Throwables.propagate;
import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.compile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener;
import org.apache.poi.hssf.eventusermodel.examples.XLS2CSVmra;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.slf4j.Logger;

/**
 * @author ajs6f
 */
public class Excel2CsvImpl implements Excel2Csv {

    private final int minColumns;

    private final static Pattern sheetMarker = compile("Sheet(\\d+) \\[(\\d+)\\]:");

    final private static Logger log = getLogger(Excel2CsvImpl.class);

    /**
     * Default constructor.
     */
    public Excel2CsvImpl() {
        this.minColumns = -1;
    }

    /**
     * @param minColumns
     */
    public Excel2CsvImpl(final int minColumns) {
        this.minColumns = minColumns;
    }

    @Override
    public File apply(final URL inputUrl) {

        final File tmpFile = createTempFile();

        final POIFSFileSystem poiFileSystem;
        try (final InputStream inputStream = inputUrl.openStream()) {
            poiFileSystem = new POIFSFileSystem(inputStream);

            try (final PrintStream outputStream = new PrintStream(tmpFile)) {

                final XLS2CSVmra poiTransformer = new XLS2CSVmra(poiFileSystem,
                        outputStream, minColumns);
                final MissingRecordAwareHSSFListener listener = new MissingRecordAwareHSSFListener(
                        poiTransformer);
                final FormatTrackingHSSFListener ftListener = new FormatTrackingHSSFListener(
                        listener);

                Field formatListener;
                try {
                    formatListener = XLS2CSVmra.class.getDeclaredField("formatListener");
                    formatListener.setAccessible(true);
                    formatListener.set(poiTransformer, ftListener);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new AssertionError(e);
                }

                final HSSFRequest request = new HSSFRequest();
                request.addListenerForAllRecords(ftListener);
                final HSSFEventFactory factory = new HSSFEventFactory();
                factory.processWorkbookEvents(request, poiFileSystem);

            } catch (final FileNotFoundException e) {
                log.error("Could not open self-created temp file!");
                throw new AssertionError(e);
            }

            final File outputFile = createTempFile();

            try (InputStream tmpStream = tmpFile.toURI().toURL().openStream();
                    OutputStream outStream = new FileOutputStream(outputFile);
                    final Scanner lines = new Scanner(tmpStream)) {
                while (lines.hasNextLine()) {
                    final String line = lines.nextLine();
                    if (!line.isEmpty()) {
                        // don't accept sheet designators
                        if (!sheetMarker.matcher(line).matches()) {
                            outStream.write((line + "\n").getBytes());
                        }
                    }
                }
            }
            return outputFile;
        } catch (final IOException e) {
            log.error("Could not read input URL: {}!", inputUrl);
            throw propagate(e);
        }
    }

    private static File createTempFile() {
        try {
            return Files.createTempFile(Excel2CsvImpl.class.getName(), randomUUID().toString()).toFile();
        } catch (final IOException e) {
            log.error("Could not create temp file!");
            throw new AssertionError(e);
        }
    }

}
