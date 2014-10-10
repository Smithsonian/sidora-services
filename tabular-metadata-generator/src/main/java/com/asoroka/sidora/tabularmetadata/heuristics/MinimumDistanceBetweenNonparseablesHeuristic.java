
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static com.google.common.collect.Sets.difference;
import static java.lang.Float.NEGATIVE_INFINITY;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public class MinimumDistanceBetweenNonparseablesHeuristic extends
        RunningMinMaxHeuristic<MinimumDistanceBetweenNonparseablesHeuristic> {

    private final int minimumDistance;

    private Map<DataType, Boolean> candidateTypes = new EnumMap<>(DataType.class);

    private Map<DataType, Float> locationsOfLastNonparseables = new EnumMap<>(DataType.class);

    private int counter = 0;

    public MinimumDistanceBetweenNonparseablesHeuristic(final int minimumDistance) {
        // assume that every type is a candidate
        for (final DataType type : DataType.values()) {
            candidateTypes.put(type, true);
        }
        for (final DataType type : DataType.values()) {
            locationsOfLastNonparseables.put(type, NEGATIVE_INFINITY);
        }
        this.minimumDistance = minimumDistance;
    }

    @Override
    protected boolean candidacy(final DataType type) {
        return candidateTypes.get(type);
    }

    @Override
    public void addValue(final String value) {
        super.addValue(value);
        counter++;
        final Set<DataType> nonparseableTypes = difference(DATATYPES, parseableAs(value));
        for (final DataType type : nonparseableTypes) {
            if (counter - locationsOfLastNonparseables.get(type) < minimumDistance) {
                // it's been too soon since the last nonparseable value of this type, knock it out of the running
                candidateTypes.put(type, false);
            } else {
                // mark that we saw a nonparseable value for this type
                locationsOfLastNonparseables.put(type, (float) counter);
            }
        }
    }

    @Override
    public MinimumDistanceBetweenNonparseablesHeuristic get() {
        return this;
    }

    @Override
    public MinimumDistanceBetweenNonparseablesHeuristic clone() {
        return new MinimumDistanceBetweenNonparseablesHeuristic(minimumDistance);
    }

}
