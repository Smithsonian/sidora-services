/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.tabularmetadata.heuristics.ranges;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static com.google.common.collect.Maps.asMap;
import static com.google.common.collect.Ordering.natural;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.datatype.ParsingException;
import com.asoroka.sidora.tabularmetadata.heuristics.AbstractHeuristic;
import com.google.common.base.Function;
import com.google.common.collect.Range;

/**
 * Calculates the ranges of values supplied for each possible parseable type, without caching the values supplied.
 * 
 * @author ajs6f
 */
public class RunningMinMaxHeuristic extends AbstractHeuristic<RunningMinMaxHeuristic> implements
        RangeDeterminingHeuristic<RunningMinMaxHeuristic> {

    /**
     * A {@link Map} from data types to the minimum value from all presented values that were parseable in that type.
     */
    protected Map<DataType, Comparable<?>> minimums;

    /**
     * A {@link Map} from data types to the maximum value from all presented values that were parseable in that type.
     */
    protected Map<DataType, Comparable<?>> maximums;

    private static final Logger log = getLogger(RunningMinMaxHeuristic.class);

    /**
     * Initialize minimums and maximums.
     */
    @Override
    public void reset() {
        this.minimums = new EnumMap<>(DataType.class);
        this.maximums = new EnumMap<>(DataType.class);
    }

    @Override
    public boolean addValue(final String value) {
        for (final DataType type : parseableAs(value)) {
            final Comparable<?> currentMin = minimums.get(type);
            final Comparable<?> currentMax = maximums.get(type);
            try {
                final Comparable<?> v = type.parse(value);
                // TODO avoid this repeated conditional
                log.trace("Trying new value {} against current min {} and current max {} for type {}", v, currentMin,
                        currentMax, type);
                minimums.put(type, (currentMin == null) ? v : natural().min(currentMin, v));
                maximums.put(type, (currentMax == null) ? v : natural().max(currentMax, v));
                log.trace("Tried new value {} and got new min {} and new max {} for type {}", v, minimums.get(type),
                        maximums
                                .get(type), type);

            } catch (final ParsingException e) {
                // we are only parsing for types that have already been checked
                throw new AssertionError("Could not parse to a type that was passed as parsing!", e);
            }
        }
        return true;
    }

    @Override
    public Map<DataType, Range<?>> getRanges() {
        return asMap(DataType.valuesSet(), getRangeForType());
    }

    private Function<DataType, Range<?>> getRangeForType() {
        return new Function<DataType, Range<?>>() {

            @Override
            public Range<?> apply(final DataType type) {
                // this test may look odd or loose, but inspection of the algorithm above will convince the examiner
                // that it is not possible for a min to exist without a max, or vice versa. So we can test for the
                // presence of either at whim.
                if (minimums.containsKey(type)) {
                    return Range.closed(minimums.get(type), maximums.get(type));
                }
                return Range.all();
            }
        };
    }

    @Override
    public RunningMinMaxHeuristic clone() {
        return new RunningMinMaxHeuristic();
    }

    @Override
    public RunningMinMaxHeuristic get() {
        return this;
    }
}
