
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.IsBlankRow.isBlankRow;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Range.closed;
import static com.google.common.collect.Range.openClosed;
import static java.util.Collections.emptyIterator;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;

public class FilteredSheet extends ReversableIterable<Row> {

    final Sheet sheet;

    private final int lastRowIndex, firstRowIndex;

    Range<Integer> dataRange;

    /**
     * For lazy initialization.
     */
    boolean initialized = false, initializing = false;

    /**
     * @see Range#isEmpty()
     */
    private static final Range<Integer> EMPTY_RANGE = openClosed(0, 0);

    static final Logger log = getLogger(FilteredSheet.class);

    public FilteredSheet(final Sheet s) {
        this.sheet = s;
        this.lastRowIndex = sheet.getLastRowNum();
        this.firstRowIndex = sheet.getFirstRowNum();
        // begin by assuming that all rows may be data rows
        dataRange = closed(firstRowIndex, lastRowIndex);
        log.debug("Found {} rows in sheet {}.", lastRowIndex - firstRowIndex + 1, sheet.getSheetName());

        // only examine and process a sheet for data rows if it has any rows
        if (isEmpty(sheet)) {
            log.debug("Found no rows in sheet {}.", sheet.getSheetName());
            dataRange = EMPTY_RANGE;
            initialized = true;
        }
    }

    private void findDataRows() {
        initializing = true;
        // find maximal row on which to center search
        final Row maximalRow = compareByRowLength.max(this);
        final int maximalRowIndex = maximalRow.getRowNum();

        log.trace("Found index of maximally long row at: {} with length: {}",
                maximalRowIndex, maximalRow.getLastCellNum());
        if (isBlankRow.apply(maximalRow)) {
            dataRange = EMPTY_RANGE;
            return;
        }
        log.trace("Found index of maximally long data row at: {} with length: {}",
                maximalRowIndex, maximalRow.getLastCellNum());
        // search for ignorable rows forwards only after the maximal row
        dataRange = closed(maximalRowIndex, lastRowIndex);
        final int nextIgnorableRowIndex =
                from(this).firstMatch(isBlankRow).transform(extractRowIndex).or(lastRowIndex + 1);
        log.trace("Found next ignorable row at index: {}", nextIgnorableRowIndex);
        final int lastDataRowIndex = nextIgnorableRowIndex - 1;

        // search for ignorable rows backwards only before the maximal row
        dataRange = closed(firstRowIndex, maximalRowIndex);
        final int previousIgnorableRowIndex =
                from(reversed(this)).firstMatch(isBlankRow).transform(extractRowIndex).or(firstRowIndex - 1);
        log.trace("Found previous ignorable row at index: {}", nextIgnorableRowIndex);
        final int firstDataRowIndex = previousIgnorableRowIndex + 1;

        dataRange = closed(firstDataRowIndex, lastDataRowIndex);
        log.trace("Found data range: {}", dataRange);
        initializing = false;
        initialized = true;
    }

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
        if (unready()) {
            findDataRows();
        }
        if (dataRange.isEmpty()) {
            return emptyIterator();
        }
        return new AbstractIterator<Row>() {

            private int forwardRowIndex = dataRange.lowerEndpoint();

            @Override
            protected Row computeNext() {
                if (!dataRange.contains(forwardRowIndex)) {
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
        if (unready()) {
            findDataRows();
        }
        if (dataRange.isEmpty()) {
            return emptyIterator();
        }
        return new AbstractIterator<Row>() {

            private int reverseRowIndex = dataRange.upperEndpoint();

            @Override
            protected Row computeNext() {
                if (!dataRange.contains(reverseRowIndex)) {
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

    private boolean unready() {
        return !(initialized || initializing);
    }
}