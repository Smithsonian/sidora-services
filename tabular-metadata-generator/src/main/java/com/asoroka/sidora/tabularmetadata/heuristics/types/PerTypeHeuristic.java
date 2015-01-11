
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.sortByHierarchy;
import static com.google.common.collect.Sets.filter;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Predicate;

/**
 * A {@link TypeDeterminingHeuristic} that uses some test that maps types to boolean acceptance values. The test is
 * created by overriding {@link #candidacy(DataType)}. Types are passed or rejected without any particular order other
 * than specificity of type within the hierarchy.
 * 
 * @author A. Soroka
 * @param <SelfType>
 */
public abstract class PerTypeHeuristic<SelfType extends PerTypeHeuristic<SelfType>> extends
        TypeCountAggregatingHeuristic<SelfType> {

    @Override
    public DataType results() {
        return sortByHierarchy(filter(DataType.valuesSet(), candidacyPredicate)).first();
    }

    private Predicate<DataType> candidacyPredicate = new Predicate<DataType>() {

        @Override
        public boolean apply(final DataType type) {
            return candidacy(type);
        }
    };

    /**
     * Subclasses must override this method with an algorithm that uses the gathered statistics (and possibly other
     * information) to make a determination about the most likely type of the proffered values.
     * 
     * @return Whether this type should be considered as a candidate for selection.
     */
    protected abstract boolean candidacy(final DataType type);

}
