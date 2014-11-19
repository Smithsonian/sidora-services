
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.TabularCell.defaultQuote;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.google.common.base.Joiner;

public class TabularRow {

    private final Row row;

    private final String quote;

    private static final String defaultDelimiter = ",";

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
        final List<String> cells = new ArrayList<>();
        for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
            final Cell cell = row.getCell(cellIndex);
            cells.add(new TabularCell(cell, quote).toString());
        }
        return joiner.join(cells);
    }

}
