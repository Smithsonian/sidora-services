
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static java.lang.Float.isNaN;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

/**
 * A heuristic that permits a certain specified fraction of values to fail at parsing to the most likely type.
 * 
 * @author A. Soroka
 */
public class FractionHeuristic extends PerTypeHeuristic<FractionHeuristic> {

    private final float fractionOfAllowedNonparseables;

    private static final Logger log = getLogger(FractionHeuristic.class);

    /**
     * @param nonParsingFraction The fraction of nonparsing values to permit
     */
    public FractionHeuristic(final float nonParsingFraction) {
        super();
        this.fractionOfAllowedNonparseables = nonParsingFraction;
    }

    @Override
    protected boolean candidacy(final DataType type) {
        final float nonParseableOccurrences = valuesSeen() - typeCounts.get(type);
        log.trace("Found {} nonparseable occurrences out of {} total values for type {}.", nonParseableOccurrences,
                valuesSeen(), type);

        final float nonParseableFraction = nonParseableOccurrences / valuesSeen();
        if (isNaN(nonParseableFraction)) {
            // there were no lexes accepted
            return true;
        }
        log.trace("For a nonparseable fraction of {}.", nonParseableFraction);
        return nonParseableFraction <= fractionOfAllowedNonparseables;
    }

    @Override
    public FractionHeuristic get() {
        return new FractionHeuristic(fractionOfAllowedNonparseables);
    }
}
