/*
 * Copyright 2018-2019 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */

package edu.si.sidora.tabular.metadata.excel2tabular;

import static edu.si.sidora.tabular.metadata.excel2tabular.TabularCell.defaultQuote;
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
