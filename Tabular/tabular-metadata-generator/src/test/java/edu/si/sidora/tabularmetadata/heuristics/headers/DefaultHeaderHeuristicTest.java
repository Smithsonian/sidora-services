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


package edu.si.sidora.tabularmetadata.heuristics.headers;

import edu.si.sidora.tabularmetadata.testframework.RowsOfRandomValuesForAllTypes;
import edu.si.sidora.tabularmetadata.testframework.TestUtilities.RandomValuesForAType;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static edu.si.sidora.tabularmetadata.datatype.DataType.String;
import static edu.si.sidora.tabularmetadata.testframework.TestUtilities.addValues;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

@RunWith(Theories.class)
public class DefaultHeaderHeuristicTest extends HeaderHeuristicTestFrame<DefaultHeaderHeuristic> {

    @Override
    protected DefaultHeaderHeuristic newTestHeuristic() {
        return new DefaultHeaderHeuristic();
    }

    @Theory
    public void testShouldAcceptStringOnlyHeaders(
            @RowsOfRandomValuesForAllTypes(numRowsPerType = 5, valuesPerType = 5) final RandomValuesForAType values) {
        assumeThat(values.type, is(String));
        final DefaultHeaderHeuristic testHeuristic = newTestHeuristic();
        addValues(testHeuristic, values);
        assertTrue(testHeuristic.results());
    }

    @Theory
    public void testShouldNotAcceptNonStringHeaders(
            @RowsOfRandomValuesForAllTypes(numRowsPerType = 5, valuesPerType = 5) final RandomValuesForAType values) {
        assumeThat(values.type, not(is(String)));
        final DefaultHeaderHeuristic testHeuristic = newTestHeuristic();
        addValues(testHeuristic, values);
        assertFalse(testHeuristic.results());
    }
}
