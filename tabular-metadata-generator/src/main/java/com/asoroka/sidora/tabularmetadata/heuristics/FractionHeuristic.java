
package com.asoroka.sidora.tabularmetadata.heuristics;

import static java.util.Objects.hash;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

/**
 * A heuristic that permits a certain specified fraction of values to fail at parsing to the most likely type.
 * 
 * @author ajs6f
 */
public class FractionHeuristic extends CountAggregatingHeuristic<FractionHeuristic> {

    private static final Logger log = getLogger(FractionHeuristic.class);

    /**
     * @param d The fraction of nonparsing values to permit
     */
    public FractionHeuristic(final float d) {
        super();
        this.fractionOfAllowedNonparseables = d;
    }

    private final float fractionOfAllowedNonparseables;

    @Override
    protected boolean candidacy(final DataType type) {
        final float nonParseableOccurrences = totalNumValues() - typeCounts.get(type);
        log.trace("Found {} nonparseable occurrences out of {} total values for type {}.", nonParseableOccurrences,
                totalNumValues(), type);
        final float nonParseableFraction = nonParseableOccurrences / totalNumValues();
        log.trace("For a nonparseable fraction of {}.", nonParseableFraction);
        return nonParseableFraction <= fractionOfAllowedNonparseables;
    }

    @Override
    public FractionHeuristic clone() {
        return new FractionHeuristic(fractionOfAllowedNonparseables);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 2 * hash(fractionOfAllowedNonparseables);
    }
}
