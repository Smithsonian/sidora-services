
package com.asoroka.sidora.excel2tabular;

import static org.apache.poi.hssf.record.BOFRecord.TYPE_WORKBOOK;
import static org.apache.poi.hssf.record.BOFRecord.TYPE_WORKSHEET;
import static org.apache.poi.hssf.record.RecordFactory.convertBlankRecords;
import static org.apache.poi.hssf.record.RecordFactory.convertRKRecords;

import org.apache.poi.hssf.eventusermodel.AbortableHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFUserException;
import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.eventusermodel.dummyrecord.MissingCellDummyRecord;
import org.apache.poi.hssf.eventusermodel.dummyrecord.MissingRowDummyRecord;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.MulBlankRecord;
import org.apache.poi.hssf.record.MulRKRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RowRecord;
import org.apache.poi.hssf.record.SharedFormulaRecord;
import org.apache.poi.hssf.record.StringRecord;

/**
 * <p>
 * A HSSFListener which tracks rows and columns, and will trigger a child HSSFListener for all rows and cells, even
 * the ones that aren't actually stored in the file. This allows client code to have a more "Excel" like view of the
 * data in the file, and not have to worry (as much) about if a particular row/cell is in the file, or was skipped
 * from being written as it was blank.
 * </p>
 * This logic was taken over from {@link org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener}, which
 * does not correctly handle abortable workflow, but is a final class and cannot be more elegantly extended.
 */
public final class AbortableMissingRecordAwareHSSFListener extends AbortableHSSFListener {

    private AbortableHSSFListener childListener;

    // Need to have different counters for cell rows and
    // row rows, as you sometimes get a RowRecord in the
    // middle of some cells, and that'd break everything
    private int lastRowRow;

    private int lastCellRow;

    private int lastCellColumn;

    /**
     * Constructs a new MissingRecordAwareHSSFListener, which will fire processRecord on the supplied child
     * HSSFListener for all Records, and missing records.
     * 
     * @param listener The HSSFListener to pass records on to
     */
    public AbortableMissingRecordAwareHSSFListener(final AbortableHSSFListener listener) {
        resetCounts();
        childListener = listener;
    }

    @Override
    public short abortableProcessRecord(final Record record) throws HSSFUserException {
        short result = 0;
        int thisRow;
        int thisColumn;
        CellValueRecordInterface[] expandedRecords = null;

        if (record instanceof CellValueRecordInterface) {
            final CellValueRecordInterface valueRec = (CellValueRecordInterface) record;
            thisRow = valueRec.getRow();
            thisColumn = valueRec.getColumn();
        } else {
            if (record instanceof StringRecord) {
                // it contains only cached result of the previous FormulaRecord evaluation
                result += childListener.abortableProcessRecord(record);
            }
            thisRow = -1;
            thisColumn = -1;

            switch (record.getSid()) {
            // the BOFRecord can represent either the beginning of a sheet or
            // the workbook
            case BOFRecord.sid:
                final BOFRecord bof = (BOFRecord) record;
                if (bof.getType() == TYPE_WORKBOOK || bof.getType() == TYPE_WORKSHEET) {
                    // Reset the row and column counts - new workbook / worksheet
                    resetCounts();
                }
                break;
            case RowRecord.sid:
                final RowRecord rowrec = (RowRecord) record;
                // System.out.println("Row " + rowrec.getRowNumber() + " found, first column at "
                // + rowrec.getFirstCol() + " last column at " + rowrec.getLastCol());

                // If there's a jump in rows, fire off missing row records
                if (lastRowRow + 1 < rowrec.getRowNumber()) {
                    for (int i = (lastRowRow + 1); i < rowrec.getRowNumber(); i++) {
                        final MissingRowDummyRecord dr = new MissingRowDummyRecord(i);
                        result += childListener.abortableProcessRecord(dr);
                    }
                }

                // Record this as the last row we saw
                lastRowRow = rowrec.getRowNumber();
                break;

            case SharedFormulaRecord.sid:
                // SharedFormulaRecord occurs after the first FormulaRecord of the cell range.
                // There are probably (but not always) more cell records after this
                // - so don't fire off the LastCellOfRowDummyRecord yet
                result += childListener.abortableProcessRecord(record);
                //$FALL-THROUGH$
            case MulBlankRecord.sid:
                // These appear in the middle of the cell records, to
                // specify that the next bunch are empty but styled
                // Expand this out into multiple blank cells
                final MulBlankRecord mbr = (MulBlankRecord) record;
                expandedRecords = convertBlankRecords(mbr);
                break;
            case MulRKRecord.sid:
                // This is multiple consecutive number cells in one record
                // Exand this out into multiple regular number cells
                final MulRKRecord mrk = (MulRKRecord) record;
                expandedRecords = convertRKRecords(mrk);
                break;
            case NoteRecord.sid:
                final NoteRecord nrec = (NoteRecord) record;
                thisRow = nrec.getRow();
                thisColumn = nrec.getColumn();
                break;
            }
        }

        // First part of expanded record handling
        if (expandedRecords != null && expandedRecords.length > 0) {
            thisRow = expandedRecords[0].getRow();
            thisColumn = expandedRecords[0].getColumn();
        }

        // If we're on cells, and this cell isn't in the same
        // row as the last one, then fire the
        // dummy end-of-row records
        if (thisRow != lastCellRow && lastCellRow > -1) {
            for (int i = lastCellRow; i < thisRow; i++) {
                int cols = -1;
                if (i == lastCellRow) {
                    cols = lastCellColumn;
                }
                result += childListener.abortableProcessRecord(new LastCellOfRowDummyRecord(i, cols));
            }
        }

        // If we've just finished with the cells, then fire the
        // final dummy end-of-row record
        if (lastCellRow != -1 && lastCellColumn != -1 && thisRow == -1) {
            result += childListener.abortableProcessRecord(new LastCellOfRowDummyRecord(lastCellRow, lastCellColumn));

            lastCellRow = -1;
            lastCellColumn = -1;
        }

        // If we've moved onto a new row, the ensure we re-set
        // the column counter
        if (thisRow != lastCellRow) {
            lastCellColumn = -1;
        }

        // If there's a gap in the cells, then fire
        // the dummy cell records
        if (lastCellColumn != thisColumn - 1) {
            for (int i = lastCellColumn + 1; i < thisColumn; i++) {
                result += childListener.abortableProcessRecord(new MissingCellDummyRecord(thisRow, i));
            }
        }

        // Next part of expanded record handling
        if (expandedRecords != null && expandedRecords.length > 0) {
            thisColumn = expandedRecords[expandedRecords.length - 1].getColumn();
        }

        // Update cell and row counts as needed
        if (thisColumn != -1) {
            lastCellColumn = thisColumn;
            lastCellRow = thisRow;
        }

        // Pass along the record(s)
        if (expandedRecords != null && expandedRecords.length > 0) {
            for (final CellValueRecordInterface r : expandedRecords) {
                result += childListener.abortableProcessRecord((Record) r);
            }
        } else {
            result += childListener.abortableProcessRecord(record);
        }
        return result;
    }

    private void resetCounts() {
        lastRowRow = -1;
        lastCellRow = -1;
        lastCellColumn = -1;
    }
}
