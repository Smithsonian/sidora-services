
package com.asoroka.sidora.tabularmetadata.heuristics.headers;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class DefaultHeaderHeuristicTest extends HeaderHeuristicTestFrame<DefaultHeaderHeuristic> {

    private static final List<String> goodData = newArrayList("NAME", "RANK", "SERIAL NUMBER");

    private static final List<String> badData = newArrayList("Kirk", "Captain", "00034");

    @Override
    protected DefaultHeaderHeuristic newTestHeuristic() {
        return new DefaultHeaderHeuristic();
    }

    @Test
    public void testDefault() {
        final DefaultHeaderHeuristic testHeuristic = newTestHeuristic();
        for (final String field : goodData) {
            testHeuristic.addValue(field);
        }
        assertTrue(testHeuristic.isHeader());
        for (final String field : badData) {
            testHeuristic.addValue(field);
        }
        assertFalse(testHeuristic.isHeader());
    }

}
