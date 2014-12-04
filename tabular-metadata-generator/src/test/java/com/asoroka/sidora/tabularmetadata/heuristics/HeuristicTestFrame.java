
package com.asoroka.sidora.tabularmetadata.heuristics;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public abstract class HeuristicTestFrame<TestHeuristic extends Heuristic<TestHeuristic, ResultType>, ResultType> {

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
