
package com.asoroka.sidora.csvmetadata.statistics;

import static com.google.common.collect.Iterators.advance;
import static com.google.common.collect.Iterators.cycle;
import static com.google.common.collect.Iterators.peekingIterator;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;

import com.asoroka.sidora.csvmetadata.heuristics.DataTypeHeuristic;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

/**
 * The heart of statistical workflow. Handed a {@link CSVParser}, this class will scan through it and supply the
 * values of fields to a "row" of {@link DataTypeHeuristic} strategies cloned from the configured choice.
 * 
 * @author ajs6f
 */
public class CsvScanner extends AbstractIterator<CSVRecord> {

    private final PeekingIterator<CSVRecord> internalScanner;

    private final Iterator<DataTypeHeuristic<?>> strategies;

    private final List<DataTypeHeuristic<?>> rowOfStrategies;

    private static final Logger log = getLogger(CsvScanner.class);

    public CsvScanner(final CSVParser parser, final DataTypeHeuristic<?> strategy) {
        super();
        this.internalScanner = peekingIterator(parser.iterator());
        final int numColumns = internalScanner.peek().size();

        log.debug("Found {} columns in our CSV.", numColumns);
        // a "row" of strategy instances of the same length as the rows in our CSV
        this.rowOfStrategies = newArrayListWithCapacity(numColumns);
        for (int i = 0; i < numColumns; i++) {
            rowOfStrategies.add(strategy.clone());
        }
        // this.strategies will cycle endlessly around our row
        this.strategies = cycle(rowOfStrategies);
    }

    @Override
    protected CSVRecord computeNext() {
        if (internalScanner.hasNext()) {
            final CSVRecord nextRecord = internalScanner.next();
            for (final String value : nextRecord) {
                strategies.next().addValue(value);
            }
            return nextRecord;
        }
        return endOfData();
    }

    /**
     * Scan rows in our CSV up to a limit.
     * 
     * @param limit The number of rows to scan, 0 for all rows.
     */
    public void scan(final int limit) {
        if (limit == 0) {
            while (hasNext()) {
                next();
            }
        } else {
            advance(this, limit);
        }
    }

    /**
     * Use this to recover and interrogate the strategy instances used in scanning.
     * 
     * @return the row of strategies used to determine the types of fields
     */
    public List<DataTypeHeuristic<?>> getStrategies() {
        return rowOfStrategies;
    }

}
