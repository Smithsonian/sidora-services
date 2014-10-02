
package com.asoroka.sidora.statistics.heuristics;

import javax.inject.Inject;

import com.asoroka.sidora.datatype.DataType;

/**
 * A heuristic that permits a certain specified fraction of values to fail at parsing.
 * 
 * @author ajs6f
 */
public class FractionHeuristic extends CountAggregatingHeuristic<FractionHeuristic> {

    /**
     * @param fractionOfAllowedNonparseables The fraction of nonparsing values to admit
     */
    @Inject
    public FractionHeuristic(final @FractionOfNonparsingValues Float fractionOfAllowedNonparseables) {
        super();
        this.fractionOfAllowedNonparseables = fractionOfAllowedNonparseables;
    }

    private final Float fractionOfAllowedNonparseables;

    @Override
    protected boolean candidacy(final DataType type) {
        final float parseableOccurences = typeCounts.get(type) / totalNumValues();
        return parseableOccurences > (1 - fractionOfAllowedNonparseables);
    }

    @Override
    public FractionHeuristic clone() {
        return new FractionHeuristic(fractionOfAllowedNonparseables);
    }

}
