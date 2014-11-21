
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.IsBlankRow.isBlankRow;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Range.closed;
import static com.google.common.collect.Range.openClosed;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.emptyIterator;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

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

    private final int lastRowIndex, firstRowIndex;

    Range<Integer> dataRange;

    final RangeSet<Integer> rowsWithMergedRegions = TreeRangeSet.create();

    /**
     * @see Range#isEmpty()
     */
    private static final Range<Integer> EMPTY_RANGE = openClosed(0, 0);

    static final Logger log = getLogger(FilteredSheet.class);

    public FilteredSheet(final Sheet s) {
        this.sheet = s;
        this.lastRowIndex = sheet.getLastRowNum();
        this.firstRowIndex = sheet.getFirstRowNum();

        log.debug("Found {} rows in sheet {}.", lastRowIndex - firstRowIndex, sheet.getSheetName());

        // only examine and process a sheet for data rows if it has any rows
        if (isEmpty(sheet)) {
            log.debug("Found no rows in sheet {}.", sheet.getSheetName());
            dataRange = EMPTY_RANGE;
        }
        else {
            // begin by assuming that all rows may be data rows
            dataRange = closed(firstRowIndex, lastRowIndex);
            findDataRows();
        }
    }

    private void findDataRows() {

        // record any merged regions to exclude rows that intersect them
        final int numMergedRegions = sheet.getNumMergedRegions();
        for (int mergedRegionIndex = 0; mergedRegionIndex < numMergedRegions; mergedRegionIndex++) {
            final CellRangeAddress mergedRegion = sheet.getMergedRegion(mergedRegionIndex);
            final Range<Integer> mergedRegionRows =
                    closed(mergedRegion.getFirstRow(), mergedRegion.getLastRow());
            rowsWithMergedRegions.add(mergedRegionRows);
        }

        final Row maximalRow = compareByRowLength.max(this);
        final int maximalRowIndex = maximalRow.getRowNum();
        log.trace("Found index of maximally long row at: {} with length: {}",
                maximalRowIndex, maximalRow.getLastCellNum());

        // search for ignorable rows forwards only after the maximal row
        dataRange = closed(maximalRowIndex, lastRowIndex);
        final int nextIgnorableRowIndex =
                from(this).firstMatch(isRowIgnored).transform(extractRowIndex).or(lastRowIndex + 1);
        final int lastDataRowIndex = max(nextIgnorableRowIndex - 1, firstRowIndex);

        // search for ignorable rows backwards only before the maximal row
        dataRange = closed(firstRowIndex, maximalRowIndex);
        final int previousIgnorableRowIndex =
                from(reversed(this)).firstMatch(isRowIgnored).transform(extractRowIndex).or(firstRowIndex - 1);
        final int firstDataRowIndex = min(previousIgnorableRowIndex + 1, lastRowIndex);

        dataRange = closed(firstDataRowIndex, lastDataRowIndex);
        log.trace("Found data range: {}", dataRange);
    }

    /**
     * Ignore a row if it is blank or contains any part of a merged region.
     * 
     * @param row
     * @return
     */
    private Predicate<Row> isRowIgnored = or(isBlankRow,
            new Predicate<Row>() {

                @Override
                public boolean apply(final Row row) {
                    return rowsWithMergedRegions.contains(row.getRowNum());
                }
            });

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

    /**
     * Iterates through {@link Row}s from {@link #sheet} bounded by {@link #dataRange}.<br/>
     * Should never return null. A null {@link Row} in the underlying sheet is returned as an empty row.
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Row> iterator() {
        if (dataRange.isEmpty()) {
            return emptyIterator();
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
                log.trace("Returning extant row with index {}", currentRowIndex);
                return nextRow;
            }
        };
    }

    /**
     * Iterates through {@link Row}s from {@link #sheet} bounded by {@link #dataRange}, from
     * {@link Range#upperEndpoint()} through {@link Range#lowerEndpoint()} .<br/>
     * Should never return null. A null {@link Row} in the underlying sheet is returned as an empty row.
     * 
     * @see ReversableIterable#reversed()
     */
    @Override
    public Iterator<Row> reversed() {
        if (dataRange.isEmpty()) {
            return emptyIterator();
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
                log.trace("Returning extant row with index {}", currentRowIndex);
                return nextRow;
            }
        };
    }
}