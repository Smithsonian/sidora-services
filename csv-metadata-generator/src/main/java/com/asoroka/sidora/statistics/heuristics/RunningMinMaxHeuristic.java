/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.statistics.heuristics;

import static com.asoroka.sidora.datatype.DataType.firstMostRestrictiveType;
import static com.google.common.collect.Ordering.natural;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;

import com.asoroka.sidora.datatype.DataType;
import com.asoroka.sidora.datatype.ParsingException;

/**
 * @author ajs6f
 */
public abstract class RunningMinMaxHeuristic<T extends RunningMinMaxHeuristic<T>> implements
        TypeDeterminationHeuristic<T> {

    protected Map<DataType, Comparable<?>> minimums = new EnumMap<>(DataType.class);

    protected Map<DataType, Comparable<?>> maximums = new EnumMap<>(DataType.class);

    private static final Logger log = getLogger(RunningMinMaxHeuristic.class);

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic#mostLikelyType()
     */
    @Override
    abstract public DataType mostLikelyType();

    protected static DataType mostLikelyType(final String value) {
        return firstMostRestrictiveType(DataType.parseableAs(value));
    }

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic#addValue(java.lang.String)
     */
    @Override
    public void addValue(final String input) {
        for (final DataType type : DataType.parseableAs(input)) {
            final Comparable<?> currentMin = minimums.get(type);
            final Comparable<?> currentMax = maximums.get(type);
            try {
                final Comparable<?> value = type.parse(input);
                minimums.put(type, (currentMin == null) ? value : natural().min(currentMin, value));
                maximums.put(type, (currentMax == null) ? value : natural().max(currentMax, value));
            } catch (final ParsingException e) {
                // NO OP
            }

        }
    }

    @Override
    public <MaxType> MaxType getMaximum() {
        return (MaxType) maximums.get(mostLikelyType());
    }

    @Override
    public <MinType> MinType getMinimum() {
        return (MinType) minimums.get(mostLikelyType());

    }

    @Override
    abstract public T clone();

}
