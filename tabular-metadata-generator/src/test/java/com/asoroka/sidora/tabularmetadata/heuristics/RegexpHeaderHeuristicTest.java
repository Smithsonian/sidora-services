
package com.asoroka.sidora.tabularmetadata.heuristics;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class RegexpHeaderHeuristicTest {

    private static final String pattern = "MARKER";

    private static final List<String> headerRow1 = asList("MARKER1", "MARKER2", "MARKER3");

    private static final List<String> headerRow2 = asList("jdfhg", "saf", "324");

    @Test
    public void test() {
        final RegexpHeaderHeuristic testHeaderStrategy = new RegexpHeaderHeuristic(pattern);
        assertTrue("Failed to pass a row that should have passed!", testHeaderStrategy.apply(headerRow1));
        assertFalse("Passed a row that shouldn't have passed!", testHeaderStrategy.apply(headerRow2));
    }

}
