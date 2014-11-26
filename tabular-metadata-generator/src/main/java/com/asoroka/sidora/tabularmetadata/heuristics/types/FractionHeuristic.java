
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static java.lang.Float.isNaN;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

/**
 * A heuristic that permits a certain specified fraction of values to fail at parsing to the most likely type.
 * 
 * @author ajs6f
 */
public class FractionHeuristic extends PerTypeHeuristic<FractionHeuristic> {

    private final float fractionOfAllowedNonparseables;

    private static final Logger log = getLogger(FractionHeuristic.class);

    /**
     * @param d The fraction of nonparsing values to permit
     */
    public FractionHeuristic(final float d) {
        super();
        this.fractionOfAllowedNonparseables = d;
    }

    @Override
    protected boolean candidacy(final DataType type) {
        final float nonParseableOccurrences = totalNumValues() - typeCounts.get(type);
        log.trace("Found {} nonparseable occurrences out of {} total values for type {}.", nonParseableOccurrences,
                totalNumValues(), type);

        final float nonParseableFraction = nonParseableOccurrences / totalNumValues();
        if (isNaN(nonParseableFraction)) {
            // there were no lexes accepted
            return true;
        }
        log.trace("For a nonparseable fraction of {}.", nonParseableFraction);
        return nonParseableFraction <= fractionOfAllowedNonparseables;
    }

    @Override
    public FractionHeuristic newInstance() {
        return new FractionHeuristic(fractionOfAllowedNonparseables);
    }

    @Override
    public FractionHeuristic get() {
        return this;
    }
}
