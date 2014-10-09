/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static com.google.common.collect.Ordering.natural;

import java.util.EnumMap;
import java.util.Map;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.datatype.ParsingException;
import com.google.common.collect.Range;

/**
 * Calculates the ranges of values supplied for each possible parseable type, without caching the values supplied.
 * 
 * @author ajs6f
 * @param <T>
 */
public abstract class RunningMinMaxHeuristic<T extends RunningMinMaxHeuristic<T>> implements
        DataTypeHeuristic<T> {

    /**
     * A {@link Map} from data types to the minimum value from all presented values that were parseable in that type.
     */
    protected Map<DataType, Comparable<?>> minimums = new EnumMap<>(DataType.class);

    /**
     * A {@link Map} from data types to the maximum value from all presented values that were parseable in that type.
     */
    protected Map<DataType, Comparable<?>> maximums = new EnumMap<>(DataType.class);

    @Override
    public void addValue(final String input) {
        for (final DataType type : parseableAs(input)) {
            final Comparable<?> currentMin = minimums.get(type);
            final Comparable<?> currentMax = maximums.get(type);
            try {
                final Comparable<?> value = type.parse(input);
                // TODO avoid this repeated conditional
                minimums.put(type, (currentMin == null) ? value : natural().min(currentMin, value));
                maximums.put(type, (currentMax == null) ? value : natural().max(currentMax, value));
            } catch (final ParsingException e) {
                // NO OP
                // we don't care if a parse attempt fails because we're garnering measures for those that succeed
            }
        }
    }

    @Override
    public Range<?> getRange() {
        return Range.closed(minimums.get(mostLikelyType()), maximums.get(mostLikelyType()));
    }

    @Override
    abstract public T clone();

    @Override
    public DataTypeHeuristic<T> get() {
        return this;
    }

}
