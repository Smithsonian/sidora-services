
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.firstMostRestrictiveType;
import static com.google.common.collect.Sets.filter;
import static java.util.Objects.hash;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Predicate;

/**
 * A {@link DataTypeHeuristic} that uses some test that maps types to boolean acceptance values. The test is created
 * by overriding {@link #candidacy(DataType)}.
 * 
 * @author ajs6f
 * @param <T>
 */
public abstract class PerTypeHeuristic<T extends PerTypeHeuristic<T>> extends ValueCountingHeuristic<T> {

    private static final Logger log = getLogger(PerTypeHeuristic.class);

    @Override
    public DataType mostLikelyType() {
        // develop a set of candidate types in a manner specific to the subclass
        final Set<DataType> possibleTypes = filter(DataType.valuesSet(), candidacyPredicate);
        log.trace("Found candidate types: {}", possibleTypes);
        // choose the first candidate type that is no less restrictive than any other
        return firstMostRestrictiveType(possibleTypes);
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
    public int hashCode() {
        return super.hashCode() + 2 * hash(totalNumValues());
    }
}
