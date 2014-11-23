
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.IsBlankRow.isBlankRow;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Range.closed;
import static com.google.common.collect.Range.openClosed;
import static java.util.Collections.emptyIterator;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;

public class FilteredSheet implements Iterable<Row> {

    final Sheet sheet;

    private final int lastRowIndex, firstRowIndex;

    Range<Integer> dataRange;

    NavigableSet<Integer> blankRows = new TreeSet<>();

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
        // only examine and process a sheet for data rows if it has any rows
        if (isEmpty(sheet)) {
            log.debug("Found no rows in sheet {}.", sheet.getSheetName());
            dataRange = EMPTY_RANGE;
            initialized = true;
        }
    }

    private void findDataRows() {
        initializing = true;

        // begin by assuming that all rows may be data rows
        dataRange = closed(firstRowIndex, lastRowIndex);

        // Because the rows in a sheet are not ordered by length, we will have to traverse all of them to find a row
        // of maximum length. This gives us an opportunity to record any blank rows at the same time.
        final Row maximalRow = compareByLengthAndRecordBlankRows.max(this);
        final int maximalRowIndex = maximalRow.getRowNum();

        if (isBlankRow(maximalRow)) {
            log.trace("The maximal row was empty, so this sheet has no data.");
            dataRange = EMPTY_RANGE;
            return;
        }
        log.trace("Found index of maximally long data row at: {} with length: {}",
                maximalRowIndex, maximalRow.getLastCellNum());
        // search for nearest blank row after the maximal row
        final Integer nextBlankRowIndex = blankRows.higher(maximalRowIndex);
        final int lastDataRowIndex = nextBlankRowIndex == null ? lastRowIndex : nextBlankRowIndex - 1;

        // search for nearest blank row before the maximal row
        final Integer previousBlankRowIndex = blankRows.lower(maximalRowIndex);
        final int firstDataRowIndex = previousBlankRowIndex == null ? firstRowIndex : previousBlankRowIndex + 1;
        dataRange = closed(firstDataRowIndex, lastDataRowIndex);
        log.trace("Found data range: {}", dataRange);
        initializing = false;
        initialized = true;
    }

    /**
     * An {@link Ordering} that compares {@link Row}s on the basis of their length and incidentally records whether a
     * row is blank .
     */
    private final Ordering<Row> compareByLengthAndRecordBlankRows = natural().onResultOf(new Function<Row, Short>() {

        @Override
        public Short apply(final Row r) {
            checkNotNull(r);
            if (isBlankRow(r)) {
                blankRows.add(r.getRowNum());
            }
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

            private int rowIndex = dataRange.lowerEndpoint();

            @Override
            protected Row computeNext() {
                if (!dataRange.contains(rowIndex)) {
                    return endOfData();
                }
                final int currentRowIndex = rowIndex;
                final Row currentRow = sheet.getRow(rowIndex++);
                if (currentRow == null) {
                    log.trace("Returning empty row with index {}", currentRowIndex);
                    return sheet.createRow(currentRowIndex);
                }
                log.trace("Returning extant row with index {}", currentRowIndex);
                return currentRow;
            }
        };
    }

    private boolean unready() {
        return !(initialized || initializing);
    }
}