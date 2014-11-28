
package com.asoroka.sidora.tabularmetadata;

import static com.google.common.collect.Iterators.advance;
import static com.google.common.collect.Iterators.cycle;
import static com.google.common.collect.Iterators.peekingIterator;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.heuristics.enumerations.EnumeratedValuesHeuristic;
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

    private final Iterator<EnumeratedValuesHeuristic<?>> enumStrategies;

    private final List<EnumeratedValuesHeuristic<?>> rowOfEnumStrategies;

    private final Iterator<RangeDeterminingHeuristic<?>> rangeStrategies;

    private final List<RangeDeterminingHeuristic<?>> rowOfRangeStrategies;

    private static final Logger log = getLogger(TabularScanner.class);

    public TabularScanner(final Iterator<CSVRecord> parser, final TypeDeterminingHeuristic<?> typeStrategy,
            final RangeDeterminingHeuristic<?> rangeStrategy, final EnumeratedValuesHeuristic<?> enumStrategy) {
        this.internalScanner = peekingIterator(parser);
        final int numColumns = internalScanner.peek().size();

        log.debug("Found {} columns in our data.", numColumns);
        // create a "row" of type strategy instances of the same length as the rows in our data
        this.rowOfTypeStrategies = new ArrayList<>(numColumns);
        int i;
        for (i = 0; i < numColumns; i++) {
            rowOfTypeStrategies.add(typeStrategy.newInstance());
        }
        // this.typeStrategies will cycle endlessly around our row
        this.typeStrategies = cycle(rowOfTypeStrategies);

        // create a "row" of range strategy instances of the same length as the rows in our data
        this.rowOfRangeStrategies = new ArrayList<>(numColumns);
        for (i = 0; i < numColumns; i++) {
            rowOfRangeStrategies.add(rangeStrategy.newInstance());
        }
        // this.rangeStrategies will cycle endlessly around our row
        this.rangeStrategies = cycle(rowOfRangeStrategies);

        // create a "row" of enum strategy instances of the same length as the rows in our data
        this.rowOfEnumStrategies = new ArrayList<>(numColumns);
        for (i = 0; i < numColumns; i++) {
            rowOfEnumStrategies.add(enumStrategy.newInstance());
        }
        // this.enumStrategies will cycle endlessly around our row
        this.enumStrategies = cycle(rowOfEnumStrategies);
    }

    @Override
    protected CSVRecord computeNext() {
        if (internalScanner.hasNext()) {
            final CSVRecord nextRecord = internalScanner.next();
            log.trace("Operating on row: {}", nextRecord);
            for (final String lex : nextRecord) {
                log.trace("Operating on lex: {}", lex);
                final boolean rangeStrategiesShouldContinue = rangeStrategies.next().addValue(lex);
                final boolean typeStrategiesShouldContinue = typeStrategies.next().addValue(lex);
                final boolean enumStrategiesShouldContinue = enumStrategies.next().addValue(lex);
                log.trace(
                        "Continue flag received from range strategy: {}, from type strategy: {}, from enum strategy: {}.",
                        rangeStrategiesShouldContinue, typeStrategiesShouldContinue, enumStrategiesShouldContinue);
                // if any heuristic is not done, we aren't done
                final boolean shouldContinue =
                        rangeStrategiesShouldContinue || typeStrategiesShouldContinue || enumStrategiesShouldContinue;
                if (!shouldContinue) {
                    log.trace("Short circuiting operation because all heuristics have finished.");
                    return endOfData();
                }
            }
            return nextRecord;
        }
        return endOfData();
    }

    /**
     * Scan rows in our data up to a limit, exhausting values from the internal parser as we do. <br/>
     * WARNING: Do not call this more than once on a {@link TabularScanner}. The internal parser of a scanner cannot
     * be reset.
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

    /**
     * Use this to recover and interrogate the enumerated-values determining strategy instances used in scanning.
     * 
     * @return the row of strategies used to determine the enumerated values presented in fields
     */
    public List<EnumeratedValuesHeuristic<?>> getEnumStrategies() {
        return rowOfEnumStrategies;
    }
}
