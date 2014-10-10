
package com.asoroka.sidora.tabularmetadata.heuristics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public class StrictHeuristicTest extends CountAggregatingHeuristicTestFrame<StrictHeuristic> {

    private static final Logger log = getLogger(StrictHeuristicTest.class);

    @Override
    protected StrictHeuristic newTestHeuristic() {
        return new StrictHeuristic();
    }

    @Test
    public void testActionWithParsingValues() {
        log.trace("testActionWithParsingValues()...");
        for (final DataType testType : DataType.values()) {
            log.debug("Testing type: {}", testType);
            final StrictHeuristic testHeuristic = newTestHeuristic();
            for (final String testValue : parseableValues.get(testType)) {
                testHeuristic.addValue(testValue);
            }
            assertEquals("Didn't get the correct type for datatype " + testType + "!", testType, testHeuristic
                    .mostLikelyType());
        }
    }

    @Test
    public void testActionWithOneNonparsingValue() {
        log.trace("testActionWithOneNonparsingValue()...");
        for (final DataType testType : oneNonparseableValue.keySet()) {
            final StrictHeuristic testHeuristic = newTestHeuristic();
            for (final String testValue : oneNonparseableValue.get(testType)) {
                testHeuristic.addValue(testValue);
            }
            assertFalse("Got the most commonly occuring type for datatype " + testType + " but shoudn't have!",
                    testHeuristic.mostLikelyType().equals(testType));
        }
    }
}
