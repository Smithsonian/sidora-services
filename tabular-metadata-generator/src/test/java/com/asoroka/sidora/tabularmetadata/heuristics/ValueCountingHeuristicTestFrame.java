
package com.asoroka.sidora.tabularmetadata.heuristics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public abstract class ValueCountingHeuristicTestFrame<T extends ValueCountingHeuristic<T>> extends
        DataTypeHeuristicTestFrame<T> {

    @Test
    public void testCountingValues() {
        final T testHeuristic = newTestHeuristic();
        for (byte i = 1; i <= 100; i++) {
            testHeuristic.addValue(String.valueOf(i));
        }
        assertEquals(100, testHeuristic.totalNumValues());
    }

}
