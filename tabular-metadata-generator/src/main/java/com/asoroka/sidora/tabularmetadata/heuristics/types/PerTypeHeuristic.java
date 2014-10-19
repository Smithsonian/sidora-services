
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.sortByHierarchy;
import static com.google.common.collect.Sets.filter;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;
import java.util.SortedSet;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Predicate;

/**
 * A {@link TypeDeterminingHeuristic} that uses some test that maps types to boolean acceptance values. The test is
 * created by overriding {@link #candidacy(DataType)}. Types are passed or rejected without any particular order other
 * than specificity of type within the hierarchy.
 * 
 * @author ajs6f
 * @param <SelfType>
 */
public abstract class PerTypeHeuristic<SelfType extends PerTypeHeuristic<SelfType> & TypeDeterminingHeuristic<SelfType>>
        extends CountAggregatingHeuristic<SelfType>
        implements TypeDeterminingHeuristic<SelfType> {

    private static final Logger log = getLogger(PerTypeHeuristic.class);

    @Override
    public SortedSet<DataType> typesAsLikely() {
        // develop a set of candidate types in a manner specific to the subclass
        final Set<DataType> possibleTypes = filter(DataType.valuesSet(), candidacyPredicate);
        // order by hierarchy
        final SortedSet<DataType> sortedCandidates = sortByHierarchy(possibleTypes);
        log.trace("Found candidate types: {}", sortedCandidates);

        return sortedCandidates;
    }

    @Override
    public DataType mostLikelyType() {
        return typesAsLikely().first();
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

    @Override
    public abstract SelfType clone();
}
