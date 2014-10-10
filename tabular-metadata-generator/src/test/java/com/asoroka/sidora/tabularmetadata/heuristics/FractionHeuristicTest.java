
package com.asoroka.sidora.tabularmetadata.heuristics;

import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public class FractionHeuristicTest extends CountAggregatingHeuristicTestFrame<FractionHeuristic> {

    private static final Logger log = getLogger(FractionHeuristicTest.class);

    @Override
    protected FractionHeuristic newTestHeuristic() {
        return new FractionHeuristic(0.2F);
    }

    @Test
    public void testActionWithParseableValues() {

        log.trace("testActionWithParseableValues()...");
        for (final DataType testType : parseableValues.keySet()) {
            log.trace("Checking test type: {}", testType);
            final FractionHeuristic testHeuristic = newTestHeuristic();
            for (final String testValue : parseableValues.get(testType)) {
                testHeuristic.addValue(testValue);
            }
            final DataType mostLikelyType = testHeuristic.mostLikelyType();
            assertEquals("Didn't get the correct type for datatype " + testType + "!", testType, mostLikelyType);
        }
    }

    @Test
    public void testActionWithOneNonparseableValue() {
        log.trace("testActionWithOneNonparseableValue()...");
        for (final DataType testType : oneNonparseableValue.keySet()) {
            log.trace("Checking test type: {}", testType);
            final FractionHeuristic testHeuristic = newTestHeuristic();
            for (final String testValue : oneNonparseableValue.get(testType)) {
                log.trace("Adding value: {}", testValue);
                testHeuristic.addValue(testValue);
            }
            final DataType mostLikelyType = testHeuristic.mostLikelyType();
            assertEquals("Failed to admit datatype but should have!", testType, mostLikelyType);
        }
        log.trace("Done with testActionWithOneNonparseableValue().");
    }
}
