
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.firstMostRestrictiveType;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.filter;

import java.util.Set;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Predicate;

public abstract class PerTypeHeuristic<T extends PerTypeHeuristic<T>> implements DataTypeHeuristic<T> {

    protected static final Set<DataType> DATATYPES = copyOf(DataType.values());

    @Override
    public DataType mostLikelyType() {
        // develop a set of candidate types in a manner specific to the subclass
        final Set<DataType> possibleTypes = filter(DATATYPES, new Predicate<DataType>() {

            @Override
            public boolean apply(final DataType type) {
                return candidacy(type);
            }

        });
        // choose the first candidate type that is no less restrictive than any other
        return firstMostRestrictiveType(possibleTypes);
    }

    /**
     * Subclasses must override this method with an algorithm that uses the gathered statistics (and possibly other
     * information) to make a determination about the most likely type of the proffered values.
     * 
     * @return Whether this type should be considered as a candidate for selection.
     */
    protected abstract boolean candidacy(final DataType type);

    @Override
    public abstract T clone();

}
