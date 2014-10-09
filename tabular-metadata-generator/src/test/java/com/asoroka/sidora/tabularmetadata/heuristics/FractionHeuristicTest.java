
package com.asoroka.sidora.tabularmetadata.heuristics;

import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public class FractionHeuristicTest extends CountAggregatingHeuristicTestFrame<FractionHeuristic> {

    private static final Logger log = getLogger(FractionHeuristicTest.class);

    @Override
    protected FractionHeuristic newTestInstance() {
        return new FractionHeuristic(0.2);
    }

    @Test
    public void testActionWithGoodValues() {

        log.trace("testActionWithGoodValues()...");
        for (final DataType testType : DataType.values()) {
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
            assertEquals("Failed to admit datatype but should have!", testType, testHeuristic.mostLikelyType());
        }
    }
}
