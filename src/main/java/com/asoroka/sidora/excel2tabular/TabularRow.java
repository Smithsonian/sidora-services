
package com.asoroka.sidora.excel2tabular;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.google.common.base.Joiner;

public class TabularRow {

    private final Row row;

    private String quote;

    private String delimiter;

    /**
     * @param row
     * @param quote
     * @param delimiter
     */
    public TabularRow(final Row row, final String quote, final String delimiter) {
        this.row = row;
        this.quote = quote;
        this.delimiter = delimiter;
    }

    @Override
    public String toString() {
        final List<String> cells = new ArrayList<>();
        for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
            final Cell cell = row.getCell(cellIndex);
            cells.add(new TabularCell(cell, quote).toString());
        }
        return joinWithDelimiter().join(cells);
    }

    private Joiner joinWithDelimiter() {
        return Joiner.on(delimiter);
    }
}
