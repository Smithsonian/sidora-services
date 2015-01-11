
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.String;
import static com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.addValues;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assume.assumeThat;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.testframework.RowsOfRandomValuesForAllTypes;
import com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.RandomValuesForAType;

@RunWith(Theories.class)
public class StrictHeuristicTest extends PerTypeHeuristicTestFrame<StrictHeuristic> {

    private static final Logger log = getLogger(StrictHeuristicTest.class);

    @Override
    protected StrictHeuristic newTestHeuristic() {
        return new StrictHeuristic();
    }

    @Theory
    public void inputsWithOneUnparseableValueShouldNotBeRecognizedAsTheirTrueType(
            @RowsOfRandomValuesForAllTypes(numRowsPerType = 5, valuesPerType = 50) final RandomValuesForAType values) {
        // nothing cannot be recognized as a String
        assumeThat(values.type, not(is(String)));
        // but a UUID could only be recognized as a String
        values.add(randomUUID());
        final StrictHeuristic testHeuristic = newTestHeuristic();
        addValues(testHeuristic, values);
        assertNotEquals(values.type, testHeuristic.results());
    }

    @Theory
    public void inputsWithNoUnparseableValuesShouldBeRecognizedAsTheirTrueType(
            @RowsOfRandomValuesForAllTypes(numRowsPerType = 5, valuesPerType = 50) final RandomValuesForAType values) {
        log.trace("StrictHeuristicTest chacking {}", values.type);
        final StrictHeuristic testHeuristic = newTestHeuristic();
        addValues(testHeuristic, values);
        assertEquals(values.type, testHeuristic.results());
    }
}
