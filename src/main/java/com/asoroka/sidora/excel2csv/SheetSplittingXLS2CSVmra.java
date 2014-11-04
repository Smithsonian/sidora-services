/**
 * 
 */

package com.asoroka.sidora.excel2csv;

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.eventusermodel.dummyrecord.MissingCellDummyRecord;
import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.slf4j.Logger;

/**
 * An adaptation of {@link XLS2CSVmra} that split spreadsheet sheets into new outputs.
 * 
 * @author ajs6f
 */
public class SheetSplittingXLS2CSVmra extends XLS2CSVmra {

    private List<File> outputs = new ArrayList<>();

    private static final Logger log = getLogger(SheetSplittingXLS2CSVmra.class);

    public SheetSplittingXLS2CSVmra(final POIFSFileSystem poiFileSystem, final int minColumns) {
        super(poiFileSystem, null, minColumns);
    }

    public List<File> getOutputs() {
        return outputs;
    }

    /**
     * Main HSSFListener method, processes events, and outputs the CSV as the file is processed.
     */
    @Override
    public void processRecord(final Record record) {
        int thisRow = -1;
        int thisColumn = -1;
        String thisStr = null;

        switch (record.getSid())
        {
        case BoundSheetRecord.sid:
            boundSheetRecords.add(record);
            break;
        case BOFRecord.sid:
            final BOFRecord br = (BOFRecord) record;
            if (br.getType() == BOFRecord.TYPE_WORKSHEET) {
                // Create sub workbook if required
                if (workbookBuildingListener != null && stubWorkbook == null) {
                    stubWorkbook = workbookBuildingListener.getStubHSSFWorkbook();
                }

                // Output the worksheet name
                // Works by ordering the BSRs by the location of
                // their BOFRecords, and then knowing that we
                // process BOFRecords in byte offset order
                sheetIndex++;
                if (orderedBSRs == null) {
                    orderedBSRs = BoundSheetRecord.orderByBofPosition(boundSheetRecords);
                }
                try {
                    final File nextSheet = createTempFile();
                    outputs.add(nextSheet);
                    if (output != null) {
                        output.close();
                    }
                    output = new PrintStream(outputs.get(sheetIndex));
                } catch (final FileNotFoundException e) {
                    log.error("Could not open self-created temp file!");
                    throw new AssertionError(e);
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
            thisStr = "";
            break;
        case BoolErrRecord.sid:
            final BoolErrRecord berec = (BoolErrRecord) record;

            thisRow = berec.getRow();
            thisColumn = berec.getColumn();
            thisStr = "";
            break;

        case FormulaRecord.sid:
            final FormulaRecord frec = (FormulaRecord) record;

            thisRow = frec.getRow();
            thisColumn = frec.getColumn();

            if (outputFormulaValues) {
                if (Double.isNaN(frec.getValue())) {
                    // Formula result is a string
                    // This is stored in the next record
                    outputNextStringRecord = true;
                    nextRow = frec.getRow();
                    nextColumn = frec.getColumn();
                } else {
                    thisStr = formatListener.formatNumberDateCell(frec);
                }
            } else {
                thisStr = '"' +
                        HSSFFormulaParser.toFormulaString(stubWorkbook, frec.getParsedExpression()) + '"';
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
            thisStr = '"' + lrec.getValue() + '"';
            break;
        case LabelSSTRecord.sid:
            final LabelSSTRecord lsrec = (LabelSSTRecord) record;

            thisRow = lsrec.getRow();
            thisColumn = lsrec.getColumn();
            if (sstRecord == null) {
                thisStr = '"' + "(No SST Record, can't identify string)" + '"';
            } else {
                thisStr = '"' + sstRecord.getString(lsrec.getSSTIndex()).toString() + '"';
            }
            break;
        case NoteRecord.sid:
            final NoteRecord nrec = (NoteRecord) record;

            thisRow = nrec.getRow();
            thisColumn = nrec.getColumn();
            // TODO: Find object to match nrec.getShapeId()
            thisStr = '"' + "(TODO)" + '"';
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
            thisStr = '"' + "(TODO)" + '"';
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
            thisStr = "";
        }

        // If we got something to print out, do so
        if (thisStr != null) {
            if (thisColumn > 0) {
                output.print(',');
            }
            output.print(thisStr);
        }

        // Update column and row count
        if (thisRow > -1)
            lastRowNumber = thisRow;
        if (thisColumn > -1)
            lastColumnNumber = thisColumn;

        // Handle end of row
        if (record instanceof LastCellOfRowDummyRecord) {
            // Print out any missing commas if needed
            if (minColumns > 0) {
                // Columns are 0 based
                if (lastColumnNumber == -1) {
                    lastColumnNumber = 0;
                }
                for (int i = lastColumnNumber; i < (minColumns); i++) {
                    output.print(',');
                }
            }

            // We're onto a new row
            lastColumnNumber = -1;

            // End the row
            output.println();
        }
    }

    private static File createTempFile() {
        try {
            return Files.createTempFile(SheetSplittingXLS2CSVmra.class.getName(), randomUUID().toString()).toFile();
        } catch (final IOException e) {
            log.error("Could not create temp file!");
            throw new AssertionError(e);
        }
    }
}
