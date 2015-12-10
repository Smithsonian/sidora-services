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

package edu.si.sidora.tabularmetadata.testframework;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static java.lang.Math.abs;
import static java.lang.Math.random;
import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.joda.time.DateTime.now;

import java.net.URI;
import java.util.ArrayList;

import edu.si.sidora.tabularmetadata.datatype.DataType;
import edu.si.sidora.tabularmetadata.datatype.GeographicValue;
import edu.si.sidora.tabularmetadata.heuristics.Heuristic;

/**
 * Utilities for testing.
 * 
 * @author A. Soroka
 */
public abstract class TestUtilities {

	public static void addValues(final Heuristic<?, ?> strategy, final Iterable<?> values) {
		values.forEach(v -> strategy.accept(v.toString()));
	}

	/**
	 * @param type a {@link DataType}
	 * @return a random value appropriate to that DataType
	 */
	static Comparable<?> generateRandomValue(final DataType type) {
		switch (type) {
		case Boolean:
			return random() > 0.5;
		case DateTime:
			return now().plus(round(random() * 1000000000000F));
		case Decimal:
			return (float) (random() - 0.5) * 10;
		case Geographic:
			if ((Boolean) generateRandomValue(DataType.Boolean)) { return new GeographicValue(asList(
					(Float) generateRandomValue(DataType.Decimal), (Float) generateRandomValue(DataType.Decimal))); }
			return new GeographicValue(asList((Float) generateRandomValue(DataType.Decimal),
					(Float) generateRandomValue(DataType.Decimal), (Float) generateRandomValue(DataType.Decimal)));
		case Integer:
			if (random() < 0.1) {
				if ((Boolean) generateRandomValue(DataType.Boolean)) { return MAX_VALUE; }
				return MIN_VALUE;
			}
			return round((Float) generateRandomValue(DataType.Decimal));
		case NonNegativeInteger:
			if (random() < 0.2) { return 0; }
			final Integer randInt = (Integer) generateRandomValue(DataType.Integer);
			if (randInt.equals(MAX_VALUE) || randInt.equals(MIN_VALUE)) { return 0; }
			return abs(randInt);
		case PositiveInteger:
			return (Integer) generateRandomValue(DataType.NonNegativeInteger) + 1;
		case String:
			return randomUUID().toString();
		case URI:
			return URI.create("info:" + generateRandomValue(DataType.String));
		default:
			throw new AssertionError("A DataType of an un-enumerated kind should never exist!");
		}
	}

	static RandomValuesForAType randomValues(final DataType type, final short numValues) {
		final RandomValuesForAType values = new RandomValuesForAType(numValues).withType(type);
		for (short valueIndex = 0; valueIndex < numValues; valueIndex++) {
			values.add(generateRandomValue(type));
		}
		return values;
	}

	public static class RandomValuesForAType extends ArrayList<Comparable<?>> {

		private static final long serialVersionUID = 1L;

		public DataType type;

		public RandomValuesForAType(final short initCapacity) {
			super(initCapacity);
		}

		public RandomValuesForAType withType(final DataType t) {
			this.type = t;
			return this;
		}
	}
}
