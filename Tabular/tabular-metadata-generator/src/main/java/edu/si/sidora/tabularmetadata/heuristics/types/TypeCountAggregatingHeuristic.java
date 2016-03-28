/*
 * Copyright 2015-2016 Smithsonian Institution.
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

package edu.si.sidora.tabularmetadata.heuristics.types;

import static edu.si.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;

import edu.si.sidora.tabularmetadata.datatype.DataType;
import edu.si.sidora.tabularmetadata.heuristics.ValueCountingHeuristic;

/**
 * A {@link TypeDeterminingHeuristic} that aggregates candidate type appearance information for its field.
 * 
 * @author A. Soroka
 * @param <SelfType>
 */
public abstract class TypeCountAggregatingHeuristic<SelfType extends TypeCountAggregatingHeuristic<SelfType>>
		extends ValueCountingHeuristic<SelfType, DataType>implements TypeDeterminingHeuristic<SelfType> {

	private static final Logger log = getLogger(TypeCountAggregatingHeuristic.class);

	/**
	 * In this {@link Map}, we aggregate counts of parseable values for each datatype.
	 */
	protected EnumMap<DataType, Integer> typeCounts;

	/**
	 * Initialize counts for each datatype.
	 */
	@Override
	public void reset() {
		super.reset();
		this.typeCounts = new EnumMap<>(DataType.class);
	}

	@Override
	public boolean accept(final String lex) {
		log.trace("Accepting lex: {}", lex);
		if (super.accept(lex)) {
			incrementCounts(parseableAs(lex));
			return true;
		}
		return false;
	}

	private void incrementCounts(final Collection<DataType> types) {
		types.forEach(this::incrementCount);
	}

	private void incrementCount(DataType type) {
		typeCounts.merge(type, 1, (oldCount, inc) -> oldCount + inc);
	}

	@Override
	public int parseableValuesSeen() {
		return typeCounts.get(results());
	}
}
