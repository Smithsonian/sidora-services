/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.String;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.firstMostRestrictiveType;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static com.google.common.collect.Sets.filter;
import static java.util.Objects.hash;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Predicate;

/**
 * A {@link DataTypeHeuristic} that aggregates candidate type appearance information for its field.
 * 
 * @author ajs6f
 * @param <T>
 */
public abstract class CountAggregatingHeuristic<T extends CountAggregatingHeuristic<T>> extends
        RunningMinMaxHeuristic<CountAggregatingHeuristic<T>> {

    /**
     * In this {@link Map}, we aggregate counts of parseable values for each datatype.
     */
    protected final EnumMap<DataType, Integer> typeCounts = new EnumMap<>(DataType.class);

    private static final Logger log = getLogger(CountAggregatingHeuristic.class);

    /**
     * Initialize counts for each datatype.
     */
    public CountAggregatingHeuristic() {
        for (final DataType type : DataType.values()) {
            typeCounts.put(type, 0);
        }
    }

    @Override
    public DataType mostLikelyType() {
        log.trace("Working with {} total values.", totalNumValues());
        // we filter to only those types which were selected as parseable for all values
        final Set<DataType> possibleTypes = filter(typeCounts.keySet(), new Predicate<DataType>() {

            @Override
            public boolean apply(final DataType type) {
                return candidacy(type);
            }
        });
        log.trace("Found candidate types: {}", possibleTypes);
        return firstMostRestrictiveType(possibleTypes);

    }

    /**
     * We make use here of the fact that {@link DataType#String} is the top type, so anything that can be parsed can
     * be parsed as a String.
     * 
     * @return The total number of values seen so far
     */
    protected int totalNumValues() {
        return typeCounts.get(String);
    }

    /**
     * Subclasses must override this method with an algorithm that uses the gathered statistics (and possibly other
     * information) to make a determination about the most likely type of the proffered values.
     * 
     * @return Whether this type should be considered as a candidate for selection.
     */
    abstract protected boolean candidacy(DataType d);

    @Override
    public void addValue(final String value) {
        super.addValue(value);
        for (final DataType type : parseableAs(value)) {
            typeCounts.put(type, typeCounts.get(type) + 1);
        }
    }

    @Override
    abstract public T clone();

    @Override
    public int hashCode() {
        return hash(typeCounts);
    }

    @Override
    public boolean equals(final Object o) {

        if (!(CountAggregatingHeuristic.class.isInstance(o))) {
            return false;
        }
        final CountAggregatingHeuristic<?> oo = (CountAggregatingHeuristic<?>) o;
        return this.hashCode() == oo.hashCode();
    }
}
