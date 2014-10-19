
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.notParseableAs;
import static com.google.common.base.Functions.constant;
import static com.google.common.collect.Maps.toMap;
import static java.lang.Float.NEGATIVE_INFINITY;
import static java.util.Objects.hash;

import java.util.EnumMap;
import java.util.Map;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public class MinimumDistanceBetweenNonparseablesHeuristic extends
        PerTypeHeuristic<MinimumDistanceBetweenNonparseablesHeuristic> {

    private final int minimumDistance;

    private Map<DataType, Boolean> candidateTypes = new EnumMap<>(DataType.class);

    /**
     * We define locations as {@link Float}s in order to use the special value {@link Float#NEGATIVE_INFINITY}, which
     * does not exist for integer types in Java.
     */
    private Map<DataType, Float> locationsOfLastNonparseables = new EnumMap<>(DataType.class);

    public MinimumDistanceBetweenNonparseablesHeuristic(final int minimumDistance) {
        // assume that every type is a candidate
        final Map<DataType, Boolean> allTrue = toMap(DataType.valuesSet(), constant(true));
        candidateTypes.putAll(allTrue);

        // record that we haven't yet seen any nonparseables
        final Map<DataType, Float> originalLocations = toMap(DataType.valuesSet(), constant(NEGATIVE_INFINITY));
        locationsOfLastNonparseables.putAll(originalLocations);

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
    public MinimumDistanceBetweenNonparseablesHeuristic clone() {
        return new MinimumDistanceBetweenNonparseablesHeuristic(minimumDistance);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 2 * hash(candidateTypes, totalNumValues(), locationsOfLastNonparseables);
    }

    @Override
    public MinimumDistanceBetweenNonparseablesHeuristic get() {
        return this;
    }

}
