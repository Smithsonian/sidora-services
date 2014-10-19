
package com.asoroka.sidora.tabularmetadata.heuristics.headers;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class RegexpHeaderHeuristicTest extends HeaderHeuristicTestFrame<RegexpHeaderHeuristic> {

    private static final String pattern = "MARKER";

    private static final List<String> headerRow1 = asList("MARKER1", "MARKER2", "MARKER3");

    private static final List<String> headerRow2 = asList("jdfhg", "saf", "324");

    @Override
    protected RegexpHeaderHeuristic newTestHeuristic() {
        return new RegexpHeaderHeuristic(pattern);
    }

    @Test
    public void test() {
        final TreatsEachFieldAlikeHeaderHeuristic<RegexpHeaderHeuristic> testHeuristic = newTestHeuristic();
        for (final String field : headerRow1) {
            testHeuristic.addValue(field);
        }
        assertTrue("Failed to pass a row that should have passed!", testHeuristic.isHeader());
        for (final String field : headerRow2) {
            testHeuristic.addValue(field);
        }
        assertFalse("Passed a row that shouldn't have passed!", testHeuristic.isHeader());
    }

}
