
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.TabularCell.defaultQuote;
import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.Iterables.transform;

import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.google.common.base.Joiner;

public class TabularRow implements Iterable<TabularCell> {

    final Row row;

    String quote = defaultQuote;

    public static final String defaultDelimiter = ",";

    private final Joiner joiner;

    /**
     * @param row
     * @param quote
     * @param delimiter
     */
    public TabularRow(final Row row, final String quote, final String delimiter) {
        this.row = row;
        this.quote = quote;
        this.joiner = Joiner.on(delimiter);
    }

    public TabularRow(final Row row, final String quote) {
        this(row, quote, defaultDelimiter);
    }

    public TabularRow(final Row row) {
        this(row, defaultQuote);
    }

    @Override
    public String toString() {
        return joiner.join(transform(this, toStringFunction()));
    }

    /**
     * We cannot use the iterator returned directly from the wrapped {@link Row} because it skips undefined cells, for
     * which we want to print delimited blanks.
     * 
     * @see Row#iterator()
     */
    @Override
    public Iterator<TabularCell> iterator() {
        return new Iterator<TabularCell>() {

            private int i = 0;

            private int numCells = row.getLastCellNum();

            @Override
            public boolean hasNext() {
                return i < numCells;
            }

            @Override
            public TabularCell next() {
                return new TabularCell(row.getCell(i++), quote);
            }

            @Override
            public void remove() {
                final Cell lastCellReturnedByNext = row.getCell(i - 1);
                row.removeCell(lastCellReturnedByNext);
            }
        };
    }
}
