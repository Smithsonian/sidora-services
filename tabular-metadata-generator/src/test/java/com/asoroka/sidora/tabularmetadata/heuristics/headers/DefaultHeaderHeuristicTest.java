
package com.asoroka.sidora.tabularmetadata.heuristics.headers;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.String;
import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.addValues;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.asoroka.sidora.tabularmetadata.test.RowsOfRandomValuesForAllTypes;
import com.asoroka.sidora.tabularmetadata.test.TestUtilities.RandomValuesForAType;

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
        assertTrue(testHeuristic.isHeader());
    }

    @Theory
    public void testShouldNotAcceptNonStringHeaders(
            @RowsOfRandomValuesForAllTypes(numRowsPerType = 5, valuesPerType = 5) final RandomValuesForAType values) {
        assumeThat(values.type, not(is(String)));
        final DefaultHeaderHeuristic testHeuristic = newTestHeuristic();
        addValues(testHeuristic, values);
        assertFalse(testHeuristic.isHeader());
    }
}
