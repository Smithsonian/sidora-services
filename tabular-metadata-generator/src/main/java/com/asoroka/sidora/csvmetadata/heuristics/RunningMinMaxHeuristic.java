/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.csvmetadata.heuristics;

import static com.asoroka.sidora.csvmetadata.datatype.DataType.parseableAs;
import static com.google.common.collect.Ordering.natural;

import java.util.EnumMap;
import java.util.Map;

import com.asoroka.sidora.csvmetadata.datatype.DataType;
import com.asoroka.sidora.csvmetadata.datatype.ParsingException;
import com.google.common.collect.Range;

/**
 * Calculates the ranges of values supplied for each possible parseable type.
 * 
 * @author ajs6f
 * @param <T>
 */
public abstract class RunningMinMaxHeuristic<T extends RunningMinMaxHeuristic<T>> implements
        DataTypeHeuristic<T> {

    protected Map<DataType, Comparable<?>> minimums = new EnumMap<>(DataType.class);

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
                // we don't care if a particular parsing fails, we are only gathering aggregate statistics
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
