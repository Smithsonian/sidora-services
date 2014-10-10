
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.heuristics.HeaderHeuristic.Default;

public class DefaultHeaderHeuristicTest extends HeaderHeuristicTestFrame<HeaderHeuristic.Default> {

    private static final List<String> goodData = newArrayList("NAME", "RANK", "SERIAL NUMBER");

    private static final List<String> badData = newArrayList("Kirk", "Captain", "00034");

    @Override
    protected Default newHeuristic() {
        return new HeaderHeuristic.Default();
    }

    @Test
    public void testDefault() {
        final HeaderHeuristic.Default testHeuristic = newHeuristic();
        assertTrue(testHeuristic.apply(goodData));
        assertFalse(testHeuristic.apply(badData));
    }

}
