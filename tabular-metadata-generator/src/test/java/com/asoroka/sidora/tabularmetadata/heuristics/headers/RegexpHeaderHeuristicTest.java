
package com.asoroka.sidora.tabularmetadata.heuristics.headers;

import static com.asoroka.sidora.tabularmetadata.test.TestUtilities.addValues;
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
        addValues(testHeuristic, headerRow1);
        assertTrue("Failed to pass a row that should have passed!", testHeuristic.isHeader());
        addValues(testHeuristic, headerRow2);
        assertFalse("Passed a row that shouldn't have passed!", testHeuristic.isHeader());
    }
}
