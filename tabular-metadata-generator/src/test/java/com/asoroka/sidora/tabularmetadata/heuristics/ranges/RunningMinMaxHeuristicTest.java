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


package com.asoroka.sidora.tabularmetadata.heuristics.ranges;

import static com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.addValues;
import static com.google.common.collect.Ordering.natural;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.HeuristicTestFrame;
import com.asoroka.sidora.tabularmetadata.testframework.RowsOfRandomValuesForAllTypes;
import com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.RandomValuesForAType;
import com.google.common.collect.Range;

/**
 * @author A. Soroka
 */
@RunWith(Theories.class)
public class RunningMinMaxHeuristicTest extends HeuristicTestFrame<RunningMinMaxHeuristic, Map<DataType, Range<?>>> {

    @DataPoints
    public static DataType[] datatypes = DataType.values();

    @Override
    protected RunningMinMaxHeuristic newTestHeuristic() {
        return new RunningMinMaxHeuristic();
    }

    /**
     * Asserts that the range of a {@link DataType} for which no values have been seen is {@link Range#all()}.
     */
    @Theory
    public void testMissingLimits(final DataType type) {
        final RunningMinMaxHeuristic testStrategy = newTestHeuristic();
        final Range<?> range = testStrategy.results().get(type);
        assertEquals("Found a defined range where we should not have!", Range.all(), range);
    }

    /**
     * Asserts that for each {@link DataType}, after supplying some comparable values of that type, the range recorded
     * should be the accurate range of the values supplied. This test does not examine interactions between types
     * (i.e. for values that could be parsed as several types).
     */
    @Theory
    public void testMinsAndMaxesShouldAlwaysBeFoundPerType(
            @RowsOfRandomValuesForAllTypes(numRowsPerType = 5, valuesPerType = 5) final RandomValuesForAType values) {

        final RunningMinMaxHeuristic testStrategy = newTestHeuristic();
        final Comparable<?> lowest = natural().min(values);
        final Comparable<?> highest = natural().max(values);
        addValues(testStrategy, values);
        final Range<?> range = testStrategy.results().get(values.type);
        final Comparable<?> calculatedMaxForType = range.upperEndpoint();
        final Comparable<?> calculatedMinForType = range.lowerEndpoint();
        assertEquals("Calculated maximum should be the highest value submitted!", highest,
                calculatedMaxForType);
        assertEquals("Calculated minimum should be the lowest value submitted!", lowest,
                calculatedMinForType);
    }
}
