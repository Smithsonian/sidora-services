
package com.asoroka.sidora.excel2csv;

import static java.lang.Double.isNaN;
import static java.util.UUID.randomUUID;
import static org.apache.poi.hssf.record.BOFRecord.TYPE_WORKSHEET;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.eventusermodel.EventWorkbookBuilder.SheetRecordCollectingListener;
import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.eventusermodel.dummyrecord.MissingCellDummyRecord;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.slf4j.Logger;

import com.google.common.base.Joiner;

/**
 * Inspired by {@link org.apache.poi.hssf.eventusermodel.examples.XLS2CSVmra }.<br/>
 * Splits multi-sheet spreadsheets into multiple CSV outputs.
 * 
 * @author ajs6f
 */
public class XLS2CSVmra implements HSSFListener {

    private List<File> outputs = new ArrayList<>();

    private static final Logger log = getLogger(XLS2CSVmra.class);

    private String delimiter = ",";

    private String quoteChar = "\"";

    public List<File> getOutputs() {
        return outputs;
    }

    protected int minColumns, lastRowNumber, lastColumnNumber, nextRow, nextColumn;

    protected POIFSFileSystem fs;

    protected PrintStream currentOutput;

    /** For parsing Formulas */
    protected SheetRecordCollectingListener workbookBuildingListener;

    protected HSSFWorkbook stubWorkbook;

    // Records we pick up as we process
    protected SSTRecord sstRecord;

    protected FormatTrackingHSSFListener formatListener;

    public void setFormatListener(final FormatTrackingHSSFListener formatListener) {
        this.formatListener = formatListener;
    }

    /**
     * Which sheet we're on, 0-indexed. We start with -1 because we increment the count at the beginning of operation
     * over a sheet.
     */
    protected int sheetIndex = -1;

    protected ArrayList<BoundSheetRecord> boundSheetRecords = new ArrayList<>();

    protected boolean outputNextStringRecord;

    /**
     * @param poiFileSystem
     * @param minColumns
     */
    public XLS2CSVmra(final POIFSFileSystem poiFileSystem, final int minColumns) {
        this.fs = poiFileSystem;
        this.minColumns = minColumns;
    }

