
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class HeaderHeuristicTest {

    private static final List<String> goodData = newArrayList("NAME", "RANK", "SERIAL NUMBER");

    private static final List<String> badData = newArrayList("Kirk", "Captain", "00034");

    private static final HeaderHeuristic<?> testHeuristic = new HeaderHeuristic.Default();

    @Test
    public void testDefault() {
        assertTrue(testHeuristic.apply(goodData));
        assertFalse(testHeuristic.apply(badData));
    }
}
