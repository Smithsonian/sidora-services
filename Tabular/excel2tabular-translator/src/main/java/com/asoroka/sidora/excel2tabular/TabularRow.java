
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.TabularCell.defaultQuote;
import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.Iterables.transform;

import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.google.common.base.Joiner;
import com.google.common.collect.AbstractIterator;

public class TabularRow implements Iterable<TabularCell> {

    final Row row;

    final int numCells;

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
        this.numCells = row.getLastCellNum();
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
     * which we want to return blank cells instead.
     * 
     * @see Row#iterator()
     */
    @Override
    public Iterator<TabularCell> iterator() {
        return new AbstractIterator<TabularCell>() {

            private int i = 0;

            @Override
            protected TabularCell computeNext() {
                if (i >= numCells) {
                    return endOfData();
                }
                final Cell nextCell = row.getCell(i++);
                return new TabularCell(nextCell, quote);
            }
        };
    }
}
