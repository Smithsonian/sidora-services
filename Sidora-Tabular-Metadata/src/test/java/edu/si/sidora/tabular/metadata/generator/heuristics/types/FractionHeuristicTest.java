/*
 * Copyright 2018-2019 Smithsonian Institution.
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

package edu.si.sidora.tabular.metadata.generator.heuristics.types;

import static edu.si.sidora.tabular.metadata.generator.datatype.DataType.NonNegativeInteger;
import static edu.si.sidora.tabular.metadata.generator.datatype.DataType.PositiveInteger;
import static edu.si.sidora.tabular.metadata.generator.datatype.DataType.String;
import static edu.si.sidora.tabular.metadata.generator.testframework.TestUtilities.addValues;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.si.sidora.tabular.metadata.generator.datatype.DataType;
import edu.si.sidora.tabular.metadata.generator.testframework.RowsOfRandomValuesForAllTypes;
import edu.si.sidora.tabular.metadata.generator.testframework.TestUtilities.RandomValuesForAType;

@RunWith(Theories.class)
public class FractionHeuristicTest extends PerTypeHeuristicTestFrame<FractionHeuristic> {

	@Override
	protected FractionHeuristic newTestHeuristic() {
		return new FractionHeuristic(0.2F);
	}

	@Theory
	public void inputsWithNoUnparseableValuesShouldBeRecognizedAsTheirTrueType(
			@RowsOfRandomValuesForAllTypes(numRowsPerType = 5, valuesPerType = 50) final RandomValuesForAType values) {
		final FractionHeuristic testHeuristic = newTestHeuristic();
		addValues(testHeuristic, values);
		final DataType type = values.type;
		if (type.equals(NonNegativeInteger)) {
			// NonNegativeInteger and PositiveInteger differ by only one value (0); it's difficult to tell them apart
			assertTrue(testHeuristic.results().equals(NonNegativeInteger)
					|| testHeuristic.results().equals(PositiveInteger));
		} else {
			assertEquals(type, testHeuristic.results());
		}
	}

	@Theory
	public void testThatInputsWithOnlyOneUnparseableValueShouldBeRecognizedAsTheirTrueType(
			@RowsOfRandomValuesForAllTypes(numRowsPerType = 5, valuesPerType = 50) final RandomValuesForAType values) {
		final FractionHeuristic testHeuristic = newTestHeuristic();
		// A UUID could only be recognized as a String
		values.add(randomUUID());
		addValues(testHeuristic, values);
		final DataType type = values.type;
		if (type.equals(NonNegativeInteger)) {
			// NonNegativeInteger and PositiveInteger differ by only one value (0); it's difficult to tell them apart
			assertTrue(testHeuristic.results().equals(NonNegativeInteger)
					|| testHeuristic.results().equals(PositiveInteger));
		} else {
			assertEquals(type, testHeuristic.results());
		}
	}

	@Theory
	public void testThatInputsWithHalfUnparseableValuesShouldBeNotRecognizedAsTheirTrueType(
			@RowsOfRandomValuesForAllTypes(numRowsPerType = 5, valuesPerType = 50) final RandomValuesForAType values) {
		// nothing cannot be recognized as a String
		assumeThat(values.type, not(is(String)));
		final FractionHeuristic testHeuristic = newTestHeuristic();
		int size = values.size();
		for (byte counter = 0; counter < size; counter++) {
			// A UUID could only be recognized as a String
			values.add(randomUUID());
		}
		addValues(testHeuristic, values);
		assertNotEquals(values.type, testHeuristic.results());
	}
}
