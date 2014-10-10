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
public abstract class RunningMinMaxHeuristic<T extends RunningMinMaxHeuristic<T>> extends PerTypeHeuristic<T> {

    /**
     * A {@link Map} from data types to the minimum value from all presented values that were parseable in that type.
     */
    private Map<DataType, Comparable<?>> minimums = new EnumMap<>(DataType.class);

    /**
     * A {@link Map} from data types to the maximum value from all presented values that were parseable in that type.
     */
    protected Map<DataType, Comparable<?>> maximums = new EnumMap<>(DataType.class);

    @Override
    public void addValue(final String value) {
        super.addValue(value);
        for (final DataType type : parseableAs(value)) {
            final Comparable<?> currentMin = minimums.get(type);
            final Comparable<?> currentMax = maximums.get(type);
            try {
                final Comparable<?> v = type.parse(value);
                // TODO avoid this repeated conditional
                minimums.put(type, (currentMin == null) ? v : natural().min(currentMin, v));
                maximums.put(type, (currentMax == null) ? v : natural().max(currentMax, v));
            } catch (final ParsingException e) {
                // NO OP
                // we don't care if a parse attempt fails because we're garnering measures for those that succeed
            }
        }
    }

    @Override
    public <MinMax extends Comparable<MinMax>> Range<MinMax> getRange() {
        final MinMax min = (MinMax) minimums.get(mostLikelyType());
        final MinMax max = (MinMax) maximums.get(mostLikelyType());
        return Range.closed(min, max);
    }

    @Override
    abstract public T clone();

}
