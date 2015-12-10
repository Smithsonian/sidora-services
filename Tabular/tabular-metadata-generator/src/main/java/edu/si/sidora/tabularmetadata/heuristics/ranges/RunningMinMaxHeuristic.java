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
import static com.google.common.collect.Range.all;
import static com.google.common.collect.Range.atLeast;
import static com.google.common.collect.Range.atMost;
import static com.google.common.collect.Range.closed;
import static edu.si.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;

import com.google.common.collect.Range;

import edu.si.sidora.tabularmetadata.datatype.DataType;
import edu.si.sidora.tabularmetadata.heuristics.AbstractHeuristic;

/**
 * Calculates the ranges of values supplied for each possible parseable type, without caching the values supplied.
 * 
 * @author A. Soroka
 */
public class RunningMinMaxHeuristic extends AbstractHeuristic<RunningMinMaxHeuristic, Map<DataType, Range<?>>>
		implements RangeDeterminingHeuristic<RunningMinMaxHeuristic> {

	/**
	 * {@link Map}s from data types to the extremal values from all presented values that were parseable in that type.
	 */
	protected Map<DataType, Comparable<?>> mins, maxs;

	private static final Logger log = getLogger(RunningMinMaxHeuristic.class);

	/**
	 * Initialize mins and maxs.
	 */
	@Override
	public void reset() {
		this.mins = new EnumMap<>(DataType.class);
		this.maxs = new EnumMap<>(DataType.class);
	}

	@Override
	public boolean accept(final String value) {
		parseableAs(value).forEach(type -> {
			final Comparable<?> v = type.parse(value);
			mins.merge(type, v, natural()::min);
			maxs.merge(type, v, natural()::max);
			log.trace("Got new min {} and new max {} for type {}", v, mins.get(type), maxs.get(type), type);
		});
		return true;
	}

	@Override
	public Map<DataType, Range<?>> results() {
		return asMap(DataType.datatypes(), type -> {
			final boolean hasMax = maxs.containsKey(type);
			final Comparable<?> min = mins.get(type);
			final Comparable<?> max = maxs.get(type);
			return mins.containsKey(type) ? hasMax ? closed(min, max) : atLeast(min) : hasMax ? atMost(max) : all();
		});
	}

	@Override
	public RunningMinMaxHeuristic get() {
		return new RunningMinMaxHeuristic();
	}
}
