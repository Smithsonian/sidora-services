
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
    protected StrictHeuristic newTestInstance() {
        return new StrictHeuristic();
    }

    @Test
    public void testActionWithGoodValues() {
        log.trace("testActionWithGoodValues()...");
        for (final DataType testType : DataType.values()) {
            log.debug("Testing type: {}", testType);
            testHeuristic = newTestInstance();
            for (final String testValue : parseableValues.get(testType)) {
                testHeuristic.addValue(testValue);
            }
            assertEquals("Didn't get the correct type for datatype " + testType + "!", testType, testHeuristic
                    .mostLikelyType());
        }
    }

    @Test
    public void testActionWithOneBadValue() {
        log.trace("testActionWithOneBadValue()...");
        for (final DataType testType : oneNonparseableValue.keySet()) {
            testHeuristic = newTestInstance();
            for (final String testValue : oneNonparseableValue.get(testType)) {
                testHeuristic.addValue(testValue);
            }
            assertFalse("Got the most commonly occuring type for datatype " + testType + " but shoudn't have!",
                    testHeuristic.mostLikelyType().equals(testType));
        }
    }
}
