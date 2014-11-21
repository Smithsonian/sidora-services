
package com.asoroka.sidora.excel2tabular;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Range.closed;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

public class FilteredSheet extends ReversableIterable<Row> {

    final Sheet sheet;

    Range<Integer> dataRange;

    final RangeSet<Integer> rowsWithMergedRegions = TreeRangeSet.create();

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
            // begin by assuming that all rows may be data rows
            dataRange = closed(firstRowIndex, lastRowIndex);
            initialize();
        }
    }

    private void initialize() {

        final int numMergedRegions = sheet.getNumMergedRegions();
        for (int j = 0; j < numMergedRegions; j++) {
            final CellRangeAddress mergedRegion = sheet.getMergedRegion(j);
            final Range<Integer> mergedRegionRows =
                    closed(mergedRegion.getFirstRow(), mergedRegion.getLastRow());
            rowsWithMergedRegions.add(mergedRegionRows);
        }

        final int lastRowIndex = sheet.getLastRowNum();
        final int firstRowIndex = sheet.getFirstRowNum();
        final Row maximalRow = compareByRowLength.max(this);
        final int maximalRowIndex = maximalRow.getRowNum();
        log.trace("Found index of maximally long row at: {} with length: {}", maximalRowIndex, maximalRow
                .getLastCellNum());

        // search only after the maximal row
        dataRange = closed(maximalRowIndex, lastRowIndex);
        final int nextIgnorableRowIndex =
                from(this).firstMatch(isRowIgnored).transform(extractRowIndex).or(lastRowIndex + 1);
        final int lastDataRowIndex = nextIgnorableRowIndex - 1;

        // search only before the maximal row
        dataRange = closed(firstRowIndex, maximalRowIndex);
        final int previousIgnorableRowIndex =
                from(reversed(this)).firstMatch(isRowIgnored).transform(extractRowIndex).or(firstRowIndex - 1);
        final int firstDataRowIndex = previousIgnorableRowIndex + 1;

        dataRange = closed(firstDataRowIndex, lastDataRowIndex);
        log.trace("Found data range: {}", dataRange);
    }

    /**
     * Ignore a row if it is empty or blank or contains any part of a merged region.
     * 
     * @param row
     * @return
     */
    private Predicate<Row> isRowIgnored = new Predicate<Row>() {

        @Override
        public boolean apply(final Row row) {
            final int rowIndex = row.getRowNum();
            log.trace("Found row at index {} with {} cells and {} physical cells.", rowIndex, row.getLastCellNum(),
                    row.getPhysicalNumberOfCells());

            if (row.getPhysicalNumberOfCells() == 0) {
                return true;
            }

            if (rowsWithMergedRegions.contains(rowIndex)) {
                log.debug("Ignoring row {} for containing merged region.", rowIndex);
                return true;
            }

            if (all(row, isBlankCell)) {
                log.trace("Found all blank cells in row number {}.", rowIndex);
                return true;
            }
            return false;
        }
    };

    static final Predicate<Cell> isBlankCell = new Predicate<Cell>() {

        @Override
        public boolean apply(final Cell cell) {
            return cell.getCellType() == CELL_TYPE_BLANK;
        }
    };

    private static final Function<Row, Integer> extractRowIndex = new Function<Row, Integer>() {

        @Override
        public Integer apply(final Row r) {
            return r.getRowNum();
        }
    };

    /**
     * An {@link Ordering} that compares two {@link Row}s on the basis of their length.
     */
    private static final Ordering<Row> compareByRowLength = natural().onResultOf(new Function<Row, Short>() {

        @Override
        public Short apply(final Row r) {
            return r.getLastCellNum();
        }
    });

    private static final <E> AbstractIterator<E> EMPTY_ITERATOR() {
        return new AbstractIterator<E>() {

            @Override
            protected E computeNext() {
                return endOfData();
            }
        };
    }

    /**
     * Should never return null. A null {@link Row} in the underlying sheet is replaced with an empty row.
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public AbstractIterator<Row> iterator() {
        if (dataRange.isEmpty()) {
            return EMPTY_ITERATOR();
        }
        return new AbstractIterator<Row>() {

            private int forwardRowIndex = dataRange.lowerEndpoint();

            @Override
            protected Row computeNext() {
                if (forwardRowIndex > dataRange.upperEndpoint()) {
                    return endOfData();
                }
                final Row nextRow = sheet.getRow(forwardRowIndex++);
                final int currentRowIndex = forwardRowIndex - 1;
                if (nextRow == null) {
                    log.trace("Returning empty row with index {}", currentRowIndex);
                    return sheet.createRow(currentRowIndex);
                }
                log.trace("Returning row with index {}", currentRowIndex);
                return nextRow;
            }
        };
    }

    /**
     * Should never return null. A null {@link Row} in the underlying sheet is replaced with an empty row.
     * 
     * @see ReversableIterable#reversed()
     */
    @Override
    public AbstractIterator<Row> reversed() {
        if (dataRange.isEmpty()) {
            return EMPTY_ITERATOR();
        }
        return new AbstractIterator<Row>() {

            private int reverseRowIndex = dataRange.upperEndpoint();

            @Override
            protected Row computeNext() {
                if (reverseRowIndex < dataRange.lowerEndpoint()) {
                    return endOfData();
                }
                final Row nextRow = sheet.getRow(reverseRowIndex--);
                final int currentRowIndex = reverseRowIndex + 1;
                if (nextRow == null) {
                    log.trace("Returning empty row with index {}", currentRowIndex);
                    return sheet.createRow(currentRowIndex);
                }
                log.trace("Returning row with index {}", currentRowIndex);
                return nextRow;
            }
        };
    }
}