    /**
     * Main HSSFListener method, processes events, and outputs the CSV as the file is processed.
     */
    @Override
    public void processRecord(final Record record) {
        int thisRow = -1;
        int thisColumn = -1;
        String thisStr = null;

        switch (record.getSid()) {

        case BoundSheetRecord.sid:
            boundSheetRecords.add((BoundSheetRecord) record);
            break;

        case BOFRecord.sid:
            final BOFRecord br = (BOFRecord) record;
            if (br.getType() == TYPE_WORKSHEET) {
                // Create sub workbook if required
                if (workbookBuildingListener != null && stubWorkbook == null) {
                    stubWorkbook = workbookBuildingListener.getStubHSSFWorkbook();
                }
                // switch to a new sheet currentOutput
                sheetIndex++;
                outputs.add(createTempFile());
                // close the last sheet's PrintStream
                if (currentOutput != null) {
                    currentOutput.close();
                }
                try {
                    currentOutput = new PrintStream(outputs.get(sheetIndex));
                } catch (final FileNotFoundException e) {
                    throw new AssertionError("Could not open self-created temp file!", e);
                }
            }
            break;

        case SSTRecord.sid:
            sstRecord = (SSTRecord) record;
            break;

        case BlankRecord.sid:
            final BlankRecord brec = (BlankRecord) record;
            thisRow = brec.getRow();
            thisColumn = brec.getColumn();
            thisStr = quote();
            break;

        case BoolErrRecord.sid:
            final BoolErrRecord berec = (BoolErrRecord) record;
            thisRow = berec.getRow();
            thisColumn = berec.getColumn();
            thisStr = quote();
            break;

        case FormulaRecord.sid:
            final FormulaRecord frec = (FormulaRecord) record;
            thisRow = frec.getRow();
            thisColumn = frec.getColumn();

            if (isNaN(frec.getValue())) {
                // Formula result is a string
                // This is stored in the next record
                outputNextStringRecord = true;
                nextRow = frec.getRow();
                nextColumn = frec.getColumn();
            } else {
                thisStr = formatListener.formatNumberDateCell(frec);
            }
            break;

        case StringRecord.sid:
            if (outputNextStringRecord) {
                // String for formula
                final StringRecord srec = (StringRecord) record;
                thisStr = srec.getString();
                thisRow = nextRow;
                thisColumn = nextColumn;
                outputNextStringRecord = false;
            }
            break;

        case LabelRecord.sid:
            final LabelRecord lrec = (LabelRecord) record;

            thisRow = lrec.getRow();
            thisColumn = lrec.getColumn();
            thisStr = quote(lrec.getValue());
            break;

        case LabelSSTRecord.sid:
            final LabelSSTRecord lsrec = (LabelSSTRecord) record;

            thisRow = lsrec.getRow();
            thisColumn = lsrec.getColumn();
            if (sstRecord == null) {
                thisStr = quote("(No SST Record, can't identify string)");
            } else {
                thisStr = quote(sstRecord.getString(lsrec.getSSTIndex()).toString());
            }
            break;

        case NumberRecord.sid:
            final NumberRecord numrec = (NumberRecord) record;

            thisRow = numrec.getRow();
            thisColumn = numrec.getColumn();

            // Format
            thisStr = formatListener.formatNumberDateCell(numrec);
            break;

        case RKRecord.sid:
            final RKRecord rkrec = (RKRecord) record;

            thisRow = rkrec.getRow();
            thisColumn = rkrec.getColumn();
            thisStr = Double.toString(rkrec.getRKNumber());
            break;

        default:
            break;
        }

        // Handle new row
        if (thisRow != -1 && thisRow != lastRowNumber) {
            lastColumnNumber = -1;
        }

        // Handle missing column
        if (record instanceof MissingCellDummyRecord) {
            final MissingCellDummyRecord mc = (MissingCellDummyRecord) record;
            thisRow = mc.getRow();
            thisColumn = mc.getColumn();
            thisStr = quote();
        }

        // If we got something to print out, do so
        if (thisStr != null) {
            if (thisColumn > 0) {
                currentOutput.print(delimiter);
            }
            currentOutput.print(thisStr);
        }

        // Update column and row count
        if (thisRow > -1)
            lastRowNumber = thisRow;
        if (thisColumn > -1)
            lastColumnNumber = thisColumn;

        // Handle end of row
        if (record instanceof LastCellOfRowDummyRecord) {
            // Print out any missing delimiters if needed
            if (minColumns > 0) {
                // Columns are 0 based
                if (lastColumnNumber == -1) {
                    lastColumnNumber = 0;
                }
                for (int i = lastColumnNumber; i < (minColumns); i++) {
                    currentOutput.print(delimiter);
                }
            }

            // We're onto a new row
            lastColumnNumber = -1;

            // End the row
            currentOutput.println();
        }
    }

    private static File createTempFile() {
        try {
            return Files.createTempFile(XLS2CSVmra.class.getName(), randomUUID().toString()).toFile();
        } catch (final IOException e) {
            throw new AssertionError("Could not create temp file!", e);
        }
    }

    private String quote(final String... quotables) {
        return quoteChar + Joiner.on("").join(quotables) + quoteChar;
    }

    /**
     * @param delimiter the delimiter to use in output between cells
     */
    public XLS2CSVmra delimiter(final String d) {
        this.delimiter = d;
        return this;
    }

    /**
     * @param quoteChar the quote string to use in output around strings. May be more than one character.
     */
    public XLS2CSVmra quoteChar(final String q) {
        this.quoteChar = q;
        return this;
    }
}
