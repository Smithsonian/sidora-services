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


package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.HeuristicTestFrame;

public class MinimumDistanceBetweenNonparseablesHeuristicTest extends
        HeuristicTestFrame<MinimumDistanceBetweenNonparseablesHeuristic, DataType> {

    private static final int MINIMUM_DISTANCE = 2;

    final List<String> passingData = asList("1", "FOO", "3", "BAR", "5");

    final List<String> nonPassingData = asList("1", "FOO", "BAR", "4", "5");

    @Override
    protected MinimumDistanceBetweenNonparseablesHeuristic newTestHeuristic() {
        return new MinimumDistanceBetweenNonparseablesHeuristic(MINIMUM_DISTANCE);
    }

    @Test
    public void testPassingAndNonpassingData() {
        final MinimumDistanceBetweenNonparseablesHeuristic testStrategy = newTestHeuristic();
        for (final String i : passingData) {
            testStrategy.addValue(i);
        }
        assertEquals(
                "Failed to accept type with nonparseable values less than or equal to the minimum distance apart!",
                PositiveInteger, testStrategy.results());
        for (final String i : nonPassingData) {
            testStrategy.addValue(i);
        }
        assertEquals("Failed to reject types with nonparseable values greater than the minimum distance apart!",
                DataType.String, testStrategy.results());
    }
}
