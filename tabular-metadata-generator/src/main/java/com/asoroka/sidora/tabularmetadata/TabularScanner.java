
package com.asoroka.sidora.tabularmetadata;

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

import com.asoroka.sidora.tabularmetadata.heuristics.ranges.RangeDeterminingHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.types.TypeDeterminingHeuristic;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

/**
 * Value scanning workflow. Handed a {@link CSVParser}, this class will scan through it and supply the values of
 * fields to a configured "row" of {@link ValueHeuristic} strategies cloned from the configured choice.
 * 
 * @author ajs6f
 */
public class TabularScanner extends AbstractIterator<CSVRecord> {

    private final PeekingIterator<CSVRecord> internalScanner;

    private final Iterator<TypeDeterminingHeuristic<?>> typeStrategies;

    private final List<TypeDeterminingHeuristic<?>> rowOfTypeStrategies;

    private final Iterator<RangeDeterminingHeuristic<?>> rangeStrategies;

    private final List<RangeDeterminingHeuristic<?>> rowOfRangeStrategies;

    private static final Logger log = getLogger(TabularScanner.class);

    public TabularScanner(final CSVParser parser, final TypeDeterminingHeuristic<?> typeStrategy,
            final RangeDeterminingHeuristic<?> rangeStrategy) {
        this.internalScanner = peekingIterator(parser.iterator());
        final int numColumns = internalScanner.peek().size();

        log.debug("Found {} columns in our data.", numColumns);
        // create a "row" of type strategy instances of the same length as the rows in our data
        this.rowOfTypeStrategies = newArrayListWithCapacity(numColumns);
        int i = 0;
        for (i = 0; i < numColumns; i++) {
            rowOfTypeStrategies.add(typeStrategy.clone());
        }
        log.trace("Using {} type strategy instances.", i);
        // this.typeStrategies will cycle endlessly around our row
        this.typeStrategies = cycle(rowOfTypeStrategies);

        // create a "row" of range strategy instances of the same length as the rows in our data
        this.rowOfRangeStrategies = newArrayListWithCapacity(numColumns);
        for (i = 0; i < numColumns; i++) {
            rowOfRangeStrategies.add(rangeStrategy.clone());
        }
        log.trace("Using {} range strategy instances.", i);
        // this.rangeStrategies will cycle endlessly around our row
        this.rangeStrategies = cycle(rowOfRangeStrategies);
    }

    @Override
    protected CSVRecord computeNext() {
        if (internalScanner.hasNext()) {
            final CSVRecord nextRecord = internalScanner.next();
            log.trace("Operating on row: {}", nextRecord);
            for (final String lex : nextRecord) {
                log.trace("Operating on lex: {}", lex);
                rangeStrategies.next().addValue(lex);
                typeStrategies.next().addValue(lex);
            }
            return nextRecord;
        }
        return endOfData();
    }

    /**
     * Scan rows in our CSV up to a limit, exhausting values from the internal parser as we do. <br/>
     * WARNING: Be careful about calling this more than once on a {@link TabularScanner}. The internal parser of a
     * scanner cannot be reset.
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
     * Use this to recover and interrogate the type determining strategy instances used in scanning.
     * 
     * @return the row of strategies used to determine the types of fields
     */
    public List<TypeDeterminingHeuristic<?>> getTypeStrategies() {

        return rowOfTypeStrategies;
    }

    /**
     * Use this to recover and interrogate the range determining strategy instances used in scanning.
     * 
     * @return the row of strategies used to determine the ranges of fields
     */
    public List<RangeDeterminingHeuristic<?>> getRangeStrategies() {
        return rowOfRangeStrategies;
    }

}
