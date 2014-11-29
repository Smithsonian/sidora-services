
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.heuristics.ValueCountingHeuristic;

public abstract class ValueCountingHeuristicTestFrame<TestHeuristic extends ValueCountingHeuristic<TestHeuristic>>
        extends HeuristicTestFrame<TestHeuristic> {

    @Test
    public void testCountingValues() {
        final TestHeuristic testHeuristic = newTestHeuristic();
        for (byte i = 1; i <= 100; i++) {
            testHeuristic.addValue("VALUE");
        }
        assertEquals(100, testHeuristic.totalNumValues());
    }
}
