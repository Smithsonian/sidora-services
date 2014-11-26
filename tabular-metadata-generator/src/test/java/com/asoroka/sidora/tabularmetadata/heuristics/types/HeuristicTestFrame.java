
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.heuristics.Heuristic;

public abstract class HeuristicTestFrame<TestHeuristic extends Heuristic<TestHeuristic>> {

    protected abstract TestHeuristic newTestHeuristic();

    @Test
    public void testClone() {
        final TestHeuristic testHeuristic = newTestHeuristic();
        assertTrue(testHeuristic.getClass().isInstance(testHeuristic.newInstance()));
    }

    @Test
    public void testGet() {
        final TestHeuristic testHeuristic = newTestHeuristic();
        assertTrue(testHeuristic == testHeuristic.get());
    }

}
