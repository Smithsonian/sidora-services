
package com.asoroka.sidora.excel2tabular;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Ordering.natural;
import static java.util.Collections.emptyIterator;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

public class FilteredSheet extends ReversableIterable<Row> {

    Sheet sheet;

    Range<Integer> dataRange;

    RangeSet<Integer> rowsWithMergedRegions = TreeRangeSet.create();

    private static final Range<Integer> EMPTY_RANGE = Range.closed(0, 0);

    static final Logger log = getLogger(FilteredSheet.class);

    public FilteredSheet(final Sheet s) {
        this.sheet = s;

        final int lastRowIndex = sheet.getLastRowNum();
        final int firstRowIndex = sheet.getFirstRowNum();
        log.debug("Found {} rows in sheet {}.", lastRowIndex - firstRowIndex, sheet.getSheetName());

        // only examine and process a sheet if it has any rows
        if (firstRowIndex == lastRowIndex) {
            log.debug("Found no rows in sheet {}.", sheet.getSheetName());
            dataRange = EMPTY_RANGE;
        }
        else {
            dataRange = Range.closed(firstRowIndex, lastRowIndex);
            initialize();
        }
    }

    private void initialize() {

        final int numMergedRegions = sheet.getNumMergedRegions();
        for (int j = 0; j < numMergedRegions; j++) {
            final CellRangeAddress mergedRegion = sheet.getMergedRegion(j);
            final Range<Integer> mergedRegionRows =
                    Range.closed(mergedRegion.getFirstRow(), mergedRegion.getLastRow());
            rowsWithMergedRegions.add(mergedRegionRows);
        }

        final int lastRowIndex = sheet.getLastRowNum();
        final int firstRowIndex = sheet.getFirstRowNum();
        final Row maximalRow = compareByRowLength.max(sheet);
        final int maximalRowIndex = maximalRow.getRowNum();
        log.trace("Found index of maximally long row at: {} with length: {}", maximalRowIndex, maximalRow
                .getLastCellNum());

        dataRange = Range.closed(maximalRowIndex, lastRowIndex);
        final int nextIgnorableRowIndex =
                from(this).firstMatch(isRowIgnored).transform(rowIndex)
                        .or(lastRowIndex + 1);
        final int lastDataRowIndex = nextIgnorableRowIndex - 1;

        dataRange = Range.closed(firstRowIndex, maximalRowIndex);
        final int previousIgnorableRowIndex =
                from(reversed(this)).firstMatch(isRowIgnored)
                        .transform(rowIndex).or(firstRowIndex - 1);
        final int firstDataRow = previousIgnorableRowIndex + 1;

        dataRange = Range.closed(firstDataRow, lastDataRowIndex);
        log.trace("Found data range: {}", dataRange);

    }

    private Predicate<Row> isRowIgnored = new Predicate<Row>() {

        @Override
        public boolean apply(final Row row) {
            return rowIsIgnored(row);
        }
    };

    /**
     * Ignore a row if it is null (doesn't exist in the sheet), blank, or contains any part of a merged region.
     * 
     * @param row
     * @return
     */
    boolean rowIsIgnored(final Row row) {
        if (row == null) {
            return true;
        }
        final int rowNum = row.getRowNum();
        log.trace("Found row at index {} with {} cells and {} physical cells.", rowNum, row.getLastCellNum(), row
                .getPhysicalNumberOfCells());

        if (row.getPhysicalNumberOfCells() == 0) {
            return true;
        }

        if (rowsWithMergedRegions.contains(rowNum)) {
            log.debug("Ignoring row {} for containing merged region.", rowNum);
            return true;
        }

        if (all(row, isBlankCell)) {
            log.trace("Found all blank cells in row number {}.", rowNum);
            return true;
        }
        return false;
    }

    private static final Predicate<Cell> isBlankCell = new Predicate<Cell>() {

        @Override
        public boolean apply(final Cell cell) {
            return cell.getCellType() == CELL_TYPE_BLANK;
        }
    };

    private static final Function<Row, Short> rowLength = new Function<Row, Short>() {

        @Override
        public Short apply(final Row r) {
            return r.getLastCellNum();
        }
    };

    private static final Function<Row, Integer> rowIndex = new Function<Row, Integer>() {

        @Override
        public Integer apply(final Row r) {
            return r.getRowNum();
        }
    };

    private static final Ordering<Row> compareByRowLength = natural().onResultOf(rowLength);

    @Override
    public Iterator<Row> iterator() {
        if (dataRange.isEmpty()) {
            return emptyIterator();
        }
        return new Iterator<Row>() {

            private int rowIndex = dataRange.lowerEndpoint();

            @Override
            public boolean hasNext() {
                return rowIndex <= dataRange.upperEndpoint();
            }

            @Override
            public Row next() {
                final Row nextRow = sheet.getRow(rowIndex++);
                if (nextRow == null) {
                    return sheet.createRow(rowIndex - 1);
                }
                log.trace("Returning row with index {}", rowIndex - 1);
                return nextRow;
            }

            @Override
            public void remove() {
                final Row rowLastReturnedByNext = sheet.getRow(rowIndex - 1);
                sheet.removeRow(rowLastReturnedByNext);
            }
        };
    }

    @Override
    public Iterator<Row> reversed() {
        if (dataRange.isEmpty()) {
            return emptyIterator();
        }
        return new Iterator<Row>() {

            private int reverseRowIndex = dataRange.upperEndpoint();

            @Override
            public boolean hasNext() {
                return reverseRowIndex >= dataRange.lowerEndpoint();
            }

            @Override
            public Row next() {
                final Row nextRow = sheet.getRow(reverseRowIndex--);
                if (nextRow == null) {
                    return sheet.createRow(reverseRowIndex + 1);
                }
                log.trace("Returning row with index {}", reverseRowIndex + 1);
                return nextRow;
            }

            @Override
            public void remove() {
                final Row rowLastReturnedByNext = sheet.getRow(reverseRowIndex + 1);
                sheet.removeRow(rowLastReturnedByNext);
            }
        };
    }
}