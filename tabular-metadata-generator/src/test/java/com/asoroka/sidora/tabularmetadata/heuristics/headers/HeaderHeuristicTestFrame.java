
package com.asoroka.sidora.tabularmetadata.heuristics.headers;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.heuristics.headers.HeaderHeuristic;

public abstract class HeaderHeuristicTestFrame<T extends HeaderHeuristic<T>> {

    protected abstract T newHeuristic();

    @Test
    public void testGet() {
        final T testHeuristic = newHeuristic();
        assertTrue(testHeuristic == testHeuristic.get());
    }

}
