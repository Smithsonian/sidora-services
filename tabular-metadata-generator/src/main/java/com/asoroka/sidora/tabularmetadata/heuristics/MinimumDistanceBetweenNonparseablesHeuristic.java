
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.notParseableAs;
import static java.lang.Float.NEGATIVE_INFINITY;
import static java.util.Objects.hash;

import java.util.EnumMap;
import java.util.Map;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public class MinimumDistanceBetweenNonparseablesHeuristic extends
        RunningMinMaxHeuristic<MinimumDistanceBetweenNonparseablesHeuristic> {

    private final int minimumDistance;

    private Map<DataType, Boolean> candidateTypes = new EnumMap<>(DataType.class);

    /**
     * We define locations as {@link Float}s in order to use the special value {@link Float#NEGATIVE_INFINITY}, which
     * does not exist for integer types in Java.
     */
    private Map<DataType, Float> locationsOfLastNonparseables = new EnumMap<>(DataType.class);

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
        for (final DataType type : notParseableAs(value)) {
            final float distanceToLastNonParseableOfThisType =
                    totalNumValues() - locationsOfLastNonparseables.get(type);
            if (distanceToLastNonParseableOfThisType < minimumDistance) {
                // it's been too soon since the last nonparseable value of this type, knock it out of the running
                candidateTypes.put(type, false);
            } else {
                // mark that we saw a nonparseable value for this type
                locationsOfLastNonparseables.put(type, (float) totalNumValues());
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

    @Override
    public int hashCode() {
        return hash(candidateTypes, totalNumValues(), locationsOfLastNonparseables);
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof MinimumDistanceBetweenNonparseablesHeuristic) {
            final MinimumDistanceBetweenNonparseablesHeuristic oo = (MinimumDistanceBetweenNonparseablesHeuristic) o;
            return this.hashCode() == oo.hashCode();
        }
        return false;
    }

}
