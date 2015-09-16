/**
 * Copyright 2015 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */

package edu.si.sidora.tabularmetadata.heuristics.ranges;

import static com.google.common.collect.Maps.asMap;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Range.atLeast;
import static com.google.common.collect.Range.atMost;
import static edu.si.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.Range;

import edu.si.sidora.tabularmetadata.datatype.DataType;
import edu.si.sidora.tabularmetadata.datatype.ParsingException;
import edu.si.sidora.tabularmetadata.heuristics.AbstractHeuristic;

/**
 * Calculates the ranges of values supplied for each possible parseable type, without caching the values supplied.
 * 
 * @author A. Soroka
 */
public class RunningMinMaxHeuristic extends AbstractHeuristic<RunningMinMaxHeuristic, Map<DataType, Range<?>>>
        implements RangeDeterminingHeuristic<RunningMinMaxHeuristic> {

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
                log.trace("Trying new value {} against current min {} and current max {} for type {}", v, currentMin,
                        currentMax, type);
                // TODO avoid this repeated conditional
                minimums.put(type, (currentMin == null) ? v : natural().min(currentMin, v));
                maximums.put(type, (currentMax == null) ? v : natural().max(currentMax, v));
                log.trace("Tried new value {} and got new min {} and new max {} for type {}", v, minimums.get(type),
                        maximums.get(type), type);

            } catch (final ParsingException e) {
                // we are only parsing for types that have already been checked
                throw new AssertionError("Could not parse to a type that was passed as parsing!", e);
            }
        }
        return true;
    }

    @Override
    public Map<DataType, Range<?>> results() {
        return asMap(DataType.valuesSet(), getRangeForType());
    }

    private Function<DataType, Range<?>> getRangeForType() {
        return new Function<DataType, Range<?>>() {

            @Override
            public Range<?> apply(final DataType type) {
                final boolean hasMin = minimums.containsKey(type);
                final boolean hasMax = maximums.containsKey(type);
                final Comparable<?> min = minimums.get(type);
                final Comparable<?> max = maximums.get(type);
                if (hasMin) {
                    if (hasMax) {
                        return Range.closed(min, max);
                    }
                    return atLeast(min);
                }
                if (hasMax) {
                    return atMost(max);
                }
                return Range.all();
            }
        };
    }

    @Override
    public RunningMinMaxHeuristic get() {
        return new RunningMinMaxHeuristic();
    }
}
